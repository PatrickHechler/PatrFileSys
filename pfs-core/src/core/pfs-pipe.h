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
 * pfs-pipe.h
 *
 *  Created on: Sep 23, 2022
 *      Author: pat
 */

#ifndef SRC_CORE_PFS_PIPE_H_
#define SRC_CORE_PFS_PIPE_H_

#include "pfs.h"

int pfsc_pipe_read(pfs_eh p, void *buffer, i64 length);

// note, that when pipe append becomes its own function the code sometimes rely on it to be a redirect to file
// (it is not checked if it is a file or pipe and just calls append)
// also note that functions like truncate are also assumed to allow pipes
int __REDIRECT(pfsc_pipe_append, (pfs_eh p, void *data, i64 length), pfsc_file_append);

i64 pfsc_pipe_length(pfs_eh p);

#endif /* SRC_CORE_PFS_PIPE_H_ */
