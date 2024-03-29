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
 * pfs-file.h
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#ifndef SRC_CORE_PFS_FILE_H_
#define SRC_CORE_PFS_FILE_H_

#include "pfs.h"

/**
 * read some data from the file
 *
 * if position + length is greater than the files length this operation will fail
 */
int pfsc_file_read(pfs_eh f, i64 position, void *buffer, i64 length);

/**
 * overwrite some data of the file
 *
 * if position + length is greater than the files length this operation will fail
 */
int pfsc_file_write(pfs_eh f, i64 position, void *data, i64 length);

/**
 * append some data to the file
 *
 * returns the number of actually appended bytes on success
 * (may be lower than the requested bytes if not enough blocks could be allocated)
 * if this operation fails -1 is returned
 */
i64 pfsc_file_append(pfs_eh f, void *data, i64 length);

/**
 * change the length of the file
 *
 * if the new_length is larger than the current length zeros are appended to the
 * file until the files-length is equal to new_length
 *
 * if the new_length is lower than the current length the files content at the
 * end of the file is removed
 */
int pfsc_file_truncate(pfs_eh f, i64 new_length);

/**
 * change the length of the file
 *
 * new_length has to be larger than the current length
 *
 * this function appends zeros to the files end until the files-length is equal to new_length
 */
int pfsc_file_truncate_grow(pfs_eh f, i64 new_length);

/**
 * get the length of a file
 *
 * if this operation fails -1 is returned
 */
i64 pfsc_file_length(pfs_eh f);

int pfsc_file_read0(bm pfs, struct pfs_file_data file, i64 position, void *buffer, i64 length, i64 file_block);
int pfsc_file_write0(bm pfs, struct pfs_file_data file, i64 position, void *buffer, i64 length, i64 file_block);


#endif /* SRC_CORE_PFS_FILE_H_ */
