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
 * pfs.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_API_PFS_H_
#define SRC_API_PFS_H_

#include "../include/patr-file-sys.h"
#include "../include/pfs-err.h"

#include <stdlib.h>

#define has_refs0(eh, small) ( (eh)->load_count > small )
#define has_refs(eh) has_refs0(eh, 0)

#define get_handle(err_ret, h_len, hs, h_num) \
	if (h_num >= h_len) { \
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG; \
		return err_ret; \
	} \
	if (!hs[h_num]) { \
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG; \
		return err_ret; \
	}

#define get_eh(err_ret, eh_num) \
		get_handle(err_ret, pfs_eh_len, pfs_ehs, eh_num) \
		if (pfs_ehs[eh_num]->handle.element_place.block == -1) { \
			(*pfs_err_loc) = PFS_ERRNO_ELEMENT_DELETED; \
			return err_ret; \
		}

#define get_sh(err_ret, sh_num) get_handle(err_ret, pfs_sh_len, pfs_shs, sh_num)
#define get_ih(err_ret, ih_num) get_handle(err_ret, pfs_ih_len, pfs_ihs, ih_num)

#define eh(err_ret) get_eh(err_ret, eh)
#define sh(err_ret) get_sh(err_ret, sh)
#define ih(err_ret) get_ih(err_ret, ih)

#define eh_hash(a) ( ( (uint64_t) ((const struct element_handle*)a)->handle.element_place.block) \
		^ ( (uint64_t) (uint32_t) ((const struct element_handle*)a)->handle.element_place.pos) )

static inline int return_handle(i64 *len, void ***hs, void *h) {
	for (int i = 0; i < (*len); i++) {
		if ((*hs)[i]) {
			continue;
		}
		(*hs)[i] = h;
		return i;
	}
	void **nhs = realloc((*hs), ((*len) + 1) * sizeof(void*));
	if (!nhs) {
		(*pfs_err_loc) = PFS_ERRNO_OUT_OF_MEMORY;
		return -1;
	}
	(*hs) = nhs;
	(*hs)[*len] = h;
	return (*len)++;
}

#define return_handle(len, hs, h) return return_handle(&len, (void ***) &hs, h);

#define c_h(e, err_ret, flag) \
	if (e->handle.direct_parent_place.block == -1) { \
		if ((PFS_F_FOLDER & flag) != flag) { \
			(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE; \
			return err_ret; \
		} \
	} else { \
		void *direct_parent_block_data = pfs->get(pfs, e->handle.direct_parent_place.block); \
		if (!direct_parent_block_data) { \
			return err_ret; \
		} \
		struct pfs_folder_entry *my_entry = direct_parent_block_data + e->handle.entry_pos; \
		if ((my_entry->flags & flag) == 0) { \
			(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE; \
			pfs->unget(pfs, pfs_ehs[eh]->handle.direct_parent_place.block); \
			return err_ret; \
		} \
	} \

#define c_r(e, err_ret) \
	if ((e->handle.direct_parent_place.block != -1) \
			&& (!pfs->unget(pfs, e->handle.direct_parent_place.block))) { \
		return err_ret; \
	}

#include "../core/pfs.h"

int childset_equal(const void *a, const void *b);
uint64_t childset_hash(const void *a);

struct element_handle {
	struct pfs_element_handle handle;
	i64 load_count;
};

_Static_assert(offsetof(struct element_handle, handle) == 0, "Error!");

struct stream_handle {
	struct element_handle *element;
	union {
		i64 pos;
		struct delegate_stream *delegate;
	};
	union {
		ui32 flags;
		ui32 delegate_ref_count;
	};
	int is_file;
};

struct iter_handle {
	struct pfs_folder_iter handle;
	struct pfs_element_handle ieh;
	struct element_handle *folder;
	i64 index;
};

_Static_assert((offsetof(struct iter_handle, ieh) & 7) == 0, "err");
_Static_assert((offsetof(struct iter_handle, folder) & 7) == 0, "err");

#ifndef I_AM_API_PFS
#define EXT extern
#define INIT(val)
#else
#define EXT
#define INIT(val) = val
static int pfs_eh_equal(const void *a, const void *b);
static uint64_t pfs_eq_hash(const void *a);
#endif

#define PFS_WELL_WDINTM(a,b,c,d,e) a, b, c, d, e

EXT struct hashset pfs_all_ehs_set INIT({
		PFS_WELL_WDINTM(
				.entries = NULL,
				.entrycount = 0,
				.equalizer = pfs_eh_equal,
				.hashmaker = pfs_eq_hash,
				.maxi = 0
		)
}
);
#undef PFS_WELL_WDINTM

EXT struct element_handle **pfs_ehs INIT(NULL);
EXT struct stream_handle **pfs_shs INIT(NULL);
EXT struct iter_handle **pfs_ihs INIT(NULL);

EXT i64 pfs_eh_len INIT(0L);
EXT i64 pfs_sh_len INIT(0L);
EXT i64 pfs_ih_len INIT(0L);

EXT struct element_handle *pfs_root INIT(NULL);
EXT struct element_handle *pfs_cwd INIT(NULL);

#undef EXT
#undef INIT

#endif /* SRC_API_PFS_H_ */
