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
 * pfs-folder.c
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#include "pfs-folder.h"
#include "../include/pfs-constants.h"

#define get_any(pfs, struct_name, any_name, block_name, place_block, place_pos, error) \
	ensure_block_is_entry(pfs, place_block); \
	void* block_name = pfs->get(pfs, place_block); \
	if (block_name == NULL) { \
		error \
	} \
	struct struct_name *any_name = block_name + place_pos;

#define get_folder3_(pfs, folder_name, block_name, place_block, place_pos, error) get_any(pfs, pfs_folder, folder_name, block_name, place_block, place_pos, error)
#define get_folder3(folder_name, block_name, place_block, place_pos, error) get_any(pfs0(f), pfs_folder, folder_name, block_name, place_block, place_pos, error)

#define get_folder2_(pfs, folder_name, block_name, place_block, place_pos) get_folder3_(pfs, folder_name, block_name, place_block, place_pos, )
#define get_folder2(folder_name, block_name, place_block, place_pos) get_folder3(folder_name, block_name, place_block, place_pos, )

#define get_folder1_(pfs, folder_name, block_name, place) get_folder2_(pfs, folder_name, block_name, (place).block, (place).pos)
#define get_folder1(folder_name, block_name, place) get_folder2(folder_name, block_name, (place).block, (place).pos)

#define get_folder0(folder_name, block_name) get_folder2(folder_name, block_name, f->element_place.block, f->element_place.pos)

#define get_folder get_folder0(folder, block_data)

