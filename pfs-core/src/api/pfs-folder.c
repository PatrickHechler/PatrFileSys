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
 * pfs-folder.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#include "pfs.h"
#include "../core/pfs-folder.h"
#include "../pfs/pfs-folder.h"
#include "../core/pfs-mount.h"

#define ch(err_ret) \
	eh(err_ret) \
	c_h(pfs_ehs[eh], err_ret, PFS_F_FOLDER)

extern i64 pfs_folder_child_count(int eh) {
	ch(-1)
	return pfsc_folder_child_count(&pfs_ehs[eh]->handle);
}

#define get_child(error_return) \
	{ \
		struct element_handle *oc = hashset_add(&pfs_all_ehs_set, eh_hash(c), c); \
		if (oc) { \
			free(c); \
			oc->load_count++; \
			c = oc; \
		} else { \
			c->load_count = 1; \
		} \
		if ((c->handle.is_mount_point)) { /*no need to check if c is already the root*/ \
			struct element_handle_mount *mc = realloc(c, sizeof(struct element_handle_mount )); \
			if (!mc || !pfsc_mount_open(&mc->handle, mc->handle.handle.fs_data->read_only)) { \
				free(c); \
				return error_return; \
			} \
			c = mc->handle.fs.root; \
		} \
	}

#define pfs_folder_child_impl(type) \
	ch(-1) \
	struct element_handle *c = malloc(sizeof(struct element_handle)); \
	if (!c) { \
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY; \
		errno = 0; \
		return -1; \
	} \
	c->load_count = 1; \
	c->handle = pfs_ehs[eh]->handle; \
	if (!pfsc_folder_##type##child_from_name(&c->handle, name)) { \
		return -1; \
	} \
	get_child(-1) \
	return_handle(pfs_eh_len, pfs_ehs, c)

extern int pfs_folder_child(int eh, const char *name) {
	pfs_folder_child_impl()
}

extern int pfs_folder_child_folder(int eh, const char *name) {
	pfs_folder_child_impl(folder_)
}

extern int pfs_folder_child_mount(int eh, const char *name) {
	pfs_folder_child_impl(mount_)
}

extern int pfs_folder_child_file(int eh, const char *name) {
	pfs_folder_child_impl(file_)
}

extern int pfs_folder_child_pipe(int eh, const char *name) {
	pfs_folder_child_impl(pipe_)
}

#define pfs_folder_create0(type, handle_possfix, action, ...) \
	ch(-1) \
	check_write_access(-1) \
	struct element_handle *c = malloc(sizeof(struct element_handle##handle_possfix)); \
	if (!c) { \
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY; \
		errno = 0; \
		return -1; \
	} \
	c->load_count = 1; \
	c->handle = pfs_ehs[eh]->handle; \
	pfs_modify_iterators(pfs_ehs[eh], UINT64_MAX); \
	if (!pfsc_folder_create_##type(&c->handle, &pfs_ehs[eh]->handle, __VA_ARGS__)) { \
		return -1; \
	} \
	action \
	struct element_handle *oc = hashset_put(&pfs_all_ehs_set, eh_hash(c), c); \
	if (oc) { \
		abort(); \
	} \
	return_handle(pfs_eh_len, pfs_ehs, c)

#define pfs_folder_create(type) pfs_folder_create0(type,,,name)

extern int pfs_folder_create_folder(int eh, const char *name) {
	pfs_folder_create(folder)
}

extern int pfs_folder_create_file(int eh, const char *name) {
	pfs_folder_create(file)
}

extern int pfs_folder_create_pipe(int eh, const char *name) {
	pfs_folder_create(pipe)
}

#define pfs_folder_create_mount(postfix, ...) pfs_folder_create0(mount_##postfix, _mount, \
		if (!pfsc_mount_open(&((struct element_handle_mount*) c)->handle, c->handle.fs_data->read_only)) { \
			free(c); \
			return -1; \
		} \
		, name, __VA_ARGS__)

extern int pfs_folder_create_mount_intern(int eh, const char *name, i64 block_count, i32 block_size) {
	pfs_folder_create_mount(intern, block_count, block_size)
}

extern int pfs_folder_create_mount_temp(int eh, const char *name, i64 block_count, i32 block_size) {
	pfs_folder_create_mount(temp, block_count, block_size)
}

extern int pfs_folder_create_mount_rfs_file(int eh, const char *name, const char *file) {
	pfs_folder_create_mount(rfs_file, file)
}
