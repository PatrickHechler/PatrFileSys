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
 * pfs-file.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#include "pfs.h"
#include "../include/pfs-file.h"

#define ch(err_ret) \
	eh(err_ret) \
	c_h(pfs_ehs[eh], err_ret, PFS_F_FILE)

#define cr(err_ret) c_r(pfs_ehs[eh], err_ret)

extern i64 pfs_file_length(int eh) {
	ch(-1)
	int res = pfsc_file_length(&pfs_ehs[eh]->handle);
	cr(-1)
	return res;
}

extern int pfs_file_truncate(int eh, i64 length) {
	if (length < 0) {
		(pfs_err) = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	ch(0)
	int res = pfsc_file_truncate(&pfs_ehs[eh]->handle, length);
	cr(0)
	return res;
}