pfs_fi pfsc_folder_iterator(pfs_eh f, int show_hidden) {
	pfs_fi result = malloc(sizeof(struct pfs_folder_iter));
	if (result == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	if (!pfsc_folder_fill_iterator(f, result, show_hidden)) {
		free(result);
		return NULL;
	}
	return result;
}

int pfsc_folder_fill_iterator(pfs_eh f, pfs_fi iter, int show_hidden) {
	get_folder
	iter->current_place.block = f->element_place.block;
	iter->current_place.pos = f->element_place.pos + sizeof(struct pfs_folder);
	iter->current_depth = 0;
	iter->current_folder_pos = f->element_place.pos;
	iter->remaining_direct_entries = folder->direct_child_count;
	iter->eh = f;
	iter->show_hidden = show_hidden;
	pfs0(f)->unget(pfs0(f), f->element_place.block);
	return 1;
}

int pfsc_folder_fill_iterator_index(pfs_eh f, pfs_fi iter, i64 index) {
	get_folder
	iter->current_place.block = f->element_place.block;
	iter->current_place.pos = f->element_place.pos + sizeof(struct pfs_folder);
	iter->current_depth = 0;
	iter->current_folder_pos = f->element_place.pos;
	iter->remaining_direct_entries = folder->direct_child_count;
	while (index >= folder->helper_index) {
		if (folder->helper_index == -1) {
			while (1) { // do not leave this loop until the index is small enugh
				if (iter->current_depth <= 0) {
					abort();
				}
				if (index < iter->remaining_direct_entries) {
					break;
				}
				i64 old_block = iter->current_place.block;
				index -= iter->remaining_direct_entries;
				iter->current_place = folder->folder_entry;
				pfs0(f)->unget(pfs0(f), old_block);
				block_data = pfs0(f)->get(pfs0(f), iter->current_place.block);
				if (!block_data) {
					return 0;
				}
				i32 *table_end = block_data + pfs0(f)->block_size - 4;
				for (i32 *table = block_data + *table_end + 4;; table += 2) {
					if (table >= table_end) {
						abort();
					}
					if (folder->folder_entry.pos < *table) {
						iter->current_folder_pos = table[-1];
						break;
					}
				}
				folder = block_data + iter->current_folder_pos;
				iter->current_depth--;
				iter->current_place.pos += sizeof(struct pfs_folder_entry);
				i64 val = iter->current_place.pos - iter->current_folder_pos
						- sizeof(struct pfs_folder);
				if (val % sizeof(struct pfs_folder_entry)) {
					abort();
				}
				iter->remaining_direct_entries = val
						/ sizeof(struct pfs_folder_entry);
			}
		} else {
			i64 old_block = iter->current_place.block;
			index -= folder->helper_index;
			iter->current_place =
					folder->entries[folder->helper_index].child_place;
			iter->current_folder_pos = iter->current_place.pos;
			iter->current_place.pos += sizeof(struct pfs_folder);
			pfs0(f)->unget(pfs0(f), old_block);
			block_data = pfs0(f)->get(pfs0(f), iter->current_place.block);
			if (!block_data) {
				return 0;
			}
		}
	}
	iter->current_place.pos += index * sizeof(struct pfs_folder_entry);
	iter->remaining_direct_entries -= index;
	pfs0(f)->unget(pfs0(f), iter->current_place.block);
	return 1;
}

int pfsc_folder_iter_next(pfs_fi fi) {
	void *cb_data = pfs0(fi->eh)->get(pfs0(fi->eh), fi->current_place.block);
	if (cb_data == NULL) {
		pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
	while (1) {
		while (fi->remaining_direct_entries <= 0) {
			if (fi->remaining_direct_entries != 0) {
				abort();
			}
			if (fi->current_depth <= 0) {
				if (fi->current_depth != 0) {
					abort();
				}
				pfs_err = PFS_ERRNO_NO_MORE_ELEMENTS;
				pfs0(fi->eh)->unget(pfs0(fi->eh), fi->current_place.block);
				return 0;
			}
			const i64 old_block = fi->current_place.block;
			struct pfs_folder *inner_folder = cb_data + fi->current_folder_pos;
			cb_data = pfs0(fi->eh)->get(pfs0(fi->eh),
					inner_folder->folder_entry.block);
			if (cb_data == NULL) {
				pfs0(fi->eh)->unget(pfs0(fi->eh), fi->current_place.block);
				return 0;
			}
			const struct pfs_place old_place = fi->current_place;
			const i32 old_folder_pos = fi->current_folder_pos;
			fi->current_place.block = inner_folder->folder_entry.block;
			fi->current_place.pos = inner_folder->folder_entry.pos
					+ sizeof(struct pfs_folder_entry);
			i32 *table_end = cb_data + pfs0(fi->eh)->block_size - 4;
			for (i32 *table = cb_data + *table_end + 4; table < table_end;
					table += 2) {
				if (inner_folder->folder_entry.pos < *table) {
					fi->current_folder_pos = table[-1];
					goto finish_search;
				}
			}
			abort();
			finish_search: ;
			i32 size = fi->current_place.pos - fi->current_folder_pos
					- sizeof(struct pfs_folder);
			i32 current_index = size / sizeof(struct pfs_folder_entry);
			if ((current_index * sizeof(struct pfs_folder_entry)) != size) {
				abort();
			}
			pfs0(fi->eh)->unget(pfs0(fi->eh),
					inner_folder->folder_entry.block);
			pfs0(fi->eh)->unget(pfs0(fi->eh), old_block);
			cb_data = pfs0(fi->eh)->get(pfs0(fi->eh), fi->current_place.block);
			if (!cb_data) {
				pfs0(fi->eh)->unget(pfs0(fi->eh), fi->current_place.block);
				fi->current_place = old_place;
				fi->current_folder_pos = old_folder_pos;
				return 0;
			}
			struct pfs_folder *folder = cb_data + fi->current_folder_pos;
			fi->remaining_direct_entries = folder->direct_child_count
					- current_index;
			fi->current_depth--;
		}
		struct pfs_folder_entry *entry = cb_data + fi->current_place.pos;
		if (!fi->show_hidden && ((entry->flags & PFS_F_HIDDEN) != 0)) {
			fi->remaining_direct_entries--;
			fi->current_place.pos += sizeof(struct pfs_folder_entry);
			continue;
		}
		if ((entry->flags & PFS_F_HELPER_FOLDER) != 0) {
			fi->current_depth++;
			fi->current_folder_pos = entry->child_place.pos;
			i64 block_num = fi->current_place.block;
			fi->current_place = (struct pfs_place ) { //
					/*			*/.block = entry->child_place.block, //
							/*	*/.pos = entry->child_place.pos
									+ sizeof(struct pfs_folder) //
					};
			pfs0(fi->eh)->unget(pfs0(fi->eh), block_num);
			cb_data = pfs0(fi->eh)->get(pfs0(fi->eh), fi->current_place.block);
			if (!cb_data) {
				return 0;
			}
			struct pfs_folder *folder = cb_data + fi->current_folder_pos;
			fi->remaining_direct_entries = folder->direct_child_count;
			continue;
		}
		struct pfs_folder *folder = cb_data + fi->current_folder_pos;
		if (folder->real_parent.block == fi->current_place.block
				|| folder->folder_entry.block == fi->current_place.block
				|| folder->real_parent.block == -1) {
			// this can not be a helper folder when they are in the same block
			fi->eh->real_parent_place.block = fi->current_place.block;
			fi->eh->real_parent_place.pos = fi->current_folder_pos;
		} else {
			void *fe_block_data = pfs0(fi->eh)->get(pfs0(fi->eh),
					folder->folder_entry.block);
			if (fe_block_data == NULL) {
				pfs0(fi->eh)->unget(pfs0(fi->eh), fi->current_place.block);
				pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
				return 0;
			}
			struct pfs_folder_entry *entry = fe_block_data
					+ folder->folder_entry.pos;
			if ((entry->flags & PFS_F_HELPER_FOLDER) != 0) {
				fi->eh->real_parent_place = folder->real_parent;
			} else {
				fi->eh->real_parent_place.block = fi->current_place.block;
				fi->eh->real_parent_place.pos = fi->current_folder_pos;
			}
			pfs0(fi->eh)->unget(pfs0(fi->eh), folder->folder_entry.block);
		}
		fi->eh->direct_parent_place.block = fi->current_place.block;
		fi->eh->direct_parent_place.pos = fi->current_folder_pos;
		i32 size = fi->current_place.pos - fi->current_folder_pos
				- sizeof(struct pfs_folder);
		fi->eh->index_in_direct_parent_list = size
				/ sizeof(struct pfs_folder_entry);
		if ((fi->eh->index_in_direct_parent_list
				* sizeof(struct pfs_folder_entry)) != size) {
			abort();
		}
		fi->eh->element_place = entry->child_place;
		fi->eh->entry_pos = fi->current_place.pos;
		pfs0(fi->eh)->unget(pfs0(fi->eh), fi->current_place.block);
		fi->current_place.pos += sizeof(struct pfs_folder_entry);
		fi->remaining_direct_entries--;
		return 1;
	}
}

static i64 count_children(bm pfs, struct pfs_place *f) {
	get_folder1_(pfs, folder, block_data, *f)
	i64 child_count = folder->direct_child_count;
	i64 result = 0L;
	for (i64 i = 0; i < child_count; i++) {
		if (folder->helper_index == i) {
			result += count_children(pfs, &folder->entries[i].child_place);
		} else {
			result++;
		}
	}
	pfs->unget(pfs, f->block);
	return result;
}

i64 pfsc_folder_child_count(pfs_eh f) {
	return count_children(pfs0(f), &f->element_place);
}

static int pfs_folder_child_from_name_impl(pfs_eh f, const char *name,
		int is_helper, ui32 neededflag) {
	const i64 old_block_num = f->element_place.block;
	const i64 name_len = strlen(name);
	get_folder
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			struct pfs_place old_place = f->direct_parent_place;
			f->direct_parent_place = f->element_place;
			f->element_place = folder->entries[i].child_place;
			int res = pfs_folder_child_from_name_impl(f, name, 1, neededflag);
			if (res != 0) {
				pfs0(f)->unget(pfs0(f), old_block_num);
				return 1;
			}
			f->element_place = f->direct_parent_place;
			f->direct_parent_place = old_place;
		}
		char *cn = block_data + folder->entries[i].name_pos;
		if (get_size_from_block_table(block_data, folder->entries[i].name_pos,
		pfs0(f)->block_size) == name_len) {
			if (memcmp(name, block_data + folder->entries[i].name_pos, name_len)
					== 0) {
				if ((folder->entries[i].flags & neededflag) == 0) {
					pfs0(f)->unget(pfs0(f), old_block_num);
					pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
					return -1;
				}
				if (is_helper) {
					f->real_parent_place = f->direct_parent_place;
				} else {
					f->real_parent_place = f->element_place;
				}
				f->direct_parent_place = f->element_place;
				f->element_place = folder->entries[i].child_place;
				f->entry_pos = ((void*) &folder->entries[i]) - block_data;
				f->index_in_direct_parent_list = i;
				if (folder->entries[i].flags & PFS_F_MOUNT) {
					f->is_mount_point = 1;
				} else {
					f->is_mount_point = 0;
				}
				pfs0(f)->unget(pfs0(f), old_block_num);
				return 1;
			}
		}
	}
	pfs0(f)->unget(pfs0(f), old_block_num);
	if (!is_helper) {
		pfs_err = PFS_ERRNO_ELEMENT_NOT_EXIST;
	}
	return 0;
}

