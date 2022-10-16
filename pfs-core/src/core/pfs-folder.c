/*
 * pfs-folder.c
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#include "pfs-folder.h"

#define get_any(struct_name, any_name, block_name, place_block, place_pos, error) \
	ensure_block_is_entry(place_block); \
	void* block_name = pfs->get(pfs, place_block); \
	if (block_name == NULL) { \
		error \
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
		return 0; \
	} \
	struct struct_name *any_name = block_name + place_pos;

#define get_folder3(folder_name, block_name, place_block, place_pos, error) get_any(pfs_folder, folder_name, block_name, place_block, place_pos, error)

#define get_folder2(folder_name, block_name, place_block, place_pos) get_folder3(folder_name, block_name, place_block, place_pos, ) \

#define get_folder1(folder_name, block_name, place) get_folder2(folder_name, block_name, (place).block, (place).pos)

#define get_folder0(folder_name, block_name) get_folder2(folder_name, block_name, f->element_place.block, f->element_place.pos)

#define get_folder get_folder0(folder, block_data)

static inline int pfs_folder_iter_impl(pfs_eh f, pfs_fi iter, int show_hidden) {
	get_folder0(folder, block_data)
	iter->current_place.block = f->element_place.block;
	iter->current_place.pos = f->element_place.pos + sizeof(struct pfs_folder);
	iter->current_depth = 0;
	iter->current_folder_pos = f->element_place.pos;
	iter->remaining_direct_entries = folder->direct_child_count;
	iter->eh = f;
	iter->show_hidden = show_hidden;
	pfs->unget(pfs, f->element_place.block);
	return 1;
}

pfs_fi pfsc_folder_iterator(pfs_eh f, int show_hidden) {
	pfs_fi result = malloc(sizeof(struct pfs_folder_iter));
	if (result == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	if (!pfs_folder_iter_impl(f, result, show_hidden)) {
		free(result);
		return NULL;
	}
	return result;
}

int pfsc_folder_fill_iterator(pfs_eh f, pfs_fi iter, int show_hidden) {
	return pfs_folder_iter_impl(f, iter, show_hidden);
}

int pfsc_folder_iter_next(pfs_fi fi) {
	void *cb_data = pfs->get(pfs, fi->current_place.block);
	if (cb_data == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
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
				pfs_errno = PFS_ERRNO_NO_MORE_ELEMNETS;
				pfs->unget(pfs, fi->current_place.block);
				return 0;
			}
			const i64 old_block = fi->current_place.block;
			struct pfs_folder *folder = cb_data + fi->current_folder_pos;
			cb_data = pfs->get(pfs, folder->folder_entry.block);
			if (cb_data == NULL) {
				pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
				pfs->unget(pfs, fi->current_place.block);
				return 0;
			}
			fi->current_place.block = folder->folder_entry.block;
			fi->current_place.pos = folder->folder_entry.pos + sizeof(struct pfs_folder_entry);
			i32 *table_end = cb_data + pfs->block_size - 4;
			for (i32 *table = cb_data + *table_end + 4; table < table_end; table += 2) {
				if (folder->folder_entry.pos < *table) {
					fi->current_folder_pos = *table;
					goto finish_search;
				}
			}
			abort();
			finish_search: ;
			i32 size = fi->current_place.pos - fi->current_folder_pos - sizeof(struct pfs_folder);
			i32 current_index = size / sizeof(struct pfs_folder_entry);
			if ((current_index * sizeof(struct pfs_folder_entry)) != size) {
				abort();
			}
			fi->remaining_direct_entries = folder->direct_child_count - current_index;
			fi->current_depth--;
			pfs->unget(pfs, old_block);
		}
		struct pfs_folder_entry *entry = cb_data + fi->current_place.pos;
		if (!fi->show_hidden && ((entry->flags & PFS_F_HIDDEN) != 0)) {
			fi->remaining_direct_entries--;
			fi->current_place.pos += sizeof(struct pfs_folder_entry);
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
			void *fe_block_data = pfs->get(pfs, folder->folder_entry.block);
			if (fe_block_data == NULL) {
				pfs->unget(pfs, fi->current_place.block);
				pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
				return 0;
			}
			struct pfs_folder_entry *entry = fe_block_data + folder->folder_entry.pos;
			if ((entry->flags & PFS_F_HELPER_FOLDER) != 0) {
				fi->eh->real_parent_place = folder->real_parent;
			} else {
				fi->eh->real_parent_place.block = fi->current_place.block;
				fi->eh->real_parent_place.pos = fi->current_folder_pos;
			}
			pfs->unget(pfs, folder->folder_entry.block);
		}
		fi->eh->direct_parent_place.block = fi->current_place.block;
		fi->eh->direct_parent_place.pos = fi->current_folder_pos;
		i32 size = fi->current_place.pos - fi->current_folder_pos - sizeof(struct pfs_folder);
		fi->eh->index_in_direct_parent_list = size / sizeof(struct pfs_folder_entry);
		if ((fi->eh->index_in_direct_parent_list * sizeof(struct pfs_folder_entry)) != size) {
			abort();
		}
		fi->eh->element_place = entry->child_place;
		fi->eh->entry_pos = fi->current_place.pos;
		pfs->unget(pfs, fi->current_place.block);
		fi->current_place.pos += sizeof(struct pfs_folder_entry);
		fi->remaining_direct_entries--;
		return 1;
	}
}

static i64 count_children(struct pfs_place *f) {
	get_folder1(folder, block_data, *f)
	i64 child_count = folder->direct_child_count;
	i64 result = 0L;
	for (i64 i = 0; i < child_count; i++) {
		if (folder->entries[i].name_pos != -1) {
			result++;
		} else {
			result += count_children(&folder->entries[i].child_place);
		}
	}
	pfs->unget(pfs, f->block);
	return result;
}

i64 pfsc_folder_child_count(pfs_eh f) {
	return count_children(&f->element_place);
}

static int pfs_folder_child_from_name_impl(pfs_eh f, const char *name, int is_helper, ui32 neededflag) {
	const i64 old_block_num = f->element_place.block;
	const i64 name_len = strlen(name);
	get_folder
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			int res = pfs_folder_child_from_name_impl(f, name, 1, neededflag);
			if (res != 0) {
				pfs->unget(pfs, old_block_num);
				return res;
			}
		}
		char *cn = block_data + folder->entries[i].name_pos;
		if (get_size_from_block_table(block_data, folder->entries[i].name_pos) == name_len) {
			if (memcmp(name, block_data + folder->entries[i].name_pos, name_len) == 0) {
				if ((folder->entries[i].flags & neededflag) == 0) {
					pfs->unget(pfs, old_block_num);
					pfs_errno = PFS_ERRNO_ELEMENT_WRONG_TYPE;
					return -1;
				}
				f->element_place.block = folder->entries[i].child_place.block;
				f->element_place.pos = folder->entries[i].child_place.pos;
				pfs->unget(pfs, old_block_num);
				return 1;
			}
		}
	}
	pfs->unget(pfs, old_block_num);
	if (!is_helper) {
		pfs_errno = PFS_ERRNO_ELEMENT_NOT_EXIST;
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

static inline int has_child_with_name(struct pfs_place place, const char *name, i64 name_len) {
	get_folder1(folder, block_data, place)
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			if (folder->helper_index != i) {
				abort();
			}
			if ((folder->entries[i].flags & (PFS_F_FOLDER | PFS_F_HELPER_FOLDER))
			        != (PFS_F_FOLDER | PFS_F_HELPER_FOLDER)) {
				abort();
			}
			if (has_child_with_name(folder->entries[i].child_place, name, name_len)) {
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
		if (get_size_from_block_table(block_data, folder->entries[i].name_pos) == name_len) {
			if (memcmp(name, block_data + folder->entries[i].name_pos, name_len) == 0) {
				pfs->unget(pfs, place.block);
				return 1;
			}
		}
	}
	pfs->unget(pfs, place.block);
	return 0;
}

static inline int add_child(pfs_eh f, pfs_eh parent, struct pfs_place real_parent, const char *name,
        i64 name_len, ui32 child_flags, struct pfs_folder_entry *overwrite_child_entry);

static inline int delegate_create_element_to_helper(const i64 my_new_size,
        struct pfs_place real_parent, int grow_success, i32 child_flags, struct pfs_folder *me,
        struct pfs_place my_place, pfs_eh f, const char *name, i64 name_len) {
	i64 helper_block;
	const int orig_grow_success = grow_success;
	if (me->helper_index == -1) {
		helper_block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES);
		if (helper_block == -1L) {
			if (grow_success) {
				shrink_folder_entry(my_place, my_new_size);
			}
			pfs->unget(pfs, my_place.block);
			return 0;
		}
		struct pfs_folder *helper = pfs->get(pfs, helper_block);
		if (helper == NULL) {
			if (grow_success) {
				shrink_folder_entry(my_place, my_new_size);
			}
			free_block(helper_block);
			pfs->unget(pfs, helper_block);
			pfs->unget(pfs, my_place.block);
			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
			return 0;
		}
		helper->element.last_mod_time = -1L;
		helper->real_parent = real_parent;
		if (grow_success) {
			grow_success = 0;
			if (!init_block(helper_block, sizeof(struct pfs_folder))) {
				free_block(helper_block);
				pfs->unget(pfs, helper_block);
				pfs->unget(pfs, my_place.block);
				pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
				return 0;
			}
			helper->direct_child_count = 0;
			me->helper_index = me->direct_child_count++;
		} else {
			if (!init_block(helper_block,
			        sizeof(struct pfs_folder) + sizeof(struct pfs_folder_entry))) {
				free_block(helper_block);
				pfs->unget(pfs, helper_block);
				pfs->unget(pfs, my_place.block);
				pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
				return 0;
			}
			me->helper_index = me->direct_child_count - 1;
			helper->direct_child_count = 1;
			helper->entries[0] = me->entries[me->direct_child_count - 1];
			void *my_block_data = ((void*) me) - my_place.pos;
			char *new_helper_child_name = my_block_data + helper->entries[0].name_pos;
			i32 name_len = get_size_from_block_table(my_block_data, helper->entries[0].name_pos);
			i32 name_pos = add_name(helper_block, new_helper_child_name, name_len);
			if (name_pos == -1) {
				free_block(helper_block);
				pfs->unget(pfs, helper_block);
				pfs->unget(pfs, my_place.block);
				pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
				return 0;
			}
			helper->entries[0].name_pos = name_pos;
			if ((helper->entries[0].flags & PFS_F_FOLDER) != 0) {
				get_folder3(helper_child_folder, helper_child_folder_block_data,
				        helper->entries[0].child_place.block, helper->entries[0].child_place.pos,
				        pfs->unget(pfs, helper_block); pfs->unget(pfs, my_place.block);)
				helper_child_folder->folder_entry.block = helper_block;
				helper_child_folder->folder_entry.pos = sizeof(struct pfs_folder);
				pfs->set(pfs, helper->entries[0].child_place.block);
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
		me->entries[me->helper_index].flags = PFS_F_FOLDER | PFS_F_HELPER_FOLDER;
	} else {
		helper_block = me->entries[me->helper_index].child_place.block;
		pfs->get(pfs, helper_block);
	}
	struct pfs_element_handle eh = *f;
	f->real_parent_place = f->element_place;
	f->index_in_direct_parent_list = me->helper_index;
	f->direct_parent_place = f->element_place;
	f->entry_pos = f->direct_parent_place.pos + sizeof(struct pfs_folder)
	        + me->helper_index * sizeof(struct pfs_folder_entry);
	f->element_place = me->entries[me->helper_index].child_place;
	if (grow_success) {
		shrink_folder_entry(my_place, my_new_size);
	}
	int res = add_child(f, NULL, real_parent, name, name_len, child_flags, NULL);
	if (!res) {
		*f = eh;
		if (orig_grow_success) {
			shrink_folder_entry(f->element_place, my_new_size)
			me->direct_child_count--;
			if (me->helper_index >= me->direct_child_count) {
				if (me->helper_index > me->direct_child_count) {
					abort();
				}
				me->helper_index = -1;
			}
		}
	}
	pfs->set(pfs, my_place.block);
	pfs->set(pfs, helper_block);
	return res;
}

static inline int add_child(pfs_eh f, pfs_eh parent, struct pfs_place real_parent, const char *name,
        i64 name_len, ui32 child_flags, struct pfs_folder_entry *overwrite_child_entry) {
	if (f == parent) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (parent != NULL) {
		*parent = *f;
	}
	get_folder0(me, my_old_block_data)
	struct pfs_place my_place = f->element_place;
	if (has_child_with_name(my_place, name, name_len)) {
		pfs->unget(pfs, my_place.block);
		pfs_errno = PFS_ERRNO_ELEMENT_ALREADY_EXIST;
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
		name_pos = add_name(my_place.block, name, name_len);
		if (name_pos == -1) {
			return delegate_create_element_to_helper(my_new_size, real_parent, new_pos != -1,
			        child_flags, me, my_place, f, name, name_len);
		}
	} else if (((me->direct_child_count) < 2) && (me->helper_index == -1)) {
		if (!allocate_new_entry(&f->element_place, -1,
		        sizeof(struct pfs_folder)
		                + (sizeof(struct pfs_folder_entry) * (me->direct_child_count + 1)))) {
			pfs->unget(pfs, my_place.block);
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
		void *my_new_block_data = pfs->get(pfs, my_place.block);
		if (my_new_block_data == NULL) {
			pfs->set(pfs, my_old_block);
			f->element_place.block = my_old_block;
			f->element_place.pos = my_old_pos;
			if (parent != NULL) {
				*parent = *f;
			}
			pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
			return 0;
		}
		me = my_new_block_data + my_place.pos;
		*me = *old_me;
		i64 entry_block = old_me->folder_entry.block;
		if (entry_block == -1) {
			entry_block = 0;
		}
		void *my_entry_block = pfs->get(pfs, entry_block);
		if (my_entry_block == NULL) {
			pfs->unget(pfs, my_place.block);
			pfs->set(pfs, my_old_block);
			f->element_place.block = my_old_block;
			f->element_place.pos = my_old_pos;
			if (parent != NULL) {
				*parent = *f;
			}
			pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
			return 0;
		}
		if (old_me->folder_entry.block != f->direct_parent_place.block) {
			abort();
		}
		name_pos = add_name(my_place.block, name, name_len);
		if (name_pos == -1) {
			free_block(f->element_place.block);
			pfs->unget(pfs, my_place.block);
			pfs->set(pfs, my_old_block);
			f->element_place.block = my_old_block;
			f->element_place.pos = my_old_pos;
			if (parent != NULL) {
				*parent = *f;
			}
			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
			return 0;
		}
		if (old_me->folder_entry.block == -1) {
			if (old_me->folder_entry.pos != -1) {
				abort();
			}
			struct pfs_b0 *b0 = my_entry_block;
			b0->root = my_place;
			pfs->set(pfs, 0L);
		} else {
			struct pfs_folder_entry *my_entry = my_entry_block + old_me->folder_entry.pos;
			my_entry->child_place = my_place;
			pfs->set(pfs, old_me->folder_entry.block);
		}
		remove_from_block_table(my_old_block, my_old_pos);
		if (pfs->block_size - 4 == *(i32*) (my_old_block_data + pfs->block_size - 4)) {
			pfs->unget(pfs, my_old_block); // no need to save a dead block
			free_block(my_old_block);
		} else {
			pfs->set(pfs, my_old_block);
		}
	} else {
		return delegate_create_element_to_helper(my_new_size, real_parent, new_pos != -1,
		        child_flags, me, my_place, f, name, name_len);
	}
	const i32 child_index = me->direct_child_count;
	if (overwrite_child_entry != NULL) {
		me->entries[child_index] = *overwrite_child_entry;
		pfs->set(pfs, my_place.block);
	} else {
		if (!allocate_new_entry(&me->entries[child_index].child_place, my_place.block,
		        child_flags == PFS_F_FOLDER ? sizeof(struct pfs_folder) :
		                (child_flags == PFS_F_FILE ? sizeof(struct pfs_file) :
		                        sizeof(struct pfs_pipe)))) {
			if (new_pos != -1) {
				shrink_folder_entry(my_place, my_new_size);
			}
			pfs->unget(pfs, my_place.block);
			return 0;
		}
		i64 now = time(NULL);
		struct pfs_place child_place = me->entries[child_index].child_place;
		me->entries[child_index].create_time = now;
		me->entries[child_index].flags = child_flags;
		me->entries[child_index].name_pos = name_pos;
		me->direct_child_count++;
		void *child_block_data = pfs->get(pfs, child_place.block);
		pfs->set(pfs, my_place.block);
		if (child_block_data == NULL) {
			child_block_data = pfs->get(pfs, child_place.block);
			if (child_block_data == NULL) {
				void *my_block_data = pfs->get(pfs, my_place.block);
				if (my_block_data == NULL) {
					abort();
				}
				me = my_block_data + my_place.pos;
				me->direct_child_count--;
				shrink_folder_entry(my_place, my_new_size)
				remove_from_block_table(my_place.block, me->entries[child_index].name_pos);
				pfs->set(pfs, my_place.block);
				pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
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
		if (child_flags == PFS_F_FOLDER) {
			struct pfs_folder *cf = child;
			cf->element.last_mod_time = time(NULL);
			cf->real_parent = real_parent;
			cf->direct_child_count = 0;
			cf->folder_entry.block = f->direct_parent_place.block;
			cf->folder_entry.pos = f->entry_pos;
			cf->helper_index = -1;
		} else if (child_flags == PFS_F_FILE) {
			struct pfs_file *cf = child;
			cf->element.last_mod_time = time(NULL);
			cf->file_length = 0;
			cf->first_block = -1;
		} else if (child_flags == PFS_F_PIPE) {
			struct pfs_pipe *cf = child;
			cf->file.element.last_mod_time = time(NULL);
			cf->file.file_length = 0;
			cf->file.first_block = -1;
			cf->start_offset = 0;
		} else {
			abort();
		}
		pfs->set(pfs, f->element_place.block);
	}
	return 1;
}

int pfsc_folder_create_folder(pfs_eh f, pfs_eh parent, const char *name) {
	return add_child(f, parent, f->element_place, name, strlen(name), PFS_F_FOLDER, NULL);
}

int pfsc_folder_create_file(pfs_eh f, pfs_eh parent, const char *name) {
	return add_child(f, parent, f->element_place, name, strlen(name), PFS_F_FILE, NULL);
}

int pfsc_folder_create_pipe(pfs_eh f, pfs_eh parent, const char *name) {
	return add_child(f, parent, f->element_place, name, strlen(name), PFS_F_PIPE, NULL);
}

int pfsc_element_get_parent(pfs_eh e) {
	if (e->real_parent_place.block == -1) {
		pfs_errno = PFS_ERRNO_ROOT_FOLDER;
		return 0;
	}
	get_folder1(parent, block_data, (e->real_parent_place))
	struct pfs_place old_place = e->element_place;
	i32 entry_pos = e->entry_pos;
	i64 direct_parent_block = e->direct_parent_place.block;
	e->element_place = e->real_parent_place;
	e->entry_pos = parent->folder_entry.pos;
	e->direct_parent_place.block = parent->folder_entry.block;
	e->real_parent_place = parent->real_parent;
	block_data = pfs->get(pfs, parent->folder_entry.block);
	pfs->unget(pfs, e->element_place.block);
	if (block_data == NULL) {
		block_data = pfs->get(pfs, parent->folder_entry.block);
		if (block_data == NULL) {
			e->real_parent_place = e->element_place;
			e->direct_parent_place.block = direct_parent_block;
			e->entry_pos = entry_pos;
			e->element_place = old_place;
			pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
			return 0;
		}
	}
	i32 *table_end = block_data + pfs->block_size - 4;
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
		pfs->unget(pfs, e->direct_parent_place.block);
		i64 zw = e->entry_pos - e->direct_parent_place.pos - sizeof(struct pfs_folder);
		e->index_in_direct_parent_list = zw / sizeof(struct pfs_folder_entry);
		if ((zw % sizeof(struct pfs_folder_entry)) != 0) {
			abort();
		}
		return 1;
	}
}

static void remove_from_parent(const struct pfs_element_handle *e, struct pfs_folder *direct_parent) {
	memmove(direct_parent->entries + e->index_in_direct_parent_list,
	        direct_parent->entries + e->index_in_direct_parent_list + 1,
	        (e->index_in_direct_parent_list - direct_parent->direct_child_count - 1)
	                * sizeof(struct pfs_folder_entry));
	direct_parent->direct_child_count--;
	if (direct_parent->helper_index > e->index_in_direct_parent_list) {
		direct_parent->helper_index--;
	} else if (direct_parent->helper_index == e->index_in_direct_parent_list) {
		abort();
	}
	shrink_folder_entry(e->direct_parent_place,
	        sizeof(struct pfs_folder)
	                + (sizeof(struct pfs_folder_entry) * (1 + direct_parent->direct_child_count)));
}

static int del_helper(struct pfs_place h) {
	struct pfs_folder *helper = pfs->get(pfs, h.block) + h.pos;
	if (helper->direct_child_count > 0) {
		if ((helper->direct_child_count > 1) || (helper->helper_index == -1)) {
			pfs->unget(pfs, h.block);
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			return 0;
		}
		if (!del_helper(helper->entries[0].child_place)) {
			pfs->unget(pfs, h.block);
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			return 0;
		}
		helper->helper_index = -1; // just to make sure
		helper->direct_child_count = 0;
	}
	remove_from_block_table(h.block, h.pos);
	pfs->set(pfs, h.block);
	return 1;
}

int pfsc_element_delete(pfs_eh e) {
	get_folder1(direct_parent, direct_parent_block, e->direct_parent_place)
	get_any(pfs_element, element, element_block, e->element_place.block, e->element_place.pos,
	        pfs->unget(pfs, e->direct_parent_place.block)
	        ;)
	if ((direct_parent->entries[e->index_in_direct_parent_list].flags & PFS_F_FOLDER) != 0) {
		struct pfs_folder *folder = (struct pfs_folder*) element;
		if (folder->direct_child_count > 0) {
			if ((folder->direct_child_count > 1) || (folder->helper_index == -1)
			        || (!del_helper(folder->entries[0].child_place))) {
				pfs->unget(pfs, e->element_place.block);
				pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
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
	pfs->set(pfs, e->direct_parent_place.block);
	pfs->set(pfs, e->element_place.block);
	return 1;
}

static int move_and_set_parent_impl(pfs_eh e, pfs_eh new_parent, char *name) {
	pfs_errno = PFS_ERRNO_NONE;
	if (e->real_parent_place.block == -1) {
		pfs_errno = PFS_ERRNO_ROOT_FOLDER;
		return 0;
	}
	struct pfs_element_handle moh = *e;
	get_any(any, old_me, my_old_block, moh.element_place.block, moh.element_place.pos,)
	get_folder3(old_parent_folder, old_parent_block_data, moh.direct_parent_place.block,
	        moh.direct_parent_place.pos, pfs->unget(pfs, moh.element_place.block)
	        ;)
	struct pfs_folder_entry *my_old_entry = old_parent_block_data + moh.entry_pos;
	if (my_old_entry->flags & PFS_F_FOLDER) {
		for (struct pfs_element_handle eh = *new_parent; 1;) {
			if (moh.element_place.block == eh.element_place.block) {
				if (moh.element_place.pos == eh.element_place.pos) {
					pfs->unget(pfs, moh.real_parent_place.block);
					pfs_errno = PFS_ERRNO_PARENT_IS_CHILD;
					return 0;
				}
			}
			if (!pfsc_element_get_parent(&eh)) {
				if (pfs_errno != PFS_ERRNO_ROOT_FOLDER) {
					pfs->unget(pfs, moh.real_parent_place.block);
					return 0;
				} else {
					pfs_errno = 0;
				}
			}
		}
	}
	const i64 new_parent_old_block = new_parent->element_place.block;
	get_folder3(new_parent_folder, new_parent_block_data, new_parent_old_block,
	        new_parent->element_place.pos,
	        pfs->unget(pfs, moh.direct_parent_place.block); pfs->unget(pfs, moh.element_place.block);)
	*e = *new_parent;
	i32 name_len;
	if (name == NULL) {
		name = old_parent_block_data + my_old_entry->name_pos;
		name_len = get_size_from_block_table(old_parent_block_data, my_old_entry->name_pos);
	} else {
		name_len = strlen(name);
	}
	if (!add_child(e, new_parent, new_parent->element_place, name, name_len, -1, my_old_entry)) {
		pfs->unget(pfs, moh.direct_parent_place.block);
		pfs->unget(pfs, moh.element_place.block);
		pfs->unget(pfs, new_parent_old_block);
		return 0; // pfs_errno has already been set
	}
	remove_from_parent(&moh, old_parent_folder);
	pfs->set(pfs, moh.direct_parent_place.block);
	pfs->set(pfs, moh.element_place.block);
	pfs->set(pfs, new_parent_old_block);
	return 1;
}

int pfsc_element_set_parent(pfs_eh e, pfs_eh new_parent) {
	return move_and_set_parent_impl(e, new_parent, NULL);
}

int pfsc_element_move(pfs_eh e, pfs_eh new_parent, char *name) {
	if (name == NULL) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	return move_and_set_parent_impl(e, new_parent, name);
}
