/*
 * pfs-constants.h
 *
 *  Created on: Jul 7, 2022
 *      Author: pat
 */

#ifndef PFS_CONSTANTS_H_
#define PFS_CONSTANTS_H_

#ifdef INTERN_PFS
#include <assert.h>
#endif /* INTERN_PFS */

#define PFS_ROOT_FOLDER_ID -2
#define PFS_INVALID_ID -1

#ifdef INTERN_PFS

#define offsetoff(T, m) __builtin_offsetof(T, m)

#define PFS_MIN_NORMAL_ID 0

/**
 * do NOT use (definitely NOT modify this file
 */
#define PFS_ELEMENT_TABLE_FILE_ID -3

#define PFS_ELEMENT_TABLE_OFFSET_BLOCK    0
#define PFS_ELEMENT_TABLE_OFFSET_POS      8
#define PFS_ELEMENT_TABLE_ELEMENT_LENGTH  12

struct pfs_access {
	i64 lock_lock;
	i64 lock_time;
};

static_assert(offsetoff(struct pfs_access, lock_lock) == 0, "err");
static_assert(offsetoff(struct pfs_access, lock_time) == 8, "err");
static_assert(sizeof(struct pfs_access) == 16, "err");

struct pfs_first_block_start {
	i64               block_count;
	i32               block_size;
	i32               root_pos;
	i64               root_block;
	i64               element_table_block;
	i32               element_table_pos;
	i32               file_sys_state_val;
	i64               file_sys_state_time;
	struct pfs_access access;
};

static_assert(offsetoff(struct pfs_first_block_start, block_count) == 0, "err");
static_assert(offsetoff(struct pfs_first_block_start, block_size) == 8, "err");
static_assert(offsetoff(struct pfs_first_block_start, root_pos) == 12, "err");
static_assert(offsetoff(struct pfs_first_block_start, root_block) == 16, "err");
static_assert(offsetoff(struct pfs_first_block_start, element_table_block) == 24, "err");
static_assert(offsetoff(struct pfs_first_block_start, element_table_pos) == 32, "err");
static_assert(offsetoff(struct pfs_first_block_start, file_sys_state_val) == 36, "err");
static_assert(offsetoff(struct pfs_first_block_start, file_sys_state_time) == 40, "err");
static_assert(offsetoff(struct pfs_first_block_start, access) == 48, "err");
static_assert(sizeof(struct pfs_first_block_start) == 64, "err");

#endif /* INTERN_PFS */

/**
 * flag used to indicate that the element is a folder
 */
#define PFS_ELEMENT_FLAG_FOLDER         0x00000001
/**
 * flag used to indicate that the element is a file
 */
#define PFS_ELEMENT_FLAG_FILE           0x00000002
/**
 * flag used to indicate that the element is a link
 */
#define PFS_ELEMENT_FLAG_LINK           0x00000004
/**
 * flag used to indicate that the element is read only
 */
#define PFS_ELEMENT_FLAG_READ_ONLY      0x00000008
/**
 * flag used to indicate that the element is executable
 */
#define PFS_ELEMENT_FLAG_EXECUTABLE     0x00000010
/**
 * flag used to indicate that the element is hidden
 */
#define PFS_ELEMENT_FLAG_HIDDEN         0x00000020
/**
 * flag used to indicate that the element is sorted<br>
 * this flag is used only on folders.
 * <p>
 * a folder assigned with the given flag will have its children sorted.<br>
 * this flag can be used to optimize search operations.
 * <p>
 * the current implementation ignores this flag, but removes it every time an element gets added.
 */
#define PFS_ELEMENT_FLAG_FOLDER_SORTED  0x00000040
/**
 * flag used to indicate that the element is encrypted.<br>
 * this flag is used only on files.
 * <p>
 * this flag does not specify in which way or with wich algorithm the given file is encrypted
 * <p>
 * the current implementation ignores this flag
 */
#define PFS_ELEMENT_FLAG_FILE_ENCRYPTED 0x00000080

#ifdef INTERN_PFS

struct pfs_table_entry {
	i32 start;
	i32 end;
};

static_assert(offsetoff(struct pfs_table_entry, start) == 0, "err");
static_assert(offsetoff(struct pfs_table_entry, end) == 4, "err");
static_assert(sizeof(struct pfs_table_entry) == 8, "err");

struct pfs_file_sys_element {
	i32 flags;
	i32 name_pos;
	i64 parentID;
	struct pfs_access access;
	i64 create_time;
	i64 last_mod_time;
	i64 last_meta_mod_time;
};

