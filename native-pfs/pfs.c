/*
 * pfs.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#define I_AM_PFS
#include "pfs-intern.h"
#include "pfs.h"

extern i64 first_block_table_block() {
	if (pfs == NULL) {
		return -2L;
	}
	struct pfs_b0 *b0 = pfs->get(pfs ,0L);
	if (b0 == NULL) {
		return -3;
	}
	i64 res = b0->block_table_first_block;
	pfs->unget(pfs, 0L);
	return res;
}

extern int pfs_format(i64 block_count) {
	if (pfs == NULL) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (pfs->block_size
			< (sizeof(struct pfs_b0) + sizeof(struct pfs_folder)
					+ (sizeof(struct pfs_folder_entry) * 2) + 30)) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		/*
		 * absolute minimum:
		 *    'super_block'
		 *  + folder
		 *  + two folder entry
		 *  + one single character name (with the '\0' character)
		 *  + table (three table_entries ('super_block', folder, name(, table.start)))
		 */
		return 0;
	}
	if (block_count < 2) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (pfs->loaded.entrycount > 0) {
		abort();
	}
	void *b0 = pfs->get(pfs, 0L);
	if (b0 == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		return 0;
	}
	i32 table_offset = pfs->block_size - 20;
	i32 *table = b0 + table_offset;
	table[0] = 0;						// table_entry0: super_block
	table[1] = sizeof(struct pfs_b0);
	table[2] = sizeof(struct pfs_b0);	// table_entry1: root_folder
	table[3] = sizeof(struct pfs_b0) + sizeof(struct pfs_folder);
	table[4] = table_offset;		// (table_entry2.start:) table_offset marker
	struct pfs_b0 *super_data = b0;
	super_data->MAGIC = PFS_MAGIC_START;
	super_data->root.block = 0L;
	super_data->root.pos = sizeof(struct pfs_b0);
	super_data->block_count = block_count;
	super_data->block_size = pfs->block_size;
	if (pfs->block_flag_bits > 0) {
		super_data->block_table_first_block = -1L;
		pfs->delete_all_flags(pfs);
		pfs->set_flags(pfs, 0L, BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES);
	} else {
		super_data->block_table_first_block = 1L;
		void *b1 = pfs->get(pfs, 1L);
		memset(b1, 0, pfs->block_size - 8);
		*(ui8*) b1 = 3;
		*(i64*) (b1 + pfs->block_size - 8) = -1L;
	}
	struct pfs_folder *root = b0 + sizeof(struct pfs_b0);
	const struct pfs_place no_parent = { .block = -1L, .pos = -1 };
	init_element(&root->element, no_parent, PFS_FLAGS_FOLDER, time(NULL));
	root->direct_child_count = 0L;
	root->helper_index = -1;
	pfs->set(pfs, 1L);
	pfs->set(pfs, 0L);
	return 1;
}

extern i64 pfs_block_count() {
	void *b0 = pfs->get(pfs, 0L);
	i64 block_count = ((struct pfs_b0*) b0)->block_count;
	pfs->unget(pfs, 0L);
	return block_count;
}

extern i32 pfs_block_size() {
	void *b0 = pfs->get(pfs, 0L);
	i32 block_size = ((struct pfs_b0*) b0)->block_size;
	pfs->unget(pfs, 0L);
	return block_size;
}

extern element pfs_root() {
	element res;
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	res.block = b0->root.block;
	res.pos = b0->root.pos;
	pfs->unget(pfs, 0L);
	return res;
}

static int del_helper(struct pfs_place h) {
	struct pfs_folder *helper = pfs->get(pfs, h.block) + h.pos;
	if (helper->direct_child_count > 0) {
		if ((helper->direct_child_count > 1) || (helper->helper_index == -1)) {
			pfs->unget(pfs, h.block);
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			return 0;
		}
		if (!del_helper(helper->entries[0].child_place)) {
			pfs->unget(pfs, h.block);
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
			return 0;
		}
		helper->helper_index = -1; // just to make sure
		helper->direct_child_count = 0;
	}
	reallocate_in_block_table(h.block, h.pos, 0, 0);
	pfs->set(pfs, h.block);
	return 1;
}

