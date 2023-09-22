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
#include "pfs-mount.h"

#define get_mount0_(me, mount_name, block_name, error_return, posfix) \
	ensure_block_is_entry(pfs(me), me->handle.element_place.block); \
	void *block_name = pfs(me)->get(pfs(me), me->handle.element_place.block); \
	if (block_name == NULL) { \
		return error_return; \
	} \
	struct pfs_mount_point##posfix *mount_name = block_name + me->handle.element_place.pos;

#define get_mount0(mount_name, block_name, error_return) get_mount0_(me, mount_name, block_name, error_return, )

#define get_mount_(me, mount_name, error_return, posfix) get_mount0_(me, mount_name, block_data, error_return, posfix)

#define get_mount(error_return) get_mount0(gmount, block_data, error_return)

struct bm_block_manager_intern {
	struct bm_block_manager bm;
	struct pfs_element_handle_mount *me;
	i64 block_count;
};

struct bm_loaded {
	i64 block;
	void *data;
	int count;
	int save;
};

int bm_equal(const void *a, const void *b);
uint64_t bm_hash(const void *a);

void* bm_lazy_get(struct bm_block_manager *bm, i64 block);
i64 get_none_flags(struct bm_block_manager *bm, i64 block);
void set_none_flags(struct bm_block_manager *bm, i64 block, ui64 flags);
i64 not_get_first_zero_flagged_block(struct bm_block_manager *bm);
void not_delete_all_flags(struct bm_block_manager *bm);

static void* pfs_inten_bm_get(struct bm_block_manager *bm, i64 block);
static int pfs_inten_bm_unget(struct bm_block_manager *bm, i64 block);
static int pfs_inten_bm_set(struct bm_block_manager *bm, i64 block);
static int pfs_inten_sync_bm(struct bm_block_manager *bm);
static int pfs_inten_bm_close_bm(struct bm_block_manager *bm);

struct pfs_ext_mnt_entry {
	const char *path;
	struct bm_file *bm;
};

int pfsc_mount_open(pfs_meh me, int read_only) {
	get_mount(0)
	ui32 f = gmount->flags;
	if (f & PFS_MOUNT_FLAGS_READ_ONLY) {
		read_only = 1;
	}
	struct element_handle *root = malloc(sizeof(struct element_handle));
	me->fs.mount_point = me;
	if (!root) {
		if (root) {
			free(root);
		}
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		errno = 0;
		pfs(me)->unget(pfs(me), me->handle.element_place.block);
		return 0;
	}
	switch (f & PFS_MOUNT_FLAGS_TYPE_MASK) {
	case PFS_MOUNT_FLAGS_INTERN: {
		struct pfs_mount_point_intern *imount =
				(struct pfs_mount_point_intern*) gmount;
		struct bm_block_manager_intern *inner = malloc(
				sizeof(struct bm_block_manager_intern));
		if (!inner) {
			pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
			errno = 0;
			pfs(me)->unget(pfs(me), me->handle.element_place.block);
			return 0;
		}
		inner->me = me;
		inner->block_count = imount->block_count;
		inner->bm.loaded.entries = NULL;
		inner->bm.loaded.entrycount = 0;
		inner->bm.loaded.maxi = 0;
		inner->bm.loaded.equalizer = bm_equal;
		inner->bm.loaded.hashmaker = bm_hash;
#define bm_set(type, target, value) { \
			type* tmp = (type*) (((void*)inner) + offsetof(struct bm_block_manager, target)); \
			*tmp = (type) value; \
		}
		bm_set(void*, lazy_get, bm_lazy_get)
		bm_set(void*, get, pfs_inten_bm_get)
		bm_set(void*, unget, pfs_inten_bm_unget)
		bm_set(void*, set, pfs_inten_bm_set)
		bm_set(void*, sync_bm, pfs_inten_sync_bm)
		bm_set(void*, close_bm, pfs_inten_bm_close_bm)
		bm_set(i32, block_size, imount->block_size)
		bm_set(int, block_flag_bits, 0)
		bm_set(void*, get_flags, get_none_flags)
		bm_set(void*, set_flags, set_none_flags)
		bm_set(void*, first_zero_flagged_block,
				not_get_first_zero_flagged_block)
		bm_set(void*, delete_all_flags, not_delete_all_flags)
#undef bm_set
		if (imount->file.file_length == 0) {
			if (!pfsc_format(&inner->bm, imount->block_count, NULL, "")) {
				free(inner);
				pfs(me)->unget(pfs(me), me->handle.element_place.block);
				return 0;
			}
		}
		pfs(me)->unget(pfs(me), me->handle.element_place.block);
		return pfsc_fill_root(&inner->bm, root, &me->fs, read_only);
	}
	case PFS_MOUNT_FLAGS_REAL_FS_FILE: {
		struct pfs_mount_point_real_fs_file *rfmount =
				(struct pfs_mount_point_real_fs_file*) gmount;
		i32 len = get_size_from_block_table(block_data,
				me->handle.element_place.pos, pfs(me)->block_size);
		char *file = malloc(
				len - sizeof(struct pfs_mount_point_real_fs_file) + 1L);
		memcpy(file, rfmount->path, len);
		file[len] = '\0';
		pfs(me)->unget(pfs(me), me->handle.element_place.block);
		// if fs is marked as read-only the fd is still open in rw mode, but that should not matter
		struct bm_block_manager *inner = bm_new_file_block_manager_path(file, read_only);
		free(file);
		if (!inner) {
			pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
			errno = 0;
			return 0;
		}
		return pfsc_fill_root(inner, root, &me->fs, read_only);
	}
	case PFS_MOUNT_FLAGS_TEMP: {
		struct pfs_mount_point_tmp *tmount =
				(struct pfs_mount_point_tmp*) gmount;
		struct bm_block_manager *inner = bm_new_ram_block_manager(
				tmount->block_count, tmount->block_size);
		if (!inner) {
			pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
			errno = 0;
			return 0;
		}
		return pfsc_fill_root(inner, root, &me->fs, read_only);
	}
	default:
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return 0;
	}
}

