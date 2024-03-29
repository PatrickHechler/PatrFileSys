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
 * pfs-element.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#include "pfs-element.h"

#define ensure_entry_blocks \
	ensure_block_is_entry(pfs0(e), e->element_place.block); \
	if (e->direct_parent_place.block != -1) { \
		ensure_block_is_entry(pfs0(e), e->direct_parent_place.block); \
		ensure_block_is_entry(pfs0(e), e->real_parent_place.block); \
	}

#define get_element0(element_name, block_name, error_result) \
	ensure_entry_blocks \
	void *block_name = pfs0(e)->get(pfs0(e), e->element_place.block); \
	if (block_name == NULL) { \
		return error_result; \
	} \
	struct pfs_element *element_name = block_name + e->element_place.pos;

#define get_element(error_result) get_element0(element, block_data, error_result)

//#define get_parent0(parent_name, block_name, error_result) \
//	ensure_entry_blocks \
//	void *block_name = pfs->get(pfs, e->direct_parent_place.block); \
//	if (block_name == NULL) { \
//		(pfs_err) = PFS_ERRNO_UNKNOWN_ERROR; \
//		return error_result; \
//	} \
//	struct pfs_folder *parent_name = block_name + e->direct_parent_place.pos; \
//
//#define get_parent(error_result) get_parent0(p, pblock, error_result)

#define get_entry0(entry_name, parent_name, block_name, error_result) \
	ensure_entry_blocks \
	void *block_name = pfs0(e)->get(pfs0(e), e->direct_parent_place.block); \
	if (block_name == NULL) { \
		return error_result; \
	} \
	struct pfs_folder *parent_name = block_name + e->direct_parent_place.pos; \
	struct pfs_folder_entry *entry_name =  &parent_name->entries[e->index_in_direct_parent_list]; \

#define get_entry(error_result) get_entry0(entry, parent, dpblock, error_result)

i32 pfsc_element_get_flags(pfs_eh e) {
	if (e->real_parent_place.block == -1) {
		return PFS_F_MOUNT | PFS_F_FOLDER;
	}
	get_entry(-1)
	ui32 flags = entry->flags;
	pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
	return flags;
}