extern int pfs_element_delete(element *e) {
	struct pfs_element *element = pfs->get(pfs, e->block) + e->pos;
	struct pfs_folder *parent = pfs->get(pfs, element->parent.block)
			+ element->parent.pos;
	if ((parent->entries[element->index_in_parent_list].flags & PFS_FLAGS_FOLDER)
			!= 0) {
		struct pfs_folder *folder = (struct pfs_folder*) element;
		if (folder->direct_child_count > 0) {
			if ((folder->direct_child_count > 1) || (folder->helper_index == -1)
					|| (!del_helper(folder->entries[0].child_place))) {
				pfs->unget(pfs, e->block);
				pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
				return 0;
			}
			folder->direct_child_count = 0;
			folder->helper_index = -1;
		}
	} else if ((parent->entries[element->index_in_parent_list].flags
			& PFS_FLAGS_FILE) != 0) {
		pfs_file_truncate(e, 0L);
	} else {
		abort();
	}
	memmove(parent->entries + element->index_in_parent_list,
			parent->entries + element->index_in_parent_list + 1,
			(element->index_in_parent_list - parent->direct_child_count - 1)
					* sizeof(struct pfs_folder_entry));
	parent->direct_child_count--;
	if (parent->helper_index > element->index_in_parent_list) {
		parent->helper_index--;
	} else if (parent->helper_index == element->index_in_parent_list) {
		abort();
	}
	shrink_folder_entry(element->parent,
			sizeof(struct pfs_folder)
					+ (sizeof(struct pfs_folder_entry)
							* (1 + parent->direct_child_count)));
	pfs->set(pfs, element->parent.block);
	pfs->set(pfs, e->block);
	return 1;
}

void init_block(i64 block, i64 size) {
	if (size > pfs->block_size - 12) {
		abort();
	}
	void *data = pfs->get(pfs, block);
	i32 table_offset = pfs->block_size - 12;
	i32 *table = data + table_offset;
	table[0] = 0;
	table[1] = size;
	table[2] = table_offset;
	pfs->set(pfs, block);
}

static i32 allocate_in_block_table(i64 block, i64 size) {
	void *block_data = pfs->get(pfs, block);
	i32 *table_end = block_data + pfs->block_size - 4;
	i32 *table = block_data + *table_end;
	if (table_end[0] - 8 < table_end[-1]) {
		pfs->unget(pfs, block);
		return -1;
	}
	for (table++; table < table_end; table += 2) {
		i32 free_size = table[1] - table[0];
		if (free_size >= size) {
			i32 result = ((free_size - size) >> 1) + *table;
			i32 end = result + size;
			table--;
			for (i64 *table_entry = block_data + *table_end - 8;
					((void*) table_entry) < ((void*) table); table_entry++) {
				table_entry[0] = table_entry[1];
			}
			*table_end -= 8;
			table[0] = result;
			table[1] = end;
			pfs->set(pfs, block);
			return result;
		}
	}
	pfs->unget(pfs, block);
	return -1;
}

static inline i32 fill_entry_and_move_data(i32 free, i32 last_end,
		const i64 new_size, const int copy, const i32 pos, i32 old_size,
		i32 **table, void *block_data, const i64 *block) {
	i32 new_pos = (free >> 1) + last_end;
	*table[0] = new_pos;
	*table[1] = new_pos + new_size;
	if (*table[1] > *table[2]) {
		abort();
	}
	if (*table[-1] > *table[0]) {
		abort();
	}
	if (copy) {
		memcpy(block_data + new_pos, block_data + pos, old_size);
	}
	pfs->set(pfs, *block);
	return new_pos;
}

