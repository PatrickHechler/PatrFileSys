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
 * pfs.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */
#define I_AM_CORE_PFS
#include "pfs.h"
#include "../include/pfs-constants.h"
#include "../include/patr-file-sys.h"

static char pfs_error_buf[128];

extern const char* pfs_error0(int pfs_err_val) {
	switch (pfs_err_val) {
	case PFS_ERRNO_NONE:
		return "no error";
	case PFS_ERRNO_UNKNOWN_ERROR:
		return "unknown error";
	case PFS_ERRNO_NO_MORE_ELEMENTS:
		return "there are no more params";
	case PFS_ERRNO_ELEMENT_WRONG_TYPE:
		return "the element has not the wanted/allowed type";
	case PFS_ERRNO_ELEMENT_NOT_EXIST:
		return "the element does not exist";
	case PFS_ERRNO_ELEMENT_ALREADY_EXIST:
		return "the element already exists";
	case PFS_ERRNO_OUT_OF_SPACE:
		return "there is not enough space on the device";
	case PFS_ERRNO_IO_ERR:
		return "IO error";
	case PFS_ERRNO_ILLEGAL_ARG:
		return "illegal argument";
	case PFS_ERRNO_ILLEGAL_STATE:
		return "some state is invalid";
	case PFS_ERRNO_OUT_OF_MEMORY:
		return "the system is out of memory";
	case PFS_ERRNO_ROOT_FOLDER:
		return "the root folder does not support this operation";
	case PFS_ERRNO_PARENT_IS_CHILD:
		return "the parent can't be made to it's own child";
	case PFS_ERRNO_ELEMENT_USED:
		return "the element is still used somewhere else";
	case PFS_ERRNO_OUT_OF_RANGE:
		return "some value was outside of the allowed range";
	case PFS_ERRNO_FOLDER_NOT_EMPTY:
		return "the operation failed, because only empty folders can be deleted";
	case PFS_ERRNO_ELEMENT_DELETED:
		return "the operation failed, because the element was deleted";
	default:
		sprintf(pfs_error_buf, "unknown value [%lx]", pfs_err_val);
		return pfs_error_buf;
	}
}

extern const char* pfs_error() {
	return pfs_error0(pfs_err);
}

extern void pfs_perror(const char *msg) {
	fprintf(stderr, "%s: %s\n", msg ? msg : "pfs-error", pfs_error());
}

