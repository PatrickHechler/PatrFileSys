/*
 * pfs-constants.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_CONSTANTS_H_
#define PFS_CONSTANTS_H_
/*
 * the magic should be changed when the version changes,
 * so old version does not load a new version which is
 * not backwards compatible
 */
#define PFS_MAGIC_START 0x31756539FC422698UL
#define PFS_B0_OFFSET_BLOCK_COUNT 24
#define PFS_B0_OFFSET_BLOCK_SIZE 20
#ifdef PFS_INTERN_H_
static_assert(PFS_B0_OFFSET_BLOCK_COUNT == offsetof(struct pfs_b0, block_count), "error!");
static_assert(PFS_B0_OFFSET_BLOCK_SIZE == offsetof(struct pfs_b0, block_size), "error!");
#endif

#define	PFS_INTERN_ENTRY_FLAGS (PFS_F_HELPER_FOLDER) /* entries with one of these flags are not allowed to be passed to the outside */
#define PFS_ESSENTIAL_FLAGS (PFS_F_FILE | PFS_F_FOLDER | PFS_F_PIPE)
#define PFS_UNMODIFIABLE_FLAGS  0x000000FFU /* these flags are not allowed to be changed */
#define PFS_F_FOLDER            0x00000001U
#define PFS_F_FILE              0x00000002U
#define PFS_F_PIPE              0x00000004U
/* not yet supported
 #define PFS_FLAGS_SYM_LINK        0x00000008U
 */
#define	PFS_F_HELPER_FOLDER     0x00000080U /* folders with this flag should not be passed to the outside */

#define	PFS_F_EXECUTABLE        0x00000100U
#define	PFS_F_ENCRYPTED         0x00000200U /* currently ignored */
#define	PFS_F_HIDDEN            0x01000000U

#define PFS_SO_ONLY_CREATE  0x00000001U /* fail if the file/pipe exist already */
#define PFS_SO_ALSO_CREATE  0x00000002U /* create the file/pipe if it does not exist, but do not fail if the file/pipe exist already (overwritten by PFS_SO_ONLY_CREATE) */
#define PFS_SO_FILE         0x00000004U /* fail if the element is a pipe and if CREATE is set create a file */
#define PFS_SO_PIPE         0x00000008U /* fail if the element is a file and if CREATE is set create a pipe */
#define PFS_SO_READ         0x00000100U /* open the stream for read access */
#define PFS_SO_WRITE        0x00000200U /* open the stream for write access */
#define PFS_SO_APPEND       0x00000400U /* open the stream for append access (before every write operation the position is set to the end of the file) */
#define PFS_SO_FILE_TRUNC   0x00010000U /* truncate the files content */
#define PFS_SO_FILE_EOF     0x00020000U /* set the position initially to the end of the file not the start */

#endif /* PFS_CONSTANTS_H_ */