int pfsc_folder_child_from_name(pfs_eh f, const char *name) {
	return pfs_folder_child_from_name_impl(f, name, 0, -1);
}

int pfsc_folder_folder_child_from_name(pfs_eh f, const char *name) {
	return 1 == pfs_folder_child_from_name_impl(f, name, 0, PFS_F_FOLDER);
}

int pfsc_folder_file_child_from_name(pfs_eh f, const char *name) {
	return 1 == pfs_folder_child_from_name_impl(f, name, 0, PFS_F_FILE);
}

int pfsc_folder_pipe_child_from_name(pfs_eh f, const char *name) {
	return 1 == pfs_folder_child_from_name_impl(f, name, 0, PFS_F_PIPE);
}

static inline int has_child_with_name(bm pfs, struct pfs_place place,
		const char *name, i64 name_len) {
	get_folder1_(pfs, folder, block_data, place)
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			if (folder->helper_index != i) {
				abort();
			}
			if ((folder->entries[i].flags & (PFS_F_FOLDER | PFS_F_HELPER_FOLDER))
					!= (PFS_F_FOLDER | PFS_F_HELPER_FOLDER)) {
				abort();
			}
			if (has_child_with_name(pfs, folder->entries[i].child_place, name,
					name_len)) {
				pfs->unget(pfs, place.block);
				return 1;
			}
			continue;
		}
		if (folder->helper_index == i) {
			abort();
		}
		if ((folder->entries[i].flags & (PFS_F_HELPER_FOLDER)) != 0) {
			abort();
		}
		if (get_size_from_block_table(block_data, folder->entries[i].name_pos,
				pfs->block_size) == name_len, pfs->block_size) {
			if (memcmp(name, block_data + folder->entries[i].name_pos, name_len)
					== 0) {
				pfs->unget(pfs, place.block);
				return 1;
			}
		}
	}
	pfs->unget(pfs, place.block);
	return 0;
}

static inline int add_child(pfs_eh f, pfs_eh parent,
		struct pfs_place real_parent, const char *name, i64 name_len,
		ui32 child_flags, struct pfs_folder_entry *overwrite_child_entry, ...);

