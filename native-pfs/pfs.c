/*
 * pfs.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#define I_AM_PFS
#include "pfs-intern.h"
#include "pfs.h"

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
		 *  + one two character name
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
	table[0] = 0; // table_entry0: super_block
	table[1] = sizeof(struct pfs_b0);
	table[2] = sizeof(struct pfs_b0); // table_entry1: root_folder
	table[3] = sizeof(struct pfs_b0) + sizeof(struct pfs_folder);
	table[4] = table_offset; // (table_entry2.start:) table_offset marker
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
	i64 now = time(NULL);
	root->element.last_mod_time = now;
	root->real_parent = no_parent;
	root->direct_child_count = 0L;
	root->folder_entry = no_parent;
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

extern int pfs_fill_root(pfs_eh overwrite_me) {
	struct pfs_b0 *b0 = pfs->get(pfs, 0L);
	if (b0 == NULL) {
		pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
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
	pfs->unget(pfs, 0L);
	return 1;
}

extern pfs_eh pfs_root() {
	struct pfs_element_handle *res = malloc(sizeof(struct pfs_element_handle));
	if (res == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		return NULL;
	}
	if (!pfs_fill_root(res)) {
		free(res);
		return NULL;
	}
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
	remove_from_block_table(h.block, h.pos);
	pfs->set(pfs, h.block);
	return 1;
}

extern int pfs_element_delete(pfs_eh e) {
	struct pfs_element *element = pfs->get(pfs, e->element_place.block) + e->element_place.pos;
	struct pfs_folder *direct_parent = pfs->get(pfs, e->direct_parent_place.block)
	        + e->direct_parent_place.pos;
	if ((direct_parent->entries[e->index_in_direct_parent_list].flags & PFS_FLAGS_FOLDER) != 0) {
		struct pfs_folder *folder = (struct pfs_folder*) element;
		if (folder->direct_child_count > 0) {
			if ((folder->direct_child_count > 1) || (folder->helper_index == -1)
			        || (!del_helper(folder->entries[0].child_place))) {
				pfs->unget(pfs, e->element_place.block);
				pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
				return 0;
			}
			folder->direct_child_count = 0;
			folder->helper_index = -1;
		}
	} else if ((direct_parent->entries[e->index_in_direct_parent_list].flags & PFS_FLAGS_FILE)
	        != 0) {
		pfs_file_truncate(e, 0L);
	} else {
		abort();
	}
	memmove(direct_parent->entries + e->index_in_direct_parent_list,
	        direct_parent->entries + e->index_in_direct_parent_list + 1,
	        (e->index_in_direct_parent_list - direct_parent->direct_child_count - 1)
	                * sizeof(struct pfs_folder_entry));
	direct_parent->direct_child_count--;
	if (direct_parent->helper_index > e->index_in_direct_parent_list) {
		direct_parent->helper_index--;
	} else if (direct_parent->helper_index == e->index_in_direct_parent_list) {
		abort();
	}
	shrink_folder_entry(e->direct_parent_place,
	        sizeof(struct pfs_folder)
	                + (sizeof(struct pfs_folder_entry) * (1 + direct_parent->direct_child_count)));
	pfs->set(pfs, e->direct_parent_place.block);
	pfs->set(pfs, e->element_place.block);
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

i32 allocate_in_block_table(i64 block, i64 size) {
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
			pfs->set(pfs, block);
			return result;
		}
	}
	pfs->unget(pfs, block);
	return -1;
}

static inline i32 fill_entry_and_move_data(i32 free, i32 last_end, const i64 new_size,
        const int copy, const i32 pos, i32 old_size, i32 *table, void *block_data, const i64 block) {
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

i32 reallocate_in_block_table(const i64 block, const i32 pos, const i64 new_size, const int copy) {
	void *block_data = pfs->get(pfs, block);
	if (block_data == NULL) {
		abort();
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
			pfs->unget(pfs, block);
			return pos;
		} else if (old_size > new_size) {
			if (new_size > 0) {
				table[1] = pos + new_size;
			} else if (new_size == 0) {
				for (i64 *cpy = ((void*) table) - 8; ((void*) cpy) >= (block_data + *table_end);
				        cpy--) {
					cpy[1] = cpy[0];
				}
				*table_end += 8;
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
			i32 new_pos = fill_entry_and_move_data(max_in_place_mov - new_size, table[-1], new_size,
			        copy, pos, old_size, table, block_data, block);
			pfs->set(pfs, block);
			return new_pos;
		}
		i32 *my_old_entry = table;
		table = block_data + *table_end;
		i32 last_end = 0;
		for (; table < my_old_entry; table += 2) {
			i32 free = (*table) - last_end;
			if (free >= new_size) {
				for (i64 *table_entry = (void*) table;
				        ((void*) table_entry) < ((void*) my_old_entry); table_entry++) {
					table_entry[1] = table_entry[0];
				}
				i32 new_pos = fill_entry_and_move_data(free, last_end, new_size, copy, pos,
				        old_size, table, block_data, block);
				pfs->set(pfs, block);
				return new_pos;
			}
			last_end = table[1];
		}
		table += 4; // skip this entry + skip next entry (already checked at the beginning if this entry can grow in place)
		for (; table <= table_end; table += 2) {
			i32 free = table[0] - table[-1];
			if (free >= new_size) {
				for (i64 *table_entry = ((void*) my_old_entry) + 8;
				        ((void*) table_entry) < ((void*) table); table_entry++) {
					table_entry[-1] = table_entry[0];
				}
				i32 new_pos = fill_entry_and_move_data(free - new_size, table[-3], new_size, copy,
				        pos, old_size, table - 2, block_data, block);
				pfs->set(pfs, block);
				return new_pos;
			}
		} // could not resize
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
	init_block(block, size);
	write->block = block;
	write->pos = 0;
	return 1;
}

void set_parent_place_from_childs(struct pfs_place e, struct pfs_place real_parent) {
	void *block_data = pfs->get(pfs, e.block);
	if (block_data == NULL) {
		abort();
	}
	struct pfs_folder *f = block_data + e.pos;
	for (i32 i = 0; i < f->direct_child_count; i++) {
		if ((f->entries[i].flags & PFS_FLAGS_FOLDER) != 0) {
			void *child_block_data = pfs->get(pfs, f->entries[i].child_place.block);
			if (child_block_data == NULL) {
				abort();
			}
			struct pfs_folder *child = child_block_data + f->entries[i].child_place.pos;
			child->real_parent = real_parent;
			child->folder_entry.block = e.block;
			child->folder_entry.pos = e.pos + sizeof(struct pfs_folder)
			        + i * sizeof(struct pfs_folder_entry);
			if (child->folder_entry.pos != ((i64) &f->entries[i]) - (i64) block_data) {
				abort();
			}
			if ((f->entries[i].flags & PFS_FLAGS_HELPER_FOLDER) != 0) {
				if (i != f->helper_index) {
					abort();
				}
				set_parent_place_from_childs(f->entries[i].child_place, real_parent);
			} else if (i == f->helper_index) {
				abort();
			}
			pfs->set(pfs, f->entries[i].child_place.block);
		}
	}
	pfs->set(pfs, e.block);
}

///**
// * only called with folder elements
// */
//static void change_place_in_parent_and_struct(i64 new_block, i32 new_pos, struct pfs_place *place) {
//	const i32 old_block = place->block;
//	const i32 old_pos = place->pos;
//	place->block = new_block;
//	place->pos = new_pos;
//	struct pfs_folder *f = pfs->get(pfs, new_block) + new_pos;
//	struct pfs_folder *parent = pfs->get(pfs, f->direct_parent.block) + f->direct_parent.pos;
//	if (parent->entries[f->index_in_parent].child_place.pos == old_pos) {
//		if (parent->entries[f->index_in_parent].child_place.block == old_block) {
//			parent->entries[f->index_in_parent].child_place.block = place->block;
//			parent->entries[f->index_in_parent].child_place.pos = place->pos;
//			return;
//		}
//	}
//	abort();
//}

i32 grow_folder_entry(const struct pfs_element_handle *e, i32 new_size) {
	i32 new_pos = reallocate_in_block_table(e->element_place.block, e->element_place.pos, new_size,
	        1);
	if (new_pos != -1) {
		//TODO remove line
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
			struct pfs_place place = { .block = e->element_place.block, .pos = new_pos };
			set_parent_place_from_childs(place, place);
		}
		return new_pos;
	}
//	if (!allow_block_change) {
	return -1;
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

i32 add_name(i64 block_num, const char *name, i64 str_len) {
	void *block = pfs->get(pfs, block_num);
	i32 pos = allocate_in_block_table(block_num, str_len);
	if (pos == -1) {
		pfs->unget(pfs, block_num);
		return -1;
	} else {
		memcpy(block + pos, name, str_len);
		pfs->set(pfs, block_num);
		return pos;
	}
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
	if (btfb != -1L) return;
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
	if (btfb != -1L) return;
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
	for (i64 current_block = first_block; 1; remain -= pfs->block_size - 8) {
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
