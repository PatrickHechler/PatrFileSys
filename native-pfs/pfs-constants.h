/*
 * pfs-constants.h
 *
 *  Created on: Jul 7, 2022
 *      Author: pat
 */

#ifndef PFS_CONSTANTS_H_
#define PFS_CONSTANTS_H_

#define PFS_ROOT_FOLDER_ID -2
#define PFS_INVALID_ID -1

#ifdef INTERN_PFS

#define PFS_MIN_NORMAL_ID 0

/**
 * do NOT use (definitely NOT modify this file
 */
#define PFS_ELEMENT_TABLE_FILE_ID -3

#define PFS_ELEMENT_TABLE_OFFSET_BLOCK    0
#define PFS_ELEMENT_TABLE_OFFSET_POS      8
#define PFS_ELEMENT_TABLE_ELEMENT_LENGTH  12

#endif // INTERN_PFS

#define PFS_FB_BLOCK_COUNT_OFF 0
#define PFS_FB_BLOCK_SIZE_OFF 8
#define PFS_FB_ROOT_POS_OFF 12
#define PFS_FB_ROOT_BLOCK_OFF 16
#define PFS_FB_ELEMENT_TABLE_BLOCK_OFF 24
#define PFS_FB_ELEMENT_TABLE_POS_OFF 32
#define PFS_FB_FILE_SYS_STATE_VAL_OFF 36
#define PFS_FB_FILE_SYS_STATE_TIME_OFF 40
#define PFS_FB_FILE_SYS_LOCK_VAL_OFF 48  /* lock directly before lock time */
#define PFS_FB_FILE_SYS_LOCK_TIME_OFF 56 /* important because of struct pfs__access { i64 lock_lock; i64 lock_time; } */
#define PFS_FB_INIT_ROOT 64

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

#define PFS_ELEMENT_OFFSET_FLAGS              0
#define PFS_ELEMENT_OFFSET_NAME               4
#define PFS_ELEMENT_OFFSET_PARENT_ID          8
#define PFS_ELEMENT_OFFSET_LOCK_VALUE         16 /* lock directly before lock time */
#define PFS_ELEMENT_OFFSET_LOCK_TIME          24 /* important because of struct pfs__access { i64 lock_lock; i64 lock_time; } */
#define PFS_ELEMENT_OFFSET_CREATE_TIME        32
#define PFS_ELEMENT_OFFSET_LAST_MOD_TIME      40
#define PFS_ELEMENT_OFFSET_LAST_META_MOD_TIME 48

#define PFS_FOLDER_OFFSET_ELEMENT_COUNT       56
#define PFS_FOLDER_OFFSET_FOLDER_ELEMENTS     60

#define PFS_FILE_OFFSET_FILE_LENGTH           56
#define PFS_FILE_OFFSET_FILE_DATA_TABLE       64

#define PFS_LINK_OFFSET_TARGET_ID             56
#define PFS_LINK_LINK_LENGTH                  64

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