int pfsc_format(i64 block_count, uuid_t uuid, char *name) {
	if (pfs == NULL) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (pfs->block_size < PFS_MIN_BLOCK_SIZE) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		/*
		 * absolute minimum:
		 *    'super_block'
		 *  + folder
		 *  + two folder entry
		 *  + one two character name
		 *  + table (three table_entries ('super_block', folder, name(, table.start)))
		 */
		return 0;
	}
	if ((pfs->block_size & 7) != 0) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (block_count < 2) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (pfs->loaded.entrycount > 0) {
		abort();
	}
	void *b0 = pfs->get(pfs, 0L);
	if (b0 == NULL) {
		pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
	i64 name_len_ = name ? strlen(name) : 0;
	i32 name_len = name_len_;
	if (name_len != name_len_) {
		pfs_err = PFS_ERRNO_OUT_OF_SPACE;
		pfs->unget(pfs, 0L);
		return 0;
	}
	i32 table_offset = pfs->block_size - 20;
	i32 *table = b0 + table_offset;
	table[0] = 0; // table_entry0: super_block
	table[1] = sizeof(struct pfs_b0) + name_len;
	table[2] = sizeof(struct pfs_b0) + name_len; // table_entry1: root_folder
	table[3] = sizeof(struct pfs_b0) + name_len + sizeof(struct pfs_folder);
	table[4] = table_offset; // (table_entry2.start:) table_offset marker
	if (table[4] - (i64) table[3]
			< sizeof(struct pfs_folder_entry) + sizeof(struct pfs_folder_entry)
					+ 3) {
		pfs_err = PFS_ERRNO_OUT_OF_SPACE;
		pfs->unget(pfs, 0L);
		return 0;
	}
	struct pfs_b0 *super_data = b0;
	super_data->MAGIC0 = PFS_MAGIC_START0;
	super_data->MAGIC1 = PFS_MAGIC_START1;
	super_data->root.block = 0L;
	super_data->root.pos = sizeof(struct pfs_b0);
	super_data->block_size = pfs->block_size;
	super_data->block_count = block_count;
	if (pfs->block_flag_bits > 0) {
		super_data->flags = B0_FLAG_BM_ALLOC;
		if (!pfs->delete_all_flags(pfs)) {
			pfs->unget(pfs, 0);
			return 0;
		}
		if (!pfs->set_flags(pfs, 0L, BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES)) {
			pfs->unget(pfs, 0);
			return 0;
		}
	} else {
		super_data->flags = 0;
		void *b1 = pfs->get(pfs, 1L);
		memset(b1, 0, pfs->block_size - 8);
		*(ui8*) b1 = 3;
		*(i64*) (b1 + pfs->block_size - 8) = -1L;
		if (!pfs->set(pfs, 1L)) {
			pfs->unget(pfs, 0L);
			return 0;
		}
	}
	if (uuid) {
		memcpy(super_data->uuid, uuid, 16);
	} else {
#if defined PFS_HALF_PORTABLE_BUILD || defined PFS_PORTABLE_BUILD
		// see java UUID.generateRandom()
		for (int i = 0; i < 16; i++) {
			super_data->uuid[i] = rand();
		}
        super_data->uuid[6]  &= 0x0f;  /* clear version        */
        super_data->uuid[6]  |= 0x40;  /* set to version 4     */
        super_data->uuid[8]  &= 0x3f;  /* clear variant        */
        super_data->uuid[8]  |= 0x80;  /* set to IETF variant  */
#else
		uuid_generate(super_data->uuid);
#endif
	}
	if (name_len) {
		memcpy(super_data->name, name, name_len);
	}
	struct pfs_folder *root = b0 + sizeof(struct pfs_b0) + name_len;
	const struct pfs_place no_parent = { .block = -1L, .pos = -1 };
	i64 now = time(NULL);
	root->element.last_mod_time = now;
	root->real_parent = no_parent;
	root->direct_child_count = 0L;
	root->folder_entry = no_parent;
	root->helper_index = -1;
	return pfs->set(pfs, 0L);
}

extern i64 pfs_block_count() {
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	if (b0 == NULL) {
		return -1L;
	}
	i64 block_count = b0->block_count;
	if (!pfs->unget(pfs, 0L)) {
		return -1L;
	}
	return block_count;
}

static inline i64 pfs_free_and_used_block_count_impl(_Bool free) {
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	if (b0 == NULL) {
		return -1L;
	}
	i64 total_block_count = b0->block_count;
	if (!pfs->unget(pfs, 0L)) {
		return -1L;
	}
	if (b0->flags & B0_FLAG_BM_ALLOC) {
		i64 fzfb = pfs->first_zero_flagged_block(pfs);
		if (fzfb == -1) {
			return 0;
		} else {
			return 1;
		}
	} else {
		i64 free_blocks = 0;
		i64 current_block = 1;
		while (1) {
			void *block_data = pfs->get(pfs, current_block);
			if (block_data == NULL) {
				return -1L;
			}
			i64 *data = block_data;
			for (i64 i = pfs->block_size - 16; i; i--) {
				if (data[i] == -1) {
					continue;
				}
				ui8 *p = (void*) &data[i];
				for (int ii = 7; ii; ii--) {
					if (p[ii] == (ui8) -1) {
						continue;
					}
					for (int iii = 7; iii; iii--) {
						if (free) {
							if ((p[ii] & (1U << iii)) == 0) {
								free_blocks++;
							}
						} else {
							if ((p[ii] & (1U << iii)) != 0) {
								free_blocks++;
							}
						}
					}
				}
			}
			i64 next = *(i64*) (block_data + pfs->block_size - 8);
			if (!pfs->unget(pfs, current_block)) {
				return -1;
			}
			if (next == -1) {
				return free_blocks;
			}
			current_block = next;
		}
	}
}

