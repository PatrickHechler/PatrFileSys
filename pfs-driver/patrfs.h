/*
 * super.h
 *
 *  Created on: Sep 25, 2023
 *      Author: pat
 */

#ifndef PATRFS_H_
#define PATRFS_H_

#include <linux/blk_types.h>

#define PATRFS_B0_FLAG_BM_ALLOC   0x00000001U
#define PATRFS_B0_FLAG_READ_ONLY  0x00000002U

#define PATRFS_MAGIC_START0 0x702f6e69622f2123UL
#define PATRFS_MAGIC_START1 0x51fcd99d300a6d76UL

struct patrfs_place {
	u64 block;
	u32 pos;
} __attribute__((packed));

struct patrfs_b0 {
	u64 MAGIC0;
	u64 MAGIC1;
	struct patrfs_place root;
	s32 block_size;
	s64 block_count;
	u32 flags;
	u8 uuid[16];
	char name[0];
} __attribute__((packed));

struct patrfs_element {
	s64 last_mod_time;
} __attribute__((packed));

struct patrfs_folder_entry {
	s32 name_pos;
	struct patrfs_place child_place;
	s64 create_time;
	u32 flags;
//	i32 padding; // no padding done here every second entry is miss-alinged
} __attribute__((packed));

struct patrfs_folder {
	struct patrfs_element element;
	struct patrfs_place real_parent;
	s32 direct_child_count;
	struct patrfs_place folder_entry;
	// direct parent is indirect known with the folder entry place (start of memory table entry)
	s32 helper_index;
	struct patrfs_folder_entry entries[];
} __attribute__((packed));

struct patrfs_file {
	struct patrfs_element element;
	s64 file_length;
	s64 first_block;
} __attribute__((packed));

struct patrfs_pipe {
	struct patrfs_file file;
	s32 start_offset;
} __attribute__((packed));


#define __PATRFS_MIN_BLOCK_SIZE ( \
		sizeof(struct patrfs_b0) \
		+ sizeof(struct patrfs_folder) \
		+ sizeof(struct patrfs_folder_entry) \
		+ sizeof(struct patrfs_folder_entry) \
		+ 30 \
	)

#define PATRFS_MIN_BLOCK_SIZE       SECTOR_SIZE
#define PATRFS_MIN_BLOCK_SIZE_SHIFT SECTOR_SHIFT

_Static_assert(__PATRFS_MIN_BLOCK_SIZE <= SECTOR_SIZE, "Error!");

#endif /* PATRFS_H_ */
