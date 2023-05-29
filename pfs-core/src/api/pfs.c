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
#include "../include/pfs.h"
#include "../include/pfs-stream.h"

int childset_equal(const void *a, const void *b) {
	const struct element_handle *ha = a, *hb = b;
	return ha->handle.element_place.block == hb->handle.element_place.block
			&& ha->handle.element_place.pos == hb->handle.element_place.pos;
}

uint64_t childset_hash(const void *a) {
	return eh_hash(a);
}

static inline void force_release_eh(struct element_handle *feh) {
	for (struct element_handle *a = feh, *b;
			a != NULL && (feh == a || !has_refs(a)); a = b) {
		b = a->parent;
		if (!b) { // root has no references
			abort();
		}
		hashset_remove(&b->children, eh_hash(a), a);
		free(a);
	}
}

static inline void release_eh(struct element_handle *feh) {
	feh->load_count--;
	if (!has_refs(feh)) {
		force_release_eh(feh);
	}
}

static inline void free_old() {
	if (pfs_ehs) {
		free(pfs_ehs);
		free(pfs_shs);
		free(pfs_ihs);
		release_eh(pfs_root);
		release_eh(pfs_cwd);
	}
}

static inline struct element_handle* open_eh(const char *path,
		struct element_handle *rot, int allow_relative,
		struct element_handle **parent_on_err) {
	if (*path == '\0') {
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		return NULL;
	}
	i64 buf_len = 1;
	char *buf = malloc(buf_len);
	if (buf == NULL) {
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
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
			if (!eh->parent) {
				(*pfs_err_loc) = PFS_ERRNO_ROOT_FOLDER;
				free(buf);
				release_eh(eh);
				return 0;
			}
			struct element_handle *oeh = eh;
			eh = oeh->parent;
			eh->load_count++;
			release_eh(oeh);
			continue;
		}
		struct element_handle *neh = malloc(sizeof(struct element_handle));
		if (!neh) {
			release_eh(eh);
			(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
			return 0;
		}
		neh->handle = eh->handle;
		if (!*end && parent_on_err) {
			if (!pfsc_folder_child_from_name(&neh->handle, buf)) {
				*parent_on_err = eh;
				free(buf);
				return 0;
			}
		} else if (!pfsc_folder_child_from_name(&neh->handle, buf)) {
			release_eh(eh);
			free(buf);
			return 0;
		}
		struct element_handle *oeh = eh;
		struct element_handle *oneh = hashset_get(&eh->children, eh_hash(neh),
				neh);
		if (oneh) {
			eh = oneh;
			eh->load_count++;
		} else {
			hashset_put(&eh->children, eh_hash(neh), neh);
			neh->parent = eh;
			neh->load_count = 1;
			neh->children.entrycount = 0;
			neh->children.maxi = 0;
			neh->children.equalizer = childset_equal;
			neh->children.hashmaker = childset_hash;
			neh->children.entries = NULL;
			eh = neh;
		}
		release_eh(oeh);
	}
	free(buf);
	eh->load_count++;
	return eh;
}

