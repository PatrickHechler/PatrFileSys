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

#define has_refs(eh) ( ( (eh)->load_count > 0) ? 1 : ( (eh)->children.entrycount != 0) )

#define get_handle(err_ret, h_len, hs, h_num) \
	if (h_num >= h_len) { \
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG; \
		return err_ret; \
	} \
	if (!hs[h_num]) { \
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG; \
		return err_ret; \
	}

#define get_eh(err_ret, eh_num) get_handle(err_ret, pfs_eh_len, pfs_ehs, eh_num)
#define get_sh(err_ret, sh_num) get_handle(err_ret, pfs_sh_len, pfs_shs, sh_num)
#define get_ih(err_ret, ih_num) get_handle(err_ret, pfs_ih_len, pfs_ihs, ih_num)

#define eh(err_ret) get_eh(err_ret, eh)
#define sh(err_ret) get_sh(err_ret, sh)
#define ih(err_ret) get_ih(err_ret, ih)

#define eh_hash(a) ( (unsigned) ( ( (unsigned) ((struct element_handle*)a)->handle.element_place.block) \
		^ ( (unsigned) ((struct element_handle*)a)->handle.element_place.pos) ) )

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
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
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
			pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE; \
			return err_ret; \
		} \
	} else { \
		void *direct_parent_block_data = pfs->get(pfs, e->handle.direct_parent_place.block); \
		if (!direct_parent_block_data) { \
			return err_ret; \
		} \
		struct pfs_folder_entry *my_entry = direct_parent_block_data + e->handle.entry_pos; \
		if ((my_entry->flags & flag) == 0) { \
			pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE; \
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
unsigned int childset_hash(const void *a);

struct element_handle {
	struct pfs_element_handle handle;
	struct element_handle *parent;
	i64 load_count;
	struct hashset children;
};

struct stream_handle {
	struct element_handle *element;
	i64 pos; /* also used as reference count for delegate streams */
	ui32 flags;
	int is_file; /* also used as fd for delegate streams */
};

struct iter_handle {
	struct pfs_folder_iter handle;
	struct pfs_element_handle ieh;
	struct element_handle *folder;
};

static_assert((offsetof(struct iter_handle, ieh) & 7) == 0, "err");
static_assert((offsetof(struct iter_handle, folder) & 7) == 0, "err");

#ifndef I_AM_API_PFS
#define EXT extern
#define INIT(val)
#else
#define EXT
#define INIT(val) = val
#endif

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
