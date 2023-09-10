//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
/*
 * pfs-element.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_ELEMENT_H_
#define SRC_INCLUDE_PFS_ELEMENT_H_

#include "patr-file-sys.h"

/*
 * closes the given element handle
 *
 * after this operation the eh-ID may be reused by the system when a new handle is opened
 *
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_close(int eh);

/*
 * returns a handle for the parent folder of the given handle
 * on error -1 is returned
 */
extern int pfs_element_parent(int eh);

/*
 * returns the absolute path of the element
 *
 * when the path is no longer needed it can be released with free
 *
 * on error NULL is returned
 */
extern char* pfs_element_path(int eh);

/*
 * fills the buffer with the absolute path of the element
 * if the absolute path is larger than the current buf_size it
 * will be resized with realloc and the new buffer size will be
 * stored in buf_size
 *
 * on error 0 is returned, on success a non-zero value
 * note that the buffer may even be resized when the operation fails
 */
extern int pfs_element_path0(int eh, char **buffer, i64 *buf_size);

/*
 * these two functions are like the pfs_element_path* functions, but
 * they return an absolute path relative from the first mount point
 */
extern char* pfs_element_fs_path(int eh);
extern int pfs_element_fs_path0(int eh, char **buffer, i64 *buf_size);

/*
 * deletes the element of the given handle
 *
 * this operation automatically closes the element handle (even if this function fails)
 *
 * after this operation the eh-ID may be reused by the system when a new handle is opened
 *
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_delete(int eh, int also_when_loaded);

/*
 * returns the flags of the element
 * on error -1 is returned
 * if the element is a mount point the flags of the mount point entry are returned
 * if the element is the root point of all file systems (PFS_F_MOUNT | PFS_F_FOLDER) is returned
 *   note the root has a unique flag combination, since no valid entry can have both the folder and the mount flag set
 */
extern ui32 pfs_element_get_flags(int eh);

#define pfs_element_is_root(eh) (pfs_element_get_flags(eh) == (PFS_F_MOUNT | PFS_F_FOLDER))

#define pfs_element_is_folder(eh) ((pfs_element_get_flags(eh) & (PFS_F_MOUNT | PFS_F_FOLDER)) != 0)

/*
 * modifies the flags of the given element
 *
 * - rem_flags and add_flags are not allowed to contain common bits
 *     (rem_flags & add_flags) has to be zero
 * - rem_flags and add_flags are not allowed to contain unmodifiable bits
 *     ((rem_flags | add_flags) & PFS_UNMODIFIABLE_FLAGS) has to be zero
 *
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_modify_flags(int eh, ui32 add_flags, ui32 rem_flags);

/*
 * returns the create time of the given element
 * on error -1 is returned
 */
extern i64 pfs_element_get_create_time(int eh);

/*
 * returns the last modify time of the given element
 * on error -1 is returned
 */
extern i64 pfs_element_get_last_modify_time(int eh);

/*
 * sets the create time of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_set_create_time(int eh, i64 ct);

/*
 * sets the last modify time of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_set_last_modify_time(int eh, i64 lmt);

/*
 * get the name of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_get_name(int eh, char **name_buf, i64 *buf_len);

/*
 * set the name of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_set_name(int eh, char *name);

/*
 * set the parent of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_set_parent(int eh, int parenteh);

/*
 * sets the name and the parent of the element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_move(int eh, int parenteh, char *name);

/*
 * returns 1 if both element handles are valid and refer to the same element
 * returns 0 if both element handles are valid, but do not refer to the same element
 * returns -1 if at least one not a valid element handle
 */
extern int pfs_element_same(int aeh, int beh);

#endif /* SRC_INCLUDE_PFS_ELEMENT_H_ */
