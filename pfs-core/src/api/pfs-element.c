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
 * pfs-element.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#include "pfs.h"
#include "../include/pfs-element.h"

extern int pfs_element_parent(int eh) {
	eh(-1)
	struct element_handle *peh = malloc(sizeof(struct element_handle));
	if (!peh) {
		errno = 0;
		(pfs_err) = PFS_ERRNO_OUT_OF_MEMORY;
		return -1;
	}
	if (pfs_ehs[eh]->handle.is_mount_point) {
		memcpy(&peh->handle, &pfs_ehs[eh]->handle.fs_data->mount_point->handle, sizeof(struct pfs_element_handle));
	} else {
		memcpy(&peh->handle, &pfs_ehs[eh]->handle, sizeof(struct pfs_element_handle));
	}
	if (!pfsc_element_get_parent(&peh->handle)) {
		free(peh);
		return -1;
	}
	void *old = hashset_add(&pfs_all_ehs_set, eh_hash(peh), peh);
	if (old) {
		peh = old;
		peh->load_count++;
	} else {
		peh->load_count = 1;
	}
	return_handle(pfs_eh_len, pfs_ehs, peh)
}

extern ui32 pfs_element_get_flags(int eh) {
	eh(-1)
	return pfsc_element_get_flags(&pfs_ehs[eh]->handle);
}

extern int pfs_element_modify_flags(int eh, ui32 add_flags, ui32 rem_flags) {
	eh(0)
	check_write_access(0)
	return pfsc_element_modify_flags(&pfs_ehs[eh]->handle, add_flags, rem_flags);
}

extern i64 pfs_element_get_create_time(int eh) {
	eh(-1)
	return pfsc_element_get_create_time(&pfs_ehs[eh]->handle);
}

extern i64 pfs_element_get_last_modify_time(int eh) {
	eh(-1)
	return pfsc_element_get_last_mod_time(&pfs_ehs[eh]->handle);
}

extern int pfs_element_set_create_time(int eh, i64 ct) {
	eh(0)
	check_write_access(0)
	return pfsc_element_set_create_time(&pfs_ehs[eh]->handle, ct);
}

extern int pfs_element_set_last_modify_time(int eh, i64 lmt) {
	eh(0)
	check_write_access(0)
	return pfsc_element_set_last_mod_time(&pfs_ehs[eh]->handle, lmt);
}

extern int pfs_element_get_name(int eh, char **name_buf, i64 *buf_len) {
	eh(0)
	return pfsc_element_get_name(&pfs_ehs[eh]->handle, name_buf, buf_len);
}

extern int pfs_element_set_name(int eh, char *name) {
	eh(0)
	check_write_access(0)
	return pfsc_element_set_name(&pfs_ehs[eh]->handle, name);
}

extern int pfs_element_set_parent(int eh, int parenteh) {
	eh(0)
	get_eh(0, parenteh)
	check_write_access(0)
	if (pfs_ehs[eh]->handle.fs_data != pfs_ehs[parenteh]->handle.fs_data) {
		pfs_err = PFS_ERRNO_DIFFERENT_FILE_SYSTEMS;
		return 0;
	}
	return pfsc_element_set_parent(&pfs_ehs[eh]->handle,
			&pfs_ehs[parenteh]->handle);
}

extern int pfs_element_move(int eh, int parenteh, char *name) {
	eh(0)
	get_eh(0, parenteh)
	return pfsc_element_move(&pfs_ehs[eh]->handle, &pfs_ehs[parenteh]->handle,
			name);
}

extern int pfs_element_same(int aeh, int beh) {
	get_eh(-1, aeh)
	get_eh(-1, beh)
	return (pfs_ehs[aeh] == pfs_ehs[beh]) ? 1 : 0;
}
