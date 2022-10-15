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
	if (eh->load_count != 0) {
		return 1;
	}
	if (eh->children.entrycount != 0) {
		return 1;
	}
	return 0;
}

static inline struct element_handle* open_eh(const char *path,
		struct element_handle *rot, int allow_relative) {
	i64 buf_len = 1;
	char *buf = malloc(buf_len);
	if (buf == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	struct element_handle *eh = rot;
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
			for (; has_refs(eh); eh = neh) {
				neh = eh->parent;
				free(eh);
			}
			pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
			return 0;
		}
		neh->handle = eh->handle;
		if (*end) {
			if (!pfsc_folder_folder_child_from_name(&neh->handle, buf)) {
				for (; has_refs(eh); eh = neh) {
					neh = eh->parent;
					free(eh);
				}
				free(buf);
				return 0;
			}
		} else if (!pfsc_folder_folder_child_from_name(&neh->handle, buf)) {
			for (; has_refs(eh); eh = neh) {
				neh = eh->parent;
				free(eh);
			}
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
		ncwd = open_eh(cur_work_dir, nrot, 0);
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
