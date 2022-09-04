/*
 * pfs-file.c
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#include "pfs-intern.h"
#include "pfs-file.h"
#include <string.h>

struct pfs_place find_place(i64 first_block, i64 remain);

static inline int read_write(element *f, i64 position, void *buffer, i64 length, int read);

extern int pfs_file_read(element *f, i64 position, void *buffer, i64 length) {
	return read_write(f, position, buffer, length, 1);
}

extern int pfs_file_write(element *f, i64 position, void *data, i64 length) {
	return read_write(f, position, data, length, 0);
}

extern int pfs_file_append(element *f, void *data, i64 length) {
	if (length <= 0) {
		if (length == 0) { // if file_length is also zero it would lead to a dead block
			pfs->get(pfs, f->block);
			pfs->unget(pfs, f->block);
			return 1;
		}
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	struct pfs_file *file = pfs->get(pfs, f->block) + f->pos;
	struct pfs_place file_end;
	if (file->file_length == 0) {
		file_end.block = allocate_block();
		file_end.pos = 0;
	} else {
		file_end = find_place(file->first_block, file->file_length);
	}
	int cpy = pfs->block_size - 8 - file_end.pos;
	while (length > 0) {
		void *cpy_target = pfs->get(pfs, file_end.block) + file_end.pos;
		memcpy(cpy_target, data, cpy);
		length -= cpy;
		data += cpy;
		i64 next_block;
		if (length > 0) {
			next_block = allocate_block();
		} else {
			next_block = -1L;
		}
		*(i64*) (cpy_target + cpy) = next_block;
		pfs->set(pfs, file_end.block);
		file_end.block = next_block;
		file_end.pos = 0;
		cpy = pfs->block_size - 8;
	}
	return 1;
}

static inline int truncate_grow(i64 new_length, struct pfs_file *file) {
	struct pfs_place file_end = find_place(file->first_block, file->file_length);
	if (file->file_length == 0) {
		file->first_block = file_end.block = allocate_block();
		if (file_end.block == -1L) {
			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
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
			*(i64*)(current_block + pfs->block_size - 8) = -1L;
			pfs->set(pfs, file_end.block);
			break;
		}
		i64 block_num = file_end.block;
		*(i64*)(current_block + pfs->block_size - 8) = file_end.block = allocate_block();
		file_end.pos = 0;
		pfs->set(pfs, block_num);
	}
	if (file->file_length != new_length) {
		abort();
	}
	return 1;
}

static inline int truncate_shrink(i64 new_length, struct pfs_file *file) {
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
	if (file->file_length != new_length) {
		abort();
	}
	return 1;
}

extern int pfs_file_truncate(element *f, i64 new_length) {
	if (new_length < 0L) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	struct pfs_file *file = pfs->get(pfs, f->block) + f->pos;
	if (file->file_length < new_length) {
		return truncate_grow(new_length, file);
	} else if (file->file_length > new_length) {
		return truncate_shrink(new_length, file);
	}  else  {
		//  file_length == new_length
		pfs->set(pfs, f->block);
		return 1;
	}
}

i64 pfs_file_length(element *f) {
	struct pfs_file *file = pfs->get(pfs, f->block) + f->pos;
	i64 len = file->file_length;
	pfs->unget(pfs, f->block);
	return len;
}

struct pfs_place find_place(i64 first_block, i64 remain) {
	for(i64 current_block = first_block;1;remain -= pfs->block_size) {
		if (remain < (pfs->block_size - 8)) {
			struct pfs_place result;
			result.block = current_block;
			result.pos = (i32) remain;
			return result;
		}
		void* cb = pfs->get(pfs, current_block);
		i64 next_block = *(i64*) (cb + pfs->block_size - 8);
		pfs->unget(pfs, current_block);
		current_block = next_block;
	}
}

static inline int read_write(element *f, i64 position, void *buffer, i64 length, int read) {
	if (position < 0 || length < 0) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	struct pfs_file *file = pfs->get(pfs, f->block) + f->pos;
	if (file->file_length - position < length) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	i64 fb = file->first_block;
	pfs->unget(pfs, f->block);
	struct pfs_place cpy_place = find_place(fb, position);
	i32 cpy = pfs->block_size - 8 - cpy_place.pos;
	while (length > 0) {
		void *copy_block = pfs->get(pfs, cpy_place.block);
		i64 next_block = *(i64*) (copy_block + pfs->block_size - 8);
		copy_block += cpy_place.pos;
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
