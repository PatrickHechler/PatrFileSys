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
 *  Created on: Sep 8, 2023
 *      Author: pat
 */

#ifndef SRC_CORE_PFS_MOUNT_H_
#define SRC_CORE_PFS_MOUNT_H_

#include "../pfs/pfs-mount.h"

int pfsc_mount_open(pfs_meh me, int read_only);

int pfsc_mount_close(pfs_meh me);

enum mount_type pfsc_mount_type(pfs_meh me);

#endif /* SRC_CORE_PFS_MOUNT_H_ */