int pfsc_element_modify_flags(pfs_eh e, ui32 add_flags, ui32 rem_flags) {
	if (e->real_parent_place.block == -1) {
		(pfs_err) = PFS_ERRNO_ROOT_FOLDER;
		return 0;
	}
	if ((add_flags & rem_flags) != 0) {
		(pfs_err) = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (((add_flags | rem_flags) & PFS_UNMODIFIABLE_FLAGS) != 0) {
		(pfs_err) = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	get_entry(0)
	entry->flags = add_flags | (entry->flags & ~rem_flags);
	pfs0(e)->set(pfs0(e), e->direct_parent_place.block);
	return 1;
}

//static i32 get_size_from_block_table(i64 block, i32 pos) {
//	void *block_data = pfs->get(pfs, block);
//	if (block_data == NULL) {
//		// block should already be loaded
//		abort();
//	}
//	i32 *table_end = block_data + (pfs->block_size - 4);
//	for (i32 *table = block_data + *table_end; 1; table += 2) {
//		if (table >= table_end) {
//			abort();
//		}
//		if (pos > *table) {
//			continue;
//		} else if (pos < *table) {
//			abort();
//		}
//		i64 name_len = table[1] - pos;
//		pfs->unget(pfs, block);
//		return name_len;
//	}
//}

i64 pfsc_element_get_name_length(pfs_eh e) {
	if (e->real_parent_place.block == -1) {
		return 0; // root name is empty
	}
	get_entry0(entry, parent, block_data, -1L)
	i32 len = get_size_from_block_table(block_data, entry->name_pos, pfs0(e)->block_size);
	pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
	return len;
}

int pfsc_element_get_name(pfs_eh e, char **buffer, i64 *buffer_len) {
	if (*buffer_len < 0) {
		(pfs_err) = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (e->real_parent_place.block == -1) { // root
		if (*buffer_len == 0) {
			void *nb;
			nb = malloc(1);
			if (nb == NULL) {
				(pfs_err) = PFS_ERRNO_OUT_OF_MEMORY;
				return 0;
			}
			*buffer_len = 1;
			*buffer = nb;
		}
		(*buffer)[0] = '\0';
	} else {
		get_entry0(entry, parent, block_data, 0)
		i64 len = pfsc_element_get_name_length(e);
		i64 size = len + 1;
		if (size == -1) {
			// (pfs_err) already set
			pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
			return 0;
		}
		if ((*buffer_len) < size) {
			void *nb;
			if (*buffer_len == 0) {
				nb = malloc(size);
			} else {
				nb = realloc(*buffer, size);
			}
			if (nb == NULL) {
				(pfs_err) = PFS_ERRNO_OUT_OF_MEMORY;
				pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
				return 0;
			}
			*buffer_len = size;
			*buffer = nb;
		}
		memcpy(*buffer, block_data + entry->name_pos, len);
		(*buffer)[len] = '\0';
		pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
	}
	return 1;
}

int pfsc_element_set_name(pfs_eh e, char *name) {
	if (e->real_parent_place.block == -1) {
		pfs_err = PFS_ERRNO_ROOT_FOLDER;
		return 0;
	}
	i64 name_len = strlen(name);
	if (!name_len) { // only root folder has an empty name
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	get_entry0(entry, direct_parent, entry_block_data, 0)
	i32 new_name_pos = reallocate_in_block_table(pfs0(e), e->direct_parent_place.block, entry->name_pos,
	        name_len, 0);
	if (new_name_pos != -1) {
		memcpy(entry_block_data + new_name_pos, name, name_len);
		pfs0(e)->set(pfs0(e), e->direct_parent_place.block);
		return 1;
	}
	struct pfs_place dpplace;
	size_t parent_size = sizeof(struct pfs_folder)
	        + (sizeof(struct pfs_folder_entry) * direct_parent->direct_child_count);
	if (!allocate_new_entry(pfs0(e), &dpplace, -1, parent_size)) {
		pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
		pfs_err = PFS_ERRNO_OUT_OF_SPACE;
		return 0;
	}
	void *new_block = pfs0(e)->get(pfs0(e), dpplace.block);
	if (new_block == NULL) {
		free_block(pfs0(e), dpplace.block);
		pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
		return 0;
	}
	struct pfs_folder *new_parent = new_block + dpplace.pos;
	memcpy(new_parent, direct_parent, parent_size);
	for (int i = 0; i < new_parent->direct_child_count; i++) {
		if (i == new_parent->helper_index) {
			continue;
		}
		char *pcn;
		i32 pcn_len;
		if (i == e->index_in_direct_parent_list) {
			pcn = name;
			pcn_len = name_len;
		} else {
			pcn = new_block + new_parent->entries[i].name_pos;
			pcn_len = get_size_from_block_table(entry_block_data, new_parent->entries[i].name_pos, pfs0(e)->block_size);
		}
		i32 pcnp = add_name(pfs0(e), dpplace.block, pcn, pcn_len);
		if (pcnp == -1) {
			free_block(pfs0(e), dpplace.block);
			pfs0(e)->unget(pfs0(e), dpplace.block);
			pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
			return 0;
		}
		new_parent->entries[i].name_pos = pcnp;
	}
	void *pfs_block = pfs0(e)->get(pfs0(e), new_parent->folder_entry.block);
	if (pfs_block == NULL) {
		free_block(pfs0(e), dpplace.block);
		pfs0(e)->unget(pfs0(e), dpplace.block);
		pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
		return 0;
	}
	struct pfs_folder_entry *pfe = pfs_block + new_parent->folder_entry.pos;
	pfe->child_place = dpplace;
	pfs0(e)->set(pfs0(e), new_parent->folder_entry.block);
	for (int i = 0; i < new_parent->direct_child_count; i++) {
		if ((new_parent->entries[i].flags & PFS_F_FOLDER) != 0) {
			void *sb_data = pfs0(e)->get(pfs0(e), new_parent->entries[i].child_place.block);
			if (sb_data == NULL) {
				void *pfs_block = pfs0(e)->get(pfs0(e), new_parent->folder_entry.block);
				if (pfs_block == NULL) {
					abort();
				}
				pfe = pfs_block + new_parent->folder_entry.pos;
				pfe->child_place = e->direct_parent_place;
				for (i--; i >= 0; i--) {
					if ((new_parent->entries[i].flags & PFS_F_FOLDER) != 0) {
						sb_data = pfs0(e)->get(pfs0(e), new_parent->entries[i].child_place.block);
						if (sb_data == NULL) {
							abort();
						}
						struct pfs_folder *sb = sb_data + new_parent->entries[i].child_place.pos;
						if (e->direct_parent_place.block == e->real_parent_place.block) {
							sb->real_parent = e->direct_parent_place;
						}
						sb->folder_entry.block = e->direct_parent_place.block;
						sb->folder_entry.pos += e->direct_parent_place.pos /* - dpplace.pos */; // dpplace.pos == 0
						pfs0(e)->set(pfs0(e), new_parent->entries[i].child_place.block);
					}
				}
				pfs0(e)->unget(pfs0(e), dpplace.block);
				pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
				pfs_err = PFS_ERRNO_OUT_OF_SPACE;
				return 0;
			}
			struct pfs_folder *sb = sb_data + new_parent->entries[i].child_place.pos;
			if (e->direct_parent_place.block == e->real_parent_place.block) {
				sb->real_parent = dpplace;
			}
			sb->folder_entry.block = dpplace.block;
			sb->folder_entry.pos -= e->direct_parent_place.pos /* - dpplace.pos */; // dpplace.pos == 0
			pfs0(e)->set(pfs0(e), new_parent->entries[i].child_place.block);
		}
	}
	remove_from_block_table(pfs0(e), e->direct_parent_place.block, e->direct_parent_place.pos);
	const i64 old_parent_block = e->direct_parent_place.block;
	e->direct_parent_place = dpplace;
	pfs0(e)->set(pfs0(e), old_parent_block);
	pfs0(e)->set(pfs0(e), dpplace.block);
	return 1;
}

i64 pfsc_element_get_create_time(pfs_eh e) {
	if (e->direct_parent_place.block == -1) {
		(pfs_err) = PFS_ERRNO_ROOT_FOLDER;
		return -1;
	}
	get_entry(-1);
	i64 ct = entry->create_time;
	pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
	return ct;
}

int pfsc_element_set_create_time(pfs_eh e, i64 new_time) {
	if (e->direct_parent_place.block == -1) {
		(pfs_err) = PFS_ERRNO_ROOT_FOLDER;
		return 0;
	}
	get_entry(-1);
	entry->create_time = new_time;
	pfs0(e)->set(pfs0(e), e->direct_parent_place.block);
	return 1;
}

i64 pfsc_element_get_last_mod_time(pfs_eh e) {
	get_element(0)
	i64 lmt = element->last_mod_time;
	pfs0(e)->unget(pfs0(e), e->element_place.block);
	return lmt;
}

int pfsc_element_set_last_mod_time(pfs_eh e, i64 new_time) {
	get_element(0)
	element->last_mod_time = new_time;
	pfs0(e)->set(pfs0(e), e->element_place.block);
	return 1;
}