static inline int delegate_create_element_to_helper(const i64 my_new_size,
		struct pfs_place real_parent, int grow_success, i32 child_flags,
		struct pfs_folder *me, struct pfs_place my_place, pfs_eh f,
		const char *name, i64 name_len, va_list vargs) {
	i64 helper_block;
	const int orig_grow_success = grow_success;
	if (me->helper_index == -1) {
		helper_block = allocate_block(pfs0(f),
		BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES);
		if (helper_block == -1L) {
			if (grow_success) {
				shrink_folder_entry(pfs0(f), my_place, my_new_size);
			}
			pfs0(f)->unget(pfs0(f), my_place.block);
			return 0;
		}
		struct pfs_folder *helper = pfs0(f)->lazy_get(pfs0(f), helper_block);
		if (helper == NULL) {
			if (grow_success) {
				shrink_folder_entry(pfs0(f), my_place, my_new_size);
			}
			pfs0(f)->unget(pfs0(f), helper_block);
			pfs0(f)->unget(pfs0(f), my_place.block);
			pfs_err = PFS_ERRNO_OUT_OF_SPACE;
			return 0;
		}
		helper->element.last_mod_time = -1L;
		helper->real_parent = real_parent;
		if (grow_success) {
			grow_success = 0;
			if (!init_block(pfs0(f), helper_block, sizeof(struct pfs_folder))) {
				// no free needed, because of b0
				pfs0(f)->unget(pfs0(f), helper_block);
				pfs0(f)->unget(pfs0(f), my_place.block);
				pfs_err = PFS_ERRNO_OUT_OF_SPACE;
				return 0;
			}
			helper->direct_child_count = 0;
			me->helper_index = me->direct_child_count++;
		} else {
			if (!init_block(pfs0(f), helper_block,
					sizeof(struct pfs_folder)
							+ sizeof(struct pfs_folder_entry))) {
				pfs0(f)->unget(pfs0(f), helper_block);
				pfs0(f)->unget(pfs0(f), my_place.block);
				pfs_err = PFS_ERRNO_OUT_OF_SPACE;
				return 0;
			}
			me->helper_index = me->direct_child_count - 1;
			helper->direct_child_count = 1;
			helper->entries[0] = me->entries[me->direct_child_count - 1];
			void *my_block_data = ((void*) me) - my_place.pos;
			char *new_helper_child_name = my_block_data
					+ helper->entries[0].name_pos;
			i32 name_len = get_size_from_block_table(my_block_data,
					helper->entries[0].name_pos, pfs0(f)->block_size);
			i32 name_pos = add_name(pfs0(f), helper_block,
					new_helper_child_name, name_len);
			if (name_pos == -1) {
				pfs0(f)->unget(pfs0(f), helper_block);
				pfs0(f)->unget(pfs0(f), my_place.block);
				pfs_err = PFS_ERRNO_OUT_OF_SPACE;
				return 0;
			}
			helper->entries[0].name_pos = name_pos;
			if ((helper->entries[0].flags & PFS_F_FOLDER) != 0) {
				get_folder3(helper_child_folder, helper_child_folder_block_data,
						helper->entries[0].child_place.block,
						helper->entries[0].child_place.pos,
						pfs0(f)->unget(pfs0(f), helper_block); pfs0(f)->unget(pfs0(f), my_place.block);)
				helper_child_folder->folder_entry.block = helper_block;
				helper_child_folder->folder_entry.pos =
						sizeof(struct pfs_folder);
				pfs0(f)->set(pfs0(f), helper->entries[0].child_place.block);
			}
		}
		helper->folder_entry.block = my_place.block;
		helper->folder_entry.pos = my_place.pos + sizeof(struct pfs_folder)
				+ (sizeof(struct pfs_folder_entry) * me->helper_index);
		helper->helper_index = -1;
		me->entries[me->helper_index].child_place.block = helper_block;
		me->entries[me->helper_index].child_place.pos = 0;
		me->entries[me->helper_index].name_pos = -1;
		me->entries[me->helper_index].create_time = -1;
		me->entries[me->helper_index].flags =
		PFS_F_FOLDER | PFS_F_HELPER_FOLDER;
	} else {
		helper_block = me->entries[me->helper_index].child_place.block;
		pfs0(f)->get(pfs0(f), helper_block);
	}
	struct pfs_element_handle eh = *f;
	f->real_parent_place = f->element_place;
	f->index_in_direct_parent_list = me->helper_index;
	f->direct_parent_place = f->element_place;
	f->entry_pos = f->direct_parent_place.pos + sizeof(struct pfs_folder)
			+ me->helper_index * sizeof(struct pfs_folder_entry);
	f->element_place = me->entries[me->helper_index].child_place;
	if (grow_success) {
		shrink_folder_entry(pfs0(f), my_place, my_new_size);
	}
	int res;
	if (child_flags == PFS_F_MOUNT) {
		ui32 mount_flags = va_arg(vargs, ui32);
		switch (mount_flags) {
		case PFS_MOUNT_FLAGS_INTERN:
		case PFS_MOUNT_FLAGS_TEMP:
			i64 bc = va_arg(vargs, i64);
			i32 bs = va_arg(vargs, i32);
			va_end(vargs);
			res = add_child(f, NULL, real_parent, name, name_len, child_flags,
			/*				*/NULL, mount_flags, bc, bs);
			break;
		case PFS_MOUNT_FLAGS_REAL_FS_FILE:
			const char *file = va_arg(vargs, const char*);
			va_end(vargs);
			res = add_child(f, NULL, real_parent, name, name_len, child_flags,
			/*				*/NULL, mount_flags, file);
			break;
		default:
			abort();
		}
	} else {
		va_end(vargs);
		res = add_child(f, NULL, real_parent, name, name_len, child_flags,
		/*				*/NULL);
	}
	if (!res) {
		*f = eh;
		if (orig_grow_success) {
			shrink_folder_entry(pfs0(f), f->element_place, my_new_size)
			me->direct_child_count--;
			if (me->helper_index >= me->direct_child_count) {
				if (me->helper_index > me->direct_child_count) {
					abort();
				}
				me->helper_index = -1;
			}
		}
	}
	pfs0(f)->set(pfs0(f), my_place.block);
	pfs0(f)->set(pfs0(f), helper_block);
	return res;
}

