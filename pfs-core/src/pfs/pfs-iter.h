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
 * pfs-iter.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_PFS_PFS_ITER_H_
#define SRC_PFS_PFS_ITER_H_

#include "patr-file-sys.h"

/*
 * closes the given folder iterator handle
 *
 * the given folder iterator handle ID may be reused by the system
 * when a new folder iterator handle is opened
 */
extern int pfs_iter_close(int ih);

/*
 * opens a new element handle for the next child element
 *
 * when there is no next child element -1 is returned and (pfs_err) is set to PFS_ERRNO_NO_MORE_ELEMENTS
 *
 * returns on success the child element handle and on error -1
 */
extern int pfs_iter_next(int ih);

#endif /* SRC_PFS_PFS_ITER_H_ */
