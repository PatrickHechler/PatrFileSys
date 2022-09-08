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

#define get_folder1(folder_name, block_name) get_folder2(folder_name, block_name, f->block, f->pos)

#define get_folder0 get_folder2(folder, block_data, f.block, f.pos)

#define get_folder get_folder1(folder, block_data)

extern i64 pfs_folder_child_count(element *f) {
	get_folder
	i64 child_count = folder->direct_child_count;
	i64 result = 0L;
	for (i64 i = 0; i < child_count; i++) {
		if (folder->entries[i].name_pos != -1) {
			result++;
		} else {
			result += pfs_folder_child_count(&folder->entries[i].child_place);
		}
	}
	pfs->unget(pfs, f->block);
	return result;
}

static int child_from_index(struct pfs_place *f, i64 index, i64 *count,
		struct pfs_place *target, int is_helper) {
	get_folder
	const i64 block = f->block;
	i64 child_cnt = 0;
	for (i32 i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos != -1) {
			if (index <= *count) {
				if (index < *count) {
					abort();
				}
				*target = folder->entries[i].child_place;
				pfs->unget(pfs, block);
				return 1;
			}
			(*count)++;
		} else {
			if (child_from_index(&folder->entries[i].child_place, index, count,
					target, 1)) {
				pfs->unget(pfs, block);
				return 1;
			}
		}
	}
	if (!is_helper) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
	}
	pfs->unget(pfs, block);
	return 0;
}

extern int pfs_folder_child_from_index(element *f, i64 index) {
	i64 cnt = 0;
	return child_from_index(f, index, &cnt, f, 0);

}

static int pfs_folder_child_from_name_impl(element *f, char *name,
		int is_helper) {
	i64 block_num = f->block;
	get_folder
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			if (pfs_folder_child_from_name_impl(f, name, 1)) {
				pfs->unget(pfs, block_num);
				return 1;
			}
		}
		char *cn = block_data + folder->entries[i].name_pos;
		if (strcmp(name, cn) == 0) {
			f->block = folder->entries[i].child_place.block;
			f->pos = folder->entries[i].child_place.pos;
			pfs->unget(pfs, block_num);
			return 1;
		}
	}
	pfs->unget(pfs, block_num);
	if (!is_helper) {
		pfs_errno = PFS_ERRNO_ELEMENT_NOT_EXIST;
	}
	return 0;
}

extern int pfs_folder_child_from_name(element *f, char *name) {
	return pfs_folder_child_from_name_impl(f, name, 0);
}

static inline int has_child_with_name(element f, char *name) {
	get_folder0
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			if (has_child_with_name(folder->entries[i].child_place, name)) {
				return 1;
			}
		}
		if (strcmp(name, block_data + folder->entries[i].name_pos) == 0) {
			pfs->unget(pfs, f.block);
			return 1;
		}
	}
	pfs->unget(pfs, f.block);
	return 0;
}

static inline int create_folder_or_file(element *f, char *name,
		int create_folder);

static inline int delegate_create_element_to_helper(const i64 my_new_size,
		int grow_success, int create_folder, struct pfs_folder *me,
		struct pfs_place my_place, element *f, char *name) {
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
			init_element(&helper->element, my_place, me->direct_child_count,
					-1L);
			helper->direct_child_count = 0;
		} else {
			init_block(new_block,
					sizeof(struct pfs_folder)
							+ sizeof(struct pfs_folder_entry));
			init_element(&helper->element, my_place, me->direct_child_count - 1,
					-1L);
			helper->direct_child_count = 1;
			helper->entries[0] = me->entries[me->direct_child_count - 1];
			i32 name_pos = add_name(new_block,
					((void*) helper) + helper->entries[0].name_pos);
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
	*f = me->entries[me->helper_index].child_place;
	pfs->unget(pfs, my_place.block);
	return create_folder_or_file(f, name, create_folder);
}