static inline int add_child(pfs_eh f, pfs_eh parent,
		struct pfs_place real_parent, const char *name, i64 name_len,
		ui32 child_flags, struct pfs_folder_entry *overwrite_child_entry, ...) {
	if (f == parent) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (parent != NULL) {
		*parent = *f;
	}
	get_folder0(me, my_old_block_data)
	struct pfs_place my_place = f->element_place;
	if (has_child_with_name(pfs0(f), my_place, name, name_len)) {
		pfs0(f)->unget(pfs0(f), my_place.block);
		pfs_err = PFS_ERRNO_ELEMENT_ALREADY_EXIST;
		return 0;
	}
	const i64 my_new_size = sizeof(struct pfs_folder)
			+ (sizeof(struct pfs_folder_entry) * (1 + me->direct_child_count));
	i32 name_pos;
	i32 new_pos = grow_folder_entry(f, my_new_size, real_parent);
	if (new_pos != -1) {
		if (new_pos != f->element_place.pos) {
			if (real_parent.block == my_place.block) {
				if (real_parent.pos != my_place.pos) {
					abort();
				}
				real_parent.pos = new_pos;
			}
			my_place.pos = f->element_place.pos = new_pos;
			me = my_old_block_data + new_pos;
			if (parent != NULL) {
				parent->element_place.pos = new_pos;
			}
		}
		name_pos = add_name(pfs0(f), my_place.block, name, name_len);
		if (name_pos == -1) {
			va_list vargs;
			va_start(vargs, overwrite_child_entry);
			return delegate_create_element_to_helper(my_new_size, real_parent,
					new_pos != -1, child_flags, me, my_place, f, name, name_len,
					vargs);
		}
	} else if (((me->direct_child_count) < 2) && (me->helper_index == -1)) {
		if (!allocate_new_entry(pfs0(f), &f->element_place, -1,
				sizeof(struct pfs_folder)
						+ (sizeof(struct pfs_folder_entry)
								* (me->direct_child_count + 1)))) {
			pfs0(f)->unget(pfs0(f), my_place.block);
			return 0;
		}
		if (parent != NULL) {
			*parent = *f;
		}
		struct pfs_folder const *old_me = me;
		const i64 my_old_block = my_place.block;
		const i32 my_old_pos = my_place.pos;
		my_place = f->element_place;
		if (real_parent.block == my_old_block) {
			if (real_parent.pos != my_old_pos) {
				abort();
			}
			real_parent = my_place;
		}
		void *my_new_block_data = pfs0(f)->get(pfs0(f), my_place.block);
		if (my_new_block_data == NULL) {
			pfs0(f)->set(pfs0(f), my_old_block);
			f->element_place.block = my_old_block;
			f->element_place.pos = my_old_pos;
			if (parent != NULL) {
				*parent = *f;
			}
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			return 0;
		}
		me = my_new_block_data + my_place.pos;
		*me = *old_me;
		i64 entry_block = old_me->folder_entry.block;
		if (entry_block == -1) {
			entry_block = 0;
		}
		void *my_entry_block = pfs0(f)->get(pfs0(f), entry_block);
		if (my_entry_block == NULL) {
			pfs0(f)->unget(pfs0(f), my_place.block);
			pfs0(f)->set(pfs0(f), my_old_block);
			f->element_place.block = my_old_block;
			f->element_place.pos = my_old_pos;
			if (parent != NULL) {
				*parent = *f;
			}
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			return 0;
		}
		if (old_me->folder_entry.block != f->direct_parent_place.block) {
			abort();
		}
		name_pos = add_name(pfs0(f), my_place.block, name, name_len);
		if (name_pos == -1) {
			free_block(pfs0(f), f->element_place.block);
			pfs0(f)->unget(pfs0(f), my_place.block);
			pfs0(f)->set(pfs0(f), my_old_block);
			f->element_place.block = my_old_block;
			f->element_place.pos = my_old_pos;
			if (parent != NULL) {
				*parent = *f;
			}
			pfs_err = PFS_ERRNO_OUT_OF_SPACE;
			return 0;
		}
		if (old_me->folder_entry.block == -1) {
			if (old_me->folder_entry.pos != -1) {
				abort();
			}
			struct pfs_b0 *b0 = my_entry_block;
			b0->root = my_place;
			pfs0(f)->set(pfs0(f), 0L);
		} else {
			struct pfs_folder_entry *my_entry = my_entry_block
					+ old_me->folder_entry.pos;
			my_entry->child_place = my_place;
			pfs0(f)->set(pfs0(f), old_me->folder_entry.block);
		}
		remove_from_block_table(pfs0(f), my_old_block, my_old_pos);
		if (pfs0(f)->block_size - 4
				== *(i32*) (my_old_block_data + pfs0(f)->block_size - 4)) {
			pfs0(f)->unget(pfs0(f), my_old_block); // no need to save a dead block
			free_block(pfs0(f), my_old_block);
		} else {
			pfs0(f)->set(pfs0(f), my_old_block);
		}
	} else {
		va_list vargs;
		va_start(vargs, overwrite_child_entry);
		return delegate_create_element_to_helper(my_new_size, real_parent,
				new_pos != -1, child_flags, me, my_place, f, name, name_len,
				vargs);
	}
	const i32 child_index = me->direct_child_count;
	if (overwrite_child_entry != NULL) {
		me->entries[child_index] = *overwrite_child_entry;
		pfs0(f)->set(pfs0(f), my_place.block);
	} else {
		size_t len;
		switch (child_flags) {
		case PFS_F_FOLDER:
			len = sizeof(struct pfs_folder);
			break;
		case PFS_F_FILE:
			len = sizeof(struct pfs_file);
			break;
		case PFS_F_PIPE:
			len = sizeof(struct pfs_pipe);
			break;
		case PFS_F_MOUNT:
			va_list vargs;
			va_start(vargs, overwrite_child_entry);
			ui32 mount_flags = va_arg(vargs, ui32);
			switch (mount_flags) {
			case PFS_MOUNT_FLAGS_INTERN:
				len = sizeof(struct pfs_mount_point_intern);
				break;
			case PFS_MOUNT_FLAGS_TEMP:
				len = sizeof(struct pfs_mount_point_tmp);
				break;
			case PFS_MOUNT_FLAGS_REAL_FS_FILE:
				len = sizeof(struct pfs_mount_point_real_fs_file)
						+ strlen(va_arg(vargs, const char*));
				break;
			default:
				abort();
			}
			va_end(vargs);
			break;
		default:
			abort();
		}
		if (!allocate_new_entry(pfs0(f), &me->entries[child_index].child_place,
				my_place.block, len)) {
			if (new_pos != -1) {
				shrink_folder_entry(pfs0(f), my_place, my_new_size);
			}
			pfs0(f)->unget(pfs0(f), my_place.block);
			return 0;
		}
		i64 now = time(NULL);
		struct pfs_place child_place = me->entries[child_index].child_place;
		me->entries[child_index].create_time = now;
		me->entries[child_index].flags = child_flags;
		me->entries[child_index].name_pos = name_pos;
		me->direct_child_count++;
		void *child_block_data = pfs0(f)->get(pfs0(f), child_place.block);
		pfs0(f)->set(pfs0(f), my_place.block);
		if (child_block_data == NULL) {
			child_block_data = pfs0(f)->get(pfs0(f), child_place.block);
			if (child_block_data == NULL) {
				void *my_block_data = pfs0(f)->get(pfs0(f), my_place.block);
				if (my_block_data == NULL) {
					abort();
				}
				me = my_block_data + my_place.pos;
				me->direct_child_count--;
				shrink_folder_entry(pfs0(f), my_place, my_new_size)
				remove_from_block_table(pfs0(f), my_place.block,
						me->entries[child_index].name_pos);
				pfs0(f)->set(pfs0(f), my_place.block);
				pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
				return 0;
			}
		}
		void *child = child_block_data + child_place.pos;
		f->real_parent_place = real_parent;
		f->index_in_direct_parent_list = child_index;
		f->direct_parent_place = my_place;
		f->entry_pos = my_place.pos + sizeof(struct pfs_folder)
				+ (child_index * sizeof(struct pfs_folder_entry));
		f->element_place = child_place;
		struct pfs_element *e = child;
		e->last_mod_time = now;
		switch (child_flags) {
		case PFS_F_FOLDER: {
			struct pfs_folder *cf = child;
			cf->real_parent = real_parent;
			cf->direct_child_count = 0;
			cf->folder_entry.block = f->direct_parent_place.block;
			cf->folder_entry.pos = f->entry_pos;
			cf->helper_index = -1;
			break;
		}
		case PFS_F_FILE: {
			struct pfs_file *cf = child;
			cf->file.file_length = 0;
			cf->file.first_block = -1;
			break;
		}
		case PFS_F_PIPE: {
			struct pfs_pipe *cf = child;
			cf->pipe.file.file_length = 0;
			cf->pipe.file.first_block = -1;
			cf->pipe.start_offset = 0;
			break;
		}
		case PFS_F_MOUNT: {
			struct pfs_mount_point *mp = child;
			va_list vargs;
			va_start(vargs, overwrite_child_entry);
			ui32 mount_flags = va_arg(vargs, ui32);
			mp->flags = mount_flags;
			switch (mount_flags) {
			case PFS_MOUNT_FLAGS_INTERN: {
				struct pfs_mount_point_intern *im = child;
				im->block_count = va_arg(vargs, i64);
				im->block_size = va_arg(vargs, i32);
				im->file.file_length = 0;
				im->file.first_block = -1;
				break;
			}
			case PFS_MOUNT_FLAGS_TEMP: {
				struct pfs_mount_point_tmp *tm = child;
				tm->block_count = va_arg(vargs, i64);
				tm->block_size = va_arg(vargs, i32);
				break;
			}
			case PFS_MOUNT_FLAGS_REAL_FS_FILE: {
				struct pfs_mount_point_real_fs_file *rfsm = child;
				i32 rfs_path_len = len
						- sizeof(struct pfs_mount_point_real_fs_file);
				memcpy(rfsm->path, va_arg(vargs, const char*), len);
				break;
			}
			default:
				abort();
			}
			va_end(vargs);
			break;
		}
		default:
			abort();
		}
		pfs0(f)->set(pfs0(f), f->element_place.block);
	}
	return 1;
}