i32 reallocate_in_block_table(const i64 block, const i32 pos,
		const i64 new_size, const int copy) {
	void *block_data = pfs->get(pfs, block);
	if (block_data == NULL) {
		abort();
	}
	i32 *table_end = block_data + pfs->block_size - 4;
	i32 *table = block_data + *table_end;
	for (; table < table_end; table += 2) {
		if (pos > *table) {
			continue;
		} else if (pos < *table) {
			abort();
		}
		i32 old_size = table[1] - pos;
		if (old_size == new_size) {
			pfs->unget(pfs, block);
			return pos;
		} else if (old_size < new_size) {
			if (new_size > 0) {
				table[1] = pos + new_size;
			} else if (new_size == 0) {
				for (i64 *cpy = block_data + *table_end;
						((void*) cpy) < ((void*) table); cpy++) {
					cpy[1] = cpy[0];
				}
			} else {
				abort();
			}
			pfs->set(pfs, block);
			return pos;
		}
		i32 max_no_move_grow = table[2] - pos;
		if (max_no_move_grow >= new_size) {
			table[1] = pos + new_size;
			pfs->set(pfs, block);
			return pos;
		}
		i32 max_in_place_mov = table[2] - table[-1];
		if (max_in_place_mov >= new_size) {
			i32 new_pos = (max_in_place_mov >> 1) + table[-1];
			i32 old_size = pos - table[2];
			table[0] = new_pos;
			table[1] = new_pos + new_size;
			if (table[1] > table[2]) {
				abort();
			}
			if (table[-1] > table[0]) {
				abort();
			}
			if (copy) {
				memmove(block_data + new_pos, block_data + pos, old_size);
			}
			pfs->set(pfs, block);
			return new_pos;
		}
		i32 *old_entry = table;
		table = block_data + *table_end;
		i32 last_end = 0;
		for (; table < old_entry; table += 2) {
			i32 free = (*table) - last_end;
			if (free >= new_size) {
				for (i64 *table_entry = (void*) table;
						((void*) table_entry) < ((void*) old_entry);
						table_entry++) {
					table_entry[1] = table_entry[0];
				}
				i32 new_pos = fill_entry_and_move_data(free, last_end, new_size,
						copy, pos, old_size, &table, block_data, &block);
				return new_pos;
			}
		}
		table += 4; // skip this entry + skip next entry (already checked at the beginning if this entry can grow in place)
		for (; table < table_end; table += 2) {
			i32 free = table[0] - table[-1];
			if (free >= new_size) {
				for (i64 *table_entry = (void*) old_entry;
						((void*) table_entry) < ((void*) table);
						table_entry++) {
					table_entry[0] = table_entry[1];
				}
				i32 new_pos = fill_entry_and_move_data(free, last_end, new_size,
						copy, pos, old_size, &table, block_data, &block);
				return new_pos;
			}
		} // could not resize
		pfs->unget(pfs, block);
		return -1;
	}
	// pos not in table
	abort();
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
	init_block(block, size);
	write->block = block;
	write->pos = 0;
	return 1;
}

static void change_place_in_parent_and_struct(i64 new_block, i32 new_pos,
		struct pfs_place *place) {
	const i32 old_block = place->block;
	const i32 old_pos = place->pos;
	place->block = new_block;
	place->pos = new_pos;
	struct pfs_folder *f = pfs->get(pfs, new_block) + new_pos;
	struct pfs_folder *parent = pfs->get(pfs, f->element.parent.block)
			+ f->element.parent.pos;
	for (int i = 0; i < parent->direct_child_count; i++) {
		if (parent->entries[i].child_place.pos == old_pos) {
			if (parent->entries[i].child_place.block == old_block) {
				parent->entries[i].child_place.block = place->block;
				parent->entries[i].child_place.pos = place->pos;
				return;
			}
		}
	}
	abort();
}

int grow_folder_entry(struct pfs_place *place, i32 new_size) {
	i32 new_pos = reallocate_in_block_table(place->block, place->pos, new_size,
			1);
	if (new_pos != -1) {
		if (place->pos != new_pos) {
			change_place_in_parent_and_struct(place->block, new_pos, place);
		}
		return 1;
	}
//	if (!allow_block_change) {
	return 0;
//	}
//	i64 new_block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_ENTRIES);
//	if (new_block == -1) {
//		return 0;
//	}
//	void *new_block_data = pfs->get(pfs, new_block);
//	init_block(new_block, new_size);
//	void *old_block_data = pfs->get(pfs, place->block);
//	memcpy(new_block_data, old_block_data + place->pos,
//			new_size - sizeof(struct pfs_folder_entry));
//	struct pfs_folder *new_entry = new_block_data;
//	if ((new_entry->element.flags & PFS_FLAGS_FOLDER) == 0) {
//		free_block(new_block);
//		pfs->unget(pfs, new_block);
//		pfs->unget(pfs, place->block);
//		abort();
//	}
//	for (int i = 0; i < new_entry->direct_child_count; i++) {
//		char *name_addr = old_block_data + new_entry->entries[i].name_pos;
//		i64 name_length = strlen(name_addr);
//		i32 new_child_name_pos = allocate_in_block_table(new_block,
//				name_length);
//		if (new_child_name_pos == -1) {
//			free_block(new_block);
//			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
//			return 0;
//		}
//		memcpy(new_block_data + new_child_name_pos, name_addr, name_length + 1); // also copy the '\0'
//		new_entry->entries[i].name_pos = new_child_name_pos;
//	}
//	change_place_in_parent_and_struct(new_block, new_pos, place);
//	return 1;
}

i32 add_name(i64 block_num, char *name) {
	i64 strsize = strlen(name) + 1;
	void *block = pfs->get(pfs, block_num);
	i32 pos = allocate_in_block_table(block_num, strsize);
	if (pos != -1) {
		memcmp(block + pos, name, strsize);
		pfs->set(pfs, block_num);
		return pos;
	}
	pfs->unget(pfs, block_num);
	return -1;
}

