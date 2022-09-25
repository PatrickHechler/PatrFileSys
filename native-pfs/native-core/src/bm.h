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

/**
 * block manager structure used by the patr-file-system
 */
struct bm_block_manager {
	/**
	 * this hash-set is only for intern use
	 */
	struct hashset loaded;
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
	 */
	int (*const close_bm)(struct bm_block_manager *bm);
	/**
	 * the size of all blocks
	 */
	const ui32 block_size;
	/**
	 * the number of bits usable for block flagging
	 * (maximum value is 63)
	 * if not all the value is lower than 63 the lowest bits will be used for the flags
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
	int (*const set_flags)(struct bm_block_manager *bm, i64 block, i64 flags);
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

static_assert(offsetof(struct bm_block_manager, get) == 32);
static_assert(offsetof(struct bm_block_manager, unget) == 40);
static_assert(offsetof(struct bm_block_manager, set) == 48);
static_assert(offsetof(struct bm_block_manager, sync_bm) == 56);
static_assert(offsetof(struct bm_block_manager, close_bm) == 64);
static_assert(offsetof(struct bm_block_manager, block_size) == 72);
static_assert(offsetof(struct bm_block_manager, block_flag_bits) == 76);
static_assert(offsetof(struct bm_block_manager, get_flags) == 80);
static_assert(offsetof(struct bm_block_manager, set_flags) == 88);
static_assert(offsetof(struct bm_block_manager, first_zero_flagged_block) == 96);
static_assert(offsetof(struct bm_block_manager, delete_all_flags) == 104);

/**
 * creates a new ram block manager
 *
 * block_count: the number of blocks in the block manager
 *
 * block_size: the size of the blocks
 */
extern struct bm_block_manager* bm_new_ram_block_manager(i64 block_count, i32 block_size);

/**
 * creates a new file block manager
 *
 * file: the file descriptor
 *
 * block_size: the size of the blocks
 */
extern struct bm_block_manager* bm_new_file_block_manager(int file, i32 block_size);

#define sread(fd, buf, count, error) \
	{ \
		void *_pntr = buf; \
		for (i64 _cnt = count; _cnt > 0; ) { \
			i64 _reat = read(fd, buf, _cnt); \
			if (_reat <= 0) { \
				if (_reat == 0) { \
					error \
				} else if ((errno == EAGAIN) || (errno == EINTR)) { \
					continue; \
				} else { \
					error \
				} \
			} \
			_cnt -= _reat; \
			_pntr += _reat; \
		} \
	}

#define new_file_bm0(bm, file, wrong_magic_error, io_error) { \
	ui64 magic; \
	i32 block_size; \
	if (lseek64(file, 0, SEEK_SET) == -1) { \
		io_error \
	} \
	sread(file, &magic, 8, io_error) \
	if (magic != PFS_MAGIC_START) { \
		wrong_magic_error \
	} \
	if (lseek64(file, PFS_B0_OFFSET_BLOCK_SIZE, SEEK_SET) == -1) { \
		io_error \
	} \
	sread(file, &block_size, 4, io_error) \
	bm = bm_new_file_block_manager(file, block_size); \
}

#define new_file_bm(bm, file, error) new_file_bm0(bm, file, error, error)

#define new_file_pfs(file, error) new_file_bm(pfs, file, error)

/**
 * creates a new ram block manager
 * the returned block manager will support the flagging of blocks
 *
 * block_count: the number of blocks in the block manager
 *
 * block_size: the size of the blocks
 */
extern struct bm_block_manager* bm_new_flaggable_ram_block_manager(i64 block_count, i32 block_size);

#endif /* BM_H_ */
