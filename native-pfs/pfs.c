/*
 * pfs.c
 *
 *  Created on: Jul 6, 2022
 *      Author: pat
 */

#define INTERN_PFS

#include "patr-file-sys.h"
#include "pfs.h"
#include "pfs-err.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stddef.h>
#include <assert.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/errno.h>

static_assert(sizeof(__time_t) == 8, "Error!");

static i64 start_time;

void pfs_init() {
	char *data = malloc(64);
	char *dat1 = data;
	i64 current = time(NULL);
	int fd = open("/proc/uptime", O_RDONLY);
	int reat = read(fd, data, 64);
	if (reat <= 0) {
		abort();
	}
	i64 sub = 0;
	int comma = 0;
	for (reat--; ' ' < *data && comma < 4; data++, reat--) {
		if (reat < 0) {
			data = dat1;
			reat = read(fd, data, 64);
			if (reat < 0) {
				abort();
			}
			reat--;
		}
		if ('.' == *data) {
			if (comma != 0) {
				abort();
			}
			comma = 1;
		} else if ('0' > *data) {
			abort();
		} else if ('9' < *data) {
			abort();
		} else {
			if (comma != 0) {
				comma++;
			}
			sub *= 10;
			sub += (*data) - '0';
		}
	}
	for (; comma < 4; comma++) {
		sub *= 10;
	}
	start_time = current - sub;
	free(dat1);
}

struct pfs__access {
	i64 lock_lock;
	i64 lock_time;
};

static void pfs__block(struct pfs_file_sys *pfs);
static void pfs__unblock(struct pfs_file_sys *pfs);
static int pfs__init_block(struct pfs_file_sys *pfs, i64 block,
		i32 start_alloc_len);
static i32 pfs__alloc(struct pfs_file_sys *pfs, i64 block, i32 len);
static i32 pfs__realloc(struct pfs_file_sys *pfs, i64 block, i32 oldpntr, i32 oldlen, i32 newlen);
static int pfs__native_ensure_access(struct pfs_file_sys *pfs,
		struct pfs__access *lock, int is_read_only, i64 forbidden_data);
static inline int pfs__ensure_access(struct pfs_file_sys *pfs, void *element,
		i64 forbidden_data) {
	return pfs__native_ensure_access(pfs,
			element + PFS_ELEMENT_OFFSET_LOCK_VALUE,
			((*(i32*) (element + PFS_ELEMENT_OFFSET_FLAGS))
					& PFS_ELEMENT_FLAG_READ_ONLY) != 0, forbidden_data);
}
static void pfs__init_child(struct pfs_file_sys *pfs, i64 parentID, i64 childID,
		int is_folder, i32 child_name_pos, i32 child_pos, i64 child_block,
		u16 *child_name_bytes, i64 targetID);
static size_t pfs__name_bytes(u16 *name);

