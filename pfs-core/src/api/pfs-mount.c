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
 * pfs-mount.c
 *
 *  Created on: Sep 9, 2023
 *      Author: pat
 */

#include "pfs.h"
#include "../core/pfs.h"
#include "../include/pfs-mount.h"

#define check_root(err_ret) \
	if (pfs_ehs[eh]->handle.direct_parent_place.block != -1L) { \
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE; \
		return err_ret; \
	} \

extern int pfs_mount_get_mount_point(int eh) {
	eh(-1L)
	return_handle(pfs_eh_len, pfs_ehs, pfs_ehs[eh]->handle.fs_data->root);
}

extern i64 pfs_block_count() {
	return pfsc_block_count(pfs(pfs_root));
}
extern i64 pfs_mount_fs_block_count(int eh) {
	eh(-1L)
	check_root(-1L)
	return pfsc_block_count(pfs(pfs_ehs[eh]));
}

extern i32 pfs_block_size() {
	return pfsc_block_size(pfs(pfs_root));
}
extern i32 pfs_mount_fs_block_size(int eh) {
	eh(-1)
	check_root(-1)
	return pfsc_block_size(pfs(pfs_ehs[eh]));
}

extern int pfs_set_uuid(const uuid_t uuid) {
	check_write_access0(pfs_root, 0)
	return pfsc_set_uuid(pfs(pfs_root), uuid);
}
extern int pfs_mount_fs_set_uuid(int eh, const uuid_t uuid) {
	eh(0)
	check_root(0)
	check_write_access0(pfs_root, 0)
	return pfsc_set_uuid(pfs(pfs_ehs[eh]), uuid);
}

extern int pfs_make_read_only() {
	if (pfs_root->handle.fs_data->read_only) {
		return 1;
	}
	int res = pfsc_make_read_only(pfs(pfs_root));
	if (res) {
		pfs_root->handle.fs_data->read_only = 1;
	}
	return res;
}
extern int pfs_mount_fs_make_read_only(int eh) {
	eh(0)
	check_root(0)
	if (pfs_ehs[eh]->handle.fs_data->read_only) {
		return 1;
	}
	int res = pfsc_make_read_only(pfs(pfs_ehs[eh]));
	if (res) {
		pfs_ehs[eh]->handle.fs_data->read_only = 1;
	}
	return res;
}

extern uuid_p_t pfs_uuid(uuid_t result) {
	return pfsc_uuid(pfs(pfs_root), result);
}
extern uuid_p_t pfs_mount_fs_uuid(int eh, uuid_t result) {
	eh(NULL)
	check_root(NULL)
	return pfsc_uuid(pfs(pfs_ehs[eh]), result);
}

extern i32 pfs_name_cpy(char *buf, i32 buf_len) {
	return pfsc_name_cpy(pfs(pfs_root), buf, buf_len);
}
extern i32 pfs_mount_fs_name_cpy(int eh, char *buf, i32 buf_len) {
	eh(-1)
	check_root(-1)
	return pfsc_name_cpy(pfs(pfs_ehs[eh]), buf, buf_len);
}

extern i32 pfs_name_len() {
	return pfsc_name_len(pfs(pfs_root));
}
extern i32 pfs_mount_fs_name_len(int eh) {
	eh(-1)
	check_root(-1)
	return pfsc_name_len(pfs(pfs_ehs[eh]));
}

extern char* pfs_name() {
	return pfsc_name(pfs(pfs_root));
}
extern char* pfs_mount_fs_name(int eh) {
	eh(NULL)
	check_root(NULL)
	return pfsc_name(pfs(pfs_ehs[eh]));
}

extern int pfs_mount_fs_is_read_only(int eh) {
	eh(-1)
	check_root(-1)
	return pfs_ehs[eh]->handle.fs_data->read_only;
}

extern enum mount_type pfs_mount_fs_type(int eh) {
	eh(-1)
	check_root(-1)
	struct pfs_element_handle_mount *mp = pfs_ehs[eh]->handle.fs_data->mount_point;
	if (mp) {
		return pfsc_mount_type(mp);
	} else {
		return mount_type_root_file_system;
	}
}
