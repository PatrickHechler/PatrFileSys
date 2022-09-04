/*
 * pfs.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#define I_AM_PFS
#include "pfs-constants.h"
#include "pfs.h"
#include "pfs-intern.h"
#include <stdlib.h>

extern int pfs_format(i64 block_count) {
	if (block_count < 2) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (pfs->loaded.entrycount > 0) {
		abort();
	}
	void *b0 = pfs->get(pfs, 0L);
	void *b1 = pfs->get(pfs, 1L);
	if (b0 == NULL || b1 == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
	i64 *mem_table = b1;
	mem_table[0] = 0L;
	mem_table[1] = 1L;
	*(i32*) (b1 + pfs->block_size - 4) = 16;
	struct pfs_b0 *super_data = b0;
	super_data->MAGIC = PFS_MAGIC_START;
	super_data->block_count = block_count;
	super_data->block_size = pfs->block_size;
	allocate_new_entry(&super_data->root, 0L, sizeof(struct pfs_folder));
	struct pfs_folder *root = pfs->get(pfs, super_data->root.block) + super_data->root.pos;
	init_element(&root->elemenet, -1L, -1, PFS_FLAGS_FOLDER);
	root->child_count = 0L;
	pfs->set(pfs, super_data->root.block);
	pfs->set(pfs, 1L);
	pfs->set(pfs, 0L);
	return 1;
}

extern i64 pfs_block_count() {
	void *b0 = pfs->get(pfs, 0L);
	i64 block_count = ((struct pfs_b0*) b0)->block_count;
	pfs->unget(pfs, 0L);
	return block_count;
}

extern i32 pfs_block_size() {
	void *b0 = pfs->get(pfs, 0L);
	i32 block_size = ((struct pfs_b0*) b0)->block_size;
	pfs->unget(pfs, 0L);
	return block_size;
}

extern element pfs_root() {
	element res;
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	res.block = b0->root.block;
	res.pos = b0->root.pos;
	pfs->unget(pfs, 0L);
	return res;
}

void init_element(struct pfs_element *element, i64 parent_block, i32 parent_pos,
		ui32 flags) {
	i64 now = time(NULL);
	element->flags = flags;
	element->create_time = now;
	element->last_mod_time = now;
	element->last_meta_mod_time = now;
	element->parent.block = parent_block;
	element->parent.pos = parent_pos;
}

void allocate_new_entry(struct pfs_place *place, i64 base_block, i32 size) {
	//TODO
	abort();
}

void reallocate_entry(struct pfs_place *place, i32 new_size) {
	//TODO
	abort();
}

i32 add_name(struct pfs_place *etry, char *name) {
	//TODO
	abort();
}

i64 allocate_block() {
	//TODO
	abort();
}

void free_block(i64 block) {
	//TODO
	abort();
}