extern i64 pfs_free_block_count() {
	return pfs_free_and_used_block_count_impl(1);
}

extern i64 pfs_used_block_count() {
	return pfs_free_and_used_block_count_impl(0);
}

extern i32 pfs_block_size() {
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	if (b0 == NULL) {
		return -1L;
	}
	i32 block_size = b0->block_size;
	if (!pfs->unget(pfs, 0L)) {
		return -1;
	}
	return block_size;
}

extern i32 pfs_name_len() {
	void *super = pfs->get(pfs, 0L);
	i32 len = (*(i32*) (super + 4 + (*(i32*) (super + pfs->block_size))))
			- offsetof(struct pfs_b0, name);
	pfs->unget(pfs, 0L);
	return len;
}

extern i32 pfs_name_cpy(char *buf, i32 buf_len) {
	void *super = pfs->get(pfs, 0L);
	i32 len = (*(i32*) (super + 4 + (*(i32*) (super + pfs->block_size))))
			- offsetof(struct pfs_b0, name);
	if (buf_len > len) {
		memcpy(buf, super + offsetof(struct pfs_b0, name), len);
		buf[len] = '\0';
	} else {
		memcpy(buf, super + offsetof(struct pfs_b0, name), buf_len);
	}
	pfs->unget(pfs, 0L);
	return len;
}

extern char* pfs_name() {
	void *super = pfs->get(pfs, 0L);
	i32 len = (*(i32*) (super + 4 + (*(i32*) (super + pfs->block_size))))
			- offsetof(struct pfs_b0, name);
	char *name = malloc(len + 1);
	memcpy(name, super + offsetof(struct pfs_b0, name), len);
	name[len] = '\0';
	pfs->unget(pfs, 0L);
	return name;
}

int pfsc_fill_root(pfs_eh overwrite_me) {
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	if (b0 == NULL) {
		return 0;
	}
	overwrite_me->real_parent_place.block = -1L;
	overwrite_me->real_parent_place.pos = -1;
	overwrite_me->index_in_direct_parent_list = -1;
	overwrite_me->direct_parent_place.block = -1L;
	overwrite_me->direct_parent_place.pos = -1;
	overwrite_me->entry_pos = -1;
	overwrite_me->element_place.block = b0->root.block;
	overwrite_me->element_place.pos = b0->root.pos;
	if (!pfs->unget(pfs, 0L)) {
		return -1;
	}
	return 1;
}

