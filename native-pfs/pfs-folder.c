/*
 * pfs-folder.c
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#include "pfs-intern.h"
#include "pfs-folder.h"

#define get_folder2(folder_name, block_name, place_block, place_pos) \
	ensure_block_is_entry(place_block); \
	void* block_name = pfs->get(pfs, place_block); \
	if (block_name == NULL) { \
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
		return 0; \
	} \
	struct pfs_folder *folder_name = block_name + place_pos;

#define get_folder1(folder_name, block_name) get_folder2(folder_name, block_name, f->element_place.block, f->element_place.pos)

#define get_folder0 get_folder2(folder, block_data, place.block, place.pos)

#define get_folder get_folder1(folder, block_data)

static i64 count_children(struct pfs_place *f) {
	get_folder2(folder, block_data, f->block, f->pos)
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

extern i64 pfs_folder_child_count(pfs_eh f) {
	return count_children(&f->element_place);
}

static int pfs_folder_child_from_name_impl(pfs_eh f, char *name, int is_helper) {
	const i64 old_block_num = f->element_place.block;
	get_folder
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			if (pfs_folder_child_from_name_impl(f, name, 1)) {
				pfs->unget(pfs, old_block_num);
				return 1;
			}
		}
		char *cn = block_data + folder->entries[i].name_pos;
		if (strcmp(name, cn) == 0) {
			f->element_place.block = folder->entries[i].child_place.block;
			f->element_place.pos = folder->entries[i].child_place.pos;
			pfs->unget(pfs, old_block_num);
			return 1;
		}
	}
	pfs->unget(pfs, old_block_num);
	if (!is_helper) {
		pfs_errno = PFS_ERRNO_ELEMENT_NOT_EXIST;
	}
	return 0;
}

extern int pfs_folder_child_from_name(pfs_eh f, char *name) {
	return pfs_folder_child_from_name_impl(f, name, 0);
}

static inline int has_child_with_name(struct pfs_place place, char *name) {
	get_folder0
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			if (has_child_with_name(folder->entries[i].child_place, name)) {
				return 1;
			}
		}
		if (strcmp(name, block_data + folder->entries[i].name_pos) == 0) {
			pfs->unget(pfs, place.block);
			return 1;
		}
	}
	pfs->unget(pfs, place.block);
	return 0;
}

static inline int create_folder_or_file(pfs_eh f, struct pfs_place direct_parent, char *name,
        int create_folder);

static inline int delegate_create_element_to_helper(const i64 my_new_size, int grow_success,
        int create_folder, struct pfs_folder *me, struct pfs_place my_place, pfs_eh f, char *name) {
	if (me->helper_index == -1) {
		i64 new_block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES);
		if (new_block == -1L) {
			shrink_folder_entry(my_place, my_new_size);
			pfs->unget(pfs, my_place.block);
			return 0;
		}
		struct pfs_folder *helper = pfs->get(pfs, new_block);
		if (grow_success) {
			init_block(new_block, sizeof(struct pfs_folder));
			helper->element.last_mod_time = -1L;
			helper->direct_child_count = 0;
		} else {
			init_block(new_block, sizeof(struct pfs_folder) + sizeof(struct pfs_folder_entry));
			helper->element.last_mod_time = -1L;
			helper->direct_child_count = 1;
			helper->entries[0] = me->entries[me->direct_child_count - 1];
			char *new_helper_child_name = ((void*) helper) + helper->entries[0].name_pos;
			i32 name_pos = add_name(new_block, new_helper_child_name, strlen(new_helper_child_name));
			if (name_pos == -1) {
				free_block(name_pos);
				pfs->unget(pfs, new_block);
				pfs->unget(pfs, my_place.block);
				pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
				return 0;
			}
		}
		helper->helper_index = -1;
	}
	if (grow_success) {
		shrink_folder_entry(my_place, my_new_size);
	}
	struct pfs_place direct_parent = me->entries[me->helper_index].child_place;
	pfs->unget(pfs, my_place.block);
	return create_folder_or_file(f, direct_parent, name, create_folder);
}

static inline int create_folder_or_file(pfs_eh f, struct pfs_place direct_parent, char *name,
        int create_folder) {
	get_folder1(me, my_old_block_data)
	struct pfs_place my_place = f->element_place;
	if (has_child_with_name(my_place, name)) {
		pfs->unget(pfs, my_place.block);
		pfs_errno = PFS_ERRNO_ELEMENT_ALREADY_EXIST;
		return 0;
	}
	const i64 my_new_size = sizeof(struct pfs_folder)
	        + (sizeof(struct pfs_folder_entry) * (1 + me->direct_child_count));
	i32 name_pos;
	int grow_success;
	if (grow_folder_entry(f, my_new_size)) {
		grow_success = 1;
		if (my_place.pos != f->element_place.pos) {
			my_place.pos = f->element_place.pos;
			me = my_old_block_data + my_place.pos;
		}
		name_pos = add_name(my_place.block, name, strlen(name));
		if (name_pos == -1) {
			return delegate_create_element_to_helper(my_new_size, grow_success, create_folder, me,
			        my_place, f, name);
		}
	} else if (((me->direct_child_count) < 2) && (me->helper_index == -1)) {
		grow_success = 0;
		if (!allocate_new_entry(&f->element_place, -1,
		        sizeof(struct pfs_folder)
		                + (sizeof(struct pfs_folder_entry) * (me->direct_child_count + 1)))) {
			pfs->unget(pfs, my_place.block);
			return 0;
		}
		struct pfs_folder *old_me = me;
		const i64 my_old_block = my_place.block;
		my_place = f->element_place;
		me = pfs->get(pfs, my_place.block) + my_place.pos;
		me->element = old_me->element;
		me->direct_child_count = old_me->direct_child_count;
		me->helper_index = -1;
		struct pfs_folder *my_parent = pfs->get(pfs, f->direct_parent_place.block)
		        + f->direct_parent_place.pos;
		my_parent->entries[f->index_in_direct_parent_list].child_place = my_place;
		pfs->set(pfs, f->direct_parent_place.block);
		pfs->set(pfs, my_old_block);
		name_pos = add_name(my_place.block, name, strlen(name));
	} else {
		grow_success = 0;
		return delegate_create_element_to_helper(my_new_size, grow_success, create_folder, me,
		        my_place, f, name);
	}
	me->entries[me->direct_child_count].name_pos = name_pos;
	if (!allocate_new_entry(&me->entries[me->direct_child_count].child_place, my_place.block,
	        create_folder ? sizeof(struct pfs_folder) : sizeof(struct pfs_file))) {
		if (grow_success) {
			shrink_folder_entry(my_place, my_new_size);
		}
		pfs->unget(pfs, my_place.block);
		return 0;
	}
	i64 now = time(NULL);
	void *child = pfs->get(pfs, me->entries[me->direct_child_count].child_place.block)
	        + +me->entries[me->direct_child_count].child_place.pos;
	if (create_folder) {
		struct pfs_folder *cf = child;
		cf->element.last_mod_time = now;
		cf->direct_child_count = 0;
		cf->helper_index = -1;
	} else {
		struct pfs_file *cf = child;
		cf->element.last_mod_time = now;
		cf->file_length = 0L;
		cf->first_block = -1L;
	}
	me->entries[me->direct_child_count].create_time = now;
	me->entries[me->direct_child_count].flags = create_folder ? PFS_FLAGS_FOLDER : PFS_FLAGS_FILE;
	me->entries[me->direct_child_count].name_pos = name_pos;
	f->real_parent_place = direct_parent;
	f->direct_parent_place = my_place;
	f->index_in_direct_parent_list = me->direct_child_count;
	f->element_place = me->entries[me->direct_child_count++].child_place;
	pfs->set(pfs, f->element_place.block);
	pfs->set(pfs, my_place.block);
	return 1;
}

extern int pfs_folder_create_folder(pfs_eh f, char *name) {
	return create_folder_or_file(f, f->element_place, name, 1);
}

extern int pfs_folder_create_file(pfs_eh f, char *name) {
	return create_folder_or_file(f, f->element_place, name, 0);
}