int pfsc_folder_create_folder(pfs_eh f, pfs_eh parent, const char *name) {
	return add_child(f, parent, f->element_place, name, strlen(name),
	PFS_F_FOLDER, NULL);
}

int pfsc_folder_create_file(pfs_eh f, pfs_eh parent, const char *name) {
	return add_child(f, parent, f->element_place, name, strlen(name),
	PFS_F_FILE, NULL);
}

int pfsc_folder_create_pipe(pfs_eh f, pfs_eh parent, const char *name) {
	return add_child(f, parent, f->element_place, name, strlen(name),
	PFS_F_PIPE, NULL);
}

int pfsc_folder_create_mount_intern(pfs_eh f, pfs_eh parent, const char *name,
		i64 block_count, i32 block_size) {
	return add_child(f, parent, f->element_place, name, strlen(name),
	PFS_F_MOUNT, NULL, PFS_MOUNT_FLAGS_INTERN, block_count, block_size);
}

int pfsc_folder_create_mount_temp(pfs_eh f, pfs_eh parent, const char *name,
		i64 block_count, i32 block_size) {
	return add_child(f, parent, f->element_place, name, strlen(name),
	PFS_F_MOUNT, NULL, PFS_MOUNT_FLAGS_TEMP, block_count, block_size);
}

