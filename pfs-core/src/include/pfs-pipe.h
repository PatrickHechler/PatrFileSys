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
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_PIPE_H_
#define SRC_INCLUDE_PFS_PIPE_H_

#include "patr-file-sys.h"

/*
 * opens a stream handle for the file/pipe
 *   note that this function works also for files
 *   add the PFS_SO_PIPE flag when no files are allowed
 *
 * on success the stream handle and on error -1 is returned
 */
extern int pfs_open_stream(int eh, i32 stream_flags);

/*
 * returns the pipe length
 * on error -1 is returned
 */
extern i64 pfs_pipe_length(int eh);

#endif /* SRC_INCLUDE_PFS_PIPE_H_ */
