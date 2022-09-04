/*
 * bm.c
 *
 *  Created on: Jul 7, 2022
 *      Author: pat
 */

#include "patr-file-sys.h"
#include "bm.h"
#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/errno.h>

static int bm_equal(const void *a, const void *b);
static int bm_hash(const void *a);

struct bm_ram {
	struct bm_block_manager bm;
	void *blocks;
};

struct bm_file {
	struct bm_block_manager bm;
	int file;
};

#define offsetof(type, member)  __builtin_offsetof (type, member)

static void* bm_ram_get(struct bm_block_manager *bm, i64 block);
static void bm_ram_unget(struct bm_block_manager *bm, i64 block);
static void bm_ram_set(struct bm_block_manager *bm, i64 block);
static void bm_ram_sync(struct bm_block_manager *bm);

static void* bm_file_get(struct bm_block_manager *bm, i64 block);
static void bm_file_unget(struct bm_block_manager *bm, i64 block);
static void bm_file_set(struct bm_block_manager *bm, i64 block);
static void bm_file_sync(struct bm_block_manager *bm);

struct bm_block_manager* bm_new_ram_block_manager(i64 block_count,
		i32 block_size) {
	struct bm_ram *bm = malloc(sizeof(struct bm_ram));
	if (bm == NULL) {
		return NULL;
	}
	bm->blocks = malloc(block_count * (i64) block_size);
	if (bm->blocks == NULL) {
		free(bm);
		return NULL;
	}
	bm->bm.loaded.entries = NULL;
	bm->bm.loaded.setsize = 0;
	bm->bm.loaded.entrycount = 0;
	bm->bm.loaded.equalizer = bm_equal;
	bm->bm.loaded.hashmaker = bm_hash;
	bm->bm.get = bm_ram_get;
	bm->bm.unget = bm_ram_unget;
	bm->bm.set = bm_ram_set;
	bm->bm.sync = bm_ram_sync;
	return &bm->bm;
}

struct bm_block_manager* bm_new_file_block_manager(int file, i32 block_size) {
	struct bm_file *bm = malloc(sizeof(struct bm_file));
	if (bm == NULL) {
		return NULL;
	}
	bm->file = file;
	bm->bm.get = bm_file_get;
	bm->bm.unget = bm_file_unget;
	bm->bm.set = bm_file_set;
	bm->bm.sync = bm_file_sync;
	bm->bm.loaded.entries = NULL;
	bm->bm.loaded.setsize = 0;
	bm->bm.loaded.entrycount = 0;
	bm->bm.loaded.equalizer = bm_equal;
	bm->bm.loaded.hashmaker = bm_hash;
	return &bm->bm;
}

static int bm_equal(const void *a, const void *b) {
	return *(i64*) a == *(i64*) b;
}

static int bm_hash(const void *a) {
	return *(int*) a;
}

struct bm_loaded {
	i64 block;
	int count;
	void *data;
	int save;
};

static void* bm_ram_get(struct bm_block_manager *bm, i64 block) {
	struct bm_ram *br = (struct bm_ram*) bm;
	struct bm_loaded *loaded = hashset_get(&br->bm.loaded, (unsigned int) block,
			&block);
	if (loaded) {
		loaded->count++;
		return loaded->data;
	}
	loaded = malloc(sizeof(struct bm_loaded));
	if (loaded == NULL) {
		return NULL;
	}
	loaded->block = block;
	loaded->count = 1;
	loaded->data = malloc(br->bm.block_size);
	loaded->save = 0;
	if (loaded->data == NULL) {
		free(loaded);
		return NULL;
	}
	memcpy(loaded->data,
			br->blocks + (block * (i64) br->bm.block_size),
			br->bm.block_size);
	hashset_put(&br->bm.loaded, (unsigned int) block, loaded);
	return loaded->data;
}

