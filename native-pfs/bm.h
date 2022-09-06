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
	void* (*get)(struct bm_block_manager *bm, i64 block);
	/**
	 * function to unload/un-get a block without saving
	 */
	void (*unget)(struct bm_block_manager *bm, i64 block);
	/**
	 * function to unload/un-get a block with saving
	 */
	void (*set)(struct bm_block_manager *bm, i64 block);
	/**
	 * synchronizes the block manager
	 */
	void (*sync)(struct bm_block_manager *bm);
	/**
	 * closes the block manager
	 */
	void (*close)(struct bm_block_manager *bm);
	/**
	 * the size of a block
	 */
	const ui32 block_size;
	/**
	 * the number of bits usable for block flagging
	 * (maximum value is 64)
	 * if not all the value is lower than 64 the lowest bits will be used for the flags
	 */
	const int block_flag_bits;
	/**
	 * get the flags of a block
	 * if not all block_flag_bits is lower than 64 the lowest block_flag_bits bits will be used for the flags
	 * the unsupported bits are filled with zeros
	 */
	ui64 (*get_flags)(struct bm_block_manager *bm, i64 block);
	/**
	 * set the flags of a block
	 * if not all block_flag_bits is lower than 64 the lowest block_flag_bits bits will be used for the flags
	 * the unsupported bits are ignored
	 */
	void (*set_flags)(struct bm_block_manager *bm, i64 block, ui64 flags);
	/**
	 * gets the first zero-flagged block
	 * if all blocks are flagged (or block flagging is not supported) -1 is returned
	 */
	i64 (*first_zero_flagged_block)(struct bm_block_manager *bm);
	/**
	 * sets all flags to zero
	 */
	void (*delete_all_flags)(struct bm_block_manager *bm);
};

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
