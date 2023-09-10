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
 * pfs-folder.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_FOLDER_H_
#define SRC_INCLUDE_PFS_FOLDER_H_

#include "patr-file-sys.h"

/*
 * opens a new folder iterator handle for the given folder
 *
 * returns on success the folder iterator handle and on error -1
 */
extern int pfs_folder_open_iter(int eh, int show_hidden);

/*
 * returns the child count of the given folder
 * on error -1 is returned
 */
extern i64 pfs_folder_child_count(int eh);

/*
 * opens a new element handle for the child element with the given name
 * returns on success the new element handle and on error -1
 */
extern int pfs_folder_child(int eh, const char *name);

/*
 * this function works like pfs_folder_child, but fails when the child is no folder
 *
 * note that mount points are also accepted as folder
 *
 * this function works like:
 *   int ceh = pfs_folder_child(eh, name);
 *   if (ceh == -1) return -1;
 *   int flags = pfs_element_get_flags(ceh);
 *   if ((flags == -1) || ((ceh & (PFS_F_FOLDER | PFS_F_MOUNT)) == 0)) {
 *     pfs_element_close(ceh);
 *     return -1;
 *   }
 *   return ceh;
 */
extern int pfs_folder_child_folder(int eh, const char *name);

/*
 * this function works like pfs_folder_child, but fails when the child is no mount point
 *
 * this function works like:
 *   int ceh = pfs_folder_child(eh, name);
 *   if (ceh == -1) return -1;
 *   int flags = pfs_element_get_flags(ceh);
 *   if ((flags == -1) || ((ceh & PFS_F_MOUNT) == 0)) {
 *     pfs_element_close(ceh);
 *     return -1;
 *   }
 *   return ceh;
 */
extern int pfs_folder_child_mount(int eh, const char *name);

/*
 * this function works like pfs_folder_child, but fails when the child is no file
 *
 * this function works like:
 *   int ceh = pfs_folder_child(eh, name);
 *   if (ceh == -1) return -1;
 *   int flags = pfs_element_get_flags(ceh);
 *   if ((flags == -1) || ((ceh & PFS_F_FILE) == 0)) {
 *     pfs_element_close(ceh);
 *     return -1;
 *   }
 *   return ceh;
 */
extern int pfs_folder_child_file(int eh, const char *name);

/*
 * this function works like pfs_folder_child, but fails when the child is no pipe
 *
 * this function works like:
 *   int ceh = pfs_folder_child(eh, name);
 *   if (ceh == -1) return -1;
 *   int flags = pfs_element_get_flags(ceh);
 *   if ((flags == -1) || ((ceh & PFS_F_PIPE) == 0)) {
 *     pfs_element_close(ceh);
 *     return -1;
 *   }
 *   return ceh;
 */
extern int pfs_folder_child_pipe(int eh, const char *name);

/*
 * the pfs_folder_descendant*() functions are like the pfs_folder_child*() functions
 * the difference is that they accept a path and not a name
 */
extern int pfs_folder_descendant(int eh, const char *path);
extern int pfs_folder_descendant_folder(int eh, const char *path);
extern int pfs_folder_descendant_mount(int eh, const char *path);
extern int pfs_folder_descendant_file(int eh, const char *path);
extern int pfs_folder_descendant_pipe(int eh, const char *path);

/*
 * creates a new folder with the given name and adds it to the given folder as a child element
 */
extern int pfs_folder_create_folder(int eh, const char *name);

/*
 * creates a new file with the given name and adds it to the given folder as a child element
 */
extern int pfs_folder_create_file(int eh, const char *name);

/*
 * creates a new pipe with the given name and adds it to the given folder as a child element
 */
extern int pfs_folder_create_pipe(int eh, const char *name);

/*
 * creates a new intern mount point with the given name and adds it to the given folder as a child element
 */
extern int pfs_folder_create_mount_intern(int eh, const char *name, i64 block_count, i32 block_size);

/*
 * creates a new temp mount point with the given name and adds it to the given folder as a child element
 */
extern int pfs_folder_create_mount_temp(int eh, const char *name, i64 block_count, i32 block_size);

/*
 * creates a new mount point with a backing file in the Linux file system with the given name and adds it to the given folder as a child element
 */
extern int pfs_folder_create_mount_rfs_file(int eh, const char *name, const char *file);

#endif /* SRC_INCLUDE_PFS_FOLDER_H_ */