pfs_eh pfsc_root() {
	struct pfs_element_handle *res = malloc(sizeof(struct pfs_element_handle));
	if (res == NULL) {
		pfs_err = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	if (!pfsc_fill_root(res)) {
		free(res);
		return NULL;
	}
	return res;
}

int init_block(i64 block, i64 size) {
	if (size > pfs->block_size - 12) {
		pfs_err = PFS_ERRNO_OUT_OF_SPACE;
		return 0;
	}
	void *data = pfs->lazy_get(pfs, block);
	if (data == NULL) {
		return 0;
	}
	i32 table_offset = pfs->block_size - 12;
	i32 *table = data + table_offset;
	table[0] = 0;
	table[1] = size;
	table[2] = table_offset;
	return pfs->set(pfs, block);
}

union pntr_val {
	i32 *pntr;
	ui64 val;
};

i32 get_size_from_block_table(void *block_data, const i32 pos) {
	union pntr_val max, min, mid;
	max.pntr = block_data + pfs->block_size - 4;
	min.pntr = block_data + *max.pntr;
	max.val -= 4;
	/*	if ((max.val & ~3UL) != max.val) {
	 abort();
	 }
	 if ((max.val & ~7UL) == max.val) {
	 abort();
	 } */
	while (1) {
		if (min.val > max.val) {
			abort();
		}
		mid.val = (min.val + ((max.val - min.val) >> 1)) & ~3UL;
		if (pos > *mid.pntr) {
			min.val = mid.val + 4;
		} else if (pos < *mid.pntr) {
			max.val = mid.val - 4;
		} else {
			mid.val = mid.val | 4; // if end of entry before got the hit
			if (pos != *mid.pntr) { // (every entry is i32-aligned and never i64-aligned)
				abort();
			}
			i32 res = mid.pntr[1] - mid.pntr[0];
			return res;
		}
	}
	// old linear search
//	for (i32 *table = table_start; 1; table += 2) {
//		if (table >= table_end) {
//			abort();
//		}
//		if (pos > *table) {
//			continue;
//		}
//		if (pos < *table) {
//			abort();
//		}
//		i32 size = table[1] - table[0];
//		pfs->unget(pfs, block);
//		return size;
//	}
}

i32 allocate_in_block_table(i64 block, i64 size) {
	void *block_data = pfs->get(pfs, block);
	if (block_data == NULL) {
		return -1;
	}
	i32 *table_end = block_data + pfs->block_size - 4;
	i32 *table = block_data + *table_end;
	if (table_end[0] - 8 < table_end[-1]) {
		pfs_err = PFS_ERRNO_OUT_OF_SPACE;
		pfs->unget(pfs, block);
		return -1;
	}
	for (table++; table < table_end; table += 2) {
		i32 free_size = table[1] - table[0];
		if (free_size >= size) {
			i32 result = ((free_size - size) >> 1) + *table;
			result = result & ~7L;
			if (result < *table) {
				result = ((free_size - size) >> 1) + *table;
				if (result < *table) {
					abort();
				}
			}
			i32 end = result + size;
			table--;
			for (i64 *table_entry = block_data + *table_end - 8;
					((void*) table_entry) < ((void*) table); table_entry++) {
				table_entry[0] = table_entry[1];
			}
			*table_end -= 8;
			table[0] = result;
			table[1] = end;
			if (!pfs->set(pfs, block)) {
				return -1;
			}
			return result;
		}
	}
	pfs->unget(pfs, block);
	return -1;
}

static inline i32 fill_entry_and_move_data(i32 free, i32 last_end,
		const i64 new_size, const int copy, const i32 pos, i32 old_size,
		i32 *table, void *block_data, const i64 block) {
	i32 new_pos = (free >> 1) + last_end;
	new_pos = new_pos & ~7L;
	if (last_end > new_pos) {
		new_pos = (free >> 1) + last_end;
	}
	table[0] = new_pos;
	table[1] = new_pos + new_size;
	if (table[1] > table[2]) {
		abort();
	}
	if (table[-1] > table[0]) {
		abort();
	}
	if (copy) {
		memcpy(block_data + new_pos, block_data + pos, old_size);
	}
	return new_pos;
}

i32 reallocate_in_block_table(const i64 block, const i32 pos,
		const i64 new_size, const int copy) {
	void *block_data = pfs->get(pfs, block);
	if (block_data == NULL) {
		return -1;
	}
	i32 *table_end = block_data + pfs->block_size - 4;
	i32 *table = block_data + *table_end;
	for (; 1; table += 2) {
		if (table >= table_end) {
			abort();
		}
		if (pos > *table) {
			continue;
		} else if (pos < *table) {
			abort();
		}
		i32 old_size = table[1] - pos;
		if (old_size == new_size) {
			if (!pfs->unget(pfs, block)) {
				return -1;
			}
			return pos;
		} else if (old_size > new_size) {
			if (new_size > 0) {
				table[1] = pos + new_size;
			} else if (new_size == 0) {
				for (i64 *cpy = ((void*) table) - 8;
						((void*) cpy) >= (block_data + *table_end); cpy--) {
					cpy[1] = cpy[0];
				}
				*table_end += 8;
			} else {
				abort();
			}
			if (!pfs->set(pfs, block)) {
				return -1;
			}
			return pos;
		}
		i32 max_no_move_grow = table[2] - pos;
		if (max_no_move_grow >= new_size) {
			table[1] = pos + new_size;
			if (!pfs->set(pfs, block)) {
				return -1;
			}
			return pos;
		}
		i32 max_in_place_mov = table[2]
				- ((table == block_data + *table_end) ? 0 : table[-1]);
		if (max_in_place_mov >= new_size) {
			i32 new_pos = fill_entry_and_move_data(max_in_place_mov - new_size,
					table[-1], new_size, copy, pos, old_size, table, block_data,
					block);
			if (!pfs->set(pfs, block)) {
				return -1;
			}
			return new_pos;
		}
		i32 *my_old_entry = table;
		table = block_data + *table_end;
		i32 last_end = 0;
		for (; table < my_old_entry; table += 2) {
			i32 free = (*table) - last_end;
			if (free >= new_size) {
				for (i64 *table_entry = (void*) table;
						((void*) table_entry) < ((void*) my_old_entry);
						table_entry++) {
					table_entry[1] = table_entry[0];
				}
				i32 new_pos = fill_entry_and_move_data(free, last_end, new_size,
						copy, pos, old_size, table, block_data, block);
				if (!pfs->set(pfs, block)) {
					return -1;
				}
				return new_pos;
			}
			last_end = table[1];
		}
		table += 4; // skip this entry + skip next entry (already checked at the beginning if this entry can grow in place)
		for (; table <= table_end; table += 2) {
			i32 free = table[0] - table[-1];
			if (free >= new_size) {
				for (i64 *table_entry = ((void*) my_old_entry) + 8;
						((void*) table_entry) < ((void*) table);
						table_entry++) {
					table_entry[-1] = table_entry[0];
				}
				i32 new_pos = fill_entry_and_move_data(free - new_size,
						table[-3], new_size, copy, pos, old_size, table - 2,
						block_data, block);
				if (!pfs->set(pfs, block)) {
					return -1;
				}
				return new_pos;
			}
		} // could not resize
		pfs_err = PFS_ERRNO_OUT_OF_SPACE;
		pfs->unget(pfs, block);
		return -1;
	}
}
int allocate_new_entry(struct pfs_place *write, i64 base_block, i32 size) {
	if (base_block != -1L) {
		i32 pos = allocate_in_block_table(base_block, size);
		if (pos != -1) {
			write->block = base_block;
			write->pos = pos;
			return 1;
		}
	}
	i64 block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES);
	if (block == -1L) {
		return 0;
	}
	if (!init_block(block, size)) {
		return 0;
	}
	write->block = block;
	write->pos = 0;
	return 1;
}