int pfsc_folder_create_mount_rfs_file(pfs_eh f, pfs_eh parent, const char *name,
		const char *file) {
	return add_child(f, parent, f->element_place, name, strlen(name),
	PFS_F_MOUNT, NULL, PFS_MOUNT_FLAGS_REAL_FS_FILE, file);
}

int pfsc_element_get_parent(pfs_eh e) {
	if (e->real_parent_place.block == -1) {
		pfs_err = PFS_ERRNO_ROOT_FOLDER;
		return 0;
	}
	get_folder1_(pfs0(e), parent, block_data, (e->real_parent_place))
	struct pfs_place old_place = e->element_place;
	i32 entry_pos = e->entry_pos;
	i64 direct_parent_block = e->direct_parent_place.block;
	e->element_place = e->real_parent_place;
	e->entry_pos = parent->folder_entry.pos;
	e->direct_parent_place.block = parent->folder_entry.block;
	e->real_parent_place = parent->real_parent;
	if (parent->real_parent.block == -1L) { // parent is root
		e->direct_parent_place.pos = -1;
		e->index_in_direct_parent_list = -1;
		pfs0(e)->unget(pfs0(e), e->element_place.block);
		return 1;
	}
	block_data = pfs0(e)->get(pfs0(e), parent->folder_entry.block);
	pfs0(e)->unget(pfs0(e), e->element_place.block);
	if (block_data == NULL) {
		block_data = pfs0(e)->get(pfs0(e), parent->folder_entry.block);
		if (block_data == NULL) {
			e->real_parent_place = e->element_place;
			e->direct_parent_place.block = direct_parent_block;
			e->entry_pos = entry_pos;
			e->element_place = old_place;
			return 0;
		}
	}
	i32 *table_end = block_data + pfs0(e)->block_size - 4;
	for (i32 *table = block_data + *table_end; 1; table += 2) {
		if (table >= table_end) {
			abort();
		}
		if (parent->folder_entry.pos > table[1]) {
			continue;
		}
		if (parent->folder_entry.pos < table[0]) {
			abort();
		}
		e->direct_parent_place.pos = *table;
		pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
		i64 zw = e->entry_pos - e->direct_parent_place.pos
				- sizeof(struct pfs_folder);
		e->index_in_direct_parent_list = zw / sizeof(struct pfs_folder_entry);
		if ((zw % sizeof(struct pfs_folder_entry)) != 0) {
			abort();
		}
		return 1;
	}
}

static void remove_from_parent(const struct pfs_element_handle *e,
		struct pfs_folder *direct_parent) {
	size_t cpyLen = (direct_parent->direct_child_count
			- e->index_in_direct_parent_list - 1)
			* sizeof(struct pfs_folder_entry);
	memmove(direct_parent->entries + e->index_in_direct_parent_list,
			direct_parent->entries + e->index_in_direct_parent_list + 1,
			cpyLen);
	direct_parent->direct_child_count--;
	if (direct_parent->helper_index > e->index_in_direct_parent_list) {
		direct_parent->helper_index--;
	} else if (direct_parent->helper_index == e->index_in_direct_parent_list) {
		abort();
	}
	shrink_folder_entry(pfs0(e), e->direct_parent_place,
			sizeof(struct pfs_folder)
					+ (sizeof(struct pfs_folder_entry)
							* (1 + direct_parent->direct_child_count)));
}

static int del_helper(bm pfs, struct pfs_place h) {
	struct pfs_folder *helper = pfs->get(pfs, h.block) + h.pos;
	if (helper->direct_child_count > 0) {
		if ((helper->direct_child_count > 1) || (helper->helper_index == -1)) {
			pfs->unget(pfs, h.block);
			pfs_err = PFS_ERRNO_FOLDER_NOT_EMPTY;
			return 0;
		}
		if (!del_helper(pfs, helper->entries[0].child_place)) {
			pfs->unget(pfs, h.block);
			return 0;
		}
		helper->helper_index = -1; // just to make sure
		helper->direct_child_count = 0;
	}
	remove_from_block_table(pfs, h.block, h.pos);
	pfs->set(pfs, h.block);
	return 1;
}

static inline i64 get_index_from_parent(pfs_eh e) {
	get_folder1_(pfs0(e), folder, block_data, e->real_parent_place);
	struct pfs_place cur_place = e->real_parent_place;
	i64 index = 0;
	while (cur_place.block != e->direct_parent_place.block) {
		index += folder->helper_index;
		i64 old_block = cur_place.block;
		cur_place = folder->entries[folder->helper_index].child_place;
		pfs0(e)->unget(pfs0(e), old_block);
		block_data = pfs0(e)->get(pfs0(e), cur_place.block);
		if (!block_data) {
			return -1;
		}
		folder = block_data + cur_place.pos;
	}
	pfs0(e)->unget(pfs0(e), cur_place.block);
	return index + e->index_in_direct_parent_list;
}

