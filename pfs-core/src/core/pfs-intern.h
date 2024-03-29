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
 * pfs-intern.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef SRC_CORE_PFS_INTERN_H_
#define SRC_CORE_PFS_INTERN_H_

#include "../pfs/patr-file-sys.h"
#include "../pfs/pfs-mount.h"
#include "../pfs/hashset.h"
#include <string.h>
#include <stdlib.h>

/*
 * only the used flag is important, the other flags are optional
 * if the used flag is is not available (block manager supports zero bits)
 * the flags are managed by the patr-file-system
 */
// the bit number of the used block flag
#define BLOCK_FLAG_USED_BIT      0
// the bit number of the file data block flag
#define BLOCK_FLAG_DATA_BIT      1
// the bit number of the file system entries block flag
#define BLOCK_FLAG_ENTRIES_BIT   2
// this flag indicates that the block is used by the file system
#define BLOCK_FLAG_USED         (1UL << BLOCK_FLAG_USED_BIT)
// this flag indicates that the block is used by a file/pipe and stores file/pipe data
#define BLOCK_FLAG_DATA         (1UL << BLOCK_FLAG_DATA_BIT)
// this flag indicates that the block is used and stores file system entries
#define BLOCK_FLAG_ENTRIES      (1UL << BLOCK_FLAG_ENTRIES_BIT)

#define PATRFS_MIN_BLOCK_SIZE ( \
		sizeof(struct pfs_b0) \
		+ sizeof(struct pfs_folder) \
		+ sizeof(struct pfs_folder_entry) \
		+ sizeof(struct pfs_folder_entry) \
		+ 30 \
	)

#define pfs_validate_b0(b0, invalid, also_in_block_table) \
	{ \
		if ((b0)->MAGIC0 != PFS_MAGIC_START0) { \
			goto invalidb0; \
		} \
		if ((b0)->MAGIC1 != PFS_MAGIC_START1) { \
			goto invalidb0; \
		} \
		if ((b0)->block_count < 2) { \
			goto invalidb0; \
		} \
		if ((b0)->block_size < PATRFS_MIN_BLOCK_SIZE) { \
			goto invalidb0; \
		} \
		if ((b0)->root.block < 0) { \
			goto invalidb0; \
		} \
		if ((b0)->root.pos < 0) { \
			goto invalidb0; \
		} \
		if (also_in_block_table) { \
			i32 b0_end = *(i32*) ((void*) (b0) + 4 \
				+ *(i32*) ((void*) (b0) + (b0)->block_size - 4)); \
			if (b0_end < sizeof(struct pfs_b0)) { \
				goto invalidb0; \
			} \
			if ((b0)->root.block == 0 && (b0)->root.pos < b0_end) { \
				/* do invalid at the end, so if it finishes, make no loop */ \
				invalidb0:; \
				invalid \
			} \
		} \
	}

struct pfs_place {
	i64 block;
	i32 pos;
} __attribute__((packed));

struct pfs_b0 {
	ui64 MAGIC0;
	ui64 MAGIC1;
	struct pfs_place root;
	i32 block_size;
	i64 block_count;
	ui32 flags;
	ui8 uuid[16];
	char name[0];
} __attribute__((packed));

_Static_assert(offsetof(struct pfs_b0, MAGIC0) == 0, "error!");

#define PATRFS_B0_FLAG_BM_ALLOC   0x00000001U
#define PATRFS_B0_FLAG_READ_ONLY  0x00000002U

struct pfs_element {
	i64 last_mod_time;
} __attribute__((packed));

struct pfs_folder_entry {
	i32 name_pos;
	struct pfs_place child_place;
	i64 create_time;
	ui32 flags;
//	i32 padding; // no padding done here every second entry is miss-alinged
} __attribute__((packed));

struct pfs_folder {
	struct pfs_element element;
	struct pfs_place real_parent;
	i32 direct_child_count;
	struct pfs_place folder_entry;
	// direct parent is indirect known with the folder entry place (start of memory table entry)
	i32 helper_index;
	struct pfs_folder_entry entries[];
} __attribute__((packed));

_Static_assert(offsetof(struct pfs_folder, entries) == sizeof(struct pfs_folder));

struct pfs_file_data {
	struct pfs_element *e;
	struct pfs_file0 *f;
};

struct pfs_file0 {
	i64 file_length;
	i64 first_block;
} __attribute__((packed));

struct pfs_file {
	struct pfs_element element;
	struct pfs_file0 file;
} __attribute__((packed));

struct pfs_pipe0 {
	struct pfs_file0 file;
	i32 start_offset;
} __attribute__((packed));

struct pfs_pipe {
	struct pfs_element element;
	struct pfs_pipe0 pipe;
} __attribute__((packed));

