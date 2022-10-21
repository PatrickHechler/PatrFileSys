/*
 * pfs.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_H_
#define SRC_INCLUDE_PFS_H_

#include "patr-file-sys.h"
#include "bm.h"

/*
 * loads the PFS from the specified block manager
 *
 * the current working directory will be set to cwd
 * cwd will always be considered as absolute path
 *
 * when cwd is NULL the current working directory will be set to the root folder
 */
extern int pfs_load(struct bm_block_manager *bm, const char *cwd);

/*
 * formats and loads the PFS from the specified block manager
 *
 * this function sets the current working directory to the root folder
 */
extern int pfs_load_and_format(struct bm_block_manager *bm, i64 block_count);

/*
 * formats the currently loaded PFS
 *
 * this function sets the current working directory to the root folder
 */
extern int pfs_format(i64 block_count);

/*
 * closes the file system
 */
extern int pfs_close();

/*
 * returns the block count available to the current PFS
 *
 * on error -1 is returned
 */
extern i64 pfs_block_count();

/*
 * returns the block size in bytes of every block of the current PFS
 *
 * on error -1 is returned
 */
extern i32 pfs_block_size();

/*
 * creates a new element handle from the given path
 *
 * the path is a null ('\0') terminated string
 *
 * if the path starts with a slash ('/') it is considered absolute
 * otherwise the path is considered relative to the current working directory
 *
 * if the path is empty (directly points to a null ('\0') character) this operation fails
 *
 * the path segments are separated by slashes ('/')
 *
 * empty path segments are ignored
 * single dot path segments ('.') are ignored
 *
 * two dot path segments ('..') are treated as a magic link to the parent folder
 *   note that the root folder has no parent folder and thus '/..' will fail
 *
 * on error -1 is returned
 */
extern int pfs_handle(const char *path);

/*
 * this function is like pfs_handle, but it fails if the given element is no folder
 *
 * this function works like:
 *   int eh = pfs_handle(path);
 *   int flags = pfs_element_get_flags(eh);
 *   if ((flags == -1) || ((flags & PFS_F_FOLDER) == 0)) {
 *     pfs_element_close(eh);
 *     return -1;
 *   }
 *   return eh;
 */
extern int pfs_handle_folder(const char *path);

/*
 * this function is like pfs_handle, but it fails if the given element is no file
 *
 * this function works like:
 *   int eh = pfs_handle(path);
 *   int flags = pfs_element_get_flags(eh);
 *   if ((flags == -1) || ((flags & PFS_F_FILE) == 0)) {
 *     pfs_element_close(eh);
 *     return -1;
 *   }
 *   return eh;
 */
extern int pfs_handle_file(const char *path);

/*
 * this function is like pfs_handle, but it fails if the given element is no pipe
 *
 * this function works like:
 *   int eh = pfs_handle(path);
 *   int flags = pfs_element_get_flags(eh);
 *   if ((flags == -1) || ((flags & PFS_F_PIPE) == 0)) {
 *     pfs_element_close(eh);
 *     return -1;
 *   }
 *   return eh;
 */
extern int pfs_handle_pipe(const char *path);

/*
 * changes the current working directory to the given folder
 */
extern int pfs_change_dir(int eh);

/*
 * changes the current working directory to the given folder
 *
 * this function works linke:
 *   int eh = pfs_open_hanlde(path);
 *   int res = pfs_cwd(eh);
 *   pfs_element_close(eh);
 *   return res;
 */
extern int pfs_change_working_directoy(char *path);

/*
 * deletes the element associated with the given path
 *
 * this function works like:
 *   int eh = pfs_handle(path);
 *   return pfs_element_delete(eh);
 *
 * on success 1 and on error 0 is returned
 */
extern int pfs_delete(const char *path);

/*
 * creates a new stream handle for the file/pipe with the given path
 *
 * this function works like:
 *   int eh = pfs_handle(path);
 *   int sh = pfs_stream_open(eh);
 *   pfs_element_close(eh);
 *   return sh;
 */
extern int pfs_stream(const char *path, i32 stream_flags);

/*
 * creates a new iterator handle for the folder with the given path
 *
 * this function works like:
 *   int eh = pfs_handle_folder(path);
 *   int sh = pfs_folder_open_iter(eh);
 *   pfs_element_close(eh);
 *   return sh;
 */
extern int pfs_iter(const char *path, int show_hidden);

#endif /* SRC_INCLUDE_PFS_H_ */
