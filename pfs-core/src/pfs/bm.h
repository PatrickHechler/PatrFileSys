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
 * bm.h
 *
 *  Created on: Jul 6, 2022
 *      Author: pat
 */

#ifndef BM_H_
#define BM_H_

#include "patr-file-sys.h"
#include "hashset.h"
#include "pfs-constants.h"

#include <stdio.h>
#include <errno.h>
#include <stdarg.h>
#include <fcntl.h>

#ifndef PFS_PORTABLE_BUILD
#include <unistd.h>
#endif

/**
 * block manager structure used by the patr-file-system
 */
struct bm_block_manager {
	/**
	 * this hash-set is only for intern use
	 */
	struct hashset loaded;
	/**
	 * like get, but this function is allowed to load garbage and not
	 * the data from the block
	 * this should be used when the old content is irrelevant/ignored
	 */
	void* (*const lazy_get)(struct bm_block_manager *bm, i64 block);
	/**
	 * function to load/get a block
	 */
	void* (*const get)(struct bm_block_manager *bm, i64 block);
	/**
	 * function to unload/un-get a block necessarily without saving the block
	 * useful when no changes has been made, but the block may been changed
	 * anyways because it also has been set
	 */
	int (*const unget)(struct bm_block_manager *bm, i64 block);
	/**
	 * function to unload/un-get a block with saving
	 */
	int (*const set)(struct bm_block_manager *bm, i64 block);
	/**
	 * synchronizes the block manager
	 */
	int (*const sync_bm)(struct bm_block_manager *bm);
	/**
	 * closes the block manager
	 *
	 * regardless of the return value, the block manager should not be used again
	 * (also not for a second close)
	 */
	int (*const close_bm)(struct bm_block_manager *bm);
	/**
	 * the size of all blocks
	 */
	const ui32 block_size;
	/**
	 * the number of bits usable for block flagging
	 * (maximum value is 63)
	 * the lowest bits will be used for the flags
	 */
	const int block_flag_bits;
	/**
	 * get the flags of a block
	 * if not all block_flag_bits is lower than 63 the lowest block_flag_bits bits will be used for the flags
	 * the unsupported bits are filled with zeros
	 * if an error occurred -1 is returned
	 */
	i64 (*const get_flags)(struct bm_block_manager *bm, i64 block);
	/**
	 * set the flags of a block
	 * if not all block_flag_bits is lower than 63 the lowest block_flag_bits bits will be used for the flags
	 * the unsupported bits are ignored
	 * if an error occurred 0 is returned otherwise 1
	 */
	int (*const set_flags)(struct bm_block_manager *bm, i64 block, ui64 flags);
	/**
	 * gets the first zero-flagged block
	 * if all blocks are flagged (or block flagging is not supported) or an error occurred -1 is returned
	 */
	i64 (*const first_zero_flagged_block)(struct bm_block_manager *bm);
	/**
	 * sets all flags to zero
	 * if an error occurred 0 is returned otherwise 1
	 */
	int (*const delete_all_flags)(struct bm_block_manager *bm);
};

/**
 * creates a new ram block manager
 *
 * block_count: the number of blocks in the block manager
 *
 * block_size: the size of the blocks
 */
extern struct bm_block_manager* bm_new_ram_block_manager(i64 block_count,
		i32 block_size);

/**
 * creates a new ram block manager
 * the returned block manager will support the flagging of blocks
 *
 * block_count: the number of blocks in the block manager
 *
 * block_size: the size of the blocks
 */
extern struct bm_block_manager* bm_new_flaggable_ram_block_manager(
		i64 block_count, i32 block_size);

/**
 * creates a new file block manager
 *
 * if a file block manager with the given file was already created
 * once, it will be returned instead.
 * if this happens, each time the file block manager is returned
 * here, it has to be closed once more to let the close actually happen
 *
 * file: the file descriptor
 *
 * block_size: the size of the blocks
 */
extern struct bm_block_manager* bm_new_file_block_manager_path_bs(const char *file,
		i32 block_size, int read_only);

extern struct bm_block_manager* bm_new_file_block_manager_path(const char *file,
		int read_only);

/*
 * operations with bm_fd:
 * i64   bm_fd_read(bm_fd fd, void *buf, size_t len)
 * i64   bm_fd_write(bm_fd fd, void *data, size_t len)
 * i64   bm_fd_pos(bm_fd fd)
 * i64   bm_fd_seek(bm_fd fd, size_t pos)
 * i64   bm_fd_seek_eof(bm_fd fd)
 * i64   bm_fd_flush(bm_fd fd)
 * bm_fd bm_fd_open(const char *file, int read_only)
 * bm_fd bm_fd_open_ro(const char *file)
 * bm_fd bm_fd_open_rw(const char *file)
 * bm_fd bm_fd_open_rw_trunc(const char *file)
 * int   bm_fd_close(bm_fd fd)
 */

#ifdef PFS_PORTABLE_BUILD
#define bm_fd_read(fd, buf, len) fread(buf, 1, len, fd)
#define bm_fd_write(fd, data, len) fwrite(data, 1, len, fd)
#define bm_fd_pos(fd) fseek(fd, 0, SEEK_CUR)
#define bm_fd_seek(fd, pos) fseek(fd, pos, SEEK_SET)
#define bm_fd_seek_eof(fd) fseek(fd, 0, SEEK_END)
#define bm_fd_flush(fd) fflush(fd)
#define bm_fd_open_ro(file) fopen(file, "rb")
#define bm_fd_open_rw(file) fopen(file, "r+b")
#define bm_fd_open_rw_trunc(file) fopen(file, "w+b")
#define bm_fd_close(fd) fclose(fd)
#else
#define bm_fd_read(fd, buf, len) read(fd, buf, len)
#define bm_fd_write(fd, data, len) write(fd, data, len)
#define bm_fd_pos(fd) lseek64(fd, 0, SEEK_CUR)
#define bm_fd_seek(fd, pos) lseek64(fd, pos, SEEK_SET)
#define bm_fd_seek_eof(fd) lseek64(fd, 0, SEEK_END)
#define bm_fd_flush(fd) fdatasync(fd)
#define bm_fd_open_ro(file) open64(file, O_RDONLY)
#define bm_fd_open_rw(file) open64(file, O_RDWR)
#define bm_fd_open_rw_trunc(file) open64(file, O_RDWR | O_CREAT | O_TRUNC \
		, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH)
#define bm_fd_close(fd) close(fd)
#endif

#define bm_fd_open(file, read_only) read_only ? bm_fd_open_ro(file) : bm_fd_open_rw(file)

#define sread(fd, buf, count, error) { \
	void *_pntr = buf; \
	for (i64 _remain = count; _remain > 0;) { \
		i64 _reat = bm_fd_read(fd, _pntr, _remain); \
		if (_reat <= 0) { \
			if ((errno == EAGAIN) || (errno == EWOULDBLOCK)) { \
				wait5ms(); \
				errno = 0; \
				continue; \
			} else if ((errno == EINTR)) { \
				errno = 0; \
				continue; \
			} else { \
				error \
			} \
		} \
		_remain -= _reat; \
		_pntr += _reat; \
	} \
}

#endif /* BM_H_ */