#define PFS_MOUNT_FLAGS_READ_ONLY 0x00000100U

#define PFS_MOUNT_FLAGS_TEMP           ((ui32) mount_type_temp           ) /* 0 */
#define PFS_MOUNT_FLAGS_INTERN         ((ui32) mount_type_intern         ) /* 1 */
#define PFS_MOUNT_FLAGS_REAL_FS_FILE   ((ui32) mount_type_linux_pfs_file ) /* 2 */
#define PFS_MOUNT_FLAGS_TYPE_MASK      ((ui32) 0x00000003U               )

struct pfs_mount_point {
	struct pfs_element element;
	ui32 flags;
} __attribute__((packed));

struct pfs_mount_point_tmp {
	struct pfs_mount_point generic;
	i32 block_size;
	i64 block_count;
} __attribute__((packed));

struct pfs_mount_point_real_fs_file {
	struct pfs_mount_point generic;
	char path[0];
} __attribute__((packed));

struct pfs_mount_point_intern {
	struct pfs_mount_point generic;
	i32 block_size;
	i64 block_count;
	struct pfs_file0 file;
} __attribute__((packed));

struct pfs_element_handle {
	struct pfs_place element_place;
	i32 index_in_direct_parent_list;
	struct pfs_file_sys_data *fs_data;
	struct pfs_place direct_parent_place;
	i32 entry_pos;
	struct pfs_place real_parent_place;
	// true if this is a mount point (for both the mount point entry and the root entry of the mounted file system)
	i32 is_mount_point; // false for the root entry of the root file system
};

struct element_handle {
	i64 load_count;
	struct pfs_element_handle handle;
};

struct pfs_file_sys_data {
	struct bm_block_manager *file_sys;
	struct element_handle *root;
	struct pfs_element_handle_mount *mount_point;
	int read_only;
};

struct pfs_element_handle_mount {
	struct pfs_element_handle handle;
	i32 padding;
	struct pfs_file_sys_data fs;
};

_Static_assert(offsetof(struct pfs_element_handle, element_place) == 0, "Error!");

_Static_assert(offsetof(struct pfs_element_handle_mount, handle) == 0, "Error!"); // needed because converting is done by realloc

struct pfs_folder_iter {
	struct pfs_place current_place;
	i32 current_depth;
	i32 current_folder_pos;
	i32 remaining_direct_entries;
	struct pfs_element_handle *eh;
	int show_hidden;
};

typedef struct bm_block_manager *bm;

typedef struct pfs_element_handle *pfs_eh;

typedef struct pfs_element_handle_mount *pfs_meh;

typedef struct pfs_folder_iter *pfs_fi;

int init_block(bm pfs, i64 block, i64 size);

i32 get_size_from_block_table(void *block_data, const i32 pos, const i32 block_size);

/**
 * tries to allocate a block-entry in the base block.
 * if the base_block has not enough space a new block
 * will be allocated
 *
 * if base_block is -1 a new block will be allocated
 *
 * if a new block was allocated write->pos will always
 * be set to zero
 */
int allocate_new_entry(bm pfs, struct pfs_place *write, i64 base_block, i32 size);

i32 allocate_in_block_table(bm pfs, i64 block, i64 size);

i32 reallocate_in_block_table(bm pfs, const i64 block, const i32 pos,
		const i64 new_size, const int copy);

#define remove_from_block_table(pfs, block, pos) reallocate_in_block_table(pfs, block, pos, 0, 0)

#define shrink_folder_entry(pfs, place, old_size) \
	{ \
		i32 new_pos = reallocate_in_block_table(pfs, place.block, place.pos, \
						(old_size) - sizeof(struct pfs_folder_entry), 1); \
		if (new_pos == -1) { \
			abort(); \
		}\
		if (new_pos != place.pos) { \
			abort(); /*shrink reallocate should always be in place */ \
		} \
	}

i32 grow_folder_entry(const struct pfs_element_handle *e, i32 new_size,
		struct pfs_place real_parent);

#define remove_table_entry(pfs, block, pos) reallocate_in_block_table(pfs, block, pos, 0, 0)

i32 add_name(bm pfs, i64 block_num, const char *name, i64 str_len);

i64 allocate_block(bm pfs, ui64 block_flags);

void free_block(bm pfs, i64 block);

void ensure_block_is_file_data(bm pfs, i64 block);

void ensure_block_is_entry(bm pfs, i64 block);

struct pfs_place find_place(bm pfs, i64 first_block, i64 remain);

#define pfs(eh) (eh)->handle.fs_data->file_sys

#define pfs0(e) (e)->fs_data->file_sys

#endif /* SRC_CORE_PFS_INTERN_H_ */
