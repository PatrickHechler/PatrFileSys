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
 * if UUID is NULL a new UUID will be generated
 *
 * this function sets the current working directory to the root folder
 */
extern int pfs_load_and_format(struct bm_block_manager *bm, i64 block_count,
		uuid_t uuid, char *fs_name);

/*
 * formats the currently loaded PFS
 *
 * if uuid is NULL a new uuid will be generated
 *
 * if fs_name is NULL it will be treated like an empty name
 *
 * this function sets the current working directory to the root folder
 */
extern int pfs_format(i64 block_count, uuid_t uuid, char *fs_name);

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
 * copies the name of the loaded file system to the given buffer
 *
 * if buf_len is larger than the names length, the name will be terminated by a \0 character
 * if the name is larger than the buffer a truncated version of the name is saved in the buffer
 *   the truncated name will not be stopped by a \0 character
 *
 * returns the length of the file system name
 *
 * on error -1 is returned
 */
extern i32 pfs_name_cpy(char *buf, i32 buf_len);

/*
 * returns the length of the name (without the \0 terminating character)
 *
 * on error -1 is returned
 */
extern i32 pfs_name_len();

/**
 * this method is like:
 *   i32 len = pfs_name_len() + 1;
 *   char *name = malloc(len);
 *   pfs_name_cpy(name, len);
 *   return name;
 *
 * on error NULL is returned
 */
extern char* pfs_name();

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
extern int pfs_delete(const char *path, int also_when_loaded);

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
