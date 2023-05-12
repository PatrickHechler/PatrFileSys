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
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef SRC_CORE_PFS_H_
#define SRC_CORE_PFS_H_

#include "pfs-intern.h"
#include "../include/patr-file-sys.h"
#include "pfs-element.h"
#include "pfs-file.h"
#include "pfs-folder.h"
#include "pfs-pipe.h"
#include "../include/bm.h"
#include "../include/pfs-constants.h"
#include "../include/pfs-err.h"

#ifndef I_AM_CORE_PFS
extern
#endif
struct bm_block_manager *pfs;

int pfsc_format(i64 block_count);

extern i64 pfs_block_count();

extern i64 pfs_free_block_count();

extern i64 pfs_used_block_count();

extern i32 pfs_block_size();

int pfsc_fill_root(pfs_eh overwrite_me);

pfs_eh pfsc_root();

/**
 * note that some function can move elements and thus make all other
 * (also the duplicated) element-handles for the given element corrupt
 *
 * (see: pfs_folder_create*)
 */
#define pfs_duplicate_handle0(eh, new_eh, out_of_mem) \
	pfs_eh new_eh = malloc(sizeof(struct pfs_element_handle)); \
	if (new_eh == NULL) { \
		out_of_mem \
	} \
	memcpy(new_eh, eh, sizeof(struct pfs_element_handle));

/**
 * note that some function can move elements and thus make all other
 * (also the duplicated) element-handles for the given element corrupt
 *
 * (see: pfs_folder_create*)
 */
#define pfs_duplicate_handle(eh, new_eh) pfs_duplicate_handle0(eh, new_eh, abort();)

#endif /* SRC_CORE_PFS_H_ */