void set_parent_place_from_childs(struct pfs_place e,
		struct pfs_place real_parent) {
	void *block_data = pfs->get(pfs, e.block);
	if (block_data == NULL) {
		abort();
	}
	struct pfs_folder *f = block_data + e.pos;
	for (i32 i = 0; i < f->direct_child_count; i++) {
		if ((f->entries[i].flags & PFS_F_FOLDER) != 0) {
			void *child_block_data = pfs->get(pfs,
					f->entries[i].child_place.block);
			if (child_block_data == NULL) {
				abort();
			}
			struct pfs_folder *child = child_block_data
					+ f->entries[i].child_place.pos;
			child->real_parent = real_parent;
			child->folder_entry.block = e.block;
			child->folder_entry.pos = e.pos + sizeof(struct pfs_folder)
					+ i * sizeof(struct pfs_folder_entry);
			if (child->folder_entry.pos
					!= ((i64) &f->entries[i]) - (i64) block_data) {
				abort();
			}
			if ((f->entries[i].flags & PFS_F_HELPER_FOLDER) != 0) {
				if (i != f->helper_index) {
					abort();
				}
				set_parent_place_from_childs(f->entries[i].child_place,
						real_parent);
			} else if (i == f->helper_index) {
				abort();
			}
			pfs->set(pfs, f->entries[i].child_place.block);
		}
	}
	pfs->set(pfs, e.block);
}

