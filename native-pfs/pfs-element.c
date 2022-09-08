/*
 * pfs-element.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#include "pfs-intern.h"
#include "pfs-element.h"

#define get_element(error_result) \
	ensure_block_is_entry(e->block); \
	void *block = pfs->get(pfs, e->block); \
	if (block == NULL) { \
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
		return error_result; \
	} \
	struct pfs_element *element = block + e->pos;

#define get_element_and_parent(error_result) \
	get_element(error_result) \
	block = pfs->get(pfs, element->parent.block); \
	if (block == NULL) { \
		pfs->unget(pfs, e->block); \
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
		return error_result; \
	} \
	struct pfs_folder *parent = block + element->parent.pos;

extern int pfs_element_get_parent(element *e) {
	ensure_block_is_entry(e->block);
	i64 old_block = e->block;
	get_element(0)
	if (element->parent.block == -1L) {
		pfs->unget(pfs, old_block);
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	*e = element->parent;
	ensure_block_is_entry(e->block);
	struct pfs_element *parent = pfs->get(pfs, e->block) + e->pos;
	if (parent->parent.block == -1L) { // root folder
		pfs->unget(pfs, old_block);
		pfs->unget(pfs, e->block);
		return 1;
	}
	while (1) {
		ensure_block_is_entry(parent->parent.block);
		block = pfs->get(pfs, parent->parent.block);
		if (block == NULL) {
			pfs->unget(pfs, old_block);
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			return 0;
		}
		struct pfs_folder *dp = block + parent->parent.block;
		if ((dp->entries[parent->index_in_parent_list].flags & PFS_FLAGS_FOLDER)
				== 0) {
			abort();
		}
		if ((dp->entries[parent->index_in_parent_list].flags
				& PFS_FLAGS_HELPER_FOLDER) == 0) {
			pfs->unget(pfs, parent->parent.block);
			pfs->unget(pfs, old_block);
			pfs->unget(pfs, e->block);
			return 1;
		}
		pfs->unget(pfs, old_block);
		old_block = e->block;
		element = parent;
		parent = &dp->element;
		*e = element->parent;
	}
}

extern ui32 pfs_element_get_flags(element *e) {
	get_element_and_parent(-1)
	ui32 my_flags = parent->entries[element->index_in_parent_list].flags;
	pfs->set(pfs, element->parent.block);
	pfs->unget(pfs, e->block);
	return my_flags;
}

extern int pfs_element_modify_flags(element *e, ui32 add_flags, ui32 rem_flags) {
	if ((((add_flags | rem_flags) & PFS_FLAGS_RESERVED) != 0)
			|| ((add_flags & rem_flags) != 0)) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	get_element_and_parent(0)
	parent->entries[element->index_in_parent_list].flags =
			(parent->entries[element->index_in_parent_list].flags & rem_flags)
					| add_flags;
	pfs->set(pfs, element->parent.block);
	pfs->unget(pfs, e->block);
	return 1;
}

extern i64 pfs_element_get_create_time(element *e) {
	get_element_and_parent(0)
	i64 ct = parent->entries[element->index_in_parent_list].create_time;
	pfs->unget(pfs, element->parent.block);
	pfs->unget(pfs, e->block);
	return ct;
}

extern i64 pfs_element_get_last_mod_time(element *e) {
	get_element(0)
	i64 lmt = element->last_mod_time;
	pfs->unget(pfs, e->block);
	return lmt;
}

extern int pfs_element_set_create_time(element *e, i64 new_time) {
	get_element_and_parent(0)
	parent->entries[element->index_in_parent_list].create_time = new_time;
	pfs->set(pfs, element->parent.block);
	pfs->unget(pfs, e->block);
	return 1;
}

extern int pfs_element_set_last_mod_time(element *e, i64 new_time) {
	get_element(0)
	element->last_mod_time = new_time;
	pfs->set(pfs, e->block);
	return 1;
}
