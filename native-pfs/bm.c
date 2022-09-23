/*
 * bm.c
 *
 *  Created on: Jul 7, 2022
 *      Author: pat
 */

#include "bm.h"
#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/errno.h>

static int bm_equal(const void *a, const void *b);
static int bm_hash(const void *a);

static ui64 get_none_flags(struct bm_block_manager *bm, i64 block);
static void set_none_flags(struct bm_block_manager *bm, i64 block, ui64 flags);
static i64 not_get_first_zero_flagged_block(struct bm_block_manager *bm);
static void not_delete_all_flags(struct bm_block_manager *bm);

struct bm_ram {
	struct bm_block_manager bm;
	void *blocks;
};

struct bm_file {
	struct bm_block_manager bm;
	int file;
};

struct bm_flag_ram {
	struct bm_ram bm;
	i64 block_count;
	ui8 *flags;
};

static_assert(offsetof(struct bm_ram, bm)
== 0, "error!");
static_assert(offsetof(struct bm_file, bm)
== 0, "error!");
static_assert(offsetof(struct bm_flag_ram, bm) == 0, "error!");

static void* bm_ram_get(struct bm_block_manager *bm, i64 block);
static void bm_ram_unget(struct bm_block_manager *bm, i64 block);
static void bm_ram_set(struct bm_block_manager *bm, i64 block);
static void bm_ram_sync(struct bm_block_manager *bm);
static void bm_ram_close(struct bm_block_manager *bm);

static void* bm_file_get(struct bm_block_manager *bm, i64 block);
static void bm_file_unget(struct bm_block_manager *bm, i64 block);
static void bm_file_set(struct bm_block_manager *bm, i64 block);
static void bm_file_sync(struct bm_block_manager *bm);
static void bm_file_close(struct bm_block_manager *bm);

static void bm_flag_ram_close(struct bm_block_manager *bm);
static ui64 get_flag_ram_flags(struct bm_block_manager *bm, i64 block);
static void set_flag_ram_flags(struct bm_block_manager *bm, i64 block, ui64 flags);
static i64 get_flag_ram_first_zero_flagged_block(struct bm_block_manager *bm);
static void delete_flag_ram_all_flags(struct bm_block_manager *bm);

