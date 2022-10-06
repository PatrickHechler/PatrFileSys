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
#define PFS_FI_SIZE 40

#ifdef PFS_INTERN_H_
static_assert(PFS_EH_SIZE == sizeof(struct pfs_element_handle), "error!");
static_assert(PFS_FI_SIZE == sizeof(struct pfs_folder_iter), "error!");
#endif

#define PFS_MAGIC_START 0xF17565393C422698UL
#define PFS_B0_OFFSET_BLOCK_COUNT 24
#define PFS_B0_OFFSET_BLOCK_SIZE 20
#ifdef PFS_INTERN_H_
static_assert(PFS_B0_OFFSET_BLOCK_COUNT == offsetof(struct pfs_b0, block_count), "error!");
static_assert(PFS_B0_OFFSET_BLOCK_SIZE == offsetof(struct pfs_b0, block_size), "error!");
#endif

#define PFS_FLAGS_ESSENTIAL_FLAGS (PFS_FLAGS_FILE | PFS_FLAGS_FOLDER | PFS_FLAGS_PIPE)
// these flags are not allowed to be changed
#define	PFS_UNMODIFIABLE_FLAGS    0x000000FFU
#define	PFS_FLAGS_FOLDER          0x00000001U
#define	PFS_FLAGS_FILE            0x00000002U
#define	PFS_FLAGS_PIPE            0x00000004U
/* not yet supported
 #define	PFS_FLAGS_SYM_LINK        0x00000008U
 #define	PFS_FLAGS_HARD_LINK       0x00000010U
 */
#define	PFS_FLAGS_HELPER_FOLDER   0x00000080U

// these flags meaning are different for flags and folders/pipes
#define	PFS_FLAGS_TYPE_SPECIFIC   0x0000FF00U
#define	PFS_FLAGS_FILE_EXECUTABLE 0x00000100U
#define	PFS_FLAGS_FILE_ENCRYPTED  0x00000200U

#define	PFS_FLAGS_HIDDEN          0x01000000U

#define PFS_SO_NOT_CREATE   0x00000000 /* fail if the file/pipe does not exist already (this is the default create option) */
#define PFS_SO_ONLY_CREATE  0x00000100 /* fail if the file/pipe exist already */
#define PFS_SO_ALSO_CREATE  0x00000200 /* create the file/pipe if it does not exist, but do not fail if the file/pipe exist already */
#define PFS_SO_FILE_TRUNC   0x00010000 /* truncate the files content */
#define PFS_SO_FILE_EOF     0x00020000 /* set the position initially to the end of the file not the start */

#endif /* PFS_CONSTANTS_H_ */
