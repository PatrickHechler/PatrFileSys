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
#include "pfs-folder.h"

#ifdef I_AM_PFS
struct bm_block_manager *pfs;
#else
extern struct bm_block_manager *pfs;
#endif

extern int pfs_format(i64 block_count);

extern i64 pfs_block_count();

extern i32 pfs_block_size();

extern element pfs_root();

#endif /* PFS_H_ */