#define setValues(name, bm_close_name, set_flags_name, get_flags_name, block_flag_bit_count, get_first_zero_flagged_block_name, delete_all_flags_name) \
	setVal(void* (**)(struct bm_block_manager*, i64)       , get                      , bm_##name##_get) \
	setVal(void  (**)(struct bm_block_manager*, i64)       , unget                    , bm_##name##_unget) \
	setVal(void  (**)(struct bm_block_manager*, i64)       , set                      , bm_##name##_set) \
	setVal(void  (**)(struct bm_block_manager*)            , sync_bm                  , bm_##name##_sync) \
	setVal(void  (**)(struct bm_block_manager*)            , close_bm                 , bm_close_name) \
	setVal(i32*                                            , block_size               , block_size) \
	setVal(i32*                                            , block_flag_bits          , block_flag_bit_count) \
	setVal(ui64  (**)(struct bm_block_manager*, i64)       , get_flags                , get_flags_name) \
	setVal(void  (**)(struct bm_block_manager*, i64, ui64) , set_flags                , set_flags_name) \
	setVal(i64   (**)(struct bm_block_manager*)            , first_zero_flagged_block , get_first_zero_flagged_block_name) \
	setVal(void  (**)(struct bm_block_manager *bm)         , delete_all_flags         , delete_all_flags_name) \

#define setNoFlagVals(name) \
		setValues(name, bm_##name##_close, set_none_flags, get_none_flags, 0, not_get_first_zero_flagged_block, not_delete_all_flags)

#define setFlagVals(name, block_flag_bit_count) \
		setValues(name, bm_flag_##name##_close, set_flag_##name##_flags, get_flag_##name##_flags, block_flag_bit_count, get_flag_##name##_first_zero_flagged_block, delete_flag_##name##_all_flags)

#define setVal(type_pntr, struct_member, value) \
		*(type_pntr) (((void*) bm) + offsetof (struct bm_block_manager, struct_member)) = value;

extern struct bm_block_manager* bm_new_ram_block_manager(i64 block_count, i32 block_size) {
	struct bm_ram *bm = malloc(sizeof(struct bm_ram));
	if (bm == NULL) {
		return NULL;
	}
	bm->bm.loaded.entries = NULL;
	bm->bm.loaded.setsize = 0;
	bm->bm.loaded.entrycount = 0;
	bm->bm.loaded.equalizer = bm_equal;
	bm->bm.loaded.hashmaker = bm_hash;
	setNoFlagVals(ram)
	bm->blocks = malloc(block_count * (i64) block_size);
	if (bm->blocks == NULL) {
		free(bm);
		return NULL;
	}
	return &bm->bm;
}

extern struct bm_block_manager* bm_new_file_block_manager(int fd, i32 block_size) {
	struct bm_file *bm = malloc(sizeof(struct bm_file));
	if (bm == NULL) {
		return NULL;
	}
	bm->bm.loaded.entries = NULL;
	bm->bm.loaded.setsize = 0;
	bm->bm.loaded.entrycount = 0;
	bm->bm.loaded.equalizer = bm_equal;
	bm->bm.loaded.hashmaker = bm_hash;
	setNoFlagVals(file)
	bm->file = fd;
	return &bm->bm;
}

extern struct bm_block_manager* bm_new_flaggable_ram_block_manager(i64 block_count, i32 block_size) {
	struct bm_flag_ram *bm = malloc(sizeof(struct bm_flag_ram));
	if (bm == NULL) {
		return NULL;
	}
	bm->bm.bm.loaded.entries = NULL;
	bm->bm.bm.loaded.setsize = 0;
	bm->bm.bm.loaded.entrycount = 0;
	bm->bm.bm.loaded.equalizer = bm_equal;
	bm->bm.bm.loaded.hashmaker = bm_hash;
	setFlagVals(ram, 8)
	bm->bm.blocks = malloc(block_count * (i64) block_size);
	if (bm->bm.blocks == NULL) {
		free(bm);
		return NULL;
	}
	bm->flags = malloc(block_count);
	if (bm->flags == NULL) {
		free(bm->bm.blocks);
		free(bm);
		return NULL;
	}
	bm->block_count = block_count;
	return &bm->bm.bm;
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
	struct bm_loaded *loaded = hashset_get(&br->bm.loaded, (unsigned int) block, &block);
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
	memcpy(loaded->data, br->blocks + (block * (i64) br->bm.block_size), br->bm.block_size);
	hashset_put(&br->bm.loaded, (unsigned int) block, loaded);
	return loaded->data;
}

static void bm_ram_unget(struct bm_block_manager *bm, i64 block) {
	struct bm_ram *br = (struct bm_ram*) bm;
	struct bm_loaded *loaded = hashset_get(&br->bm.loaded, (unsigned int) block, &block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		hashset_remove(&br->bm.loaded, (unsigned int) block, loaded);
		if (loaded->save) {
			memcpy((void*) ((i64) br->blocks + (block * (i64) br->bm.block_size)), loaded->data,
			        br->bm.block_size);
		}
		free(loaded->data);
		free(loaded);
	}
}

static void bm_ram_set(struct bm_block_manager *bm, i64 block) {
	struct bm_ram *br = (struct bm_ram*) bm;
	struct bm_loaded *loaded = hashset_get(&br->bm.loaded, (unsigned int) block, &block);
	if (loaded == NULL) {
		abort();
	}
	loaded->save = 1;
	if (--loaded->count == 0) {
		hashset_remove(&br->bm.loaded, (unsigned int) block, loaded);
		memcpy((void*) ((i64) br->blocks + (block * (i64) br->bm.block_size)), loaded->data,
		        br->bm.block_size);
		free(loaded->data);
		free(loaded);
	}
}

static void bm_ram_sync(struct bm_block_manager *bm) {
	//nothing to do
}

static void bm_ram_close(struct bm_block_manager *bm) {
	struct bm_ram *br = (struct bm_ram*) bm;
	if (bm->loaded.entrycount > 0) {
		abort();
	}
	free(br->blocks);
}

static void* bm_file_get(struct bm_block_manager *bm, i64 block) {
	struct bm_file *bf = (struct bm_file*) bm;
	struct bm_loaded *loaded = hashset_get(&bf->bm.loaded, (unsigned int) block, &block);
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
		perror("error");
		fflush(NULL);
		abort();
	}
	for (i64 need = bf->bm.block_size; need;) {
		i64 size = read(bf->file, loaded->data, bf->bm.block_size);
		if (size == -1) {
			switch (errno) {
			case EINTR:
			case EAGAIN:
				continue;
			}
			abort();
		} else if (size == 0) { // EOF
			memset((void*) ((i64) loaded->data + bf->bm.block_size - need), 0, need);
			break;
		}
		need -= size;
	}
	hashset_put(&bf->bm.loaded, (unsigned int) block, loaded);
	return loaded->data;
}

static inline void save_block(i64 block, struct bm_file *bf, struct bm_loaded *loaded) {
	if (lseek64(bf->file, block * (i64) bf->bm.block_size, SEEK_SET) == -1) {
		abort();
	}
	void *data = loaded->data;
	for (i64 need = bf->bm.block_size; need;) {
		i64 wrote = write(bf->file, data, need);
		if (wrote == -1) {
			if (errno == EINTR) {
				continue;
			}
			abort();
		}
		need -= wrote;
		data += wrote;
	}
}

static void bm_file_unget(struct bm_block_manager *bm, i64 block) {
	struct bm_file *bf = (struct bm_file*) bm;
	struct bm_loaded *loaded = hashset_get(&bf->bm.loaded, (unsigned int) block, &block);
	if (loaded == NULL) {
		abort();
	}
	if (--loaded->count == 0) {
		hashset_remove(&bf->bm.loaded, (unsigned int) block, loaded);
		if (loaded->save) {
			save_block(block, bf, loaded);
		}
		free(loaded->data);
		free(loaded);
	}
}

static void bm_file_set(struct bm_block_manager *bm, i64 block) {
	struct bm_file *bf = (struct bm_file*) bm;
	struct bm_loaded *loaded = hashset_get(&bf->bm.loaded, (unsigned int) block, &block);
	if (loaded == NULL) {
		abort();
	}
	loaded->save = 1;
	if (--loaded->count == 0) {
		hashset_remove(&bf->bm.loaded, (unsigned int) block, loaded);
		save_block(block, bf, loaded);
		free(loaded->data);
		free(loaded);
	}
}

static void bm_file_sync(struct bm_block_manager *bm) {
	struct bm_file *bf = (struct bm_file*) bm;
	fsync(bf->file);
}

static void bm_file_close(struct bm_block_manager *bm) {
	struct bm_file *bf = (struct bm_file*) bm;
	if (bm->loaded.entrycount > 0) {
		abort();
	}
	close(bf->file);
	free(bm->loaded.entries);
}

static ui64 get_none_flags(struct bm_block_manager *bm, i64 block) {
	return 0L;
}
static void set_none_flags(struct bm_block_manager *bm, i64 block, ui64 flags) {

}

static i64 not_get_first_zero_flagged_block(struct bm_block_manager *bm) {
	return -1L;
}

static void not_delete_all_flags(struct bm_block_manager *bm) {

}

static void bm_flag_ram_close(struct bm_block_manager *bm) {
	struct bm_flag_ram *f = (struct bm_flag_ram*) bm;
	free(f->flags);
	bm_ram_close(bm);
}

static ui64 get_flag_ram_flags(struct bm_block_manager *bm, i64 block) {
	return ((struct bm_flag_ram*) bm)->flags[block];
}

static void set_flag_ram_flags(struct bm_block_manager *bm, i64 block, ui64 flags) {
	((struct bm_flag_ram*) bm)->flags[block] = (ui8) flags;
}

static i64 get_flag_ram_first_zero_flagged_block(struct bm_block_manager *bm) {
	struct bm_flag_ram *f = (struct bm_flag_ram*) bm;
	for (i64 i = 0; i < f->block_count; i++) {
		if (f->flags[i] != 0) {
			continue;
		} else {
			return i;
		}
	}
	return -1L;
}

static void delete_flag_ram_all_flags(struct bm_block_manager *bm) {
	memset(((struct bm_flag_ram*) bm)->flags, 0, ((struct bm_flag_ram*) bm)->block_count);
}