int pfsc_element_delete(pfs_eh e, i64 *former_index, pfs_eh old_parent) {
	get_folder1_(pfs0(e), direct_parent, direct_parent_block,
			e->direct_parent_place)
	get_any(pfs0(e), pfs_element, element, element_block,
			e->element_place.block, e->element_place.pos,
			pfs0(e)->unget(pfs0(e), e->direct_parent_place.block) ;)
	if (former_index) {
		i64 index = get_index_from_parent(e);
		if (index == -1) {
			pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
			pfs0(e)->unget(pfs0(e), e->element_place.block);
			return 0;
		}
		*former_index = index;
	}
	if (old_parent) {
		*old_parent = *e;
		if (!pfsc_element_get_parent(old_parent)) {
			pfs0(e)->unget(pfs0(e), e->direct_parent_place.block);
			pfs0(e)->unget(pfs0(e), e->element_place.block);
			return 0;
		}
	}
	if ((direct_parent->entries[e->index_in_direct_parent_list].flags
			& PFS_F_FOLDER) != 0) {
		struct pfs_folder *folder = (struct pfs_folder*) element;
		if (folder->direct_child_count > 0) {
			if ((folder->direct_child_count > 1)
					|| (folder->helper_index == -1)) {
				pfs_err = PFS_ERRNO_FOLDER_NOT_EMPTY;
				pfs0(e)->unget(pfs0(e), e->element_place.block);
				return 0;
			} else if (!del_helper(pfs0(e), folder->entries[0].child_place)) {
				pfs0(e)->unget(pfs0(e), e->element_place.block);
				return 0;
			}
			folder->direct_child_count = 0;
			folder->helper_index = -1;
		}
	} else if ((direct_parent->entries[e->index_in_direct_parent_list].flags
			& (PFS_F_FILE | PFS_F_PIPE)) != 0) {
		pfsc_file_truncate(e, 0L);
	} else {
		abort();
	}
	remove_from_parent(e, direct_parent);
	pfs0(e)->set(pfs0(e), e->direct_parent_place.block);
	pfs0(e)->set(pfs0(e), e->element_place.block);
	return 1;
}

static int move_and_set_parent_impl(pfs_eh e, pfs_eh new_parent, char *name,
		i64 *former_index, pfs_eh old_parent) {
	pfs_err = PFS_ERRNO_NONE;
	if (e->real_parent_place.block == -1) {
		pfs_err = PFS_ERRNO_ROOT_FOLDER;
		return 0;
	}
	if (e->fs_data != new_parent->fs_data) {
		pfs_err = PFS_ERRNO_DIFFERENT_FILE_SYSTEMS;
		return 0;
	}
	struct pfs_element_handle moh = *e;
	get_any(pfs0(e), any, old_me, my_old_block, moh.element_place.block,
			moh.element_place.pos,)
	get_folder3_(pfs0(e), old_parent_folder, old_parent_block_data,
			moh.direct_parent_place.block, moh.direct_parent_place.pos,
			pfs0(e)->unget(pfs0(e), moh.element_place.block) ;)
	struct pfs_folder_entry *my_old_entry = old_parent_block_data
			+ moh.entry_pos;
	if (former_index) {
		i64 index = get_index_from_parent(e);
		if (index == -1) {
			pfs0(e)->unget(pfs0(e), moh.element_place.block);
			pfs0(e)->unget(pfs0(e), moh.direct_parent_place.block);
			return 0;
		}
		*former_index = index;
	}
	if (old_parent) {
		*old_parent = moh;
		if (!pfsc_element_get_parent(old_parent)) {
			return 0;
		}
	}
	if (my_old_entry->flags & PFS_F_FOLDER) {
		for (struct pfs_element_handle eh = *new_parent; 1;) {
			if ((moh.element_place.block == eh.element_place.block)
					&& (moh.element_place.pos == eh.element_place.pos)) {
				pfs0(e)->unget(pfs0(e), moh.element_place.block);
				pfs0(e)->unget(pfs0(e), moh.direct_parent_place.block);
				pfs_err = PFS_ERRNO_PARENT_IS_CHILD;
				return 0;
			}
			if (eh.direct_parent_place.block == -1L) {
				break;
			}
			if (!pfsc_element_get_parent(&eh)) {
				pfs0(e)->unget(pfs0(e), moh.element_place.block);
				pfs0(e)->unget(pfs0(e), moh.direct_parent_place.block);
				return 0;
			}
		}
	}
	const i64 new_parent_old_block = new_parent->element_place.block;
	get_folder3_(pfs0(e), new_parent_folder, new_parent_block_data,
			new_parent_old_block, new_parent->element_place.pos,
			pfs0(e)->unget(pfs0(e), moh.direct_parent_place.block); pfs0(e)->unget(pfs0(e), moh.element_place.block);)
	*e = *new_parent;
	i32 name_len;
	if (name == NULL) {
		name = old_parent_block_data + my_old_entry->name_pos;
		name_len = get_size_from_block_table(old_parent_block_data,
				my_old_entry->name_pos, pfs0(e)->block_size);
	} else {
		name_len = strlen(name);
	}
	if (!add_child(e, new_parent, new_parent->element_place, name, name_len, -1,
			my_old_entry)) {
		pfs0(e)->unget(pfs0(e), moh.direct_parent_place.block);
		pfs0(e)->unget(pfs0(e), moh.element_place.block);
		pfs0(e)->unget(pfs0(e), new_parent_old_block);
		return 0; // pfs_err has already been set
	}
	remove_from_parent(&moh, old_parent_folder);
	pfs0(e)->set(pfs0(e), moh.direct_parent_place.block);
	pfs0(e)->set(pfs0(e), moh.element_place.block);
	pfs0(e)->set(pfs0(e), new_parent_old_block);
	return 1;
}

int pfsc_element_set_parent(pfs_eh e, pfs_eh new_parent, i64 *former_index,
		pfs_eh old_parent) {
	return move_and_set_parent_impl(e, new_parent, NULL, former_index,
			old_parent);
}

int pfsc_element_move(pfs_eh e, pfs_eh new_parent, char *name,
		i64 *former_index, pfs_eh old_parent) {
	if (name == NULL) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	return move_and_set_parent_impl(e, new_parent, name, former_index,
			old_parent);
}
