/*
 * pfs-intern.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef SRC_CORE_PFS_INTERN_H_
#define SRC_CORE_PFS_INTERN_H_

#include "../include/patr-file-sys.h"

/*
 * only the used flag is important, the other flags are optional
 * if the used flag is is not available (block manager supports zero bits)
 * the flag is replaced by the patr-file-system
 */
// the bit number of the used block flag
#define BLOCK_FLAG_USED_BIT      0
// the bit number of the file data block flag
#define BLOCK_FLAG_DATA_BIT 1
// the bit number of the file system entries block flag
#define BLOCK_FLAG_ENTRIES_BIT   2
// this flag indicates that the block is used by the file system
#define BLOCK_FLAG_USED         (1UL << BLOCK_FLAG_USED_BIT)
// this flag indicates that the block is used by a file/pipe and stores file/pipe data
#define BLOCK_FLAG_DATA         (1UL << BLOCK_FLAG_DATA_BIT)
// this flag indicates that the block is used and stores file system entries
#define BLOCK_FLAG_ENTRIES      (1UL << BLOCK_FLAG_ENTRIES_BIT)

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

static_assert(offsetof(struct pfs_b0, MAGIC) == 0, "error!");

struct pfs_element {
	i64 last_mod_time;
} __attribute__((packed));

struct pfs_folder_entry {
	i32 name_pos; // for correct alignment of the place
	struct pfs_place child_place;
	i64 create_time;
	ui32 flags;
// i32 padding; // no padding done here every second entry is miss-alinged
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

static_assert(offsetof(struct pfs_folder, entries) == sizeof(struct pfs_folder));

struct pfs_file {
	struct pfs_element element;
	i64 file_length;
	i64 first_block;
} __attribute__((packed));

struct pfs_pipe {
	struct pfs_file file;
	i32 start_offset;
} __attribute__((packed));

struct pfs_element_handle {
	struct pfs_place real_parent_place;
	i32 index_in_direct_parent_list;
	struct pfs_place direct_parent_place;
	i32 entry_pos;
	struct pfs_place element_place;
};

struct pfs_folder_iter {
	struct pfs_place current_place;
	i32 current_depth;
	i32 current_folder_pos;
	i32 remaining_direct_entries;
	struct pfs_element_handle *eh;
	int show_hidden;
};

typedef struct pfs_element_handle *pfs_eh;

typedef struct pfs_folder_iter *pfs_fi;

int init_block(i64 block, i64 size);

i32 get_size_from_block_table(void *block_data, const i32 pos);

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
int allocate_new_entry(struct pfs_place *write, i64 base_block, i32 size);

i32 allocate_in_block_table(i64 block, i64 size);

i32 reallocate_in_block_table(const i64 block, const i32 pos,
		const i64 new_size, const int copy);

#define remove_from_block_table(block, pos) reallocate_in_block_table(block, pos, 0, 0)

#define shrink_folder_entry(place, old_size) \
	{ \
		i32 new_pos = reallocate_in_block_table(place.block, place.pos, \
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

#define remove_table_entry(block, pos) reallocate_in_block_table(block, pos, 0, 0)

i32 add_name(i64 block_num, const char *name, i64 str_len);

i64 allocate_block(ui64 block_flags);

void free_block(i64 block);

void ensure_block_is_file_data(i64 block);

void ensure_block_is_entry(i64 block);

struct pfs_place find_place(i64 first_block, i64 remain);

#endif /* SRC_CORE_PFS_INTERN_H_ */
