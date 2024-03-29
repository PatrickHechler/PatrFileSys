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
 * bm.c
 *
 *  Created on: Jul 7, 2022
 *      Author: pat
 */
#include "../pfs/bm.h"
#include "../pfs/pfs-err.h"
#include "../core/pfs-intern.h"

#include <stdlib.h>
#include <errno.h>
#include <assert.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/errno.h>
#include <sys/file.h>

int bm_equal(const void *a, const void *b);
uint64_t bm_hash(const void *a);

i64 get_none_flags(struct bm_block_manager *bm, i64 block);
void set_none_flags(struct bm_block_manager *bm, i64 block, ui64 flags);
i64 not_get_first_zero_flagged_block(struct bm_block_manager *bm);
void not_delete_all_flags(struct bm_block_manager *bm);

struct bm_file {
	struct bm_block_manager bm;
	const char *path;
	bm_fd file;
	unsigned load_count;
};

struct bm_ram {
	struct bm_block_manager bm;
	void **blocks;
	i64 block_count;
};

struct bm_flag_ram {
	struct bm_ram bm;
	ui8 *flags;
};

_Static_assert(offsetof(struct bm_ram, bm)
== 0, "error!");
_Static_assert(offsetof(struct bm_file, bm)
== 0, "error!");
_Static_assert(offsetof(struct bm_flag_ram, bm) == 0, "error!");

void* bm_lazy_get(struct bm_block_manager *bm, i64 block);

static void* bm_ram_get(struct bm_block_manager *bm, i64 block);
static int bm_ram_unget(struct bm_block_manager *bm, i64 block);
static int bm_ram_set(struct bm_block_manager *bm, i64 block);
static int bm_ram_sync(struct bm_block_manager *bm);
static int bm_ram_close(struct bm_block_manager *bm);

static void* bm_file_get(struct bm_block_manager *bm, i64 block);
static int bm_file_unget(struct bm_block_manager *bm, i64 block);
static int bm_file_set(struct bm_block_manager *bm, i64 block);
static int bm_file_sync(struct bm_block_manager *bm);
static int bm_file_close(struct bm_block_manager *bm);

static int bm_flag_ram_close(struct bm_block_manager *bm);
static i64 get_flag_ram_flags(struct bm_block_manager *bm, i64 block);
static void set_flag_ram_flags(struct bm_block_manager *bm, i64 block,
		ui64 flags);
static i64 get_flag_ram_first_zero_flagged_block(struct bm_block_manager *bm);
static void delete_flag_ram_all_flags(struct bm_block_manager *bm);

