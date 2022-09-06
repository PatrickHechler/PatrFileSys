/*
 * pfs-folder.c
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#include "pfs-intern.h"
#include "pfs-folder.h"

extern i64 pfs_folder_child_count(element *f) {
	struct pfs_folder *folder = pfs->get(pfs, f->block) + f->pos;
	ensure_block_is_entry(f->block);
	if (folder == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return -1L;
	}
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
		struct pfs_place *target) {
	ensure_block_is_entry(f->block);
	const i64 block = f->block;
	struct pfs_folder *folder = pfs->get(pfs, block) + f->pos;
	if (folder == NULL) {
		pfs->unget(pfs, f->block);
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
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
					target)) {
				pfs->unget(pfs, block);
				return 1;
			}
		}
	}
	if ((folder->element.flags & PFS_FLAGS_HELPER_FOLDER) != 0) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
	}
	pfs->unget(pfs, block);
	return 0;
}

extern int pfs_folder_child_from_index(element *f, i64 index) {
	i64 cnt = 0;
	return child_from_index(f, index, &cnt, f);

}

extern int pfs_folder_child_from_name(element *f, char *name) {
	ensure_block_is_entry(f->block);
	i64 block_num = f->block;
	void *block_data = pfs->get(pfs, block_num);
	struct pfs_folder *folder = block_data + f->pos;
	if (folder == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			if (pfs_folder_child_from_name(f, name)) {
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
	if ((folder->element.flags & PFS_FLAGS_HELPER_FOLDER) == 0) {
		pfs_errno = PFS_ERRNO_ELEMENT_NOT_EXIST;
	}
	return 0;
}

static inline int has_child_with_name(element *f, char *name) {
	void *block_data = pfs->get(pfs, f->block);
	struct pfs_folder *folder = block_data + f->pos;
	for (int i = 0; i < folder->direct_child_count; i++) {
		if (folder->entries[i].name_pos == -1) {
			if (!has_child_with_name(&folder->entries[i].child_place, name)) {
				return 1;
			}
		}
		if (strcmp(name, block_data + folder->entries[i].name_pos) == 0) {
			pfs->unget(pfs, f->block);
			return 1;
		}
	}
	pfs->unget(pfs, f->block);
	return 0;
}

static inline int create_folder_or_file(element *f, char *name,
		int create_folder) {
	ensure_block_is_entry(f->block);
	struct pfs_place parent = *f;
	const i64 old_parent_block = parent.block;
	struct pfs_folder *p = pfs->get(pfs, old_parent_block) + parent.pos;
	if (has_child_with_name(f, name)) {
		pfs->unget(pfs, parent.block);
		pfs_errno = PFS_ERRNO_ELEMENT_ALREADY_EXIST;
		return 0;
	}
	size_t new_parent_size = sizeof(struct pfs_folder)
			+ (p->direct_child_count + 1) * sizeof(struct pfs_folder_entry);
	if (!grow_folder_entry(&parent, new_parent_size, p->helper_index == -1)) {
		if (p->helper_index != -1) {
			if (p->helper_index >= p->direct_child_count) {
				abort();
			}
			*f = p->entries[p->helper_index].child_place;
			pfs->unget(pfs, old_parent_block);
			return create_folder_or_file(f, name, create_folder);
		}
		pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
		pfs->unget(pfs, old_parent_block);
		return 0;
	}
	i32 name_pos = add_name(&parent, name);
	if (name_pos == -1) {
		i64 new_block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES);
		if (new_block == -1) {
			shrink_folder_entry(parent, new_parent_size);
			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
			pfs->unget(pfs, old_parent_block);
			return 0;
		}
		p = pfs->get(pfs, parent.block) + parent.pos;
		if (p == NULL) {
			pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
			pfs->unget(pfs, old_parent_block);
			shrink_folder_entry(parent, new_parent_size);
			return 0;
		}
		p->entries[p->direct_child_count].name_pos = -1;
		p->entries[p->direct_child_count].child_place.pos = 0;
		p->entries[p->direct_child_count].child_place.block = new_block;
		struct pfs_folder *helper = pfs->get(pfs, new_block);
		if (helper == NULL) {
			shrink_folder_entry(parent, new_parent_size);
			pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
			pfs->unget(pfs, parent.block);
			pfs->unget(pfs, old_parent_block);
			return 0;
		}
		init_block(new_block, sizeof(struct pfs_folder));
		init_element(&helper->element, parent,
				PFS_FLAGS_FOLDER | PFS_FLAGS_HELPER_FOLDER);
		helper->direct_child_count = 0;
		struct pfs_place h;
		h.block = new_block;
		h.pos = 0;
		int res = create_folder_or_file(&h, name, create_folder);
		if (res) {
			p->direct_child_count++;
			pfs->set(pfs, new_block);
			pfs->set(pfs, parent.block);
		} else {
			shrink_folder_entry(parent, new_parent_size);
			pfs->unget(pfs, new_block);
			pfs->unget(pfs, parent.block);
		}
		return res;
	}
	if (!allocate_new_entry(f, old_parent_block, sizeof(struct pfs_folder))) {
		shrink_folder_entry(parent, new_parent_size);
		remove_table_entry(parent.block, name_pos);
		pfs->unget(pfs, old_parent_block);
		return 0;
	}
	void *cb = pfs->get(pfs, f->block);
	if (cb == NULL) {
		shrink_folder_entry(parent, new_parent_size);
		pfs->unget(pfs, old_parent_block);
		pfs->unget(pfs, f->block);
		*f = parent;
		return 0;
	}
	void *child = cb + f->pos;
	if (create_folder) {
		struct pfs_folder *folder = child;
		init_element(&folder->element, parent, PFS_FLAGS_FOLDER);
		folder->direct_child_count = 0;
		folder->helper_index = -1;
	} else {
		struct pfs_file *file = child;
		init_element(&file->element, parent, PFS_FLAGS_FILE);
		file->file_length = 0L;
		file->first_block = -1L;
	}
	p = pfs->get(pfs, parent.block) + parent.pos;
	p->entries[p->direct_child_count].name_pos = -1;
	p->entries[p->direct_child_count].child_place.block = f->block;
	p->entries[p->direct_child_count].child_place.pos = f->pos;
	p->direct_child_count++;
	pfs->set(pfs, f->block);
	pfs->unget(pfs, old_parent_block);
	pfs->set(pfs, parent.block);
	return 1;
}

extern int pfs_folder_create_folder(element *f, char *name) {
	return create_folder_or_file(f, name, 1);
}

extern int pfs_folder_create_file(element *f, char *name) {
	return create_folder_or_file(f, name, 0);
}