int pfs_fs_format(struct pfs_file_sys *pfs, i64 block_count) {
	if (block_count < 2) {
		return 0;
	}
	pfs__block(pfs);
	const i32 block_size = pfs->bm->block_size;
	void *b0 = pfs->bm->get(pfs->bm, 0);
	if (!pfs__native_ensure_access(pfs, b0 + PFS_FB_FILE_SYS_LOCK_VAL_OFF, 0,
			PFS_LOCK_NO_DELETE_ALLOWED_LOCK | PFS_LOCK_NO_WRITE_ALLOWED_LOCK)) {
		return 0;
	}
	if (!pfs__init_block(pfs, 0,
	PFS_FB_INIT_ROOT + PFS_FOLDER_OFFSET_FOLDER_ELEMENTS + 8)) {
		return 0;
	}
	start_time = time(NULL);
	void *b1 = pfs->bm->get(pfs->bm, 1);
	*(i64*) b1 = 0;
	*(i64*) (b1 + 8) = 2;
	*(i64*) (b1 + block_size - 4) = 16;
	pfs->bm->set(pfs->bm, 1);
	i32 element_table_pos = pfs__alloc(pfs, 0, PFS_FILE_OFFSET_FILE_DATA_TABLE);
	*(i64*) (b0 + PFS_FB_BLOCK_COUNT_OFF) = block_count;
	*(i32*) (b0 + PFS_FB_BLOCK_SIZE_OFF) = block_size;
	*(i32*) (b0 + PFS_FB_ROOT_POS_OFF) = PFS_FB_INIT_ROOT;
	*(i64*) (b0 + PFS_FB_ROOT_BLOCK_OFF) = 0;
	*(i64*) (b0 + PFS_FB_ELEMENT_TABLE_BLOCK_OFF) = 0;
	*(i32*) (b0 + PFS_FB_ELEMENT_TABLE_POS_OFF) = element_table_pos;
//	FB_FILE_SYS_STATE_VALUE // doing this with state lock
//	FB_FILE_SYS_STATE_TIME
	*(i64*) (b0 + PFS_FB_FILE_SYS_LOCK_VAL_OFF) = PFS_NO_LOCK;
	*(i64*) (b0 + PFS_FB_FILE_SYS_LOCK_TIME_OFF) = PFS_NO_TIME;
	pfs__init_child(pfs, PFS_INVALID_ID, PFS_ROOT_FOLDER_ID, 1, -1,
	PFS_FB_INIT_ROOT, 0, NULL, PFS_INVALID_ID);
	pfs__init_child(pfs, PFS_INVALID_ID, PFS_ELEMENT_TABLE_FILE_ID, 1, -1,
			element_table_pos, 0, NULL, PFS_INVALID_ID);
	pfs->lock = PFS_NO_LOCK;
	pfs->bm->set(pfs->bm, 0);
	pfs__unblock(pfs);
	return 1;
}
//int pfs_fs_set_block_count(struct pfs_file_sys *pfs, i64 block_count);
//void pfs_fs_lock_fs(struct pfs_file_sys *pfs, i64 lock_data);
//i64 pfs_fs_block_count(struct pfs_file_sys *pfs);
//i32 pfs_fs_block_size(struct pfs_file_sys *pfs);
//int pfs_element_get_parent(struct pfs_element *element);
//int pfs_element_move(struct pfs_element *element, wchar_t *new_name,
//i32 pfs_element_get_flags(struct pfs_element *element);
//int pfs_element_get_flag(struct pfs_element *element, i32 add, i32 rem);
//int pfs_element_get_create(struct pfs_element *element, i64 *time);
//int pfs_element_get_las_mod(struct pfs_element *element, i64 *time);
//int pfs_element_get_last_meta_mod(struct pfs_element *element, i64 *time);
//int pfs_element_get_create(struct pfs_element *element, i64 time);
//int pfs_element_get_las_mod(struct pfs_element *element, i64 time);
//int pfs_element_get_last_meta_mod(struct pfs_element *element, i64 time);
//i64 pfs_element_get_lock_data(struct pfs_element *element);
//int pfs_element_get_lock_time(struct pfs_element *element, i64 *time);
//int pfs_element_lock(struct pfs_element *element, i64 lock);
//int pfs_element_remove_lock(struct pfs_element *element, i64 lock);//
//int pfs_element_delete(struct pfs_element *element);
//int pfs_element_get_name(struct pfs_element *element, i32 size, wchar_t **name);
//i64 pfs_file_read(struct pfs_element *file, i64 off, void *data, i64 len);
//int pfs_file_write(struct pfs_element *file, i64 off, void *data, i64 len);
//int pfs_file_append(struct pfs_element *file, void *data, i64 len);
//int pfs_file_truncate(struct pfs_element *file, i64 new_len);
//int pfs_folder_add_folder(struct pfs_element *folder, wchar_t *name);
//int pfs_folder_add_file(struct pfs_element *folder, wchar_t *name);
//int pfs_folder_add_folder(struct pfs_element *folder, wchar_t *name, i64 targetID);
//i32 pfs_folder_child_count(struct pfs_element *folder);
//int pfs_folder_get_child(struct pfs_element *folder, wchar_t *name);
//int pfs_folder_get_child(struct pfs_element *folder, i32 index);
//int pfs_link_get_target(struct pfs_element *link);
//int pfs_link_set_target(struct pfs_element *link, i64 targetID);

const static struct timespec waittime = { 0L, 50000000L, };

enum pfs___ts_res {
	pfs___ts_res_err = -1,
	pfs___ts_res_alive = 1,
	pfs___ts_res_block_free = 2,
};

static int pfs___table_grow(struct pfs_file_sys *pfs, i64 block, i32 add_index, i32 start, i32 end);
static enum pfs___ts_res pfs___table_shrink(struct pfs_file_sys *pfs, i64 block, i32 rem_index, int allow_free_block);

