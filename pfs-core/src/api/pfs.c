//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
/*
 * pfs.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#define I_AM_API_PFS
#include "pfs.h"
#include "../core/pfs-intern.h"
#include "../core/pfs-mount.h"
#include "../include/pfs.h"
#include "../include/pfs-stream.h"

static int pfs_eh_equal(const void *a, const void *b) {
	const struct element_handle *ha = a, *hb = b;
	return ha->handle.element_place.block == hb->handle.element_place.block
			&& ha->handle.element_place.pos == hb->handle.element_place.pos
			&& ha->handle.fs_data == hb->handle.fs_data;
}
static uint64_t pfs_eq_hash(const void *a) {
	return eh_hash(a);
}

static inline void release_eh(struct element_handle *feh) {
	feh->load_count--;
	if (!has_refs(feh)) {
		if (!feh->handle.is_mount_point) {
			hashset_remove(&pfs_all_ehs_set, eh_hash(feh), feh);
			free(feh);
		}
	}
}

static i64 pfs_element__path0_rec_impl(struct pfs_element_handle *eh,
		char **buffer, i64 *buf_size, i64 cur_size, int cont_on_mounts) {
	if (cont_on_mounts && eh->is_mount_point) {
		eh = &eh->fs_data->mount_point->handle;
	}
	if (eh->direct_parent_place.block == -1L) {
		if (*buf_size <= cur_size) {
			*buf_size = cur_size;
			*buffer = realloc(*buffer, cur_size);
		}
		return 0L;
	}
	i64 name_len = pfsc_element_get_name_length(eh) + 1;
	if (name_len < 0) {
		return -1L;
	}
	cur_size += name_len;
	if (((ui64) name_len + (ui64) cur_size) & 0x8000000000000000UL) {
		pfs_err = PFS_ERRNO_OUT_OF_RANGE;
		return -1L;
	}
	struct pfs_element_handle peh = *eh;
	if (!pfsc_element_get_parent(&peh)) {
		return -1L;
	}
	i64 off = pfs_element__path0_rec_impl(&peh, buffer, buf_size, cur_size,
			cont_on_mounts);
	if (off < 0L) {
		return -1L;
	}
	char *b = (*buffer) + off;
	b[0] = '/';
	b++;
	if (!pfsc_element_get_name(eh, &b, buf_size)) {
		return -1L;
	}
	return off + name_len;
}
static inline int pfs_element__path0_impl(int eh, char **buffer, i64 *buf_size,
		int cont_on_mounts) {
	eh(0)
	i64 off = pfs_element__path0_rec_impl(&pfs_ehs[eh]->handle, buffer,
			buf_size, 1L, cont_on_mounts);
	if (off < 0L) {
		return 1;
	} else if (off == 0L) { //only root has trailing slash
		(*buffer)[0] = '/';
		off++;
	}
	(*buffer)[off] = '\0';
	return 0;
}

extern int pfs_element_fs_path0(int eh, char **buffer, i64 *buf_size) {
	return pfs_element__path0_impl(eh, buffer, buf_size, 0);
}
extern int pfs_element_path0(int eh, char **buffer, i64 *buf_size) {
	return pfs_element__path0_impl(eh, buffer, buf_size, 1);
}
extern char* pfs_element_fs_path(int eh) {
	char *res = NULL;
	size_t s = 0;
	if (!pfs_element_fs_path0(eh, &res, &s)) {
		if (res) {
			free(res);
		}
		return NULL;
	}
	return res;
}
extern char* pfs_element_path(int eh) {
	char *res = NULL;
	size_t s = 0;
	if (!pfs_element_path0(eh, &res, &s)) {
		if (res) {
			free(res);
		}
		return NULL;
	}
	return res;
}

static inline struct element_handle* open_eh(const char *path,
		struct element_handle *rot, int allow_relative,
		struct element_handle **parent_on_err) {
	if (*path == '\0') {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return NULL;
	}
	i64 buf_len = 1;
	char *buf = malloc(buf_len);
	if (buf == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	struct element_handle *eh =
			(allow_relative && (*path != '/')) ? pfs_cwd : rot;
	eh->load_count++;
	for (const char *cur = path, *end;; cur = end) {
		while (*cur == '/') {
			cur++;
		}
		if (*cur == '\0') {
			break;
		}
		end = strchrnul(cur, '/');
		i64 len = ((i64) end) - ((i64) cur);
		if (len >= buf_len) {
			buf = realloc(buf, len + 1);
			buf_len = len + 1;
		}
		memcpy(buf, cur, len);
		buf[len] = '\0';
		switch (len) {
		case 0:
			continue;
		case 1:
			if ('.' != *cur) {
				break;
			}
			continue;
		case 2:
			if (('.' != cur[1]) || ('.' != cur[0])) {
				break;
			}
			struct element_handle *new = malloc(sizeof(struct element_handle));
			new->handle = eh->handle;
			release_eh(eh);
			if (!pfsc_element_get_parent(&new->handle)) {
				return 0;
			}
			struct element_handle *al = hashset_get(&pfs_all_ehs_set,
					eh_hash(new), new);
			if (al != NULL) {
				eh = al;
				eh->load_count++;
				free(new);
			} else {
				eh = new;
				eh->load_count = 1;
				if (hashset_put(&pfs_all_ehs_set, eh_hash(new), new) != NULL) {
					abort();
				}
			}
			continue;
		}
		struct element_handle *neh = malloc(sizeof(struct element_handle));
		if (!neh) {
			release_eh(eh);
			pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
			return 0;
		}
		neh->handle = eh->handle;
		if (!*end && parent_on_err) {
			if (!pfsc_folder_child_from_name(&neh->handle, buf)) {
				*parent_on_err = eh;
				free(buf);
				free(neh);
				return 0;
			}
		} else if (!pfsc_folder_child_from_name(&neh->handle, buf)) {
			release_eh(eh);
			free(neh);
			free(buf);
			return 0;
		}
		struct element_handle *oeh = eh;
		eh = hashset_get(&pfs_all_ehs_set, eh_hash(neh), neh);
		if (eh) {
			eh->load_count++;
			free(neh);
		} else {
			if (neh->handle.is_mount_point) {
				neh = realloc(neh, sizeof(struct element_handle_mount));
				struct element_handle_mount *mneh =
						(struct element_handle_mount*) neh;
				if (!pfsc_mount_open(&(mneh)->handle,
						mneh->handle.handle.fs_data->read_only)) {
					neh->handle.is_mount_point = 0;
					release_eh(neh);
					return NULL;
				} // assume that the folder functions are used more often than the mount functions
				neh = mneh->handle.fs.root;
			}
			hashset_put(&pfs_all_ehs_set, eh_hash(neh), neh);
			neh->load_count = 1;
			eh = neh;
		}
		release_eh(oeh);
	}
	free(buf);
	eh->load_count++;
	return eh;
}

extern int pfs_load(struct bm_block_manager *bm, const char *cur_work_dir,
		int read_only) {
	if (bm == NULL) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	struct pfs_b0 *b0 = bm->get(bm, 0L);
	pfs_validate_b0(b0,
			pfs_err = PFS_ERRNO_ILLEGAL_DATA; bm->unget(bm, 0L); return 0;, 1)
	struct element_handle **nehs = malloc(sizeof(struct element_handle*));
	struct stream_handle **nshs = malloc(sizeof(struct stream_handle*));
	struct iter_handle **nihs = malloc(sizeof(struct iter_handle*));
	struct element_handle *nrot = malloc(sizeof(struct element_handle));
	struct pfs_file_sys_data *fs_data = malloc(
			sizeof(struct pfs_file_sys_data));
	if (!nehs || !nshs || !nihs || !nrot || !fs_data) {
		if (nehs) {
			free(nehs);
		}
		if (nshs) {
			free(nshs);
		}
		if (nihs) {
			free(nihs);
		}
		if (nrot) {
			free(nrot);
		}
		if (fs_data) {
			free(fs_data);
		}
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		bm->unget(bm, 0L);
		return 0;
	}
	if (!pfsc_fill_root(bm, nrot, fs_data, read_only)) {
		free(nehs);
		free(nshs);
		free(nihs);
		free(nrot);
		bm->unget(bm, 0L);
		return 0;
	}
	nrot->load_count = 1;
	if (cur_work_dir) {
		struct element_handle *ncwd = open_eh(cur_work_dir, nrot, 0, NULL);
		if (!ncwd) {
			free(nehs);
			free(nshs);
			free(nihs);
			free(nrot);
			bm->unget(bm, 0L);
			return 0;
		}
		pfs_close();
		pfs_cwd = ncwd;
	} else {
		nrot->load_count++;
		pfs_cwd = nrot;
	}
	bm->unget(bm, 0L);
	pfs_root = nrot;
	pfs_ehs = nehs;
	pfs_shs = nshs;
	pfs_ihs = nihs;
	pfs_eh_len = 1;
	pfs_sh_len = 1;
	pfs_ih_len = 1;
	pfs_shs[0] = NULL;
	pfs_ehs[0] = NULL;
	pfs_ihs[0] = NULL;
	return 1;
}

extern int pfs_format(i64 block_count, uuid_t uuid, char *name) {
	return pfs_load_and_format(pfs_root->handle.fs_data->file_sys, block_count,
			uuid, name);
}
extern int pfs_load_and_format(struct bm_block_manager *bm, i64 block_count,
		uuid_t uuid, char *name) {
	if (bm == NULL) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	struct element_handle **nehs = malloc(sizeof(struct element_handle*));
	struct stream_handle **nshs = malloc(sizeof(struct stream_handle*));
	struct iter_handle **nihs = malloc(sizeof(struct iter_handle*));
	struct element_handle *nrot = malloc(sizeof(struct element_handle));
	struct pfs_file_sys_data *fs_data = malloc(
			sizeof(struct pfs_file_sys_data));
	if (!nehs || !nshs || !nihs || !nrot || !fs_data) {
		if (nehs) {
			free(nehs);
		}
		if (nshs) {
			free(nshs);
		}
		if (nihs) {
			free(nihs);
		}
		if (nrot) {
			free(nrot);
		}
		if (fs_data) {
			free(fs_data);
		}
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return 0;
	}
	if (!pfsc_format(bm, block_count, uuid, name)) {
		free(nehs);
		free(nshs);
		free(nihs);
		free(nrot);
		free(fs_data);
		return 0;
	}
	if (!pfsc_fill_root(bm, nrot, fs_data, 0)) {
		free(nehs);
		free(nshs);
		free(nihs);
		free(nrot);
		free(fs_data);
		return 0;
	}
	nrot->load_count = 2;
	pfs_close();
	pfs_ehs = nehs;
	pfs_shs = nshs;
	pfs_ihs = nihs;
	pfs_eh_len = 1;
	pfs_sh_len = 1;
	pfs_ih_len = 1;
	pfs_root = nrot;
	pfs_cwd = nrot;
	pfs_shs[0] = NULL;
	pfs_ehs[0] = NULL;
	pfs_ihs[0] = NULL;
	return 1;
}

static int pfs_close_for_each(void *arg0, void *element) {
	struct element_handle *eh = element;
	if (eh->handle.is_mount_point) {
		int val = pfsc_mount_close(element);
		if (!val) {
			*(int*) arg0 = 0;
		}
	}
	free(eh);
	return 1;
}
extern int pfs_close() {
	int result = 1;
	if (pfs_root) {
		result = pfs(pfs_root)->close_bm(pfs(pfs_root));
	}
	hashset_for_each(&pfs_all_ehs_set, pfs_close_for_each, &result);
	hashset_clear(&pfs_all_ehs_set);
	pfs_root = NULL;
	pfs_cwd = NULL;
	if (pfs_ehs) {
		free(pfs_ehs);
		pfs_ehs = NULL;
	}
	if (pfs_shs) {
		free(pfs_shs);
		pfs_shs = NULL;
	}
	if (pfs_ihs) {
		free(pfs_ihs);
		pfs_ihs = NULL;
	}
	return result;
}

// block_count and block_size are implemented in core/pfs.c

static inline int handle0(struct element_handle *eh, ui32 flag) {
	if (!eh) {
		return -1;
	}
	if (flag) {
		if (eh->handle.real_parent_place.block == -1) {
			// the root folder has no flags
			if ((flag & (PFS_F_FOLDER | PFS_F_MOUNT)) == 0) {
				pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
				return -1;
			}
		} else {
			ui32 flags = pfsc_element_get_flags(&eh->handle);
			if (flags == (ui32) -1) {
				return -1;
			}
			if ((flag & flags) == 0) {
				pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
				return -1;
			}
		}
	}
	return_handle(pfs_eh_len, pfs_ehs, eh);
}

static inline int handle(const char *path, ui32 flag) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	return handle0(eh, flag);
}

static inline int handle2(struct element_handle *relative, const char *path,
		ui32 flag) {
	struct element_handle *eh = open_eh(path, relative, 0, NULL);
	return handle0(eh, flag);
}

extern int pfs_handle(const char *path) {
	return handle(path, 0);
}

extern int pfs_handle_folder(const char *path) {
	return handle(path, PFS_F_FOLDER | PFS_F_MOUNT);
}

extern int pfs_handle_mount(const char *path) {
	return handle(path, PFS_F_MOUNT);
}

extern int pfs_handle_file(const char *path) {
	return handle(path, PFS_F_FILE);
}

extern int pfs_handle_pipe(const char *path) {
	return handle(path, PFS_F_PIPE);
}

extern int pfs_folder_descendant(int eh, const char *path) {
	eh(0)
	return handle2(pfs_ehs[eh], path, 0);
}

extern int pfs_folder_descendant_folder(int eh, const char *path) {
	eh(0)
	return handle2(pfs_ehs[eh], path, PFS_F_FOLDER | PFS_F_MOUNT);
}

extern int pfs_folder_descendant_mount(int eh, const char *path) {
	eh(0)
	return handle2(pfs_ehs[eh], path, PFS_F_MOUNT);
}

extern int pfs_folder_descendant_file(int eh, const char *path) {
	eh(0)
	return handle2(pfs_ehs[eh], path, PFS_F_FILE);
}

extern int pfs_folder_descendant_pipe(int eh, const char *path) {
	eh(0)
	return handle2(pfs_ehs[eh], path, PFS_F_PIPE);
}

extern int pfs_change_dir(int eh) {
	eh(0)
	ui32 flags = pfsc_element_get_flags(&pfs_ehs[eh]->handle);
	if (flags == -1) {
		return 0;
	}
	if ((flags & PFS_F_FOLDER) != 0) {
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return 0;
	}
	release_eh(pfs_cwd);
	pfs_cwd = pfs_ehs[eh];
	pfs_cwd->load_count++;
	return 1;
}

extern int pfs_change_dir_path(char *path) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	if (!eh) {
		return -1;
	}
	ui32 flags = pfsc_element_get_flags(&eh->handle);
	if (flags == -1) {
		return -1;
	}
	if ((flags & PFS_F_FOLDER) != 0) {
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return -1;
	}
	release_eh(pfs_cwd);
	pfs_cwd = eh;
	return 1;
}

static inline void pfs_close_iter_impl(int ih) {
	release_eh(pfs_ihs[ih]->folder);
	pfs_ihs[ih] = NULL;
}

static int delete_element(struct element_handle **eh, int also_when_loaded) {
	if (!also_when_loaded && has_refs0(*eh, 1)) { // 1 and 2 because this handle itself does not count
		if (*eh != pfs_cwd || has_refs0(*eh, 2)) {
			release_eh(*eh);
			*eh = NULL;
			pfs_err = PFS_ERRNO_ELEMENT_USED;
			return 0;
		}
	}
	i64 former_index;
	struct element_handle peh;
	int res = pfsc_element_delete(&(*eh)->handle, &former_index, &peh.handle);
	struct element_handle *npeh = hashset_get(&pfs_all_ehs_set, eh_hash(&peh), &peh);
	if (npeh) {
		pfs_modify_iterators(npeh, former_index);
	}
	if (res && has_refs0(*eh, 1)) {
		if (!also_when_loaded) {
			abort();
		}
		memset(&(*eh)->handle, 0xFF, sizeof(struct pfs_element_handle));
	}
	release_eh(*eh);
	*eh = NULL;
	return res;
}

extern int pfs_delete(const char *path, int also_when_loaded) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	check_write_access0(eh, 0)
	return delete_element(&eh, also_when_loaded);
}

extern int pfs_element_delete(int eh, int also_when_loaded) {
	eh(0)
	check_write_access2(pfs_ehs[eh]->handle, release_eh(pfs_ehs[eh]); return 0;)
	return delete_element(&pfs_ehs[eh], also_when_loaded);
}

static inline int open_sh(struct element_handle *eh, ui32 stream_flags) {
	if (stream_flags & (PFS_SO_WRITE | PFS_SO_APPEND)) {
		check_write_access0(eh, -1)
	}
	struct stream_handle *sh = malloc(sizeof(struct stream_handle));
	if (!sh) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(eh);
		return -1;
	}
	sh->element = eh;
	sh->pos = 0;
	sh->flags = stream_flags;
	void *block_data = pfs(eh)->get(pfs(eh),
			eh->handle.direct_parent_place.block);
	if (!block_data) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		free(sh);
		release_eh(eh);
		return -1;
	}
	struct pfs_folder_entry *e = block_data + eh->handle.entry_pos;
	sh->is_file = (e->flags & PFS_F_FILE) != 0;
	if (!sh->is_file) {
		check_write_access0(pfs_root, -1)
		sh->pos = -1;
		if ((e->flags & PFS_F_PIPE) == 0) {
			pfs(eh)->unget(pfs(eh), eh->handle.direct_parent_place.block);
			pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			free(sh);
			release_eh(eh);
			return -1;
		}
	}
	if (eh->handle.direct_parent_place.block
			!= eh->handle.element_place.block) {
		pfs(eh)->unget(pfs(eh), eh->handle.direct_parent_place.block);
	}
	if (sh->is_file) {
		if (stream_flags & PFS_SO_PIPE) {
			if (eh->handle.direct_parent_place.block
					== eh->handle.element_place.block) {
				pfs(eh)->unget(pfs(eh),
						eh->handle.direct_parent_place.block);
			}
			pfs_err = PFS_ERRNO_ILLEGAL_ARG;
			free(sh);
			release_eh(eh);
			return -1;
		}
	} else if (stream_flags & PFS_SO_FILE) {
		if (eh->handle.direct_parent_place.block
				== eh->handle.element_place.block) {
			pfs(eh)->unget(pfs(eh), eh->handle.direct_parent_place.block);
		}
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		free(sh);
		release_eh(eh);
		return -1;
	}
	if (eh->handle.direct_parent_place.block
			!= eh->handle.element_place.block) {
		block_data = pfs(eh)->get(pfs(eh), eh->handle.element_place.block);
	}
	if (!block_data) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		free(sh);
		release_eh(eh);
		return -1;
	}
	struct pfs_file *f = block_data + eh->handle.element_place.pos;
	if (stream_flags & PFS_SO_FILE_TRUNC) {
		if (!sh->is_file) {
			pfs(eh)->unget(pfs(eh), eh->handle.element_place.block);
			pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			free(sh);
			release_eh(eh);
			return -1;
		}
		check_write_access0(pfs_root, -1)
		if (!pfsc_file_truncate(&eh->handle, 0)) {
			pfs(eh)->unget(pfs(eh), eh->handle.element_place.block);
			free(sh);
			release_eh(eh);
			return -1;
		}
	}
	if (stream_flags & PFS_SO_FILE_EOF) {
		if (sh->is_file) {
			sh->pos = f->file.file_length;
		}
	}
	pfs(eh)->unget(pfs(eh), eh->handle.element_place.block);
	return_handle(pfs_sh_len, pfs_shs, sh);
}

struct fd_del_str {
	struct delegate_stream val;
	bm_fd fd;
};

static i64 fd_write(struct delegate_stream *ds, void *data, i64 len) {
	struct fd_del_str *fdds = (struct fd_del_str*) ds;
	i64 wrote = bm_fd_write(fdds->fd, data, len);
	if (wrote == -1) {
		pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
	return wrote;
}
static i64 fd_read(struct delegate_stream *ds, void *buf, i64 len) {
	struct fd_del_str *fdds = (struct fd_del_str*) ds;
	i64 reat = bm_fd_read(fdds->fd, buf, len);
	if (reat == -1) {
#ifdef PFS_PORTABLE_BUILD
		if (feof(fdds->fd)) {
			clearerr(fdds->fd);
		} else {
			switch (errno) {
			case EIO:
				pfs_err = PFS_ERRNO_IO_ERR;
				break;
			default:
				pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
				break;
			}
			errno = 0;
		}
		return 0;
#else
		switch (errno) {
		case EIO:
			pfs_err = PFS_ERRNO_IO_ERR;
			break;
		default:
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		errno = 0;
		return 0;
#endif
	}
	return reat;
}
static i64 fd_get_pos(struct delegate_stream *ds) {
	struct fd_del_str *fdds = (struct fd_del_str*) ds;
	i64 sek = bm_fd_pos(fdds->fd);
	if (sek == -1) {
		switch (errno) {
		case EIO:
			pfs_err = PFS_ERRNO_IO_ERR;
			break;
		default:
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		errno = 0;
	}
	return sek;
}
static int fd_set_pos(struct delegate_stream *ds, i64 pos) {
	struct fd_del_str *fdds = (struct fd_del_str*) ds;
	i64 sek = bm_fd_seek(fdds->fd, pos);
	if (sek == -1) {
		switch (errno) {
		case EIO:
			pfs_err = PFS_ERRNO_IO_ERR;
			break;
		default:
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		errno = 0;
		return 0;
	}
	return 1;
}
static i64 fd_add_pos(struct delegate_stream *ds, i64 add) {
	struct fd_del_str *fdds = (struct fd_del_str*) ds;
	i64 sek = bm_fd_pos(fdds->fd); // add a signed value
	if (sek == -1) {
		switch (errno) {
		case EIO:
			pfs_err = PFS_ERRNO_IO_ERR;
			break;
		default:
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		errno = 0;
	}
	sek = bm_fd_seek(fdds->fd, sek + add);
	if (sek == -1) {
		switch (errno) {
		case EIO:
			pfs_err = PFS_ERRNO_IO_ERR;
			break;
		default:
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		errno = 0;
		return 0;
	}
	return 1;
}
static i64 fd_seek_eof(struct delegate_stream *ds) {
	struct fd_del_str *fdds = (struct fd_del_str*) ds;
	i64 sek = bm_fd_seek_eof(fdds->fd);
	if (sek == -1) {
		switch (errno) {
		case EIO:
			pfs_err = PFS_ERRNO_IO_ERR;
			break;
		default:
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		errno = 0;
	}
	return sek;
}
static int fd_close(struct delegate_stream *ds) {
	struct fd_del_str *fdds = (struct fd_del_str*) ds;
	int res = bm_fd_close(fdds->fd);
	free(fdds);
	return res;
}
extern int pfs_stream_open_delegate_fd(bm_fd fd, i32 stream_flags) {
	if (stream_flags
			& (PFS_SO_ONLY_CREATE | PFS_SO_ALSO_CREATE | PFS_SO_FILE_TRUNC)) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return -1;
	}
	for (int i = 0; i < pfs_sh_len; i++) {
		if (!pfs_shs[i]) {
			continue;
		}
		if (pfs_shs[i]->element || !pfs_shs[i]->is_file) {
			continue;
		}
		if (((struct fd_del_str*) pfs_shs[i]->delegate)->fd == fd) {
			pfs_shs[i]->pos++;
			return_handle(pfs_sh_len, pfs_shs, pfs_shs[i]);
		}
	}
	struct stream_handle *res = malloc(
			sizeof(struct stream_handle) + sizeof(struct fd_del_str));
	struct delegate_stream *del = (struct delegate_stream*) (res + 1);
	res->element = NULL;
	res->delegate = del;
	res->is_file = 1;
	res->delegate_ref_count = 1;
	if (stream_flags & PFS_SO_FILE) {
		del->get_pos = fd_get_pos;
		del->set_pos = fd_set_pos;
		del->add_pos = fd_add_pos;
		del->seek_eof = fd_seek_eof;
	} else {
		del->get_pos = NULL;
		del->set_pos = NULL;
		del->add_pos = NULL;
		del->seek_eof = NULL;
	}
	if (stream_flags & PFS_SO_READ) {
		del->read = fd_read;
	} else {
		del->read = NULL;
	}
	if (stream_flags & (PFS_SO_WRITE | PFS_SO_APPEND)) {
		del->write = fd_write;
	} else {
		del->write = NULL;
	}
	del->close = fd_close;
	return_handle(pfs_sh_len, pfs_shs, res);
}

extern int pfs_stream_open_delegate(struct delegate_stream *stream) {
	for (int i = 0; i < pfs_sh_len; i++) {
		if (!pfs_shs[i]) {
			continue;
		}
		if (pfs_shs[i]->element || pfs_shs[i]->is_file) {
			continue;
		}
		if (pfs_shs[i]->delegate == stream) {
			pfs_shs[i]->pos++;
			return_handle(pfs_sh_len, pfs_shs, pfs_shs[i]);
		}
	}
	struct stream_handle *res = malloc(sizeof(struct stream_handle));
	res->element = NULL;
	res->delegate = stream;
	res->is_file = 1;
	res->delegate_ref_count = 1;
	return_handle(pfs_sh_len, pfs_shs, res);
}

extern int pfs_open_stream(int eh, i32 stream_flags) {
	eh(-1)
	if (stream_flags & PFS_SO_ONLY_CREATE) {
		pfs_err = PFS_ERRNO_ELEMENT_ALREADY_EXIST;
		return -1;
	}
	int res = open_sh(pfs_ehs[eh], stream_flags);
	if (res != -1) {
		pfs_ehs[eh]->load_count++;
	}
	return res;
}

extern int pfs_stream(const char *path, i32 stream_flags) {
	const ui32 old_err = pfs_err;
	struct element_handle *peh = NULL;
	struct element_handle *eh = open_eh(path, pfs_root, 1, &peh);
	if (!eh) {
		if (!peh
				|| (stream_flags & (PFS_SO_ONLY_CREATE | PFS_SO_ALSO_CREATE))
						== 0) {
			return -1;
		}
		check_write_access0(pfs_root, -1)
		pfs_err = old_err;
		eh = malloc(sizeof(struct element_handle));
		if (!eh) {
			pfs_err = PFS_ERRNO_ILLEGAL_ARG;
			return -1;
		}
		eh->handle = peh->handle;
		const char *name = strrchr(path, '/');
		name = name == NULL ? path : (name + 1);
		if (stream_flags & PFS_SO_PIPE) {
			if (!pfsc_folder_create_pipe(&eh->handle, &peh->handle, name)) {
				return -1;
			}
		} else if (stream_flags & PFS_SO_FILE) {
			if (!pfsc_folder_create_file(&eh->handle, &peh->handle, name)) {
				return -1;
			}
		} else {
			pfs_err = PFS_ERRNO_ILLEGAL_ARG;
			return -1;
		}
		struct element_handle *old = hashset_add(&pfs_all_ehs_set, eh_hash(eh),
				eh);
		if (old) {
			free(eh);
			eh = old;
			eh->load_count++;
		} else {
			eh->load_count = 1;
		}
		release_eh(peh);
	} else if (stream_flags & PFS_SO_ONLY_CREATE) {
		release_eh(eh);
		return -1;
	}
	return open_sh(eh, stream_flags);
}

extern int pfs_folder_open_iter(int eh, int show_hidden) {
	eh(-1)
	c_h(pfs_ehs[eh], -1, PFS_F_FOLDER)
	pfs_ehs[eh]->load_count++;
	struct iter_handle *ih = malloc(sizeof(struct iter_handle));
	if (!ih) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(pfs_ehs[eh]);
		pfs(pfs_ehs[eh])->unget(pfs(pfs_ehs[eh]),
				pfs_ehs[eh]->handle.direct_parent_place.block);
		return -1;
	}
	ih->ieh = pfs_ehs[eh]->handle;
	if (!pfsc_folder_fill_iterator(&ih->ieh, &ih->handle, show_hidden)) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(pfs_ehs[eh]);
		pfs(pfs_ehs[eh])->unget(pfs(pfs_ehs[eh]),
				pfs_ehs[eh]->handle.direct_parent_place.block);
		return -1;
	}
	if ((pfs_ehs[eh]->handle.direct_parent_place.block != -1)
			&& (!pfs(pfs_ehs[eh])->unget(pfs(pfs_ehs[eh]),
					pfs_ehs[eh]->handle.direct_parent_place.block))) {
		release_eh(pfs_ehs[eh]);
		return -1;
	}
	ih->folder = pfs_ehs[eh];
	return_handle(pfs_ih_len, pfs_ihs, ih);
}

extern int pfs_iter(const char *path, int show_hidden) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	if (eh->handle.direct_parent_place.block != -1L) {
		if (!pfs(eh)->get(pfs(eh), eh->handle.direct_parent_place.block)) {
			return -1;
		}
		ui32 flags = pfsc_element_get_flags(&eh->handle);
		if (flags == -1) {
			release_eh(eh);
			pfs(eh)->unget(pfs(eh), eh->handle.direct_parent_place.block);
			return -1;
		}
		if ((flags & PFS_F_FOLDER) == 0) {
			pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			release_eh(eh);
			pfs(eh)->unget(pfs(eh), eh->handle.direct_parent_place.block);
			return -1;
		}
	}
	struct iter_handle *ih = malloc(sizeof(struct iter_handle));
	if (!ih) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(eh);
		pfs(eh)->unget(pfs(eh), eh->handle.direct_parent_place.block);
		return -1;
	}
	ih->ieh = eh->handle;
	if (!pfsc_folder_fill_iterator(&ih->ieh, &ih->handle, show_hidden)) {
		release_eh(eh);
		pfs(eh)->unget(pfs(eh), eh->handle.direct_parent_place.block);
		return -1;
	}
	if (eh->handle.direct_parent_place.block != -1L) {
		if (!pfs(eh)->unget(pfs(eh), eh->handle.direct_parent_place.block)) {
			release_eh(eh);
			return -1;
		}
	}
	ih->folder = eh;
	return_handle(pfs_ih_len, pfs_ihs, ih);
}

void (pfs_modify_iterators)(struct element_handle const *eh, ui64 former_index,
		int removed) {
	for (int i = pfs_ih_len; --i >= 0; ) {
		if (pfs_ihs[i] && ((pfs_ihs[i]->folder == eh) || !eh)) {
			memset(&pfs_ihs[i]->handle, 0xFF, sizeof(struct pfs_folder_iter));
			if (removed) {
				if (pfs_ihs[i]->index >= former_index) {
					pfs_ihs[i]->index--;
				}
			} else {
				if (pfs_ihs[i]->index < former_index) {
					pfs_ihs[i]->index++;
				}
			}
		}
	}
}

extern int pfs_iter_next(int ih) {
	ih(-1)
	if (pfs_ihs[ih]->handle.current_place.block == -1L) {
		memcpy(&pfs_ihs[ih]->ieh, &pfs_ihs[ih]->folder->handle,
				sizeof(struct pfs_element_handle));
		if (!pfsc_folder_fill_iterator(&pfs_ihs[ih]->ieh, &pfs_ihs[ih]->handle,
				pfs_ihs[ih]->handle.show_hidden)) {
			pfs_ihs[ih]->handle.current_depth = -1;
			return -1;
		}
		for (i64 i = pfs_ihs[ih]->index; i > 0; --i) {
			if (!pfsc_folder_iter_next(&pfs_ihs[ih]->handle)) {
				pfs_ihs[ih]->handle.current_depth = -1;
				return -1;
			}
		}
	}
	if (!pfsc_folder_iter_next(&pfs_ihs[ih]->handle)) {
		return -1;
	}
	struct element_handle *c = hashset_get(&pfs_all_ehs_set,
			eh_hash(&pfs_ihs[ih]->ieh),
			((void*) &pfs_ihs[ih]->ieh)
					- offsetof(struct element_handle, handle));
	if (c) {
		c->load_count++;
	} else {
		c = malloc(sizeof(struct element_handle));
		if (!c) {
			pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
			return -1;
		}
		c->handle = pfs_ihs[ih]->ieh;
		c->load_count = 1;
		void *old = hashset_add(&pfs_all_ehs_set, eh_hash(c), c);
		if (old) {
			free(c);
			c = old;
			c->load_count++;
		}
	}
	return_handle(pfs_eh_len, pfs_ehs, c)
}

extern int pfs_element_close(int eh) {
	get_handle(0, pfs_eh_len, pfs_ehs, eh)
	// eh(0) fails when the element was deleted
	release_eh(pfs_ehs[eh]);
	pfs_ehs[eh] = NULL;
	return 1;
}

extern int pfs_iter_close(int ih) {
	ih(0)
	pfs_close_iter_impl(ih);
	return 1;
}

extern int pfs_stream_close(int sh) {
	sh(0)
	if (pfs_shs[sh]->element) {
		release_eh(pfs_shs[sh]->element);
		pfs_shs[sh] = NULL;
	} else if (--pfs_shs[sh]->delegate_ref_count <= 0) {
		if (pfs_shs[sh]->pos < 0) {
			abort();
		}
		struct delegate_stream *del = pfs_shs[sh]->delegate;
		pfs_shs[sh] = NULL;
		if (del->close) {
			return pfs_shs[sh]->delegate->close(pfs_shs[sh]->delegate);
		}
	}
	return 1;
}

// never used locally, only here for export
extern void pfs_free(void *pntr) {
	free(pntr);
}
extern void* pfs_malloc(i64 size) {
	return malloc(size);
}
extern void* pfs_realloc(void *oldpntr, i64 size) {
	return realloc(oldpntr, size);
}
