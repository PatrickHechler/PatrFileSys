/*
 * pfs-intern.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_INTERN_H_
#define PFS_INTERN_H_

#include "patr-file-sys.h"

/*
 * only the used flag is important, the other flags are optional
 * if the used flag is is not available (block manager supports zero bits)
 * the flag is replaced by the patr-file-system
 */
// the bit number of the used block flag
#define BLOCK_FLAG_USED_BIT      0
// the bit number of the file data block flag
#define BLOCK_FLAG_FILE_DATA_BIT 1
// the bit number of the file system entries block flag
#define BLOCK_FLAG_ENTRIES_BIT   2
// this flag indicates that the block is used by the file system
#define BLOCK_FLAG_USED      (1L << BLOCK_FLAG_USED_BIT)
// this flag indicates that the block is used by a file and stores file data
#define BLOCK_FLAG_FILE_DATA (1L << BLOCK_FLAG_FILE_DATA_BIT)
// this flag indicates that the block is used and stores file system entries
#define BLOCK_FLAG_ENTRIES   (1L << BLOCK_FLAG_ENTRIES_BIT)

struct pfs_place {
	i64 block;
	i32 pos;
} __attribute__((packed));

struct pfs_b0 {
	ui64 MAGIC;
	struct pfs_place root;
	i32 block_size;
	i64 block_count;
	i64 block_table_first_block;
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
	struct pfs_element element;
	i32 direct_child_count;
	i32 helper_index; // only max one helper for each folder
	struct pfs_folder_entry entries[];
} __attribute__((packed));

struct pfs_file {
	struct pfs_element element;
	i64 file_length;
	i64 first_block;
} __attribute__((packed));

void init_element(struct pfs_element *elemnet, const struct pfs_place parent,
		ui32 element_flags);

void init_block(i64 block, i64 size);

int allocate_new_entry(struct pfs_place *write, i64 base_block, i32 size);

//should only be used with implicit defines outside of pfs.c
i32 reallocate_in_block_table(const i64 block, const i32 pos,
		const i64 new_size, const int copy);

#define shrink_folder_entry(/*struct pfs_place */ place, /* i32 */ old_size) \
	if ( (place.pos = reallocate_in_block_table(place.block, place.pos, \
			(old_size) - sizeof(struct pfs_folder_entry), 1)) == -1) { \
		abort(); \
	}

int grow_folder_entry(struct pfs_place *place, i32 new_size);

#define remove_table_entry(block, pos) reallocate_in_block_table(block, pos, 0, 0)

i32 add_name(i64 block_num, char *name);

i64 allocate_block(ui64 block_flags);

void free_block(i64 block);

void ensure_block_is_file_data(i64 block);

void ensure_block_is_entry(i64 block);

struct pfs_place find_place(i64 first_block, i64 remain);

#endif /* PFS_INTERN_H_ */