static inline int save_block(struct bm_block_manager_intern *bmi,
		struct bm_loaded *loaded) {
	get_mount_(bmi->me, imount, 0, _intern)
	i64 fpos = imount->block_size * loaded->block;
	struct pfs_file_data file0 = { .e = &imount->generic.element, .f =
			&imount->file };
	pfsc_file_write0(&bmi->bm, file0, fpos, loaded->data, bmi->bm.block_size,
			bmi->me->handle.element_place.block);
	return 1;
}

static void* pfs_inten_bm_get(struct bm_block_manager *bm, i64 block) {
	struct bm_block_manager_intern *bi = (struct bm_block_manager_intern*) bm;
	struct bm_loaded *loaded = hashset_get(&bi->bm.loaded, (uint64_t) block,
			&block);
	if (loaded) {
		loaded->count++;
		return loaded->data;
	}
	get_mount_(bi->me, imount, NULL, _intern)
	loaded = malloc(sizeof(struct bm_loaded));
	if (loaded == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		errno = 0;
		return NULL;
	}
	loaded->block = block;
	loaded->data = malloc(bi->bm.block_size);
	loaded->count = 1;
	loaded->save = 0;
	if (loaded->data == NULL) {
		free(loaded);
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		errno = 0;
		return NULL;
	}
	i64 fpos = block * bi->bm.block_size;
	struct pfs_file_data file0 = {                 //
			/*	  */.e = &imount->generic.element, //
					.f = &imount->file             //
			};
	if (!pfsc_file_read0(pfs(bi->me), file0, fpos, loaded->data,
			bi->bm.block_size, bi->me->handle.element_place.block)) {
		free(loaded);
		free(loaded->data);
		return NULL;
	}
	if (hashset_put(&bi->bm.loaded, (uint64_t) block, loaded) != NULL) {
		abort();
	}
	return loaded->data;
}
static int pfs_inten_bm_unget(struct bm_block_manager *bm, i64 block) {
	struct bm_block_manager_intern *bi = (struct bm_block_manager_intern*) bm;
	struct bm_loaded *loaded = hashset_get(&bi->bm.loaded, (uint64_t) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		if (hashset_remove(&bi->bm.loaded, (uint64_t) block, loaded)
				!= loaded) {
			abort();
		}
		int res = 1;
		if (loaded->save) {
			res = save_block(bi, loaded);
		}
		free(loaded->data);
		free(loaded);
		return res;
	}
	return 1;
}
static int pfs_inten_bm_set(struct bm_block_manager *bm, i64 block) {
	struct bm_block_manager_intern *bi = (struct bm_block_manager_intern*) bm;
	struct bm_loaded *loaded = hashset_get(&bi->bm.loaded, (uint64_t) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		if (hashset_remove(&bi->bm.loaded, (uint64_t) block, loaded)
				!= loaded) {
			abort();
		}
		int res = save_block(bi, loaded);
		free(loaded->data);
		free(loaded);
		return res;
	}
	loaded->save = 1;
	return 1;
}

static int save_block_ret1(void *arg0, void *element) {
	save_block(arg0, element);
	return 1;
}

static int pfs_inten_sync_bm(struct bm_block_manager *bm) {
	struct bm_block_manager_intern *bi = (struct bm_block_manager_intern*) bm;
	return pfs(bi->me)->sync_bm(pfs(bi->me));
}

static int pfs_inten_bm_close_bm(struct bm_block_manager *bm) {
	if (bm->loaded.entrycount > 0) {
		hashset_for_each(&bm->loaded, save_block_ret1, bm);
		abort();
	}
	free(bm->loaded.entries);
	free(bm);
	return 1;
}

int pfsc_mount_close(pfs_meh me) {
	int res = me->fs.file_sys->close_bm(me->fs.file_sys);
	free(me->fs.root);
	return res;
}

enum mount_type pfsc_mount_type(pfs_meh me) {
	get_mount(0)
	return gmount->flags & PFS_MOUNT_FLAGS_TYPE_MASK;
}
