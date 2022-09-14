/*
 * pfs-element.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#include "pfs-intern.h"
#include "pfs-element.h"

#define ensure_entry_blocks \
	ensure_block_is_entry(e->element_place.block); \
	ensure_block_is_entry(e->direct_parent_place.block); \
	ensure_block_is_entry(e->real_parent_place.block); \


#define get_element0(element_name, block_name, error_result) \
	ensure_entry_blocks \
	void *block_name = pfs->get(pfs, e->element_place.block); \
	if (block_name == NULL) { \
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
		return error_result; \
	} \
	struct pfs_element *element_name = block_name + e->element_place.pos;

#define get_element(error_result) get_element0(element, block_data, error_result)

//#define get_parent0(parent_name, block_name, error_result) \
//	ensure_entry_blocks \
//	void *block_name = pfs->get(pfs, e->direct_parent_place.block); \
//	if (block_name == NULL) { \
//		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
//		return error_result; \
//	} \
//	struct pfs_folder *parent_name = block_name + e->direct_parent_place.pos; \
//
//#define get_parent(error_result) get_parent0(p, pblock, error_result)

#define get_entry0(entry_name, block_name, error_result) \
	ensure_entry_blocks \
	void *block_name = pfs->get(pfs, e->direct_parent_place.block); \
	if (block_name == NULL) { \
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
		return error_result; \
	} \
	struct pfs_folder_entry *entry_name = block_name + e->direct_parent_place.pos + \
			offsetof(struct pfs_folder, entries) + (sizeof(struct pfs_folder_entry) * e->index_in_direct_parent_list); \

#define get_entry(error_result) get_entry0(entry, eblock, error_result)

extern ui64 pfs_element_get_flags(pfs_eh e) {
	get_entry(0)
	ui64 flags = entry->flags;
	pfs->unget(pfs, e->direct_parent_place.block);
	return flags;
}

extern int pfs_element_modify_flags(pfs_eh e, ui64 add_flags, ui64 rem_flags) {
	if ((add_flags & rem_flags) != 0) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (((add_flags | rem_flags) & PFS_FLAGS_RESERVED) != 0) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	get_entry(0)
	entry->flags = add_flags | (entry->flags & ~rem_flags);
	pfs->unget(pfs, e->direct_parent_place.block);
	return 1;
}

extern i64 pfs_element_get_name_length(pfs_eh e) {
	if (e->real_parent_place.block == -1) {
		return 0; // root
	}
	get_entry0(entry, block_data, -1L)
	i32* table_end = block_data + (pfs->block_size - 4);
	for (i32* table = block_data + *table_end; table < table_end; table += 2) {
		if (entry->name_pos > *table) {
			continue;
		} else if (entry->name_pos < *table) {
			abort();
		}
		i64 name_len = table[1] - entry->name_pos;
		pfs->unget(pfs, e->direct_parent_place.block);
		return name_len;
	}
	abort();
}

extern i64 pfs_element_get_last_mod_time(pfs_eh e) {
	get_element(0)
	i64 lmt = element->last_mod_time;
	pfs->unget(pfs, e->element_place.block);
	return lmt;
}

extern int pfs_element_set_last_mod_time(pfs_eh e, i64 new_time) {
	get_element(0)
	element->last_mod_time = new_time;
	pfs->set(pfs, e->element_place.block);
	return 1;
}