static void pfs__block(struct pfs_file_sys *pfs) {
	if (pfs->block != 0) {
		abort();
	}
	i64 zero = 0;
	if (hashset_get(&pfs->bm->loaded, 0, &zero) != NULL) {
		abort();
	}
	while (1) {
		pfs->bm->sync(pfs->bm);
		void *b0 = pfs->bm->get(pfs->bm, 0L);
		i32 oldblock = *(i32*) (b0 + PFS_FB_FILE_SYS_STATE_VAL_OFF);
		if (oldblock != 0) {
			i64 oldtime = *(i64*) (b0 + PFS_FB_FILE_SYS_STATE_TIME_OFF);
			if (oldtime > start_time) {
				pfs->bm->unget(pfs->bm, 0L);
				continue;
			}
		}
		i32 block_key = rand() | 1;
		*(i32*) (b0 + PFS_FB_FILE_SYS_STATE_VAL_OFF) = block_key;
		*(i64*) (b0 + PFS_FB_FILE_SYS_STATE_VAL_OFF) = time(NULL);
		pfs->bm->set(pfs->bm, 0L);
		nanosleep(&waittime, NULL);
		pfs->bm->sync(pfs->bm);
		b0 = pfs->bm->get(pfs->bm, 0L);
		if (block_key == *(i32*) (b0 + PFS_FB_FILE_SYS_STATE_VAL_OFF)) {
			pfs->bm->unget(pfs->bm, 0L);
			pfs->block = block_key;
			return;
		}
		pfs->bm->unget(pfs->bm, 0L);
	}
}
static void pfs__unblock(struct pfs_file_sys *pfs) {
	if (pfs->block == 0) {
		abort();
	}
	i64 zero = 0;
	if (hashset_get(&pfs->bm->loaded, 0, &zero) != NULL) {
		abort();
	}
	void *b0 = pfs->bm->get(pfs->bm, 0L);
	if (pfs->block != *(i32*) (b0 + PFS_FB_FILE_SYS_STATE_VAL_OFF)) {
		const char *msg =
				/*PATR*/"[ERROR/WARN]: unblock called, but I do not block the file system\n"
						"              (the block is set internally, but not in the file system)\n";
		write(STDERR_FILENO, msg, strlen(msg));
		pfs->block = 0;
		return;
	}
	*(i32*) (b0 + PFS_FB_FILE_SYS_STATE_VAL_OFF) = 0;
	*(i64*) (b0 + PFS_FB_FILE_SYS_STATE_TIME_OFF) = PFS_NO_TIME;
	pfs->bm->set(pfs->bm, 0L);
	pfs->block = 0;
	return;
}