static void bm_ram_unget(struct bm_block_manager *bm, i64 block) {
	struct bm_ram *br = (struct bm_ram*) bm;
	struct bm_loaded *loaded = hashset_get(&br->bm.loaded, (unsigned int) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		hashset_remove(&br->bm.loaded, (unsigned int) block, loaded);
		if (loaded->save) {
			memcpy(
					(void*) ((i64) br->blocks
							+ (block * (i64) br->bm.block_size)), loaded->data,
					br->bm.block_size);
		}
		free(loaded);
	}
}

static void bm_ram_set(struct bm_block_manager *bm, i64 block) {
	struct bm_ram *br = (struct bm_ram*) bm;
	struct bm_loaded *loaded = hashset_get(&br->bm.loaded, (unsigned int) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		hashset_remove(&br->bm.loaded, (unsigned int) block, loaded);
		memcpy((void*) ((i64) br->blocks + (block * (i64) br->bm.block_size)),
				loaded->data, br->bm.block_size);
		free(loaded);
	} else {
		loaded->save = 1;
	}
}

static void bm_ram_sync(struct bm_block_manager *bm) {
	//nothing to do
}

static void* bm_file_get(struct bm_block_manager *bm, i64 block) {
	struct bm_file *bf = (struct bm_file*) bm;
	struct bm_loaded *loaded = hashset_get(&bf->bm.loaded, (unsigned int) block,
			&block);
	if (loaded) {
		loaded->count++;
		return loaded->data;
	}
	loaded = malloc(sizeof(struct bm_loaded));
	if (loaded == NULL) {
		return NULL;
	}
	loaded->block = block;
	loaded->count = 1;
	loaded->data = malloc(bf->bm.block_size);
	loaded->save = 0;
	if (loaded->data == NULL) {
		free(loaded);
		return NULL;
	}
	if (lseek64(bf->file, block * (i64) bf->bm.block_size, SEEK_SET) == -1) {
		abort();
	}
	for (i64 need = bf->bm.block_size; need;) {
		i64 size = read(bf->file, loaded->data, bf->bm.block_size);
		if (size == -1) {
			if (errno == EINTR) {
				continue;
			}
			abort();
		} else if (size == 0) { // EOF
			memset((void*) ((i64) loaded->data + bf->bm.block_size - need), 0,
					need);
			break;
		}
		need -= size;
	}
	hashset_put(&bf->bm.loaded, (unsigned int) block, loaded);
	return loaded->data;
}

static void bm_file_unget(struct bm_block_manager *bm, i64 block) {
	struct bm_file *bf = (struct bm_file*) bm;
	struct bm_loaded *loaded = hashset_get(&bf->bm.loaded, (unsigned int) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		hashset_remove(&bf->bm.loaded, (unsigned int) block, loaded);
		if (loaded->save) {
			if (lseek64(bf->file, block * (i64) bf->bm.block_size, SEEK_SET)
					== -1) {
				abort();
			}
			for (i64 need = bf->bm.block_size; need;) {
				i64 wrote = write(bf->file, loaded->data, need);
				if (wrote == -1) {
					if (errno == EINTR) {
						continue;
					}
					abort();
				}
			}
		}
		free(loaded);
	}
}

static void bm_file_set(struct bm_block_manager *bm, i64 block) {
	struct bm_file *bf = (struct bm_file*) bm;
	struct bm_loaded *loaded = hashset_get(&bf->bm.loaded, (unsigned int) block,
			&block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		hashset_remove(&bf->bm.loaded, (unsigned int) block, loaded);
		if (lseek64(bf->file, block * (i64) bf->bm.block_size, SEEK_SET)
				== -1) {
			abort();
		}
		for (i64 need = bf->bm.block_size; need;) {
			i64 wrote = write(bf->file, loaded->data, need);
			if (wrote == -1) {
				if (errno == EINTR) {
					continue;
				}
				abort();
			}
		}
		free(loaded);
	}
}

static void bm_file_sync(struct bm_block_manager *bm) {
	struct bm_file *bf = (struct bm_file*) bm;
	fsync(bf->file);
}
