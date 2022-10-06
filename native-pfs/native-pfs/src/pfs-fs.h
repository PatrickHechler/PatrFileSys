/*
 * pfs-fs.h
 *
 * the PFS-API is built of three different handles:
 *  - element handles
 *  - stream handles
 *  - iterator handles
 *
 * the element handles can be used to extract information
 * over file system elements like folders, files and pipes.
 * from an element handle it is also possible to create
 * pipe/file steams and folder iterators.
 *
 * pipe stream handles can be used to read and append to/from
 * pipes.
 * file stream handles can be used to read, write and append to
 * and files.
 * file streams also support the changing of the current file
 * position (seek).
 *
 * iterator handles can iterate over the child elements of a
 * folder.
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef NATIVE_PFS_SRC_PFS_FS_H_
#define NATIVE_PFS_SRC_PFS_FS_H_

#include "bm.h"
#include "pfs-constants.h"
#include "pfs-err.h"
#include "pfs-element.h"
#include "pfs-file.h"
#include "pfs-pipe.h"
#include "pfs-folder.h"

/*
 * loads the file system from the given block manager
 *
 * this operation will fail if the block manager does
 * not contain a valid patr-file-sys.
 *
 * on success 1 is returned, otherwise 0
 */
extern int pfs_load(struct bm_block_manager *bm);

/*
 * loads the file system from the given block manager
 *
 * this function will not check for a valid patr-file-sys to
 * be on the given block manager.
 *
 * this function will directly format the given block manager.
 *
 * on success 1 is returned, otherwise 0
 */
extern int pfs_load_and_format(struct bm_block_manager *bm, i64 block_count);

/*
 * formats the file system
 *
 * after this operation a new patr-file-sys will be created on
 * the underlying block device.
 *
 * on success 1 is returned, otherwise 0
 */
extern int pfs_format(i64 block_count);

/*
 * returns the block count of the file system
 *
 * the file system will allocate only blocks with numbers lower
 * than the returned value
 *
 * on error -1 is returned
 */
extern i64 pfs_block_count();

/*
 * returns the block size of the file system
 *
 * on error -1 is returned
 */
extern i32 pfs_block_size();

/*
 * creates a new handle for the root folder
 */
extern int pfs_root();

/*
 * closes the given element handle
 */
extern int pfs_close_eh(int eh);

/*
 * closes the given stream handle
 */
extern int pfs_close_sh(int sh);

/*
 * closes the given folder iterator
 *
 * note that even after the folder iterator has iterated over all
 * elements of it's folder it still needs to be explicitly closed.
 */
extern int pfs_close_fi(int fi);

/*
 * creates a new element handle for the element with the given name
 *
 * name is a '\0' terminated UTF-8 character sequence and the
 * path separator is the '/' character
 *
 * empty path segments will be ignored
 * a single dot ('.') as path segment will be ignored
 * two dots ('..') as path segment will be a magic link to the parent
 * folder. note that the root folder has no parent folder and thus
 * using '/..' will lead to a PFS_ERRNO_ELEMENT_NOT_EXIST error.
 *
 * if name starts with a slash ('/') it will be interpreted as
 * an absolute path (relative to the root directory).
 * if the name starts with any other character than a slash ('/') it
 * will be interpreted relative to the current working directory.
 *
 * if the operation fails -1 will be returned
 */
extern int pfs_open_eh(char *name);

/*
 * works like pfs_open_eh, but fails if the element is no folder
 */
extern int pfs_open_eh_folder(char *name);

/*
 * works like pfs_open_eh, but fails if the element is no file
 */
extern int pfs_open_eh_file(char *name);

/*
 * works like pfs_open_eh, but fails if the element is no pipe
 */
extern int pfs_open_eh_pipe(char *name);

/*
 * creates a new file stream for the file with the given name
 *
 * name is a '\0' terminated UTF-8 character sequence and the
 * path separator is the '/' character
 *
 * (when PFS_SO_ONLY_CREATE/PFS_SO_ALSO_CREATE is not set)
 * this operation is similar to:
 *   int eh = pfs_open_eh(name);
 *   int fs = pfs_file_stream(eh, stream_flags);
 *   pfs_close_eh(eh);
 *   return fs;
 *
 * if the operation fails -1 will be returned
 */
extern int pfs_open_fs(char *name, int stream_flags);

/*
 * creates a new pipe stream for the pipe with the given name
 *
 * name is a '\0' terminated UTF-8 character sequence and the
 * path separator is the '/' character
 *
 * (when PFS_SO_ONLY_CREATE/PFS_SO_ALSO_CREATE is not set)
 * this operation is similar to:
 *   int eh = pfs_open_eh_pipe(name);
 *   int fs = pfs_pipe_stream(eh, stream_flags);
 *   pfs_close_eh(eh);
 *   return fs;
 *
 * if the operation fails -1 will be returned
 */
extern int pfs_open_ps(char *name, int stream_flags);

/*
 * creates a new folder iterator for the folder with the given name
 *
 * name is a '\0' terminated UTF-8 character sequence and the
 * path separator is the '/' character
 *
 * this operation is similar to:
 *   int eh = pfs_open_eh(name);
 *   int fs = pfs_folder_iter(eh);
 *   pfs_close_eh(eh);
 *   return fs;
 *
 * multiple stream_open flags can be set by or'ing them together
 * (flag_a | flag_b)
 * possible stream_flags are:
 * - PFS_SO_ONLY_CREATE
 *    - fail if the file/pipe exist already
 * - PFS_SO_ALSO_CREATE
 *    - create the file/pipe if it does not exist, but do not fail if the file/pipe exist already
 *    - ignored when PFS_SO_ONLY_CREATE is also set
 * - PFS_SO_FILE_TRUNC
 *    - truncate the files content
 * - PFS_SO_FILE_EOF
 *    - set the position initially to the end of the file not the start
 *
 * if the operation fails -1 will be returned
 */
extern int pfs_open_fi(char *name);

#endif /* NATIVE_PFS_SRC_PFS_FS_H_ */
