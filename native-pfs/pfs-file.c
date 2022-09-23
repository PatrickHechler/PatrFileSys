/*
 * pfs-file.c
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#include "pfs-intern.h"
#include "pfs-file.h"
#include <string.h>

#define get_file(error_result) \
	void *file_block_data = pfs->get(pfs, f->element_place.block); \
	if (file_block_data == NULL) { \
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR; \
		return error_result; \
	} \
	struct pfs_file *file = file_block_data + f->element_place.pos;

static inline int read_write(pfs_eh f, i64 position, void *buffer, i64 length, int read);

extern int pfs_file_read(pfs_eh f, i64 position, void *buffer, i64 length) {
	return read_write(f, position, buffer, length, 1);
}

extern int pfs_file_write(pfs_eh f, i64 position, void *data, i64 length) {
	return read_write(f, position, data, length, 0);
}

extern i64 pfs_file_append(pfs_eh f, void *data, i64 length) {
	if (length <= 0) {
		if (length == 0) {
			return 0;
		}
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return -1;
	}
	get_file(-1)
	struct pfs_place file_end;
	if (file->file_length == 0) {
		file->first_block = file_end.block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_FILE_DATA);
		file_end.pos = 0;
	} else {
		file_end = find_place(file->first_block, file->file_length);
	}
	i64 appended = 0L;
	int cpy = pfs->block_size - 8 - file_end.pos;
	while (file_end.block != -1) {
		void *block_data = pfs->get(pfs, file_end.block);
		if (block_data == NULL) {
			pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		void *cpy_target = block_data + file_end.pos;
		if (cpy > length) {
			cpy = length;
		}
		memcpy(cpy_target, data, cpy);
		length -= cpy;
		data += cpy;
		appended += cpy;
		i64 next_block;
		if (length > 0) {
			next_block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_FILE_DATA);
			if (next_block == -1) {
				pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
			}
		} else {
			next_block = -1L;
		}
		*(i64*) (cpy_target + cpy) = next_block;
		pfs->set(pfs, file_end.block);
		file_end.block = next_block;
		file_end.pos = 0;
		cpy = pfs->block_size - 8;
	}
	file->file_length += appended;
	pfs->set(pfs, f->element_place.block);
	return appended;
}

static inline int truncate_shrink(i64 new_length, pfs_eh f) {
	struct pfs_file *file = pfs->get(pfs, f->element_place.block) + f->element_place.pos;
	i64 remain = file->file_length - new_length;
	struct pfs_place place = find_place(file->first_block, new_length);
	if (new_length == 0) {
		file->first_block = -1L;
	}
	file->file_length = new_length;
	while (remain > 0) {
		const i64 current_block_num = place.block;
		void *current_block = pfs->get(pfs, current_block_num);
		place.block = *(i64*) (current_block + pfs->block_size - 8);
		*(i64*) (current_block + pfs->block_size - 8) = -1L;
		if (place.pos == 0) {
			free_block(current_block_num);
		}
		i64 free_data = pfs->block_size - 8 - place.pos;
		remain -= free_data;
		pfs->set(pfs, current_block_num);
	}
	pfs->set(pfs, f->element_place.block);
	return 1;
}

static inline int truncate_grow(i64 new_length, pfs_eh f) {
	struct pfs_file *file = pfs->get(pfs, f->element_place.block) + f->element_place.pos;
	const i64 old_length = file->file_length;
	struct pfs_place file_end = find_place(file->first_block, old_length);
	if (old_length == 0) {
		file->first_block = file_end.block = allocate_block(
		BLOCK_FLAG_USED | BLOCK_FLAG_FILE_DATA);
		if (file_end.block == -1L) {
			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
			pfs->unget(pfs, f->element_place.block);
			return 0;
		}
	}
	while (1) {
		i64 set = pfs->block_size - 8 - file_end.pos;
		if (set + file->file_length > new_length) {
			set = new_length - file->file_length;
		}
		void *current_block = pfs->get(pfs, file_end.block);
		void *set_data = current_block + file_end.pos;
		memset(set_data, 0, set);
		file->file_length += set;
		if (file->file_length >= new_length) {
			*(i64*) (current_block + pfs->block_size - 8) = -1L;
			pfs->set(pfs, file_end.block);
			break;
		}
		const i64 current_block_num = file_end.block;
		*(i64*) (current_block + pfs->block_size - 8) = file_end.block = allocate_block(
		BLOCK_FLAG_USED | BLOCK_FLAG_FILE_DATA);
		file_end.pos = 0;
		pfs->set(pfs, current_block_num);
		if (file_end.block == -1L) {
			truncate_shrink(old_length, f);
			pfs->set(pfs, f->element_place.block);
			return 0;
		}
	}
	if (file->file_length != new_length) {
		abort();
	}
	pfs->set(pfs, f->element_place.block);
	return 1;
}

extern int pfs_file_truncate(pfs_eh f, i64 new_length) {
	if (new_length < 0L) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	get_file(0)
	int res;
	if (file->file_length < new_length) {
		res = truncate_grow(new_length, f);
	} else { // shrink can handle no change
		res = truncate_shrink(new_length, f);
	}
	pfs->unget(pfs, f->element_place.block);
	return res;
}

i64 pfs_file_length(pfs_eh f) {
	get_file(-1)
	i64 len = file->file_length;
	pfs->unget(pfs, f->element_place.block);
	return len;
}

static inline int read_write(pfs_eh f, i64 position, void *buffer, i64 length, int read) {
	if (position < 0 || length < 0) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	get_file(0)
	if (file->file_length - position < length) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	i64 fb = file->first_block;
	pfs->unget(pfs, f->element_place.block);
	struct pfs_place cpy_place = find_place(fb, position);
	i32 cpy = pfs->block_size - 8 - cpy_place.pos;
	while (length > 0) {
		void *copy_block = pfs->get(pfs, cpy_place.block);
		i64 next_block = *(i64*) (copy_block + pfs->block_size - 8);
		copy_block += cpy_place.pos;
		if (cpy > length) {
			cpy = length;
		}
		if (read) {
			memcpy(buffer, copy_block, cpy);
			pfs->unget(pfs, cpy_place.block);
		} else {
			memcpy(copy_block, buffer, cpy);
			pfs->set(pfs, cpy_place.block);
		}
		length -= cpy;
		buffer += cpy;
		cpy_place.block = next_block;
		cpy_place.pos = 0;
		cpy = pfs->block_size - 8;
	}
	return 1;
}