static inline int create_folder_or_file(element *f, char *name,
		int create_folder) {
	struct pfs_place my_place = *f;
	get_folder1(me, my_old_block_data)
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
		if (my_place.pos != f->pos) {
			my_place.pos = f->pos;
			me = my_old_block_data + my_place.pos;
		}
		name_pos = add_name(my_place.block, name);
		if (name_pos == -1) {
			return delegate_create_element_to_helper(my_new_size, grow_success,
					create_folder, me, my_place, f, name);
		}
	} else if (((me->direct_child_count) < 2) && (me->helper_index == -1)) {
		grow_success = 0;
		if (!allocate_new_entry(f, -1,
				sizeof(struct pfs_folder)
						+ (sizeof(struct pfs_folder_entry)
								* (me->direct_child_count + 1)))) {
			pfs->unget(pfs, my_place.block);
			return 0;
		}
		struct pfs_folder *old_me = me;
		const i64 my_old_block = my_place.block;
		my_place = *f;
		me = pfs->get(pfs, my_place.block) + my_place.pos;
		me->element = old_me->element;
		me->direct_child_count = old_me->direct_child_count;
		me->helper_index = -1;
		struct pfs_folder *my_parent = pfs->get(pfs, me->element.parent.block)
				+ me->element.parent.pos;
		my_parent->entries[me->element.index_in_parent_list].child_place =
				my_place;
		pfs->set(pfs, me->element.parent.block);
		pfs->set(pfs, my_old_block);
		name_pos = add_name(my_place.block, name);
	} else {
		grow_success = 0;
		return delegate_create_element_to_helper(my_new_size, grow_success,
				create_folder, me, my_place, f, name);
	}
	me->entries[me->direct_child_count].name_pos = name_pos;
	if (!allocate_new_entry(&me->entries[me->direct_child_count].child_place,
			my_place.block,
			create_folder ?
					sizeof(struct pfs_folder) : sizeof(struct pfs_file))) {
		if (grow_success) {
			shrink_folder_entry(my_place, my_new_size);
		}
		pfs->unget(pfs, my_place.block);
		return 0;
	}
	i64 now = time(NULL);
	void *child = pfs->get(pfs,
			me->entries[me->direct_child_count].child_place.block)
			+ +me->entries[me->direct_child_count].child_place.pos;
	init_element(child, my_place, me->direct_child_count, now);
	if (create_folder) {
		struct pfs_folder *cf = child;
		cf->direct_child_count = 0;
		cf->helper_index = -1;
	} else {
		struct pfs_file *cf = child;
		cf->file_length = 0L;
		cf->first_block = -1L;
	}
	*f = me->entries[me->direct_child_count++].child_place;
	pfs->set(pfs, f->block);
	pfs->set(pfs, my_place.block);
	return 1;
}