#define setValues(name, bm_close_name, set_flags_name, get_flags_name, block_flag_bit_count, get_first_zero_flagged_block_name, delete_all_flags_name) \
	setVal(void* (**)(struct bm_block_manager*, i64)       , lazy_get                 , bm_lazy_get) \
	setVal(void* (**)(struct bm_block_manager*, i64)       , get                      , bm_##name##_get) \
	setVal(int   (**)(struct bm_block_manager*, i64)       , unget                    , bm_##name##_unget) \
	setVal(int   (**)(struct bm_block_manager*, i64)       , set                      , bm_##name##_set) \
	setVal(int   (**)(struct bm_block_manager*)            , sync_bm                  , bm_##name##_sync) \
	setVal(int   (**)(struct bm_block_manager*)            , close_bm                 , bm_close_name) \
	setVal(i32*                                            , block_size               , block_size) \
	setVal(i32*                                            , block_flag_bits          , block_flag_bit_count) \
	setVal(i64   (**)(struct bm_block_manager*, i64)       , get_flags                , get_flags_name) \
	setVal(void  (**)(struct bm_block_manager*, i64, ui64) , set_flags                , set_flags_name) \
	setVal(i64   (**)(struct bm_block_manager*)            , first_zero_flagged_block , get_first_zero_flagged_block_name) \
	setVal(void  (**)(struct bm_block_manager *bm)         , delete_all_flags         , delete_all_flags_name) \

#define setNoFlagVals(name) \
		setValues(name, bm_##name##_close, set_none_flags, get_none_flags, 0, not_get_first_zero_flagged_block, not_delete_all_flags)

#define setFlagVals(name, block_flag_bit_count) \
		setValues(name, bm_flag_##name##_close, set_flag_##name##_flags, get_flag_##name##_flags, block_flag_bit_count, get_flag_##name##_first_zero_flagged_block, delete_flag_##name##_all_flags)

#define setVal(type_pntr, struct_member, value) \
		*(type_pntr) (((void*) bm) + offsetof (struct bm_block_manager, struct_member)) = value;

extern struct bm_block_manager* bm_new_ram_block_manager(i64 block_count,
		i32 block_size) {
	struct bm_ram *bm = malloc(sizeof(struct bm_ram));
	if (bm == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	bm->bm.loaded.entries = NULL;
	bm->bm.loaded.maxi = 0;
	bm->bm.loaded.entrycount = 0;
	bm->bm.loaded.equalizer = bm_equal;
	bm->bm.loaded.hashmaker = bm_hash;
	setNoFlagVals(ram)
	bm->blocks = calloc(block_count, sizeof(void*));
	bm->block_count = block_count;
	if (bm->blocks == NULL) {
		free(bm);
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	return &(bm->bm);
}

static int bm_mnt_equal(const void *a, const void *b) {
	const struct bm_file *ha = a, *hb = b;
	return strcmp(ha->path, hb->path) == 0;
}
static uint64_t bm_mnt_hash(const void *a) {
	const struct bm_file *ha = a;
	uint64_t hash = 0;
	for (const char *p = ha->path; *p; p++) {
		hash = *p + (hash * 73);
	}
	return hash;
}

static struct hashset pfs_all_ext_mnt = {   //
		/*	  */.entries = NULL,            //
				.entrycount = 0,            //
				.equalizer = bm_mnt_equal, //
				.hashmaker = bm_mnt_hash,  //
				.maxi = 0                   //
		};

static inline struct bm_block_manager* bm_new_file_block_manager(bm_fd fd,
		i32 block_size, const char *path) {
	if (block_size <= 0) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return NULL;
	}
#if !defined PFS_PORTABLE_BUILD && !defined PFS_HALF_PORTABLE_BUILD
	if (flock(fd, LOCK_EX | LOCK_NB) == -1) {
		switch (errno) {
		case EBADF:
			errno = 0;
			pfs_err = PFS_ERRNO_ILLEGAL_ARG;
			return NULL;
		case ENOLCK:
			errno = 0;
			pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
			return NULL;
		case EWOULDBLOCK:
			errno = 0;
			pfs_err = PFS_ERRNO_IO_ERR;
			return NULL;
		default:
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			return NULL;
		}
	}
#endif // !PFS_HALF_PORTABLE_BUILD && !PFS_PORTABLE_BUILD
	struct bm_file *bm = malloc(sizeof(struct bm_file));
	if (bm == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	bm->bm.loaded.entries = NULL;
	bm->bm.loaded.maxi = 0;
	bm->bm.loaded.entrycount = 0;
	bm->bm.loaded.equalizer = bm_equal;
	bm->bm.loaded.hashmaker = bm_hash;
	setNoFlagVals(file)
	bm->file = fd;
	bm->load_count = 1;
	bm->path = path;
	if (hashset_put(&pfs_all_ext_mnt, bm_hash(bm), bm) != NULL) {
		abort();
	}
	return &(bm->bm);
}

extern struct bm_block_manager* bm_new_file_block_manager_path_bs(
		const char *file, i32 block_size, int read_only) {
	bm_fd fd;
	if (read_only) {
		fd = bm_fd_open_ro(file);
	} else {
#ifdef PFS_PORTABLE_BUILD // fopen has no mode for open existing (but do not truncate) or create new
		fd = bm_fd_open_rw(file);
		if (!fd) {
			int e = errno;
			if (e == EINVAL || e == ENOENT) {
				errno = 0;
				fd = bm_fd_open_rw_trunc(file);
			}
		}
#else
		fd = open64(file, O_RDWR | O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH);
#endif
	}
#ifdef PFS_PORTABLE_BUILD
	if (fd == NULL)
#else // PFS_PORTABLE_BUILD
	if (fd == -1)
#endif // PFS_PORTABLE_BUILD
			{
		switch (errno) {
		case EEXIST:
			pfs_err = PFS_ERRNO_ELEMENT_NOT_EXIST;
			break;
		case EIO:
			pfs_err = PFS_ERRNO_IO_ERR;
			break;
		default:
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
			break;
		}
		return NULL;
	}
	struct bm_file tf;
	tf.path = file;
	struct bm_file *f = hashset_get(&pfs_all_ext_mnt,
			bm_mnt_hash(&tf),
			&tf);
	if (f) {
		if (f->bm.block_size != block_size) {
			abort();
		}
		if (++f->load_count == 0) {
			abort();
		}
		return &f->bm;
	}
	return bm_new_file_block_manager(fd, block_size, strdup(file));
}

extern struct bm_block_manager* bm_new_file_block_manager_path(const char *file,
		int read_only) {
	bm_fd fd = bm_fd_open(file, read_only);
	struct pfs_b0 b0;
	bm_fd_read(fd, &b0, sizeof(struct pfs_b0));
	long value = sizeof(struct pfs_folder_entry);
	pfs_validate_b0(&b0, pfs_err = PFS_ERRNO_ILLEGAL_DATA; return NULL;, 0);
	struct bm_file *f = hashset_get(&pfs_all_ext_mnt,
			bm_mnt_hash(&file - offsetof(struct bm_file, path)),
			&file - offsetof(struct bm_file, path));
	if (f) {
		if (f->bm.block_size != b0.block_size) {
			abort();
		}
		if (++f->load_count == 0) {
			abort();
		}
		return &f->bm;
	}
	return bm_new_file_block_manager(fd, b0.block_size, strdup(file));
}

extern struct bm_block_manager* bm_new_flaggable_ram_block_manager(
		i64 block_count, i32 block_size) {
	struct bm_flag_ram *bm = malloc(sizeof(struct bm_flag_ram));
	if (bm == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	bm->bm.bm.loaded.entries = NULL;
	bm->bm.bm.loaded.maxi = 0;
	bm->bm.bm.loaded.entrycount = 0;
	bm->bm.bm.loaded.equalizer = bm_equal;
	bm->bm.bm.loaded.hashmaker = bm_hash;
	setFlagVals(ram, 8)
	bm->bm.blocks = calloc(block_count, sizeof(void*));
	bm->bm.block_count = block_count;
	if (bm->bm.blocks == NULL) {
		free(bm);
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	bm->flags = calloc(1, block_count);
	if (bm->flags == NULL) {
		free(bm->bm.blocks);
		free(bm);
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	return &(bm->bm.bm);
}

int bm_equal(const void *a, const void *b) {
	return *(i64*) a == *(i64*) b;
}

uint64_t bm_hash(const void *a) {
	return *(uint64_t*) a;
}

struct bm_loaded {
	i64 block;
	void *data;
	int count;
	int save;
};

_Static_assert(offsetof(struct bm_loaded, block) == 0, "Error!");

void* bm_lazy_get(struct bm_block_manager *bm, i64 block) {
	struct bm_loaded *loaded = hashset_get(&bm->loaded, (uint64_t) block,
			&block);
	if (loaded) {
		loaded->count++;
		return loaded->data;
	}
	loaded = malloc(sizeof(struct bm_loaded));
	if (loaded == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		errno = 0;
		return NULL;
	}
	loaded->block = block;
	loaded->count = 1;
	loaded->data = calloc(1, bm->block_size);
	loaded->save = 0;
	if (loaded->data == NULL) {
		free(loaded);
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		errno = 0;
		return NULL;
	}
	if (hashset_put(&bm->loaded, (uint64_t) block, loaded) != NULL) {
		abort();
	}
	return loaded->data;
}

static void* bm_ram_get(struct bm_block_manager *bm, i64 block) {
	struct bm_ram *br = (struct bm_ram*) bm;
	struct bm_loaded *loaded = hashset_get(&br->bm.loaded, (uint64_t) block,
			&block);
	if (loaded) {
		loaded->count++;
		return loaded->data;
	}
	loaded = malloc(sizeof(struct bm_loaded));
	if (loaded == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		errno = 0;
		return NULL;
	}
	loaded->block = block;
	loaded->count = 1;
	loaded->data = malloc(br->bm.block_size);
	loaded->save = 0;
	if (loaded->data == NULL) {
		free(loaded);
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		errno = 0;
		return NULL;
	}
	if (br->blocks[block]) {
		memcpy(loaded->data, br->blocks[block], br->bm.block_size);
	} else {
		memset(loaded->data, 0, br->bm.block_size);
	}
	if (hashset_put(&br->bm.loaded, (uint64_t) block, loaded) != NULL) {
		abort();
	}
	return loaded->data;
}

static int bm_ram_unget(struct bm_block_manager *bm, i64 block) {
	struct bm_ram *br = (struct bm_ram*) bm;
	struct bm_loaded *loaded = hashset_get(&br->bm.loaded, (uint64_t) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		if (hashset_remove(&br->bm.loaded, (uint64_t) block, loaded)
				!= loaded) {
			abort();
		}
		if (loaded->save) {
			if (!br->blocks[block]) {
				br->blocks[block] = malloc(br->bm.block_size);
				if (!br->blocks[block]) {
					errno = 0;
					br->blocks[block] = loaded->data;
					goto l;
				}
			}
			memcpy(br->blocks[block], loaded->data, br->bm.block_size);
		}
		free(loaded->data);
		l: free(loaded);
	}
	return 1;
}

static int bm_ram_set(struct bm_block_manager *bm, i64 block) {
	struct bm_ram *br = (struct bm_ram*) bm;
	struct bm_loaded *loaded = hashset_get(&br->bm.loaded, (uint64_t) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	loaded->save = 1;
	if (--loaded->count == 0) {
		if (hashset_remove(&br->bm.loaded, (uint64_t) block, loaded)
				!= loaded) {
			abort();
		}
		if (!br->blocks[block]) {
			br->blocks[block] = malloc(br->bm.block_size);
			if (!br->blocks[block]) {
				errno = 0;
				br->blocks[block] = loaded->data;
				goto l;
			}
		}
		memcpy(br->blocks[block], loaded->data, br->bm.block_size);
		free(loaded->data);
		l: free(loaded);
	}
	return 1;
}

static int bm_ram_sync(struct bm_block_manager *bm) {
	return 1;
}

static int bm_ram_close(struct bm_block_manager *bm) {
	struct bm_ram *br = (struct bm_ram*) bm;
	if (bm->loaded.entrycount > 0) {
		abort();
	}
	for (i64 bc = br->block_count; bc--;) {
		if (br->blocks[bc]) {
			free(br->blocks[bc]);
		}
	}
	free(br->blocks);
	free(bm);
	return 1;
}

static void* bm_file_get(struct bm_block_manager *bm, i64 block) {
	struct bm_file *bf = (struct bm_file*) bm;
	struct bm_loaded *loaded = hashset_get(&bf->bm.loaded, (uint64_t) block,
			&block);
	if (loaded) {
		loaded->count++;
		return loaded->data;
	}
	loaded = malloc(sizeof(struct bm_loaded));
	if (loaded == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		errno = 0;
		return NULL;
	}
	loaded->block = block;
	loaded->count = 1;
	loaded->data = malloc(bf->bm.block_size);
	loaded->save = 0;
	if (loaded->data == NULL) {
		free(loaded);
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		errno = 0;
		return NULL;
	}
	if (bm_fd_seek(bf->file, block * bf->bm.block_size) == -1) {
		free(loaded);
		free(loaded->data);
		pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		return NULL;
	}
	void *buf = loaded->data;
	for (i64 remain = bf->bm.block_size; remain > 0;) {
		i64 reat = bm_fd_read(bf->file, buf, remain);
		if (reat <= 0) {
#ifdef PFS_PORTABLE_BUILD
			if (!feof(bf->file)) {
#else
			if (reat) {
#endif
				switch (errno) {
#if EWOULDBLOCK != EAGAIN
				case EWOULDBLOCK:
#endif
				case EAGAIN:
					wait5ms()
					;
					errno = 0;
					continue;
				case EINTR:
					errno = 0;
					continue;
				default:
					free(loaded);
					free(loaded->data);
					pfs_err = PFS_ERRNO_IO_ERR;
					return NULL;
				}
			}
#ifdef PFS_PORTABLE_BUILD
			clearerr(bf->file);
#endif
			memset(buf, 0, remain);
			break;
		}
		remain -= reat;
		buf += reat;
	}
	if (hashset_put(&bf->bm.loaded, (uint64_t) block, loaded) != NULL) {
		abort();
	}
	return loaded->data;
}

static inline int save_block(struct bm_file *bf, struct bm_loaded *loaded) {
	if (bm_fd_seek(bf->file, loaded->block * (i64) bf->bm.block_size) == -1) {
		abort();
	}
	void *data = loaded->data;
	for (i64 need = bf->bm.block_size;;) {
		i64 wrote = bm_fd_write(bf->file, data, need);
		if (wrote == -1) {
			int e = errno;
			errno = 0;
			switch (e) {
#if EWOULDBLOCK != EAGAIN
			case EWOULDBLOCK:
#endif
			case EAGAIN:
			case EINTR:
				continue;
			case EIO:
				pfs_err = PFS_ERRNO_IO_ERR;
				return 0;
			default:
				pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
				return 0;
			}
		}
		if (need -= wrote) {
			data += wrote;
			continue;
		}
		break;
	}
	return 1;
}

static int bm_file_unget(struct bm_block_manager *bm, i64 block) {
	struct bm_file *bf = (struct bm_file*) bm;
	struct bm_loaded *loaded = hashset_get(&bf->bm.loaded, (uint64_t) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		if (hashset_remove(&bf->bm.loaded, (uint64_t) block, loaded)
				!= loaded) {
			abort();
		}
		int res = 1;
		if (loaded->save) {
			res = save_block(bf, loaded);
		}
		free(loaded->data);
		free(loaded);
		return res;
	}
	return 1;
}

static int bm_file_set(struct bm_block_manager *bm, i64 block) {
	struct bm_file *bf = (struct bm_file*) bm;
	struct bm_loaded *loaded = hashset_get(&bf->bm.loaded, (uint64_t) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		if (hashset_remove(&bf->bm.loaded, (uint64_t) block, loaded)
				!= loaded) {
			abort();
		}
		int res = save_block(bf, loaded);
		free(loaded->data);
		free(loaded);
		return res;
	}
	loaded->save = 1;
	return 1;
}

static int bm_file_sync(struct bm_block_manager *bm) {
	struct bm_file *bf = (struct bm_file*) bm;
	bm_fd_flush(bf->file);
	if (errno) {
		if (errno == EIO) {
			pfs_err = PFS_ERRNO_IO_ERR;
		} else {
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		}
		errno = 0;
		return 0;
	}
	return 1;
}

static int save_block_ret1(void *arg0, void *element) {
	save_block(arg0, element);
	return 1;
}

static int bm_file_close(struct bm_block_manager *bm) {
	struct bm_file *bf = (struct bm_file*) bm;
	if (--bf->load_count) {
		return 1;
	}
	hashset_remove(&pfs_all_ext_mnt, bm_mnt_hash(bf), bf);
	if (bf->bm.loaded.entrycount > 0) {
		hashset_for_each(&bf->bm.loaded, save_block_ret1, bf);
		abort();
	}
	free(bf->bm.loaded.entries);
#if !defined PFS_PORTABLE_BUILD && !defined PFS_HALF_PORTABLE_BUILD
	flock(bf->file, LOCK_UN);
#endif // !PFS_HALF_PORTABLE_BUILD && !PFS_PORTABLE_BUILD
	if (bm_fd_close(bf->file) == -1) {
		switch (errno) {
		case EIO:
			pfs_err = PFS_ERRNO_IO_ERR;
			break;
		default:
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		}
		errno = 0;
		free(bm);
		return 0;
	}
	free(bm);
	return 1;
}

i64 get_none_flags(struct bm_block_manager *bm, i64 block) {
	return 0L;
}
void set_none_flags(struct bm_block_manager *bm, i64 block, ui64 flags) {
}

i64 not_get_first_zero_flagged_block(struct bm_block_manager *bm) {
	return -1L;
}

void not_delete_all_flags(struct bm_block_manager *bm) {
}

static int bm_flag_ram_close(struct bm_block_manager *bm) {
	struct bm_flag_ram *f = (struct bm_flag_ram*) bm;
	free(f->flags);
	return bm_ram_close(bm);
}

static i64 get_flag_ram_flags(struct bm_block_manager *bm, i64 block) {
	return ((struct bm_flag_ram*) bm)->flags[block];
}

static void set_flag_ram_flags(struct bm_block_manager *bm, i64 block,
		ui64 flags) {
	((struct bm_flag_ram*) bm)->flags[block] = (ui8) flags;
}

static i64 get_flag_ram_first_zero_flagged_block(struct bm_block_manager *bm) {
	struct bm_flag_ram *f = (struct bm_flag_ram*) bm;
	for (i64 i = 0; i < f->bm.block_count; i++) {
		if (f->flags[i] != 0) {
			continue;
		} else {
			return i;
		}
	}
	return -1L;
}

static void delete_flag_ram_all_flags(struct bm_block_manager *bm) {
	memset(((struct bm_flag_ram*) bm)->flags, 0,
			((struct bm_flag_ram*) bm)->bm.block_count);
}
