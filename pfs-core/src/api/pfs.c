/*
 * pfs.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#define I_AM_API_PFS
#include "pfs.h"
#include "../include/pfs.h"

static inline void free_old() {
	if (pfs_ehs) {
		free(pfs_ehs);
		free(pfs_shs);
		free(pfs_ihs);
		if (pfs_root != pfs_cwd) {
			free(pfs_root);
		}
		free(pfs_cwd);
	}
}

int childset_equal(const void *a, const void *b) {
	const struct element_handle *ha = a, *hb = b;
	return ha->handle.element_place.block == hb->handle.element_place.block
			&& ha->handle.element_place.pos == hb->handle.element_place.pos;
}

unsigned int childset_hash(const void *a) {
	return eh_hash(a);
}

static inline void release_eh(struct element_handle *feh) {
	feh->load_count--;
	for (struct element_handle *a = feh, *b; a != NULL && !has_refs(a); a = b) {
		b = a->parent;
		if (!b) { // root
			abort();
		}
		hashset_remove(&b->children, eh_hash(a), a);
		free(a);
	}
}

static inline struct element_handle* open_eh(const char *path,
		struct element_handle *rot, int allow_relative,
		struct element_handle **parent_on_err) {
	if (*path == '\0') {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return NULL;
	}
	i64 buf_len = 1;
	char *buf = malloc(buf_len);
	if (buf == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	struct element_handle *eh =
			(allow_relative ? (*path == '/') : 0) ? rot : pfs_cwd;
	for (const char *cur = path, *end; *(cur - 1); cur = end + 1) {
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
				pfs_errno = PFS_ERRNO_ROOT_FOLDER;
				free(buf);
				return 0;
			}
			if (has_refs(eh)) {
				struct element_handle *oeh = eh;
				eh = eh->parent;
				hashset_remove(&eh->children, eh_hash(oeh), oeh);
				free(oeh);
			} else {
				eh = eh->parent;
			}
			continue;
		}
		struct element_handle *neh = malloc(sizeof(struct element_handle));
		if (!neh) {
			release_eh(eh);
			pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
			return 0;
		}
		neh->handle = eh->handle;
		if (*end || parent_on_err) {
			if (!pfsc_folder_folder_child_from_name(&neh->handle, buf)) {
				if (!*end) {
					if (parent_on_err) {
						*parent_on_err = eh->parent;
						(*parent_on_err)->load_count++;
					}
				}
				release_eh(eh);
				free(buf);
				return 0;
			}
		} else if (!pfsc_folder_child_from_name(&neh->handle, buf)) {
			release_eh(eh);
			free(buf);
			return 0;
		}
		struct element_handle *oneh = hashset_get(&eh->children, eh_hash(neh),
				neh);
		if (oneh) {
			eh = oneh;
		} else {
			hashset_put(&eh->children, eh_hash(neh), neh);
			neh->parent = eh;
			neh->load_count = 0;
			neh->children.entrycount = 0;
			neh->children.setsize = 0;
			neh->children.equalizer = childset_equal;
			neh->children.hashmaker = childset_hash;
			neh->children.entries = NULL;
			eh = neh;
		}
	}
	free(buf);
	eh->load_count++;
	return eh;
}

extern int pfs_load(struct bm_block_manager *bm, const char *cur_work_dir) {
	if (bm == NULL) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	struct pfs_b0 *b0 = bm->get(bm, 0L);
	if (b0->MAGIC != PFS_MAGIC_START) {
		bm->unget(bm, 0L);
		pfs_errno = PFS_ERRNO_ILLEGAL_MAGIC;
		return 0;
	}
	struct element_handle **nehs = malloc(sizeof(struct element_handle*));
	struct stream_handle **nshs = malloc(sizeof(struct stream_handle*));
	struct iter_handle **nihs = malloc(sizeof(struct iter_handle*));
	struct element_handle *nrot = malloc(sizeof(struct element_handle));
	if (!nihs || !nrot) {
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
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		return 0;
	}
	struct bm_block_manager *old = pfs;
	pfs = bm;
	if (!pfsc_fill_root(&nrot->handle)) {
		free(nehs);
		free(nshs);
		free(nihs);
		free(nrot);
		pfs = old;
		return 0;
	}
	nrot->parent = NULL;
	nrot->load_count = 1;
	nrot->children.setsize = 0;
	nrot->children.entrycount = 0;
	nrot->children.equalizer = childset_equal;
	nrot->children.hashmaker = childset_hash;
	nrot->children.entries = NULL;
	struct element_handle *ncwd;
	if (cur_work_dir) {
		ncwd = open_eh(cur_work_dir, nrot, 0, NULL);
		if (!ncwd) {
			free(nehs);
			free(nshs);
			free(nihs);
			free(nrot);
			pfs = old;
			return 0;
		}
	} else {
		nrot->load_count++;
		ncwd = pfs_root;
	}
	free_old();
	pfs_ehs = nehs;
	pfs_shs = nshs;
	pfs_ihs = nihs;
	pfs_eh_len = 1;
	pfs_sh_len = 1;
	pfs_ih_len = 1;
	pfs_root = nrot;
	pfs_cwd = ncwd;
	pfs_shs[0] = NULL;
	pfs_ehs[0] = NULL;
	pfs_ihs[0] = NULL;
	return 1;
}

extern int pfs_load_and_format(struct bm_block_manager *bm, i64 block_count) {
	if (bm == NULL) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
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
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		return 0;
	}
	if (!pfsc_format(block_count)) {
		free(nehs);
		free(nshs);
		free(nihs);
		free(nrot);
		return 0;
	}
	nrot->parent = NULL;
	nrot->load_count = 2;
	nrot->children.setsize = 0;
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
	void *nehs = realloc(pfs_ehs, pfs_eh_len + 1);
	if (!nehs) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
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
	ui32 flags = pfsc_element_get_flags(&eh->handle);
	if (flags == -1) {
		return -1;
	}
	if ((flags & flag) != 0) {
		pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return -1;
	}
	return_handle(pfs_eh_len, pfs_ehs, eh);
}

extern int pfs_handle_folder(const char *path) {
	return handle(path, PFS_F_FOLDER);
}

extern int pfs_handle_file(const char *path) {
	return handle(path, PFS_F_FOLDER);
}

extern int pfs_handle_pipe(const char *path) {
	return handle(path, PFS_F_FOLDER);
}

extern int pfs_change_dir(int eh) {
	eh(0)
	ui32 flags = pfsc_element_get_flags(&pfs_ehs[eh]->handle);
	if (flags == -1) {
		return 0;
	}
	if ((flags & PFS_F_FOLDER) != 0) {
		pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
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
		pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return -1;
	}
	release_eh(pfs_cwd);
	pfs_cwd = eh;
	return 1;
}

extern int pfs_delete(const char *path) {
	struct element_handle *eh = open_eh(path, pfs_root, 1, NULL);
	if (!eh) {
		return -1;
	}
	int res = pfsc_element_delete(&eh->handle);
	if (res) {
		if (eh == pfs_cwd) {
			pfs_cwd = pfs_root;
		}
		for (int i = 0; i < pfs_eh_len; i++) {
			if (eh == pfs_ehs[i]) {
				pfs_ehs[i] = NULL;
			}
		}
	}
	return res;
}

static inline int open_sh(struct element_handle *eh, ui32 stream_flags) {
	struct stream_handle *sh = malloc(sizeof(struct stream_handle));
	if (!sh) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(eh);
		return -1;
	}
	sh->element = eh;
	sh->pos = 0;
	sh->flags = stream_flags;
	void *block_data = pfs->get(pfs, eh->handle.direct_parent_place.block);
	if (!block_data) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
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
			pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			free(sh);
			release_eh(eh);
			return -1;
		}
	}
	pfs->unget(pfs, eh->handle.direct_parent_place.block);
	if (sh->is_file) {
		if (stream_flags & PFS_SO_PIPE) {
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			free(sh);
			release_eh(eh);
			return -1;
		}
	} else if (stream_flags & PFS_SO_FILE) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		free(sh);
		release_eh(eh);
		return -1;
	}
	block_data = pfs->get(pfs, eh->handle.element_place.block);
	if (!block_data) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		free(sh);
		release_eh(eh);
		return -1;
	}
	struct pfs_file *f = block_data + eh->handle.element_place.pos;
	if (stream_flags & PFS_SO_FILE_TRUNC) {
		if (!sh->is_file) {
			pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			free(sh);
			release_eh(eh);
			return -1;
		}
		if (!pfsc_file_truncate(&eh->handle, 0)) {
			free(sh);
			release_eh(eh);
			return -1;
		}
	}
	if (stream_flags & PFS_SO_FILE_EOF) {
		if (sh->is_file) {
			sh->pos = f->file_length;
		} else {
			// ignored
		}
	}
	return_handle(pfs_sh_len, pfs_shs, sh);
}

extern int pfs_open_stream(int eh, i32 stream_flags) {
	eh(-1)
	if (stream_flags & PFS_SO_ONLY_CREATE) {
		pfs_errno = PFS_ERRNO_ELEMENT_ALREADY_EXIST;
		return -1;
	}
	int res = open_sh(pfs_ehs[eh], stream_flags);
	if (res) {
		pfs_ehs[eh]->load_count++;
	}
	return res;
}

extern int pfs_stream(const char *path, i32 stream_flags) {
	const ui32 old_err = pfs_errno;
	struct element_handle *peh;
	struct element_handle *eh = open_eh(path, pfs_root, 1, &peh);
	if (!eh) {
		if ((stream_flags & (PFS_SO_ONLY_CREATE | PFS_SO_ALSO_CREATE)) == 0) {
			return -1;
		}
		if (!peh) {
			return -1;
		}
		pfs_errno = old_err;
		eh = malloc(sizeof(struct element_handle));
		if (!eh) {
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			return -1;
		}
		eh->handle = peh->handle;
		eh->parent = peh;
		eh->load_count = 1;
		eh->children.entrycount = 0;
		eh->children.setsize = 0;
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
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
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
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(pfs_ehs[eh]);
		pfs->unget(pfs, pfs_ehs[eh]->handle.direct_parent_place.block);
		return -1;
	}
	ih->ieh = pfs_ehs[eh]->handle;
	if (!pfsc_folder_fill_iterator(&ih->ieh, &ih->handle, show_hidden)) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(pfs_ehs[eh]);
		pfs->unget(pfs, pfs_ehs[eh]->handle.direct_parent_place.block);
		return -1;
	}
	if (!pfs->unget(pfs, pfs_ehs[eh]->handle.direct_parent_place.block)) {
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
			pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			release_eh(eh);
			pfs->unget(pfs, eh->handle.direct_parent_place.block);
			return -1;
		}
	}
	struct iter_handle *ih = malloc(sizeof(struct iter_handle));
	if (!ih) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		release_eh(eh);
		pfs->unget(pfs, eh->handle.direct_parent_place.block);
		return -1;
	}
	ih->ieh = eh->handle;
	if (!pfsc_folder_fill_iterator(&ih->ieh, &ih->handle, show_hidden)) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
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
			pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
			return -1;
		}
		c->children.entries = NULL;
		c->children.entrycount = 0;
		c->children.equalizer = childset_equal;
		c->children.hashmaker = childset_hash;
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
	release_eh(pfs_ihs[ih]->folder);
	pfs_ihs[ih] = NULL;
	return 1;
}

extern int pfs_stream_close(int sh) {
	sh(0)
	release_eh(pfs_shs[sh]->element);
	pfs_shs[sh] = NULL;
	return 1;
}