i32 grow_folder_entry(const struct pfs_element_handle *e, i32 new_size,
		struct pfs_place real_parent) {
	i32 new_pos = reallocate_in_block_table(e->element_place.block,
			e->element_place.pos, new_size, 1);
	if (new_pos == -1) {
		return -1;
	}
	if (e->element_place.pos != new_pos) {
		if (e->direct_parent_place.block == -1) {
			struct pfs_b0 *b0 = pfs->get(pfs, 0L);
			if (b0 == NULL) {
				abort();
			}
			b0->root.pos = new_pos;
			pfs->set(pfs, 0L);
		} else {
			void *block_data = pfs->get(pfs, e->direct_parent_place.block);
			if (block_data == NULL) {
				abort();
			}
			struct pfs_folder_entry *entry = block_data + e->entry_pos;
			entry->child_place.pos = new_pos;
			pfs->set(pfs, e->direct_parent_place.block);
		}
		struct pfs_place place = { .block = e->element_place.block, .pos =
				new_pos };
		if (real_parent.block == place.block) {
			real_parent.pos = new_pos;
		}
		set_parent_place_from_childs(place, real_parent);
	}
	return new_pos;
}

i32 add_name(i64 block_num, const char *name, i64 str_len) {
	void *block = pfs->get(pfs, block_num);
	if (block == NULL) {
		return -1;
	}
	i32 pos = allocate_in_block_table(block_num, str_len);
	if (pos == -1) {
		pfs->unget(pfs, block_num);
		return -1;
	} else {
		memcpy(block + pos, name, str_len);
		if (!pfs->set(pfs, block_num)) {
			return -1;
		}
		return pos;
	}
}

static inline i64 allocate_block_without_bm(struct pfs_b0 *b0) {
	const i64 block_count = b0->block_count;
	i64 current_block_num = 1;
	void *current_block = pfs->get(pfs, current_block_num);
	i64 result = 0L;
	while (1) {
		i64 next_block_num = *(i64*) (current_block + pfs->block_size - 8);
		ui8 *block_data = current_block;
		i32 remain = pfs->block_size - 8;
		for (; remain >= 8; block_data += 8, remain -= 8) {
			if ((*(ui64*) block_data) != 0xFFFFFFFFFFFFFFFFL) {
				break;
			}
			result += 64;
			if (result >= block_count) {
				pfs_err = PFS_ERRNO_OUT_OF_SPACE;
				pfs->unget(pfs, current_block_num);
				pfs->unget(pfs, 0L);
				return -1L;
			}
		}
		for (; remain > 0; block_data++, remain--) {
			if ((*block_data) == 0xFF) {
				result += 8;
				if (result >= block_count) {
					pfs_err = PFS_ERRNO_OUT_OF_SPACE;
					pfs->unget(pfs, current_block_num);
					pfs->unget(pfs, 0L);
					return -1L;
				}
				continue;
			}
			for (unsigned int bit = 0; bit < 8; bit++) {
				if (((*block_data) & (1U << bit)) == 0) {
					*block_data |= 1U << bit;
					if (!pfs->set(pfs, current_block_num)) {
						pfs->unget(pfs, 0L);
						return -1L;
					}
					if (!pfs->unget(pfs, 0L)) {
						return -1L;
					}
					if (result == 0L) {
						abort();
					}
					return result;
				}
				result++;
				if (result >= block_count) {
					pfs_err = PFS_ERRNO_OUT_OF_SPACE;
					pfs->unget(pfs, current_block_num);
					pfs->unget(pfs, 0L);
					return -1L;
				}
			}
			abort();
		}
		if (next_block_num == -1L) {
			if (result + 1 >= block_count) {
				pfs_err = PFS_ERRNO_OUT_OF_SPACE;
				pfs->unget(pfs, current_block_num);
				return -1L;
			}
			*(i64*) (current_block + pfs->block_size - 8) = result;
			if (!pfs->set(pfs, current_block_num)) {
				pfs->unget(pfs, 0L);
				return -1L;
			}
			current_block = pfs->lazy_get(pfs, result);
			memset(current_block, 0, pfs->block_size - 8);
			*(unsigned char*) current_block = 3;
			*(i64*) (current_block + pfs->block_size - 8) = -1L;
			if (!pfs->set(pfs, result)) {
				pfs->unget(pfs, 0L);
				return -1L;
			}
			if (!pfs->unget(pfs, 0L)) {
				return -1L;
			}
			return result + 1;
		} else {
			if (!pfs->unget(pfs, current_block_num)) {
				return -1L;
			}
			current_block_num = next_block_num;
			current_block = pfs->get(pfs, current_block_num);
			if (!current_block) {
				return -1L;
			}
		}
	}
}