//static inline int create_folder_or_file_old(element *f, char *name,
//		int create_folder) {
//	ensure_block_is_entry(f->block);
//	struct pfs_place parent = *f;
//	const i64 old_parent_block = parent.block;
//	void *old_parent_block_data = pfs->get(pfs, old_parent_block);
//	struct pfs_folder *p = old_parent_block_data + parent.pos;
//	if (has_child_with_name(f, name)) {
//		pfs->unget(pfs, parent.block);
//		pfs_errno = PFS_ERRNO_ELEMENT_ALREADY_EXIST;
//		return 0;
//	}
//	size_t new_parent_size = sizeof(struct pfs_folder)
//			+ (p->direct_child_count + 1) * sizeof(struct pfs_folder_entry);
//	if (!grow_folder_entry(&parent, new_parent_size)) {
//		if (p->helper_index != -1) {
//			if (p->helper_index >= p->direct_child_count) {
//				abort();
//			}
//			*f = p->entries[p->helper_index].child_place;
//			pfs->unget(pfs, old_parent_block);
//			return create_folder_or_file(f, name, create_folder);
//		} else if (p->direct_child_count > 1) {
//			i64 new_block_num = allocate_block(
//			BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES);
//			if (new_block_num != -1L) {
//				void *new_block = pfs->get(pfs, new_block_num);
//				init_block(new_block_num,
//						sizeof(struct pfs_folder)
//								+ (sizeof(struct pfs_folder_entry) * 2));
//				i32 old_name_pos =
//						p->entries[p->direct_child_count - 1].name_pos;
//				i32 first_pos = add_name(new_block_num,
//						old_parent_block_data + old_name_pos);
//				i32 second_pos = add_name(new_block_num, name);
//				struct pfs_folder *helper = new_block;
//				init_element(&helper->element, parent,
//				PFS_FLAGS_FOLDER | PFS_FLAGS_HELPER_FOLDER, -1);
//				helper->direct_child_count = 2;
//				helper->helper_index = -1;
//				helper->entries[0].child_place =
//						p->entries[p->direct_child_count - 1].child_place;
//				helper->entries[0].name_pos = first_pos;
//				if (!allocate_new_entry(&helper->entries[1].child_place,
//						new_block_num,
//						create_folder ?
//								sizeof(struct pfs_folder) :
//								sizeof(struct pfs_file))) {
//					free_block(new_block_num);
//					pfs->set(pfs, new_block_num);
//					pfs->set(pfs, helper->entries[1].child_place.block);
//					return 0;
//				}
//				helper->entries[1].name_pos = second_pos;
//				void *child = pfs->get(pfs,
//						helper->entries[1].child_place.block)
//						+ helper->entries[1].child_place.pos;
//				if (child == NULL) {
//					free_block(new_block_num);
//					pfs->set(pfs, new_block_num);
//					pfs->set(pfs, helper->entries[1].child_place.block);
//					return 0;
//				}
//				p->helper_index = p->direct_child_count - 1;
//				p->entries[p->helper_index].name_pos = -1;
//				p->entries[p->helper_index].child_place.block = new_block_num;
//				p->entries[p->helper_index].child_place.pos = 0;
//				p->entries[p->helper_index].flags = PFS_FLAGS_FOLDER
//						| PFS_FLAGS_HELPER_FOLDER;
//				i64 now = time(NULL);
//				if (create_folder) {
//					init_element(child, p->entries[p->helper_index].child_place,
//					PFS_FLAGS_FOLDER, now);
//					struct pfs_folder *folder_child = child;
//					folder_child->direct_child_count = 0;
//					folder_child->helper_index = -1;
//				} else {
//					init_element(child, p->entries[p->helper_index].child_place,
//					PFS_FLAGS_FILE, now);
//					struct pfs_file *file_child = child;
//					file_child->file_length = 0L;
//					file_child->first_block = -1L;
//				}
//				helper->entries[1].create_time = now;
//				helper->entries[1].flags = ((struct pfs_element*) child)->flags;
//				pfs->set(pfs, new_block_num);
//				pfs->set(pfs, helper->entries[1].child_place.block);
//				return 1;
//			}
//		}
//		pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
//		pfs->unget(pfs, old_parent_block);
//		return 0;
//	}
//	const i32 name_pos = add_name(parent.block, name);
//	if (name_pos == -1) {
//		i64 new_block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES);
//		if (new_block == -1) {
//			shrink_folder_entry(parent, new_parent_size);
//			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
//			pfs->unget(pfs, old_parent_block);
//			return 0;
//		}
//		p = pfs->get(pfs, parent.block) + parent.pos;
//		if (p == NULL) {
//			pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
//			pfs->unget(pfs, old_parent_block);
//			shrink_folder_entry(parent, new_parent_size);
//			return 0;
//		}
//		p->entries[p->direct_child_count].name_pos = -1;
//		p->entries[p->direct_child_count].child_place.pos = 0;
//		p->entries[p->direct_child_count].child_place.block = new_block;
//		struct pfs_folder *helper = pfs->get(pfs, new_block);
//		if (helper == NULL) {
//			shrink_folder_entry(parent, new_parent_size);
//			pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
//			pfs->unget(pfs, parent.block);
//			pfs->unget(pfs, old_parent_block);
//			return 0;
//		}
//		init_block(new_block, sizeof(struct pfs_folder));
//		init_element(&helper->element, parent,
//		PFS_FLAGS_FOLDER | PFS_FLAGS_HELPER_FOLDER, -1);
//		helper->direct_child_count = 0;
//		struct pfs_place h;
//		h.block = new_block;
//		h.pos = 0;
//		int res = create_folder_or_file(&h, name, create_folder);
//		if (res) {
//			p->direct_child_count++;
//			pfs->set(pfs, new_block);
//			pfs->set(pfs, parent.block);
//		} else {
//			shrink_folder_entry(parent, new_parent_size);
//			pfs->unget(pfs, new_block);
//			pfs->unget(pfs, parent.block);
//		}
//		return res;
//	}
//	if (!allocate_new_entry(f, old_parent_block,
//			create_folder ?
//					sizeof(struct pfs_folder) : sizeof(struct pfs_file))) {
//		shrink_folder_entry(parent, new_parent_size);
//		remove_table_entry(parent.block, name_pos);
//		pfs->unget(pfs, old_parent_block);
//		return 0;
//	}
//	void *cb = pfs->get(pfs, f->block);
//	if (cb == NULL) {
//		shrink_folder_entry(parent, new_parent_size);
//		pfs->unget(pfs, old_parent_block);
//		pfs->unget(pfs, f->block);
//		*f = parent;
//		return 0;
//	}
//	void *child = cb + f->pos;
//	if (create_folder) {
//		struct pfs_folder *folder = child;
//		init_element(&folder->element, parent, PFS_FLAGS_FOLDER);
//		folder->direct_child_count = 0;
//		folder->helper_index = -1;
//	} else {
//		struct pfs_file *file = child;
//		init_element(&file->element, parent, PFS_FLAGS_FILE);
//		file->file_length = 0L;
//		file->first_block = -1L;
//	}
//	p = pfs->get(pfs, parent.block) + parent.pos;
//	p->entries[p->direct_child_count].name_pos = name_pos;
//	p->entries[p->direct_child_count].child_place.block = f->block;
//	p->entries[p->direct_child_count].child_place.pos = f->pos;
//	p->direct_child_count++;
//	pfs->set(pfs, f->block);
//	pfs->unget(pfs, old_parent_block);
//	pfs->set(pfs, parent.block);
//	return 1;
//}

extern int pfs_folder_create_folder(element *f, char *name) {
	return create_folder_or_file(f, name, 1);
}

extern int pfs_folder_create_file(element *f, char *name) {
	return create_folder_or_file(f, name, 0);
}
