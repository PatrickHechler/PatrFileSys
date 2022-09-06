/*
 * pfs-element.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#include "pfs-intern.h"
#include "pfs-element.h"

extern int pfs_element_get_parent(element *e) {
	ensure_block_is_entry(e->block);
	while (1) {
		const i64 old_block = e->block;
		struct pfs_element *element = pfs->get(pfs, old_block) + e->pos;
		if (element->parent.block == -1L) {
			pfs->unget(pfs, old_block);
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			return 0;
		}
		*e = element->parent;
		ensure_block_is_entry(e->block);
		struct pfs_element *parent = pfs->get(pfs, e->block) + e->pos;
		pfs->unget(pfs, old_block);
		// do not load->unload->load->unload when parent and child is in the same block
		if ((parent->flags & PFS_FLAGS_HELPER_FOLDER) == 0) {
			if ((parent->flags & PFS_FLAGS_FOLDER) == 0) {
				abort();
			}
			return 1;
		}
	}
}

extern ui32 pfs_element_get_flags(element *e) {
	ensure_block_is_entry(e->block);
	struct pfs_element *element = pfs->get(pfs, e->block) + e->pos;
	if (element == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return -1L;
	}
	ui32 flags = element->flags;
	pfs->unget(pfs, e->block);
	return flags;
}

extern int pfs_element_modify_flags(element *e, ui32 add_flags, ui32 rem_flags) {
	ensure_block_is_entry(e->block);
	if ((((add_flags | rem_flags) & PFS_FLAGS_RESERVED) != 0)
			|| ((add_flags & rem_flags) != 0)) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	struct pfs_element *element = pfs->get(pfs, e->block) + e->pos;
	if (element == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return -1L;
	}
	element->flags = (element->flags & ~rem_flags) | add_flags;
	pfs->set(pfs, e->block);
	return 1;
}

#define element_get_time(time_name) \
	struct pfs_element *element = pfs->get(pfs, e->block) + e->pos; \
	if (element == NULL) { \
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
		return -1L; \
	} \
	i64 ct = element->time_name; \
	pfs->unget(pfs, e->block); \
	return ct; \

extern i64 pfs_element_get_create_time(element *e) {
	element_get_time(create_time)
}

extern i64 pfs_element_get_last_mod_time(element *e) {
	element_get_time(last_mod_time)
}

extern i64 pfs_element_get_last_meta_mod_time(element *e) {
	element_get_time(last_meta_mod_time)
}

#define element_set_time(time_name) \
	struct pfs_element *element = pfs->get(pfs, e->block) + e->pos; \
	if (element == NULL) { \
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
		return 0; \
	} \
	element->time_name = new_time; \
	pfs->set(pfs, e->block); \
	return 1; \

extern int pfs_element_set_create_time(element *e, i64 new_time) {
	element_set_time(create_time)
}

extern int pfs_element_set_last_mod_time(element *e, i64 new_time) {
	element_set_time(last_mod_time)
}

extern int pfs_element_set_last_meta_mod_time(element *e, i64 new_time) {
	element_set_time(last_meta_mod_time)
}
