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
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/errno.h>

static i64 start_time;

static void pfs_init() {
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

static int pfs__fs_do_format(i64 block_count, int force);
static void pfs__block();
static void pfs__unblock();
static int pfs__init_block(i64 block, i32 start_alloc_len);
static i32 pfs__alloc(i64 block, i32 len);
static i32 pfs__realloc(i64 block, i32 oldpos, i32 oldlen, i32 newlen,
		int do_copy);
static int pfs__native_ensure_access(struct pfs_access *lock, int is_read_only,
		i64 forbidden_data);
static inline int pfs__ensure_access(struct pfs_file_sys_element *element,
		i64 forbidden_data) {
	return pfs__native_ensure_access(&element->access,
			(element->flags & PFS_ELEMENT_FLAG_READ_ONLY) != 0, forbidden_data);
}
static void pfs__init_child(i64 parentID, i64 childID, int is_folder,
		i32 child_name_pos, i32 child_pos, i64 child_block,
		u16 *child_name_bytes, i64 targetID);
static size_t pfs__name_bytes(u16 *name);
static int pfs__lock(struct pfs_access *access);
static int pfs__update(struct pfs_element *element);

int pfs_fs_format(i64 block_count) {
	if (block_count < 2) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	pfs__block();
	int ret = pfs__fs_do_format(block_count, 0);
	pfs__unblock();
	return ret;
}
int pfs_fs_create(i64 block_count) {
	if (block_count < 2) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	int ret = pfs__fs_do_format(block_count, 1);
	return ret;

}
int pfs_fs_set_block_count(i64 block_count) {
	if (block_count < 2) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	void *data = pfs->bm->get(pfs->bm, 1);
	i32 end = *(ui32*) (data + pfs->bm->block_size - 4);
	for (i32 off = 0; off < end; off++) {
		struct pfs_allocated_blocks *current = data + off;
		if (current->end > block_count) {
			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
			pfs->bm->unget(pfs->bm, 1);
			return 0;
		}
	}
	pfs->bm->unget(pfs->bm, 1);
	data = pfs->bm->get(pfs->bm, 0);
	((struct pfs_first_block_start*) data)->block_count = block_count;
	return 1;
}
int pfs_fs_lock_fs(i64 lock_data) {
	if (lock_data & (PFS_LOCK_NO_DATA)) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	pfs__block();
	struct pfs_first_block_start *b0 = pfs->bm->get(pfs->bm, 0);
	int ret = pfs__lock(&b0->access);
	pfs->bm->set(pfs->bm, 0);
	pfs__unblock();
	return ret;
}
i64 pfs_fs_block_count() {
	struct pfs_first_block_start *b0 = pfs->bm->get(pfs->bm, 0);
	i64 block_count = b0->block_count;
	pfs->bm->unget(pfs->bm, 0);
	return block_count;
}
i32 pfs_fs_block_size() {
	struct pfs_first_block_start *b0 = pfs->bm->get(pfs->bm, 0);
	i64 block_size = b0->block_size;
	pfs->bm->unget(pfs->bm, 0);
	return block_size;
}

int pfs_element_get_parent(struct pfs_element *element) {
	if (!pfs__update(element)) {
		return 0;
	}
	void *data = pfs->bm->get(pfs->bm, element->block);
	struct pfs_file_sys_element *e = data + element->pos;
	if (e->parentID == PFS_INVALID_ID) {
		pfs->bm->unget(pfs->bm, element->block);
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	element->id = e->parentID;
	element->lock = PFS_NO_LOCK;
	pfs->bm->unget(pfs->bm, element->block);
	return 1;
}
int pfs_element_move(struct pfs_element *element, u16 *new_name,
		struct pfs_element *new_parent, i64 old_parent_lock) {
	pfs__block();
	if (!pfs__update(element)) {
		pfs__unblock();
		return 0;
	}
	void *data = pfs->bm->get(pfs->bm, element->block);
	struct pfs_file_sys_element *e = data + element->pos;
	if (new_name != NULL || new_parent != NULL) {
		if (!pfs__ensure_access(e, PFS_LOCK_NO_META_CHANGE_ALLOWED_LOCK)) {
			pfs->bm->unget(pfs->bm, element->block);
			return 0;
		}
	}
	u16 *name = new_name == NULL ? pfs__get_name(e) : new_name;
	i32 name_len = pfs__name_bytes(name);
	if (new_parent != NULL) {
		if (e->parentID == PFS_INVALID_ID) {
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			pfs->bm->unget(pfs->bm, element->block);
			pfs__unblock();
			return 0;
		}
		if (!pfs__update(new_parent)) {
			pfs->bm->unget(pfs->bm, element->block);
			pfs__unblock();
			return 0;
		}
		data = pfs->bm->get(pfs->bm, new_parent->block);
		struct pfs_file_sys_folder *np = data + new_parent->pos;
		if (!pfs__check_no_child(np, name, name_len)) {
			pfs->bm->unget(pfs->bm, element->block);
			pfs->bm->unget(pfs->bm, new_parent->block);
			pfs__unblock();
			return 0;
		}
		struct pfs_element old_parent;
		old_parent.id = e->parentID;
		if (!pfs__update(&old_parent)) {
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			pfs->bm->unget(pfs->bm, new_parent->block);
			pfs->bm->unget(pfs->bm, element->block);
			pfs__unblock();
			return 0;
		}
		data = pfs->bm->get(pfs->bm, old_parent.block);
		struct pfs_file_sys_folder *op = data + old_parent.block;
		if (!pfs__ensure_access(np, PFS_LOCK_NO_WRITE_ALLOWED_LOCK)
				| !pfs__ensure_access(op, PFS_LOCK_NO_WRITE_ALLOWED_LOCK)) {
			pfs->bm->unget(pfs->bm, old_parent.block);
			pfs->bm->unget(pfs->bm, new_parent->block);
			pfs->bm->unget(pfs->bm, element->block);
			pfs__unblock();
			return 0;
		}
		i32 nps = pvm__element_size(np);
		if (!pfs__resize(new_parent, nps, nps + 8)) {
			pfs->bm->unget(pfs->bm, old_parent.block);
			pfs->bm->unget(pfs->bm, new_parent->block);
			pfs->bm->unget(pfs->bm, element->block);
			pfs__unblock();
			return 0;
		}
		i32 ops = pvm__element_size(op);
		if (!pfs__resize(new_parent, ops, ops - 8)) {
			pfs__resize(new_parent, nps + 8, nps);
			pfs->bm->unget(pfs->bm, old_parent.block);
			pfs->bm->unget(pfs->bm, new_parent->block);
			pfs->bm->unget(pfs->bm, element->block);
			pfs__unblock();
			return 0;
		}
		for (int i = 0;; i++) {
			if (i >= op->element_count) {
				pfs__resize(new_parent, ops - 8, ops);
				pfs__resize(new_parent, nps, nps + 8);
				pfs->bm->unget(pfs->bm, old_parent.block);
				pfs->bm->unget(pfs->bm, new_parent->block);
				pfs->bm->unget(pfs->bm, element->block);
				pfs__unblock(); // correctly unblock the fs before abort
				abort();
			}
			if (op->childIDs[i] == element->id) {
				memcpy(&op->childIDs[i], &op->childIDs[i + 1],
						(op->element_count - i - 1) * sizeof(i64));
				op->element_count--;
			}
		}
		np->childIDs[np->element_count++] = element->id;
		pfs->bm->set(pfs->bm, old_parent.block);
		pfs->bm->set(pfs->bm, new_parent->block);
	}
	if (new_name != NULL) {
		if (new_parent == NULL) {
			struct pfs_element old_parent;
			old_parent.id = e->parentID;
			pfs__update(&old_parent);
			data = pfs->bm->get(pfs->bm, old_parent.block);
			struct pfs_file_sys_folder *op = data + old_parent.pos;
			if (!pfs__check_no_child(op, new_name, name_len)) {
				pfs->bm->unget(pfs->bm, element->block);
				return 0;
			}
		}
		pfs__set_name(e, new_name, name_len);
	}
	pfs->bm->set(pfs->bm, element->block);
	pfs__unblock();
	return 1;
}
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
//int pfs_element_remove_lock(struct pfs_element *element, i64 lock);
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
	pfs___ts_res_err = -1, pfs___ts_res_alive = 1, pfs___ts_res_block_free = 2,
};
static int pfs___table_grow(i64 block, i32 add_index, i32 start, i32 end);
static enum pfs___ts_res pfs___table_shrink(i64 block, i32 rem_index,
		int allow_free_block);
static int pfs___remove(i64 block, i32 rem_from, i32 rem_to, i32 remindex,
		int allow_free_block);

static int pfs__fs_do_format(i64 block_count, int force) {
	const i32 block_size = pfs->bm->block_size;
	struct pfs_first_block_start *b0 = pfs->bm->get(pfs->bm, 0);
	if (!force) {
		if (!pfs__native_ensure_access(&b0->access, 0,
		PFS_LOCK_NO_DELETE_ALLOWED_LOCK | PFS_LOCK_NO_WRITE_ALLOWED_LOCK)) {
			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
			pfs->bm->unget(pfs->bm, 0);
			return 0;
		}
	}
	if (!pfs__init_block(0,
			sizeof(struct pfs_first_block_start)
					+ sizeof(struct pfs_file_sys_folder) + 8)) {
		pfs->bm->unget(pfs->bm, 0);
		return 0;
	}
	start_time = time(NULL);
	i32 element_table_pos = pfs__alloc(0, sizeof(struct pfs_file_sys_file));
	if (element_table_pos == -1) {
		pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
		pfs->bm->unget(pfs->bm, 0);
		return 0;
	}
	b0->block_count = block_count;
	b0->block_size = block_size;
	b0->root_pos = sizeof(struct pfs_first_block_start);
	b0->root_block = 0;
	b0->element_table_block = 0;
	b0->element_table_pos = element_table_pos;
	if (force) {
		b0->file_sys_state_val = 0;
		b0->file_sys_state_time = PFS_NO_TIME;
	}
	b0->access.lock_lock = PFS_NO_LOCK;
	b0->access.lock_time = PFS_NO_TIME;
	pfs__init_child(PFS_INVALID_ID, PFS_ROOT_FOLDER_ID, 1, -1,
			sizeof(struct pfs_first_block_start), 0, NULL, PFS_INVALID_ID);
	pfs__init_child(PFS_INVALID_ID, PFS_ELEMENT_TABLE_FILE_ID, 1, -1,
			element_table_pos, 0, NULL, PFS_INVALID_ID);
	pfs->lock = PFS_NO_LOCK;
	pfs->bm->set(pfs->bm, 0);
	void *b1 = pfs->bm->get(pfs->bm, 1);
	*(i64*) b1 = 0;
	*(i64*) (b1 + 8) = 2;
	*(i32*) (b1 + block_size - 4) = 16;
	pfs->bm->set(pfs->bm, 1);
	return 1;
}
static void pfs__block() {
	if (pfs->block != 0) {
		abort();
	}
	i64 zero = 0;
	if (hashset_get(&pfs->bm->loaded, 0, &zero) != NULL) {
		abort();
	}
	while (1) {
		pfs->bm->sync(pfs->bm);
		struct pfs_first_block_start *b0 = pfs->bm->get(pfs->bm, 0L);
		i32 oldblock = (b0->file_sys_state_val);
		if (oldblock != 0) {
			if (b0->file_sys_state_time
					> start_time|| b0->file_sys_state_time == PFS_NO_TIME) {
				pfs->bm->unget(pfs->bm, 0L);
				continue;
			}
		}
		i32 block_key = rand() | 1;
		b0->file_sys_state_val = block_key;
		b0->file_sys_state_time = time(NULL);
		pfs->bm->set(pfs->bm, 0L);
		nanosleep(&waittime, NULL);
		pfs->bm->sync(pfs->bm);
		b0 = pfs->bm->get(pfs->bm, 0L);
		if (block_key == (b0->file_sys_state_val)) {
			pfs->bm->unget(pfs->bm, 0L);
			pfs->block = block_key;
			return;
		}
		pfs->bm->unget(pfs->bm, 0L);
	}
}
static void pfs__unblock() {
	if (pfs->block == 0) {
		abort();
	}
	i64 zero = 0;
	if (hashset_get(&pfs->bm->loaded, 0, &zero) != NULL) {
		abort();
	}
	struct pfs_first_block_start *b0 = pfs->bm->get(pfs->bm, 0L);
	if (pfs->block != b0->file_sys_state_val) {
		const char *msg =
				/*PATR*/"[ERROR/WARN]: unblock called, but I do not block the file system\n"
						"              (the block is set internally, but not in the file system)\n";
		write(STDERR_FILENO, msg, strlen(msg));
		pfs->block = 0;
		return;
	}
	b0->file_sys_state_val = 0;
	b0->file_sys_state_time = PFS_NO_TIME;
	pfs->bm->set(pfs->bm, 0L);
	pfs->block = 0;
	return;
}

static int pfs__init_block(i64 block, i32 start_alloc_len) {
	void *data = pfs->bm->get(pfs->bm, block);
	i32 block_size = pfs->bm->block_size;
	i32 table_start = block_size - 20;
	if (table_start < start_alloc_len) {
		pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
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
static i32 pfs__alloc(i64 block, i32 len) {
	void *data = pfs->bm->get(pfs->bm, block);
	i32 table_start = *(i32*) (data + pfs->bm->block_size - 4);
	i32 entrycount = ((ui32) (pfs->bm->block_size - 4 - table_start)) >> 1;
	void *table = data + table_start;
	if ((*(i32*) table) >= len) {
		int start = *(i32*) table;
		if (start == len) {
			*(i32*) table = 0;
		} else {
			if (!pfs___table_grow(block, 0, 0, len)) {
				start -= len;
				*(i32*) table = start;
				pfs->bm->set(pfs->bm, block);
				return start;
			}
		}
		pfs->bm->set(pfs->bm, block);
		return 0;
	}
	for (i32 i = 0; i < entrycount - 1; i++) {
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
				switch (pfs___table_shrink(block, i, 0)) {
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
				if (!pfs___table_grow(block, i, my_start, my_end)) {
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
static i32 pfs__realloc(i64 block, i32 oldpos, i32 oldlen, i32 newlen,
		int do_copy) {
	void *data = pfs->bm->get(pfs->bm, block);
	ui32 table_start = *(ui32*) (data + pfs->bm->block_size - 4);
	i32 entrycount = (pfs->bm->block_size - 4 - table_start) >> 3;
	ui32 min = 0;
	ui32 max = entrycount - 1;
	ui32 mid;
	struct pfs_table_entry *table = data + table_start;
	while (max >= min) {
		mid = (min + max) >> 1;
		if (oldpos < table[mid].start) {
			max = mid - 1;
		} else if (table[mid].end < oldpos) {
			min = mid + 1;
		} else if (newlen < oldlen) {
			int ret = pfs___remove(block, oldpos + newlen, oldlen - newlen, mid,
					1);
			pfs->bm->set(pfs->bm, block);
			if (ret == -3) {
				return -3;
			} else if (ret == -2) {
				return -2;
			} else if (newlen == 0) {
				return -1;
			} else {
				return oldpos;
			}
		} else {
			int need_new_place = 1;
			if (mid < entrycount - 1 && table[mid].end == oldpos + oldlen) {
				ui32 free = table[mid + 1].start - table[mid].end;
				if (free >= newlen - oldlen) {
					need_new_place = 0;
				}
			}
			if (need_new_place) {
				i32 new_pos = pfs__alloc(block, newlen);
				pfs___remove(block, oldpos, oldpos + oldlen, mid, 0);
				if (do_copy) {
					memcpy(data + new_pos, data + oldpos, oldlen);
				}
				pfs->bm->set(pfs->bm, block);
				return new_pos;
			} else {
				table[mid].end = oldpos + newlen;
				pfs->bm->set(pfs->bm, block);
				return oldpos;
			}
		}
	}
	abort();
}
static int pfs__native_ensure_access(struct pfs_access *access_data,
		int is_read_only, i64 forbidden_data) {
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
static void pfs__init_child(i64 parentID, i64 childID, int is_folder,
		i32 child_name_pos, i32 child_pos, i64 child_block,
		u16 *child_name_bytes, i64 targetID) {
	void *data = pfs->bm->get(pfs->bm, child_block);
	struct pfs_file_sys_element *child = data + child_pos;
	i64 current_time = time(NULL);
	child->flags = (is_folder ? PFS_ELEMENT_FLAG_FOLDER : PFS_ELEMENT_FLAG_FILE)
			| (targetID == PFS_INVALID_ID ? 0 : PFS_ELEMENT_FLAG_LINK);
	child->name_pos = child_name_pos;
	if (child_name_pos != -1) {
		memcpy(data + child_name_pos, child_name_bytes,
				pfs__name_bytes(child_name_bytes));
	}
	child->parentID = parentID;
	child->access.lock_lock = PFS_NO_LOCK;
	child->access.lock_time = PFS_NO_TIME;
	child->create_time = current_time;
	child->last_mod_time = current_time;
	child->last_meta_mod_time = current_time;
	if (targetID != PFS_INVALID_ID) {
		((struct pfs_file_sys_link*) child)->targetID = targetID;
	} else if (is_folder) {
		((struct pfs_file_sys_folder*) child)->element_count = 0;
	} else {
		((struct pfs_file_sys_file*) child)->length = 0;
	}
	pfs->bm->set(pfs->bm, child_block);
}
static size_t pfs__name_bytes(u16 *name) {
	size_t u16_count;
	for (u16_count = 0; name[u16_count]; u16_count++)
		;
	return (u16_count + 1) << 1;
}