i64 get_block_table_first_block() {
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	i64 btfb = b0->block_table_first_block;
	pfs->unget(pfs, 0L);
	return btfb;
}

static inline i64 allocate_block_without_bm(i64 btfb) {
	const i64 block_count = pfs_block_count();
	i64 current_block_num = btfb;
	void *current_block = pfs->get(pfs, current_block_num);
	for (i64 result = 0L; 1;) {
		i64 next_block_num = *(i64*) (current_block + pfs->block_size - 8);
		ui8 *block_data = current_block;
		i32 remain = pfs->block_size - 8;
		for (; remain > 0; block_data++, remain--) {
			for (unsigned int bit = 0; bit < 8; bit++) {
				if (((*block_data) & (1U << bit)) == 0) {
					*block_data |= 1U << bit;
					pfs->set(pfs, current_block_num);
					if (result == 0L) {
						abort();
					}
					return result;
				}
				result++;
				if (result >= block_count) {
					pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
					return -1L;
				}
			}
		}
		if (next_block_num == -1L) {
			next_block_num = result;
			*(i64*) (current_block + pfs->block_size - 8) = next_block_num;
			pfs->set(pfs, current_block_num);
			current_block = pfs->get(pfs, current_block_num);
			current_block_num = next_block_num;
			memset(current_block, 0, pfs->block_size - 8);
			*(unsigned int*) current_block |= 1;
			*(i64*) (current_block + pfs->block_size - 8) = -1L;
		} else {
			pfs->unget(pfs, current_block_num);
			current_block = pfs->get(pfs, current_block_num);
			current_block_num = next_block_num;
		}
	}
}

i64 allocate_block(ui64 block_flags) {
	i64 btfb = get_block_table_first_block();
	if (btfb == -1L) {
		i64 fzfb = pfs->first_zero_flagged_block(pfs);
		if (fzfb == -1L) {
			if (pfs->block_flag_bits <= 0) {
				abort();
			}
			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
			return -1L;
		}
		const i64 block_count = pfs_block_count();
		if (fzfb >= block_count) {
			pfs_errno = PFS_ERRNO_OUT_OF_SPACE;
			return -1L;
		}
		pfs->set_flags(pfs, fzfb, block_flags);
		return fzfb;
	} else {
		return allocate_block_without_bm(btfb);
	}
}

void free_block(i64 free_this_block) {
	i64 btfb = get_block_table_first_block();
	if (btfb == -1L) {
		pfs->set_flags(pfs, free_this_block, 0L);
	} else {
		i64 remain = free_this_block >> 3;
		struct pfs_place place = find_place(btfb, remain);
		ui8 *data = pfs->get(pfs, place.block) + place.pos;
		*data &= ~(1U << (free_this_block & 7));
		pfs->set(pfs, place.block);
	}
}

void ensure_block_is_file_data(i64 block) {
	i64 btfb = get_block_table_first_block();
	if (btfb != -1L)
		return;
	if (pfs->block_flag_bits > BLOCK_FLAG_USED_BIT) {
		ui64 block_flags = pfs->get_flags(pfs, block);
		if ((block_flags & BLOCK_FLAG_FILE_DATA) == 0) {
			abort();
		}
		if ((block_flags & BLOCK_FLAG_ENTRIES) != 0) {
			abort();
		}
	}
}

void ensure_block_is_entry(i64 block) {
	i64 btfb = get_block_table_first_block();
	if (btfb != -1L)
		return;
	if (pfs->block_flag_bits > BLOCK_FLAG_USED_BIT) {
		ui64 block_flags = pfs->get_flags(pfs, block);
		if ((block_flags & BLOCK_FLAG_FILE_DATA) != 0) {
			abort();
		}
		if ((block_flags & BLOCK_FLAG_ENTRIES) == 0) {
			if (pfs->block_flag_bits >= BLOCK_FLAG_ENTRIES) {
				abort();
			}
		}
	}
}

struct pfs_place find_place(i64 first_block, i64 remain) {
	for (i64 current_block = first_block; 1; remain -= pfs->block_size) {
		if (remain < (pfs->block_size - 8)) {
			struct pfs_place result;
			result.block = current_block;
			result.pos = (i32) remain;
			return result;
		}
		void *cb = pfs->get(pfs, current_block);
		i64 next_block = *(i64*) (cb + pfs->block_size - 8);
		pfs->unget(pfs, current_block);
		current_block = next_block;
	}
}
