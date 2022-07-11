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
	 * the size of a block
	 */
	const ui32 block_size;
};

/**
 * creates a new ram block manager
 *
 * block_count: the number of blocks in the block manager
 *
 * block_size: the size of the blocks
 */
struct bm_block_manager* bm_new_ram_block_manager(i64 block_count, i32 block_size);

/**
 * creates a new file block manager
 *
 * file: the file descriptor
 *
 * block_size: the size of the blocks
 */
struct bm_block_manager* bm_new_file_block_manager(int file, i32 block_size);

#endif /* BM_H_ */
