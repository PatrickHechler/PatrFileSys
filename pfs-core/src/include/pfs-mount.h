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
 * pfs-mount.h
 *
 *  Created on: Sep 6, 2023
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_MOUNT_H_
#define SRC_INCLUDE_PFS_MOUNT_H_

#include "patr-file-sys.h"

enum mount_type {
	mount_type_temp             = 0x0,
	mount_type_intern           = 0x1,
	mount_type_linux_pfs_file   = 0x2,
	mount_type_root_file_system = 0x3,
};

/*
 * opens and returns an handle for the root folder/mount point of the file system of the given handle
 *
 * doing this on a mount point does not duplicate the entry, but return the root folder/mount point
 * on which the file system entry of the mount point lies.
 *
 * this operation fails, when the absolute root folder is passed
 */
extern int pfs_mount_get_mount_point(int eh);

extern i64 pfs_mount_fs_block_count(int eh);

extern i32 pfs_mount_fs_block_size(int eh);

extern uuid_p_t pfs_mount_fs_uuid(int eh, uuid_t result);

extern int pfs_mount_fs_set_uuid(int eh, const uuid_t uuid);

extern i32 pfs_mount_fs_name_cpy(int eh, char *buf, i32 buf_len);

extern i32 pfs_mount_fs_name_len(int eh);

extern char* pfs_mount_fs_name(int eh);

/*
 * returns 1 value if the mount point is a read only mount or
 * the file system (the mounted or the one which surrounds the mount) has the read only flag set.
 * if not returns 0
 * on error returns -1
 */
extern int pfs_mount_fs_is_read_only(int eh);

extern enum mount_type pfs_mount_fs_type(int eh);

#endif /* SRC_INCLUDE_PFS_MOUNT_H_ */
