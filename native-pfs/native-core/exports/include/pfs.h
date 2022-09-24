/*
 * pfs.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_H_
#define PFS_H_

#include "bm.h"
#include "pfs-constants.h"
#include "pfs-err.h"
#include "pfs-element.h"
#include "pfs-file.h"
#include "pfs-pipe.h"
#include "pfs-folder.h"

#ifndef I_AM_PFS
extern
#endif
struct bm_block_manager *pfs;

extern int pfs_format(i64 block_count);

extern i64 pfs_block_count();

extern i32 pfs_block_size();

extern int pfs_fill_root(pfs_eh overwrite_me);

extern pfs_eh pfs_root();

/**
 * note that some function can move elements and thus make all other
 * (also the duplicated) element-handles for the given element corrupt
 *
 * (see: pfs_folder_create*)
 */
#define pfs_duplicate_handle0(eh, new_eh, out_of_mem) \
	pfs_eh new_eh = malloc(PFS_EH_SIZE); \
	if (new_eh == NULL) { \
		out_of_mem \
	} \
	memcpy(new_eh, eh, PFS_EH_SIZE);

/**
 * note that some function can move elements and thus make all other
 * (also the duplicated) element-handles for the given element corrupt
 *
 * (see: pfs_folder_create*)
 */
#define pfs_duplicate_handle(eh, new_eh) pfs_duplicate_handle0(eh, new_eh, abort();)

#endif /* PFS_H_ */
