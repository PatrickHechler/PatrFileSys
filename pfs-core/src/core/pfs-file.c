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
 * pfs-file.c
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#include "pfs-pipe.h"
#include "pfs-file.h"
#include <stddef.h>
#include <string.h>

#define get_file(error_result) \
	void *file_block_data = pfs->get(pfs, f->element_place.block); \
	if (file_block_data == NULL) { \
		return error_result; \
	} \
	struct pfs_file *file = file_block_data + f->element_place.pos;

#define get_pipe(error_result) \
	void *file_block_data = pfs->get(pfs, p->element_place.block); \
	if (file_block_data == NULL) { \
		pfs_err = PFS_ERRNO_UNKNOWN_ERROR; \
		return error_result; \
	} \
	struct pfs_pipe *pipe = file_block_data + p->element_place.pos;

static inline int read_write(pfs_eh f, i64 position, void *buffer, i64 length,
		const int read, const int pipe);

int pfsc_file_read(pfs_eh f, i64 position, void *buffer, i64 length) {
	return read_write(f, position, buffer, length, 1, 0);
}

int pfsc_file_write(pfs_eh f, i64 position, void *data, i64 length) {
	return read_write(f, position, data, length, 0, 0);
}

i64 pfsc_file_append(pfs_eh f, void *data, i64 length) {
	if (length <= 0) {
		if (length == 0) {
			return 0;
		}
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return -1;
	}
	get_file(-1)
	file->element.last_mod_time = time(NULL);
	struct pfs_place file_end;
	if (file->file_length == 0) {
		file_end.block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_DATA);
		file->first_block = file_end.block;
		file_end.pos = 0;
	} else {
		file_end = find_place(file->first_block, file->file_length);
	}
	i64 appended = 0L;
	int cpy = pfs->block_size - 8 - file_end.pos;
	while (file_end.block != -1) {
		void *block_data;
		if (file_end.pos) {
			block_data = pfs->get(pfs, file_end.block);
		} else {
			block_data = pfs->lazy_get(pfs, file_end.block);
		}
		if (block_data == NULL) {
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
		file->file_length += cpy;
		i64 next_block;
		if (length > 0) {
			next_block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_DATA);
		} else {
			next_block = -1L;
		}
		*(i64*) (block_data + pfs->block_size - 8) = next_block;
		pfs->set(pfs, file_end.block);
		file_end.block = next_block;
		if (file_end.pos) {// no need to set them to the values they already have
			file_end.pos = 0;
			cpy = pfs->block_size - 8;
		}
	}
	pfs->set(pfs, f->element_place.block);
	return appended;
}

static inline int truncate_shrink(pfs_eh f, i64 new_length) {
	struct pfs_file *file = pfs->get(pfs, f->element_place.block)
			+ f->element_place.pos;
	i64 remain = file->file_length - new_length;
	struct pfs_place place = find_place(file->first_block, new_length);
	int first = 1;
	if (new_length == 0) {
		file->first_block = -1L;
		first = 0;
	}
	file->file_length = new_length;
	while (remain > 0) {
		const i64 current_block_num = place.block;
		void *current_block = pfs->get(pfs, current_block_num);
		place.block = *(i64*) (current_block + pfs->block_size - 8);
		if (first) {
			*(i64*) (current_block + pfs->block_size - 8) = -1L;
			pfs->set(pfs, current_block_num);
			first = 0;
		} else {
			pfs->unget(pfs, current_block_num);
		}
		if (place.pos) {
			place.pos = 0;
		} else {
			free_block(current_block_num);
		}
		i64 free_data = pfs->block_size - 8 - place.pos;
		remain -= free_data;
	}
	pfs->set(pfs, f->element_place.block);
	return 1;
}

