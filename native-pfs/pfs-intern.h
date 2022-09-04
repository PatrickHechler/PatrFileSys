/*
 * pfs-intern.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_INTERN_H_
#define PFS_INTERN_H_

#include "pfs.h"

struct pfs_place {
	i64 block;
	i32 pos;
} __attribute__((packed));

struct pfs_b0 {
	ui64 MAGIC;
	struct pfs_place root;
	i32 block_size;
	i64 block_count;
} __attribute__((packed));

struct pfs_element {
	struct pfs_place parent;
	ui32 flags;
	i64 create_time;
	i64 last_mod_time;
	i64 last_meta_mod_time;
} __attribute__((packed));

struct pfs_folder_entry {
	struct pfs_place child_place;
	i32 name_pos;
} __attribute__((packed));

struct pfs_folder {
	struct pfs_element elemenet;
	i64 child_count;
	struct pfs_folder_entry entries[];
} __attribute__((packed));

struct pfs_file {
	struct pfs_element element;
	i64 file_length;
	i64 first_block;
} __attribute__((packed));


void init_element(struct pfs_element *elemnet, i64 parent_block, i32 parent_pos, ui32 flags);

void allocate_new_entry(struct pfs_place *write, i64 base_block, i32 size);

void reallocate_entry(struct pfs_place *place, i32 new_size);

i32 add_name(struct pfs_place *etry, char *name);

i64 allocate_block();

void free_block(i64 block);

#endif /* PFS_INTERN_H_ */