i64 allocate_block(ui64 block_flags) {
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	if (b0->flags & B0_FLAG_BM_ALLOC) {
		i64 fzfb = pfs->first_zero_flagged_block(pfs);
		if (fzfb == -1L) {
			if (pfs->block_flag_bits <= 0) {
				abort();
			}
			pfs_err = PFS_ERRNO_OUT_OF_SPACE;
			return -1L;
		}
		if (fzfb >= b0->block_count) {
			pfs_err = PFS_ERRNO_OUT_OF_SPACE;
			return -1L;
		}
		pfs->unget(pfs, 0L);
		pfs->set_flags(pfs, fzfb, block_flags);
		return fzfb;
	} else {
		return allocate_block_without_bm(b0);
	}
}

void free_block(i64 free_this_block) {
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	ui32 flags = b0->flags;
	pfs->unget(pfs, 0L);
	if (flags & B0_FLAG_BM_ALLOC) {
		if (!pfs->set_flags(pfs, free_this_block, 0L)) {
			abort();
		}
	} else {
		i64 remain = free_this_block >> 3;
		struct pfs_place place = find_place(1, remain);
		ui8 *data = pfs->get(pfs, place.block) + place.pos;
		*data &= ~(1U << (free_this_block & 7));
		pfs->set(pfs, place.block);
	}
}

static inline void ensure_used_block_has_flag(i64 block, ui64 flag) {
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	ui32 flags = b0->flags;
	pfs->unget(pfs, 0L);
	if (flags & B0_FLAG_BM_ALLOC) {
		ui64 block_flags = pfs->get_flags(pfs, block);
		if ((block_flags & BLOCK_FLAG_USED) == 0) {
			abort();
		}
		if ((block_flags & flag) != flag) {
			if ((1L << (pfs->block_flag_bits - 1)) >= flag) {
				abort();
			}
		}
		if (block_flags & ~(BLOCK_FLAG_USED | flag)) {
			abort();
		}
	}
}

void ensure_block_is_file_data(i64 block) {
	ensure_used_block_has_flag(block, BLOCK_FLAG_DATA);
}

void ensure_block_is_entry(i64 block) {
	ensure_used_block_has_flag(block, BLOCK_FLAG_ENTRIES);
}

struct pfs_place find_place(const i64 first_block, i64 remain) {
	for (i64 current_block = first_block; 1; remain -= pfs->block_size - 8) {
		if (remain < (pfs->block_size - 8)) {
			struct pfs_place result = { //
					/*	  */.block = current_block, //
							.pos = (i32) remain //
					};
			return result;
		} else if (remain == (pfs->block_size - 8)) {
			void *cb = pfs->get(pfs, current_block);
			if (!cb) {
				struct pfs_place res = { .block = -1, .pos = -1 };
				return res;
			}
			i64 next_block = *(i64*) (cb + pfs->block_size - 8);
			if (!pfs->unget(pfs, current_block)) {
				struct pfs_place res = { .block = -1, .pos = -1 };
				return res;
			}
			struct pfs_place result;
			if (next_block == -1) {
				result.block = current_block;
				result.pos = pfs->block_size - 8;
			} else {
				result.block = next_block;
				result.pos = 0;
			}
			return result;
		}
		void *cb = pfs->get(pfs, current_block);
		if (!cb) {
			struct pfs_place res = { .block = -1, .pos = -1 };
			return res;
		}
		i64 next_block = *(i64*) (cb + pfs->block_size - 8);
		if (!pfs->unget(pfs, current_block)) {
			struct pfs_place res = { .block = -1, .pos = -1 };
			return res;
		}
		current_block = next_block;
	}
}
