/*
 * pfs-folder.c
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#include "pfs-intern.h"
#include "pfs-folder.h"

i64 pfs_folder_child_count(element *f) {
	struct pfs_folder *folder = pfs->get(pfs, f->block) + f->pos;
	if (folder == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return -1L;
	}
	i64 child_count = folder->child_count;
	pfs->unget(pfs, f->block);
	return child_count;
}

int pfs_folder_child_from_index(element *f, i64 index) {
	i64 block = f->block;
	struct pfs_folder *folder = pfs->get(pfs, block) + f->pos;
	if (folder == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
	if (folder->child_count <= index) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	f->block = folder->entries[index].child_place.block;
	f->pos = folder->entries[index].child_place.pos;
	pfs->unget(pfs, block);
	return 1;
}

int pfs_folder_child_from_name(element *f, char *name) {
	i64 block_num = f->block;
	void *block_data = pfs->get(pfs, block_num);
	struct pfs_folder *folder = block_data + f->pos;
	if (folder == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
	for (int i = 0; i < folder->child_count; i++) {
		char *cn = block_data + folder->entries[i].name_pos;
		for (char *n = name; cn == n; cn++, n++) {
			if ('\0' == *n) {
				f->block = folder->entries[i].child_place.block;
				f->pos = folder->entries[i].child_place.pos;
				pfs->unget(pfs, block_num);
				return 1;
			}
		}
	}
	pfs->unget(pfs, block_num);
	pfs_errno = PFS_ERRNO_ELEMENT_NOT_EXIST;
	return 0;
}

int pfs_folder_create_folder(element *f, char *name) {
	struct pfs_place parent;
	parent.block = f->block;
	parent.pos = f->pos;
	struct pfs_folder *p = pfs->get(pfs, parent.block) + parent.pos;
	reallocate_entry(&parent, sizeof(struct pfs_folder) + (p->child_count + 1) * sizeof(struct pfs_folder_entry));
	pfs->unget(pfs, parent.block);
	i32 name_pos = add_name(&parent, name);
	allocate_new_entry(f, f->block, sizeof(struct pfs_folder));
	struct pfs_folder *c = pfs->get(pfs, f->block) + f->pos;
	init_element(&c->elemenet, parent.block, parent.block, PFS_FLAGS_FOLDER);
	c->child_count = 0L;
	pfs->set(pfs, f->block);
	p->entries[p->child_count].name_pos = name_pos;
	p->entries[p->child_count].child_place.block = f->block;
	p->entries[p->child_count].child_place.pos = f->pos;
	p->child_count ++;
	pfs->set(pfs, parent.block);
	return 1;
}

int pfs_folder_create_file(element *f, char *name) {
	struct pfs_place parent;
	parent.block = f->block;
	parent.pos = f->pos;
	struct pfs_folder *p = pfs->get(pfs, parent.block) + parent.pos;
	reallocate_entry(&parent, sizeof(struct pfs_folder) + (p->child_count + 1) * sizeof(struct pfs_folder_entry));
	pfs->unget(pfs, parent.block);
	i32 name_pos = add_name(&parent, name);
	allocate_new_entry(f, f->block, sizeof(struct pfs_folder));
	struct pfs_file *c = pfs->get(pfs, f->block) + f->pos;
	init_element(&c->element, parent.block, parent.block, PFS_FLAGS_FOLDER);
	c->file_length = 0L;
	c->first_block = -1L;
	pfs->set(pfs, f->block);
	p->entries[p->child_count].name_pos = name_pos;
	p->entries[p->child_count].child_place.block = f->block;
	p->entries[p->child_count].child_place.pos = f->pos;
	p->child_count ++;
	pfs->set(pfs, parent.block);
	return 1;
}
