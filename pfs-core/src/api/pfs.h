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

#include "../pfs/patr-file-sys.h"
#include "../pfs/pfs-err.h"

#include <stdlib.h>

#define has_refs0(eh, small) ( (eh)->load_count > small )
#define has_refs(eh) has_refs0(eh, 0)

// first check the two pointers
// if the pointers are different:
// // if they are on the same mount points they they are not equal
// // if they are of a different block manager they are not equal
// // if they are on a different location they are not equal
#define element_equal(a, b) ( \
		((a) == (b)) \
		|| ( \
			((a)->handle.fs_data != (b)->handle.fs_data) \
			&& (&(a)->handle.fs_data->file_sys == &(b)->handle.fs_data->file_sys) \
			&& ((a)->handle.element_place.block == (b)->handle.element_place.block) \
			&& ((a)->handle.element_place.pos == (b)->handle.element_place.pos) \
		) \
)

#define get_handle(err_ret, h_len, hs, h_num) \
	if (h_num >= h_len) { \
		pfs_err = PFS_ERRNO_ILLEGAL_ARG; \
		return err_ret; \
	} \
	if (!hs[h_num]) { \
		pfs_err = PFS_ERRNO_ILLEGAL_ARG; \
		return err_ret; \
	}

#define get_eh(err_ret, eh_num) \
		get_handle(err_ret, pfs_eh_len, pfs_ehs, eh_num) \
		if (pfs_ehs[eh_num]->handle.element_place.block == -1) { \
			pfs_err = PFS_ERRNO_ELEMENT_DELETED; \
			return err_ret; \
		}

#define get_sh(err_ret, sh_num) get_handle(err_ret, pfs_sh_len, pfs_shs, sh_num)
#define get_ih(err_ret, ih_num) get_handle(err_ret, pfs_ih_len, pfs_ihs, ih_num)

#define eh(err_ret) get_eh(err_ret, eh)
#define sh(err_ret) get_sh(err_ret, sh)
#define ih(err_ret) get_ih(err_ret, ih)

#define check_write_access2(eh, err_act) \
	if ((eh).fs_data->read_only) { \
		pfs_err = PFS_ERRNO_READ_ONLY; \
		err_act \
	}

#define check_write_access1(eh, err_ret) check_write_access2(*(eh), return err_ret;)

#define check_write_access0(eh, err_ret) check_write_access2((eh)->handle, return err_ret;)

#define check_write_access(err_ret) check_write_access0(pfs_ehs[eh], err_ret)

#define eh_hash(a) ( \
			( \
				((uint64_t) ((const struct element_handle*)a)->handle.element_place.block) \
				^ ((uint64_t) (uint32_t) ((const struct element_handle*)a)->handle.element_place.pos) \
			) \
			^ ((uint64_t) ((const struct element_handle*)a)->handle.fs_data) \
		)

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
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
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
			pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE; \
			return err_ret; \
		} \
	} else { \
		void *direct_parent_block_data = pfs(e)->get(pfs(e), e->handle.direct_parent_place.block); \
		if (!direct_parent_block_data) { \
			return err_ret; \
		} \
		struct pfs_folder_entry *my_entry = direct_parent_block_data + e->handle.entry_pos; \
		if ((my_entry->flags & flag) == 0) { \
			pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE; \
			pfs(pfs_ehs[eh])->unget(pfs(pfs_ehs[eh]), pfs_ehs[eh]->handle.direct_parent_place.block); \
			return err_ret; \
		} \
	} \

#define c_r(e, err_ret) \
	if ((e->handle.direct_parent_place.block != -1) \
			&& (!pfs(e)->unget(pfs(e), e->handle.direct_parent_place.block))) { \
		return err_ret; \
	}

#include "../core/pfs.h"

void pfs_modify_iterators(struct element_handle const *eh, ui64 former_index, int removed);

#define pfs_modify_iterators(eh, former_index) pfs_modify_iterators(eh, former_index, 1)

struct element_handle_mount {
	i64 load_count;
	struct pfs_element_handle_mount handle;
};

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
	ui64 index;
};

_Static_assert((offsetof(struct iter_handle, ieh)
& 7) == 0, "err");
_Static_assert((offsetof(struct iter_handle, folder) & 7) == 0, "err");


#ifdef I_AM_API_PFS
#define PFS_EXT
#define PFS_INIT(...) = __VA_ARGS__
static int pfs_eh_equal(const void *a, const void *b);
static uint64_t pfs_eq_hash(const void *a);
#else
#define PFS_EXT extern
#define PFS_INIT(...)
#endif

PFS_EXT struct hashset pfs_all_ehs_set PFS_INIT( {
			.entries = NULL,
			.entrycount = 0,
			.equalizer = pfs_eh_equal,
			.hashmaker = pfs_eq_hash,
			.maxi = 0
		}
);

PFS_EXT struct element_handle **pfs_ehs PFS_INIT(NULL);
PFS_EXT struct stream_handle **pfs_shs PFS_INIT(NULL);
PFS_EXT struct iter_handle **pfs_ihs PFS_INIT(NULL);

PFS_EXT i64 pfs_eh_len PFS_INIT(0);
PFS_EXT i64 pfs_sh_len PFS_INIT(0);
PFS_EXT i64 pfs_ih_len PFS_INIT(0);

PFS_EXT struct element_handle *pfs_root PFS_INIT(NULL);
PFS_EXT struct element_handle *pfs_cwd PFS_INIT(NULL);

#undef PFS_EXT
#undef PFS_INIT

#endif /* SRC_API_PFS_H_ */