int pfsc_file_truncate_grow(pfs_eh f, i64 new_length) {
	get_file(0)
	const i64 old_length = file->file_length;
	file->element.last_mod_time = time(NULL);
	struct pfs_place file_end;
	if (file->file_length == 0) {
		file_end.block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_DATA);
		file->first_block = file_end.block;
		file_end.pos = 0;
	} else {
		file_end = find_place(file->first_block, file->file_length);
	}
	i64 add_length = new_length - file->file_length;
	int set_len = pfs->block_size - 8 - file_end.pos;
	while (file_end.block != -1) {
		void *block_data;
		if (file_end.pos) {
			block_data = pfs->get(pfs, file_end.block);
		} else {
			block_data = pfs->lazy_get(pfs, file_end.block);
		}
		if (block_data == NULL) {
			break;
		}
		void *set_target = block_data + file_end.pos;
		if (set_len > add_length) {
			set_len = add_length;
		}
		memset(set_target, 0, set_len);
		add_length -= set_len;
		file->file_length += set_len;
		i64 next_block;
		if (add_length > 0) {
			next_block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_DATA);
		} else {
			next_block = -1L;
		}
		*(i64*) (block_data + pfs->block_size - 8) = next_block;
		pfs->set(pfs, file_end.block);
		file_end.block = next_block;
		if (file_end.pos) {// no need to set them to the values they already have
			file_end.pos = 0;
			set_len = pfs->block_size - 8;
		}
	}
	pfs->set(pfs, f->element_place.block);
	return 1;
}

int pfsc_file_truncate(pfs_eh f, i64 new_length) {
	if (new_length < 0L) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	get_file(0)
	file->element.last_mod_time = time(NULL);
	int res;
	if (file->file_length < new_length) {
		res = pfsc_file_truncate_grow(f, new_length);
	} else { // shrink can handle no change
		res = truncate_shrink(f, new_length);
	}
	pfs->unget(pfs, f->element_place.block);
	return res;
}

i64 pfsc_file_length(pfs_eh f) {
	get_file(-1)
	i64 len = file->file_length;
	pfs->unget(pfs, f->element_place.block);
	return len;
}

i64 pfsc_pipe_length(pfs_eh p) {
	get_pipe(-1)
	i64 len = pipe->file.file_length - pipe->start_offset;
	pfs->unget(pfs, p->element_place.block);
	return len;
}

int pfsc_pipe_read(pfs_eh p, void *buffer, i64 length) {
	return read_write(p, 0, buffer, length, 1, 1);
}

static inline int read_write(pfs_eh f, i64 position, void *buffer, i64 length,
		const int read, const int pipe) {
	if (position < 0 || length <= 0) {
		if (length == 0) {
			return 1;
		}
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	get_file(0)
	if (pipe) {
		position = ((struct pfs_pipe*) file)->start_offset;
	}
	if (file->file_length - position < length) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		pfs->unget(pfs, f->element_place.block);
		return 0;
	}
	i64 fb = file->first_block;
	struct pfs_place cpy_place;
	if (!pipe) {
		if (!read) {
			file->element.last_mod_time = time(NULL);
			pfs->set(pfs, f->element_place.block);
		} else {
			pfs->unget(pfs, f->element_place.block);
		}
		cpy_place = find_place(fb, position);
	} else {
		cpy_place.block = fb;
		cpy_place.pos = position;
	}
	i32 cpy = pfs->block_size - 8 - cpy_place.pos;
	while (1) {
		void *copy_block = pfs->get(pfs, cpy_place.block);
		i64 next_block = *(i64*) (copy_block + pfs->block_size - 8);
		copy_block += cpy_place.pos;
		if (cpy > length) {
			cpy = length;
		}
		if (read) {
			memcpy(buffer, copy_block, cpy);
			if (pipe) {
				if (cpy_place.pos + cpy == pfs->block_size - 8) {
					file->file_length -= pfs->block_size - 8;
					file->first_block = next_block;
					free_block(cpy_place.block);
				}
			}
			pfs->unget(pfs, cpy_place.block);
		} else {
			memcpy(copy_block, buffer, cpy);
			pfs->set(pfs, cpy_place.block);
		}
		length -= cpy;
		buffer += cpy;
		if (length == 0) {
			if (pipe) {
				if (file->file_length > 0) {
					((struct pfs_pipe*) file)->start_offset = cpy_place.pos
							+ cpy;
				} else if (file->file_length < 0) {
					abort();
				} else {
					((struct pfs_pipe*) file)->start_offset = 0;
					file->first_block = -1;
				}
				pfs->set(pfs, f->element_place.block);
			}
			return 1;
		}
		cpy_place.block = next_block;
		cpy_place.pos = 0;
		cpy = pfs->block_size - 8;
	}
}