static int pfs__init_block(struct pfs_file_sys *pfs, i64 block,
		i32 start_alloc_len) {
	void *data = pfs->bm->get(pfs->bm, block);
	i32 block_size = pfs->bm->block_size;
	i32 table_start = block_size - 20;
	if (table_start < start_alloc_len) {
		return 0;
	}
	*(i32*) (data + block_size - 4) = table_start;
	*(i32*) (data + table_start + 12) = block_size;
	*(i32*) (data + table_start + 8) = table_start;
	*(i32*) (data + table_start + 4) = start_alloc_len;
	*(i32*) (data + table_start) = 0;
	pfs->bm->set(pfs->bm, block);
	return 1;
}
static i32 pfs__alloc(struct pfs_file_sys *pfs, i64 block, i32 len) {
	void *data = pfs->bm->get(pfs->bm, block);
	i32 table_start = *(i32*) (data + pfs->bm->block_size - 4);
	i32 entrycount = ((ui32) (pfs->bm->block_size - 4 - table_start)) >> 1;
	void *table = data + table_start;
	if ((*(i32*) table) >= len) {
		int start = *(i32*) table;
		if (start == len) {
			*(i32*) table = 0;
		} else {
			if (!pfs___table_grow(pfs, block, 0, 0, len))  {
				start -= len;
				*(i32*) table = start;
				pfs->bm->set(pfs->bm, block);
				return start;
			}
		}
		pfs->bm->set(pfs->bm, block);
		return 0;
	}
	for (i32 i = 0; i < entrycount - 1; i ++) {
		i32 off = (i << 3);
		i32 this_end = *(i32*) (table + off + 4);
		i32 next_start = *(i32*) (table + off + 8);
		i32 free_place = next_start - this_end;
		if (free_place >= len) {
			i32 space = (free_place - len) >> 1;
			i32 my_start = this_end + space;
			i32 my_end = my_start + len;
			if (free_place == len) {
				i32 new_start = *(i32*) (table + off);
				switch (pfs___table_shrink(pfs, block, i, 0)) {
				case pfs___ts_res_alive:
					*(i32*) (table + off + 8) = new_start;
					break;
				case pfs___ts_res_block_free:
					// can not happen
					abort();
				case pfs___ts_res_err:
					pfs->bm->unget(pfs->bm, block);
					return -1;
				default:
					abort();
				}
			} else if (space == 0) {
				*(i32*) (table + off + 4) = my_end;
			} else {
				if (!pfs___table_grow(pfs, block, i, my_start, my_end)) {
					pfs->bm->unget(pfs->bm, block);
					return -1;
				}
			}
			return my_start;
		}
	}
	pfs->bm->set(pfs->bm, block);
	return -1;
}
static i32 pfs__realloc(struct pfs_file_sys *pfs, i64 block, i32 oldpntr, i32 oldlen, i32 newlen) {
	void *data = pfs->bm->get(pfs->bm, block);

}
static int pfs__native_ensure_access(struct pfs_file_sys *pfs,
		struct pfs__access *access_data, int is_read_only, i64 forbidden_data) {
	if (forbidden_data & (PFS_LOCK_NO_DATA)) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (access_data->lock_time != PFS_NO_TIME) {
		if (access_data->lock_lock & forbidden_data) {
			pfs_errno = PFS_ERRNO_ELEMENT_LOCKED;
			return 0;
		} else if (is_read_only
				&& (forbidden_data
						& (PFS_LOCK_NO_WRITE_ALLOWED_LOCK
								| PFS_LOCK_NO_DELETE_ALLOWED_LOCK))) {
			pfs_errno = PFS_ERRNO_READ_ONLY;
			return 0;
		}
	}
	return 1;
}
static void pfs__init_child(struct pfs_file_sys *pfs, i64 parentID, i64 childID,
		int is_folder, i32 child_name_pos, i32 child_pos, i64 child_block,
		u16 *child_name_bytes, i64 targetID) {
	void *data = pfs->bm->get(pfs->bm, child_block);
	void *child = data + child_pos;
	i64 current_time = time(NULL);
	*(i32*) (child + PFS_ELEMENT_OFFSET_FLAGS) = (
			is_folder ? PFS_ELEMENT_FLAG_FOLDER : PFS_ELEMENT_FLAG_FILE)
			| (targetID == PFS_INVALID_ID ? 0 : PFS_ELEMENT_FLAG_LINK);
	*(i64*) (child + PFS_ELEMENT_OFFSET_NAME) = child_name_pos;
	if (child_name_pos != -1) {
		memcpy(data + child_name_pos, child_name_bytes,
				pfs__name_bytes(child_name_bytes));
	}
	*(i64*) (child + PFS_ELEMENT_OFFSET_NAME) = parentID;
	*(i64*) (child + PFS_ELEMENT_OFFSET_LOCK_VALUE) = PFS_NO_LOCK;
	*(i64*) (child + PFS_ELEMENT_OFFSET_LOCK_TIME) = PFS_NO_TIME;
	*(i64*) (child + PFS_ELEMENT_OFFSET_CREATE_TIME) = current_time;
	*(i64*) (child + PFS_ELEMENT_OFFSET_LAST_MOD_TIME) = current_time;
	*(i64*) (child + PFS_ELEMENT_OFFSET_LAST_META_MOD_TIME) = current_time;
	if (targetID != PFS_INVALID_ID) {
		*(i64*) (child + PFS_LINK_OFFSET_TARGET_ID) = targetID;
	} else if (is_folder) {
		*(i32*) (child + PFS_FOLDER_OFFSET_ELEMENT_COUNT) = 0;
	} else {
		*(i64*) (child + PFS_FILE_OFFSET_FILE_LENGTH) = 0;
	}
	pfs->bm->set(pfs->bm, child_block);
}
static size_t pfs__name_bytes(u16 *name) {
	size_t u16_count;
	for (u16_count = 0; name[u16_count]; u16_count++)
		;
	return (u16_count + 1) << 1;
}