static_assert(offsetoff(struct pfs_file_sys_element, flags) == 0, "err");
static_assert(offsetoff(struct pfs_file_sys_element, name_pos) == 4, "err");
static_assert(offsetoff(struct pfs_file_sys_element, parentID) == 8, "err");
static_assert(offsetoff(struct pfs_file_sys_element, access) == 16, "err");
static_assert(offsetoff(struct pfs_file_sys_element, create_time) == 32, "err");
static_assert(offsetoff(struct pfs_file_sys_element, last_mod_time) == 40, "err");
static_assert(offsetoff(struct pfs_file_sys_element, last_meta_mod_time) == 48, "err");

struct pfs_folder_childID {
	i32 low_bits;
	i32 high_bits;
};

static_assert(offsetoff(struct pfs_folder_childID, low_bits) == 0, "err");
static_assert(offsetoff(struct pfs_folder_childID, high_bits) == 4, "err");
static_assert(sizeof(struct pfs_folder_childID) == 8, "err");

struct pfs_file_sys_folder {
	struct pfs_file_sys_element element;
	i32 element_count;
	i64 childIDs[];
} __attribute__((packed)); //needed because of padding at the end, which corrupts the sizeof

static_assert(offsetoff(struct pfs_file_sys_folder, element) == 0, "err");
static_assert(offsetoff(struct pfs_file_sys_folder, element_count) == 56, "err");
static_assert(offsetoff(struct pfs_file_sys_folder, childIDs) == 60, "err");
static_assert(offsetoff(struct pfs_file_sys_folder, childIDs[0]) == 60, "err");
static_assert(offsetoff(struct pfs_file_sys_folder, childIDs[1]) == 68, "err");
static_assert(sizeof(struct pfs_file_sys_folder) == 60, "err");

struct pfs_allocated_blocks {
	i64 start;
	i64 end;
};

static_assert(offsetoff(struct pfs_allocated_blocks, start) == 0, "err");
static_assert(offsetoff(struct pfs_allocated_blocks, end) == 8, "err");
static_assert(sizeof(struct pfs_allocated_blocks) == 16, "err");

struct pfs_file_sys_file {
	struct pfs_file_sys_element element;
	i64 length;
	struct pfs_allocated_blocks data_table[];
};

static_assert(offsetoff(struct pfs_file_sys_file, element) == 0, "err");
static_assert(offsetoff(struct pfs_file_sys_file, length) == 56, "err");
static_assert(offsetoff(struct pfs_file_sys_file, data_table) == 64, "err");
static_assert(sizeof(struct pfs_file_sys_file) == 64, "err");

struct pfs_file_sys_link {
	struct pfs_file_sys_element element;
	i64 targetID;
};

static_assert(offsetoff(struct pfs_file_sys_link, element) == 0, "err");
static_assert(offsetoff(struct pfs_file_sys_link, targetID) == 56, "err");
static_assert(sizeof(struct pfs_file_sys_link) == 64, "err");

#endif /* INTERN_PFS */

#define PFS_NO_LOCK                          0L
#define PFS_LOCK_START_MAX_VALUE             0x00000000FFFFFFFFL
#define PFS_LOCK_SECOND_RANDOM_BYTE_AND      0x0FF0000000000000L
#define PFS_LOCK_SECOND_RANDOM_BYTE_SHIFT    52
#define PFS_LOCK_NO_READ_ALLOWED_LOCK        0x0000000100000000L
#define PFS_LOCK_NO_WRITE_ALLOWED_LOCK       0x0000000200000000L
#define PFS_LOCK_NO_DELETE_ALLOWED_LOCK      0x0000000400000000L
#define PFS_LOCK_NO_META_CHANGE_ALLOWED_LOCK 0x0000000800000000L
#define PFS_LOCK_SHARED_COUNTER_SHIFT        36
#define PFS_LOCK_SHARED_COUNTER_AND          0x000FFFF000000000L
#define PFS_LOCK_SHARED_RANDOM               (PFS_LOCK_SECOND_RANDOM_BYTE_AND | PFS_LOCK_START_MAX_VALUE)
#define PFS_LOCK_SHARED_COUNTER_MAX_VALUE    (PFS_LOCK_SHARED_COUNTER_AND >>> PFS_LOCK_SHARED_COUNTER_SHIFT)
#define PFS_LOCK_SHARED_LOCK                 0x4000000000000000L
#define PFS_LOCK_LOCKED_LOCK                 0x8000000000000000L
#define PFS_LOCK_NO_DATA                     (PFS_LOCK_SHARED_RANDOM | PFS_LOCK_SHARED_COUNTER_AND)
#define PFS_LOCK_DATA                        (~PFS_LOCK_NO_DATA)

#define PFS_NO_TIME -1L

#endif /* PFS_CONSTANTS_H_ */