extern int pfs_load(struct bm_block_manager *bm, const char *cur_work_dir) {
	if (bm == NULL) {
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	struct pfs_b0 *b0 = bm->get(bm, 0L);
	pfs_validate_b0(*b0,
			(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_SUPER_BLOCK; bm->unget(bm, 0L); return 0;)
	struct element_handle **nehs = malloc(sizeof(struct element_handle*));
	struct stream_handle **nshs = malloc(sizeof(struct stream_handle*));
	struct iter_handle **nihs = malloc(sizeof(struct iter_handle*));
	struct element_handle *nrot = malloc(sizeof(struct element_handle));
	if (!nrot) {
		if (nehs) {
			free(nehs);
		}
		if (nshs) {
			free(nshs);
		}
		if (nihs) {
			free(nihs);
		}
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		bm->unget(bm, 0L);
		return 0;
	}
	pfs = bm;
	if (!pfsc_fill_root(&nrot->handle)) {
		free(nehs);
		free(nshs);
		free(nihs);
		free(nrot);
		bm->unget(bm, 0L);
		return 0;
	}
	nrot->parent = NULL;
	nrot->load_count = 1;
	nrot->children.maxi = 0;
	nrot->children.entrycount = 0;
	nrot->children.equalizer = childset_equal;
	nrot->children.hashmaker = childset_hash;
	nrot->children.entries = NULL;
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
		free_old();
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

extern int pfs_load_and_format(struct bm_block_manager *bm, i64 block_count) {
	if (bm == NULL) {
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	struct bm_block_manager *old = pfs;
	pfs = bm;
	int res = pfs_format(block_count);
	if (!res) {
		pfs = old;
	}
	return res;
}

extern int pfs_format(i64 block_count) {
	struct element_handle **nehs = malloc(sizeof(struct element_handle*));
	struct stream_handle **nshs = malloc(sizeof(struct stream_handle*));
	struct iter_handle **nihs = malloc(sizeof(struct iter_handle*));
	struct element_handle *nrot = malloc(sizeof(struct element_handle));
	if (!nehs || !nshs || !nihs || !nrot) {
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
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		return 0;
	}
	if (!pfsc_format(block_count)) {
		free(nehs);
		free(nshs);
		free(nihs);
		free(nrot);
		return 0;
	}
	if (!pfsc_fill_root(&nrot->handle)) {
		free(nehs);
		free(nshs);
		free(nihs);
		free(nrot);
		return 0;
	}
	nrot->parent = NULL;
	nrot->load_count = 2;
	nrot->children.maxi = 0;
	nrot->children.entrycount = 0;
	nrot->children.equalizer = childset_equal;
	nrot->children.hashmaker = childset_hash;
	nrot->children.entries = NULL;
	free_old();
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

extern int pfs_close() {
	if (pfs) {
		if (!pfs->close_bm(pfs)) {
			return 0;
		}
	}
	if (pfs_ehs) {
		free_old();
		pfs_ehs = NULL;
		pfs_shs = NULL;
		pfs_ihs = NULL;
		pfs_root = NULL;
		pfs_cwd = NULL;
	}
	return 1;
}

// block_count and block_size are implemented in core/pfs.c

extern int pfs_handle(const char *path) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	if (!eh) {
		return -1;
	}
	for (int i = 0; i < pfs_eh_len; i++) {
		if (pfs_ehs[i]) {
			continue;
		}
		pfs_ehs[i] = eh;
		return i;
	}
	void *nehs = realloc(pfs_ehs,
			(pfs_eh_len + 1) * sizeof(struct element_handle*));
	if (!nehs) {
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		return -1;
	}
	pfs_ehs = nehs;
	pfs_ehs[pfs_eh_len] = eh;
	return pfs_eh_len++;
}

static inline int handle(const char *path, ui32 flag) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	if (!eh) {
		return -1;
	}
	if (eh->handle.real_parent_place.block == -1) {
		// the root folder has no flags
		if ((flag & PFS_F_FOLDER) != flag) {
			(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			return -1;
		}
	} else {
		ui32 flags = pfsc_element_get_flags(&eh->handle);
		if (flags == (ui32) -1) {
			return -1;
		}
		if ((flag & flags) != flag) {
			(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			return -1;
		}
	}
	return_handle(pfs_eh_len, pfs_ehs, eh);
}

extern int pfs_handle_folder(const char *path) {
	return handle(path, PFS_F_FOLDER);
}

extern int pfs_handle_file(const char *path) {
	return handle(path, PFS_F_FILE);
}

extern int pfs_handle_pipe(const char *path) {
	return handle(path, PFS_F_PIPE);
}

extern int pfs_change_dir(int eh) {
	eh(0)
	ui32 flags = pfsc_element_get_flags(&pfs_ehs[eh]->handle);
	if (flags == -1) {
		return 0;
	}
	if ((flags & PFS_F_FOLDER) != 0) {
		(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return 0;
	}
	release_eh(pfs_cwd);
	pfs_cwd = pfs_ehs[eh];
	pfs_cwd->load_count++;
	return 1;
}

extern int pfs_change_working_directoy(char *path) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	if (!eh) {
		return -1;
	}
	ui32 flags = pfsc_element_get_flags(&eh->handle);
	if (flags == -1) {
		return -1;
	}
	if ((flags & PFS_F_FOLDER) != 0) {
		(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
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

static int delete_element(struct element_handle **eh) {
	if (has_refs0(*eh, 1)) { // 1 and 2 because this handle itself does not count
		if (*eh == pfs_cwd && has_refs0(*eh, 2)) {
			pfs_cwd = pfs_root;
		} else {
			release_eh(*eh);
			*eh = NULL;
			(*pfs_err_loc) = PFS_ERRNO_ELEMENT_USED;
			return 0;
		}
	}
	i64 former_index;
	int res = pfsc_element_delete(&(*eh)->handle, &former_index);
	if (res) {
		for (int i = pfs_ih_len; i--;) {
			if (!pfs_ihs[i]) {
				continue;
			}
			if (pfs_ihs[i]->folder != (*eh)->parent) {
				continue;
			}
			if (pfs_ihs[i]->index >= former_index) {
				if (pfs_ihs[i]->index == former_index) {
					abort();
				}
				pfs_ihs[i]->index--;
				if (!pfsc_folder_fill_iterator_index(
						&pfs_ihs[i]->folder->handle, &pfs_ihs[i]->handle,
						pfs_ihs[i]->index)) {
					abort(); // check what to do when it happens
					// well, thats (possibly) better than a crash
					// pfs_close_iter_impl(i);
				}
			}
		}
		force_release_eh(*eh);
	} else {
		release_eh(*eh);
	}
	*eh = NULL;
	return res;
}

extern int pfs_delete(const char *path) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	return delete_element(&eh);
}

extern int pfs_element_delete(int eh) {
	eh(0)
	return delete_element(&pfs_ehs[eh]);
}

static inline int open_sh(struct element_handle *eh, ui32 stream_flags) {
	struct stream_handle *sh = malloc(sizeof(struct stream_handle));
	if (!sh) {
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(eh);
		return -1;
	}
	sh->element = eh;
	sh->pos = 0;
	sh->flags = stream_flags;
	void *block_data = pfs->get(pfs, eh->handle.direct_parent_place.block);
	if (!block_data) {
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		free(sh);
		release_eh(eh);
		return -1;
	}
	struct pfs_folder_entry *e = block_data + eh->handle.entry_pos;
	sh->is_file = (e->flags & PFS_F_FILE) != 0;
	if (!sh->is_file) {
		sh->pos = -1;
		if ((e->flags & PFS_F_PIPE) == 0) {
			pfs->unget(pfs, eh->handle.direct_parent_place.block);
			(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			free(sh);
			release_eh(eh);
			return -1;
		}
	}
	if (eh->handle.direct_parent_place.block
			!= eh->handle.element_place.block) {
		pfs->unget(pfs, eh->handle.direct_parent_place.block);
	}
	if (sh->is_file) {
		if (stream_flags & PFS_SO_PIPE) {
			if (eh->handle.direct_parent_place.block
					== eh->handle.element_place.block) {
				pfs->unget(pfs, eh->handle.direct_parent_place.block);
			}
			(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
			free(sh);
			release_eh(eh);
			return -1;
		}
	} else if (stream_flags & PFS_SO_FILE) {
		if (eh->handle.direct_parent_place.block
				== eh->handle.element_place.block) {
			pfs->unget(pfs, eh->handle.direct_parent_place.block);
		}
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		free(sh);
		release_eh(eh);
		return -1;
	}
	if (eh->handle.direct_parent_place.block
			!= eh->handle.element_place.block) {
		block_data = pfs->get(pfs, eh->handle.element_place.block);
	}
	if (!block_data) {
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		free(sh);
		release_eh(eh);
		return -1;
	}
	struct pfs_file *f = block_data + eh->handle.element_place.pos;
	if (stream_flags & PFS_SO_FILE_TRUNC) {
		if (!sh->is_file) {
			pfs->unget(pfs, eh->handle.element_place.block);
			(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			free(sh);
			release_eh(eh);
			return -1;
		}
		if (!pfsc_file_truncate(&eh->handle, 0)) {
			pfs->unget(pfs, eh->handle.element_place.block);
			free(sh);
			release_eh(eh);
			return -1;
		}
	}
	if (stream_flags & PFS_SO_FILE_EOF) {
		if (sh->is_file) {
			sh->pos = f->file_length;
		}
	}
	pfs->unget(pfs, eh->handle.element_place.block);
	return_handle(pfs_sh_len, pfs_shs, sh);
}

struct fd_del_str {
	struct delegate_stream val;
	bm_fd fd;
};

static i64 fd_write(struct delegate_stream *ds, void *data, i64 len) {
	struct fd_del_str *fdds = (struct fd_del_str*) ds;
	i64 wrote = write(fdds->fd, data, len);
	if (wrote == -1) {
		(*pfs_err_loc) = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
	return wrote;
}
static i64 fd_read(struct delegate_stream *ds, void *buf, i64 len) {
	struct fd_del_str *fdds = (struct fd_del_str*) ds;
	i64 reat = read(fdds->fd, buf, len);
	if (reat == -1) {
#ifdef PORTABLE_BUILD
		if (feof(fdds->fd)) {
			clearerr(bf->fd);
		} else {
			switch (errno) {
			case EIO:
				(*pfs_err_loc) = PFS_ERRNO_IO_ERR;
				break;
			default:
				(*pfs_err_loc) = PFS_ERRNO_UNKNOWN_ERROR;
				break;
			}
			errno = 0;
		}
		return 0;
#else
		switch (errno) {
		case EIO:
			(*pfs_err_loc) = PFS_ERRNO_IO_ERR;
			break;
		default:
			(*pfs_err_loc) = PFS_ERRNO_UNKNOWN_ERROR;
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
			(*pfs_err_loc) = PFS_ERRNO_IO_ERR;
			break;
		default:
			(*pfs_err_loc) = PFS_ERRNO_UNKNOWN_ERROR;
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
			(*pfs_err_loc) = PFS_ERRNO_IO_ERR;
			break;
		default:
			(*pfs_err_loc) = PFS_ERRNO_UNKNOWN_ERROR;
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
			(*pfs_err_loc) = PFS_ERRNO_IO_ERR;
			break;
		default:
			(*pfs_err_loc) = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		errno = 0;
	}
	sek = bm_fd_seek(fdds->fd, sek + add);
	if (sek == -1) {
		switch (errno) {
		case EIO:
			(*pfs_err_loc) = PFS_ERRNO_IO_ERR;
			break;
		default:
			(*pfs_err_loc) = PFS_ERRNO_UNKNOWN_ERROR;
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
			(*pfs_err_loc) = PFS_ERRNO_IO_ERR;
			break;
		default:
			(*pfs_err_loc) = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		errno = 0;
	}
	return sek;
}

extern int pfs_stream_open_delegate_fd(bm_fd fd, i32 stream_flags) {
	if (stream_flags
			& (PFS_SO_ONLY_CREATE | PFS_SO_ALSO_CREATE | PFS_SO_FILE_TRUNC)) {
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		return -1;
	}
	for (int i = 0; i < pfs_sh_len; i++) {
		if (!pfs_shs[i]) {
			continue;
		}
		if (pfs_shs[i]->element || !pfs_shs[i]->is_file) {
			continue;
		}
		if (((struct fd_del_str*)pfs_shs[i]->delegate)->fd == fd) {
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
		(*pfs_err_loc) = PFS_ERRNO_ELEMENT_ALREADY_EXIST;
		return -1;
	}
	int res = open_sh(pfs_ehs[eh], stream_flags);
	if (res != -1) {
		pfs_ehs[eh]->load_count++;
	}
	return res;
}

extern int pfs_stream(const char *path, i32 stream_flags) {
	const ui32 old_err = (*pfs_err_loc);
	struct element_handle *peh;
	struct element_handle *eh = open_eh(path, pfs_root, 1, &peh);
	if (!eh) {
		if ((stream_flags & (PFS_SO_ONLY_CREATE | PFS_SO_ALSO_CREATE)) == 0) {
			return -1;
		}
		if (!peh) {
			return -1;
		}
		(*pfs_err_loc) = old_err;
		eh = malloc(sizeof(struct element_handle));
		if (!eh) {
			(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
			return -1;
		}
		eh->handle = peh->handle;
		eh->parent = peh;
		eh->load_count = 1;
		eh->children.entrycount = 0;
		eh->children.maxi = 0;
		eh->children.equalizer = childset_equal;
		eh->children.hashmaker = childset_hash;
		eh->children.entries = NULL;
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
			(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
			return -1;
		}
		hashset_put(&peh->children, eh_hash(eh), eh);
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
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(pfs_ehs[eh]);
		pfs->unget(pfs, pfs_ehs[eh]->handle.direct_parent_place.block);
		return -1;
	}
	ih->ieh = pfs_ehs[eh]->handle;
	if (!pfsc_folder_fill_iterator(&ih->ieh, &ih->handle, show_hidden)) {
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(pfs_ehs[eh]);
		pfs->unget(pfs, pfs_ehs[eh]->handle.direct_parent_place.block);
		return -1;
	}
	if ((pfs_ehs[eh]->handle.direct_parent_place.block != -1)
			&& (!pfs->unget(pfs, pfs_ehs[eh]->handle.direct_parent_place.block))) {
		release_eh(pfs_ehs[eh]);
		return -1;
	}
	ih->folder = pfs_ehs[eh];
	return_handle(pfs_ih_len, pfs_ihs, ih);
}

extern int pfs_iter(const char *path, int show_hidden) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	if (eh->handle.direct_parent_place.block != -1L) {
		if (!pfs->get(pfs, eh->handle.direct_parent_place.block)) {
			return -1;
		}
		ui32 flags = pfsc_element_get_flags(&eh->handle);
		if (flags == -1) {
			release_eh(eh);
			pfs->unget(pfs, eh->handle.direct_parent_place.block);
			return -1;
		}
		if ((flags & PFS_F_FOLDER) == 0) {
			(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			release_eh(eh);
			pfs->unget(pfs, eh->handle.direct_parent_place.block);
			return -1;
		}
	}
	struct iter_handle *ih = malloc(sizeof(struct iter_handle));
	if (!ih) {
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(eh);
		pfs->unget(pfs, eh->handle.direct_parent_place.block);
		return -1;
	}
	ih->ieh = eh->handle;
	if (!pfsc_folder_fill_iterator(&ih->ieh, &ih->handle, show_hidden)) {
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(eh);
		pfs->unget(pfs, eh->handle.direct_parent_place.block);
		return -1;
	}
	if (eh->handle.direct_parent_place.block != -1L) {
		if (!pfs->unget(pfs, eh->handle.direct_parent_place.block)) {
			release_eh(eh);
			return -1;
		}
	}
	ih->folder = eh;
	return_handle(pfs_ih_len, pfs_ihs, ih);
}

extern int pfs_iter_next(int ih) {
	ih(-1)
	if (!pfsc_folder_iter_next(&pfs_ihs[ih]->handle)) {
		return -1;
	}
	struct element_handle *c = hashset_get(&pfs_ihs[ih]->folder->children,
			eh_hash(&pfs_ihs[ih]->ieh), &pfs_ihs[ih]->ieh);
	if (c) {
		c->load_count++;
	} else {
		c = malloc(sizeof(struct element_handle));
		if (!c) {
			(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
			return -1;
		}
		c->children.entrycount = 0;
		c->children.maxi = 0;
		c->children.equalizer = childset_equal;
		c->children.hashmaker = childset_hash;
		c->children.entries = NULL;
		c->handle = pfs_ihs[ih]->ieh;
		c->load_count = 1;
		c->parent = pfs_ihs[ih]->folder;
		hashset_put(&pfs_ihs[ih]->folder->children, eh_hash(&pfs_ihs[ih]->ieh),
				&pfs_ihs[ih]->ieh);
	}
	return_handle(pfs_eh_len, pfs_ehs, c)
}

extern int pfs_element_close(int eh) {
	eh(0)
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
	} else if (--pfs_shs[sh]->pos <= 0) {
		if (pfs_shs[sh]->pos < 0) {
			abort();
		}
		if (close(pfs_shs[sh]->is_file) == -1) {
			(*pfs_err_loc) = PFS_ERRNO_UNKNOWN_ERROR;
			return 0;
		}
	}
	pfs_shs[sh] = NULL;
	return 1;
}
