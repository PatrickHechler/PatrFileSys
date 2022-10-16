/*
 * pfs.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#define I_AM_API_PFS
#include "pfs.h"
#include "../include/pfs.h"

static void free_old() {
	if (ehs) {
		free(ehs);
		free(shs);
		free(ihs);
		free(root);
		free(cwd);
	}
}

static int childset_equal(const void *a, const void *b) {
	const struct element_handle *ha = a, *hb = b;
	return ha->handle.element_place.block == hb->handle.element_place.block
			&& ha->handle.element_place.pos == hb->handle.element_place.pos;
}

static inline unsigned int eh_hash(const struct element_handle *a) {
	return ((unsigned) a->handle.element_place.block)
			^ ((unsigned) a->handle.element_place.pos);
}

static unsigned int childset_hash(const void *a) {
	return eh_hash(a);
}

static inline int has_refs(struct element_handle *eh) {
	if (eh->load_count > 0) {
		return 1;
	} else if (eh->load_count < 0) {
		abort();
	}
	if (eh->children.entrycount != 0) {
		return 1;
	}
	return 0;
}

static inline void free_eh(struct element_handle *feh) {
	feh->load_count--;
	for (struct element_handle *a = feh, *b; a != NULL && has_refs(a); a = b) {
		b = a->parent;
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
			(allow_relative ? (*path == '/') : 0) ? rot : cwd;
	for (const char *cur = path, *end; *cur; cur = end) {
		end = strchrnul(cur, '/');
		i64 len = ((i64) end) - ((i64) cur);
		if (len >= buf_len) {
			buf = realloc(buf, len + 1);
			buf_len = len + 1;
		}
		memcpy(buf, cur, len);
		buf[len] = '\0';
		switch (len) {
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
			free_eh(eh);
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
				free_eh(eh);
				free(buf);
				return 0;
			}
		} else if (!pfsc_folder_child_from_name(&neh->handle, buf)) {
			free_eh(eh);
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
		ncwd = root;
	}
	free_old();
	ehs = nehs;
	shs = nshs;
	ihs = nihs;
	eh_len = 1;
	sh_len = 1;
	ih_len = 1;
	root = nrot;
	cwd = ncwd;
	shs[0] = NULL;
	ehs[0] = NULL;
	ihs[0] = NULL;
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
	if (!pfs_format(block_count)) {
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
	ehs = nehs;
	shs = nshs;
	ihs = nihs;
	eh_len = 1;
	sh_len = 1;
	ih_len = 1;
	root = nrot;
	cwd = nrot;
	shs[0] = NULL;
	ehs[0] = NULL;
	ihs[0] = NULL;
	return 1;
}

// block_count and block_size are implemented in core/pfs.c

extern int pfs_handle(const char *path) {
	struct element_handle *eh = open_eh(path, root, 1, NULL);
	if (!eh) {
		return -1;
	}
	for (int i = 0; i < eh_len; i++) {
		if (ehs[i]) {
			continue;
		}
		ehs[i] = eh;
		return i;
	}
	void *nehs = realloc(ehs, eh_len + 1);
	if (!nehs) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		return -1;
	}
	ehs = nehs;
	ehs[eh_len] = eh;
	return eh_len++;
}

#define return_handle(len, hs, h) \
	for (int i = 0; i < len; i++) { \
		if (hs[i]) { \
			continue; \
		} \
		hs[i] = h; \
		return i; \
	} \
	void *nhs = realloc(hs, len + 1); \
	if (!nhs) { \
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY; \
		return -1; \
	} \
	hs = nhs; \
	hs[len] = h; \
	return len++;

static inline int handle(const char *path, ui32 flag) {
	struct element_handle *eh = open_eh(path, root, 1, NULL);
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
	return_handle(eh_len, ehs, eh);
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

extern int pfs_cwd(int eh) {
	if (eh >= eh_len) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (!ehs[eh]) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	ui32 flags = pfsc_element_get_flags(&ehs[eh]->handle);
	if (flags == -1) {
		return -1;
	}
	if ((flags & PFS_F_FOLDER) != 0) {
		pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return -1;
	}
	free_eh(cwd);
	cwd = ehs[eh];
	cwd->load_count++;
	return 1;
}

extern int pfs_change_working_directoy(char *path) {
	struct element_handle *eh = open_eh(path, root, 1, NULL);
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
	free_eh(cwd);
	cwd = eh;
	return 1;
}

extern int pfs_delete(const char *path) {
	struct element_handle *eh = open_eh(path, root, 1, NULL);
	if (!eh) {
		return -1;
	}
	int res = pfsc_element_delete(&eh->handle);
	if (res) {
		if (eh == cwd) {
			cwd = root;
		}
		for (int i = 0; i < eh_len; i++) {
			if (eh == ehs[i]) {
				ehs[i] = NULL;
			}
		}
	}
	return res;
}

extern int pfs_stream(const char *path, i32 stream_flags) {
	struct element_handle *peh;
	struct element_handle *eh = open_eh(path, root, 1, &peh);
	if (!eh) {
		if ((stream_flags & (PFS_SO_ONLY_CREATE | PFS_SO_ALSO_CREATE)) != 0) {
			if (peh) {
				eh = malloc(sizeof(struct element_handle));
				if (!eh) {
					pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
					return 0;
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
					if (!pfsc_folder_create_pipe(&eh->handle, &peh->handle,
							name)) {
						return -1;
					}
				} else if (stream_flags & PFS_SO_FILE) {
					if (!pfsc_folder_create_file(&eh->handle, &peh->handle,
							name)) {
						return -1;
					}
				} else {
					pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
					return -1;
				}
				hashset_put(&peh->children, eh_hash(eh), eh);
			}
		}
		return -1;
	} else if (stream_flags & PFS_SO_ONLY_CREATE) {
		free_eh(eh);
		return -1;
	}
	struct stream_handle *sh = malloc(sizeof(struct stream_handle));
	if (!sh) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		free_eh(eh);
		return -1;
	}
	sh->element = eh;
	sh->pos = 0;
	sh->flags = stream_flags;
	void *block_data = pfs->get(pfs, eh->handle.direct_parent_place.block);
	if (!block_data) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		free(sh);
		free_eh(eh);
		return -1;
	}
	struct pfs_folder_entry *e = block_data + eh->handle.entry_pos;
	sh->is_file = (e->flags & PFS_F_FILE) != 0;
	if (!sh->is_file) {
		if ((e->flags & PFS_F_PIPE) == 0) {
			pfs->unget(pfs, eh->handle.direct_parent_place.block);
			pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			free(sh);
			free_eh(eh);
			return -1;
		}
	}
	pfs->unget(pfs, eh->handle.direct_parent_place.block);
	if (sh->is_file) {
		if (stream_flags & PFS_SO_PIPE) {
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			free(sh);
			free_eh(eh);
			return -1;
		}
	} else if (stream_flags & PFS_SO_FILE) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		free(sh);
		free_eh(eh);
		return -1;
	}
	block_data = pfs->get(pfs, eh->handle.element_place.block);
	if (!block_data) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		free(sh);
		free_eh(eh);
		return -1;
	}
	struct pfs_file *f = block_data + eh->handle.element_place.pos;
	sh->place.block = f->first_block;
	sh->place.pos = sh->is_file ? 0 : ((struct pfs_pipe*) f)->start_offset;
	if (stream_flags & PFS_SO_FILE_TRUNC) {
		if (sh->is_file) {
			pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			free(sh);
			free_eh(eh);
			return -1;
		}
		if (!pfsc_file_truncate(&eh->handle, 0)) {
			free(sh);
			free_eh(eh);
			return -1;
		}
	}
	if (stream_flags & PFS_SO_FILE_EOF) {
		if (sh->is_file) {
			pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			free(sh);
			free_eh(eh);
			return -1;
		}
		sh->pos = pfsc_file_length(&eh->handle);
		if (sh->pos == -1) {
			free(sh);
			free_eh(eh);
			return -1;
		}
		sh->place = find_place(sh->place.block, sh->pos);
		if (sh->place.block == -1) {
			free(sh);
			free_eh(eh);
			return -1;
		}
	}
	return_handle(sh_len, shs, sh);
}

extern int pfs_iter(const char *path, int show_hidden) {
	struct element_handle *eh = open_eh(path, root, 1, NULL);
	ui32 flags = pfsc_element_get_flags(&eh->handle);
	if (flags == -1) {
		return -1;
	}
	if ((flags & PFS_F_FOLDER) == 0) {
		pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		free_eh(eh);
		return -1;
	}
	struct iter_handle *ih = malloc(sizeof(struct iter_handle));
	if (!ih) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		free_eh(eh);
		return -1;
	}
	ih->ieh = eh->handle;
	if (!pfsc_folder_fill_iterator(&ih->ieh, &ih->handle, show_hidden)) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		free_eh(eh);
		return -1;
	}
	ih->folder = eh;
	return_handle(ih_len, ihs, ih);
}
