/*
 * pfs-constants.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_CONSTANTS_H_
#define PFS_CONSTANTS_H_

#include "pfs-err.h"

#define PFS_EH_SIZE 44

#define PFS_NO_TIME -1L

#ifdef PFS_INTERN_H_
static_assert(PFS_EH_SIZE == sizeof(struct pfs_element_handle), "error!");
#endif

#define PFS_MAGIC_START 0xF17565393C422698UL
#define PFS_B0_OFFSET_BLOCK_COUNT 24
#define PFS_B0_OFFSET_BLOCK_SIZE 20
#ifdef PFS_INTERN_H_
static_assert(PFS_B0_OFFSET_BLOCK_COUNT == offsetof(struct pfs_b0, block_count), "error!");
static_assert(PFS_B0_OFFSET_BLOCK_SIZE == offsetof(struct pfs_b0, block_size), "error!");
#endif

#define	PFS_FLAGS_RESERVED        0x0000FFFFU
// these flags are reserved for intern use
#define	PFS_FLAGS_FOLDER          0x00000001U
#define	PFS_FLAGS_FILE            0x00000002U
#define	PFS_FLAGS_HELPER_FOLDER   0x00000004U
#define	PFS_FLAGS_FOLDER_SORTED   0x00000100U
#define	PFS_FLAGS_FOLDER_HASH     0x00000200U

#define	PFS_FLAGS_FILE_ENCRYPTED  0x00010000U
#define	PFS_FLAGS_HIDDEN          0x01000000U

#endif /* PFS_CONSTANTS_H_ */
