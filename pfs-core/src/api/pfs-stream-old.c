/*
 * pfs-stream.c
 *
 * instead of the other files this API file really implements parts of the PFS
 * this is because the core does not support streams
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#include "pfs.h"
#include "../include/pfs-stream.h"

extern i64 pfs_stream_write(int sh, void *data, i64 len) {
	sh(0)
	if ((pfs_shs[sh]->flags & (PFS_SO_APPEND | PFS_SO_WRITE)) == 0) {
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (len <= 0) {
		if (len < 0) {
			(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		}
		return 0;
	}
	void *my_block_data = pfs->get(pfs,
			pfs_shs[sh]->element->handle.element_place.block);
	if (!my_block_data) {
		return 0;
	}
	i64 wrote = 0;
	struct pfs_file *f = my_block_data
			+ pfs_shs[sh]->element->handle.element_place.pos;
	struct pfs_place place;
	if ((!pfs_shs[sh]->is_file) || (pfs_shs[sh]->flags & PFS_SO_APPEND)) {
		if (pfs_shs[sh]->is_file) {
			pfs_shs[sh]->pos = f->file_length;
		}
		place = find_place(f->first_block, f->file_length);
	} else if (f->file_length < pfs_shs[sh]->pos) {
		place = find_place(f->first_block, f->file_length);
		void *write_block_data = pfs->get(pfs, place.block);
		i64 last_block_num = -1L;
		i64 len2 = place.pos - f->file_length;
		for (; 1;) {
			if (!write_block_data) {
				break;
			}
			i64 cpy = pfs->block_size - pfs_shs[sh]->pos - 8;
			if (cpy > len2) {
				cpy = len2;
			}
			memset(write_block_data + place.pos, 0, cpy);
			last_block_num = place.block;
			len2 -= cpy;
			data += cpy;
			f->file_length += cpy;
			place.pos = 0;
			if (len2 <= 0) {
				break;
			}
			i64 last_block_num = place.block;
			if (-1 != *(i64*) (write_block_data + pfs->block_size - 8)) {
				place.block = *(i64*) (write_block_data + pfs->block_size - 8);
			} else {
				place.block = allocate_block(BLOCK_FLAG_USED | BLOCK_FLAG_DATA);
				*(i64*) (write_block_data + pfs->block_size - 8) = place.block;
			}
			pfs->set(pfs, last_block_num);
			write_block_data = pfs->get(pfs, place.block);
		}
		pfs->set(pfs, place.block);
		if (f->file_length != pfs_shs[sh]->pos) {
			abort();
		}
	} else {
		place = find_place(f->first_block, pfs_shs[sh]->pos);
	}
	void *write_block_data = pfs->get(pfs, place.block);
	for (; 1;) { // write and append
		if (!write_block_data) {
			break;
		}
		i64 cpy = pfs->block_size - pfs_shs[sh]->pos - 8;
		if (cpy > len) {
			cpy = len;
		}
		memcpy(write_block_data + place.pos, data, cpy);
		len -= cpy;
		wrote += cpy;
		data += cpy;
		pfs_shs[sh]->pos += cpy;
		if (pfs_shs[sh]->pos > f->file_length) {
			f->file_length = pfs_shs[sh]->pos;
		}
		place.pos = 0;
		if (len <= 0) {
			break;
		}
		i64 last_block_num = place.block;
		if (-1 != *(i64*) (write_block_data + pfs->block_size - 8)) {
			place.block = *(i64*) (write_block_data + pfs->block_size - 8);
		} else {
			place.block = allocate_block(
			/*			*/BLOCK_FLAG_USED | BLOCK_FLAG_DATA);
			*(i64*) (write_block_data + pfs->block_size - 8) = place.block;
		}
		pfs->set(pfs, last_block_num);
		write_block_data = pfs->get(pfs, place.block);
	}
	pfs->set(pfs, place.block);
	pfs->set(pfs, pfs_shs[sh]->element->handle.element_place.block);
	if (len < 0) {
		abort();
	}
	return wrote;
}

extern i64 pfs_stream_read(int sh, void *buffer, i64 len) {
	sh(0)
	if ((pfs_shs[sh]->flags & PFS_SO_READ) == 0) {
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (len <= 0) {
		if (len < 0) {
			(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		}
		return 0;
	}
	void *my_block_data = pfs->get(pfs,
			pfs_shs[sh]->element->handle.element_place.block);
	if (!my_block_data) {
		return 0;
	}
	i64 reat = 0L;
	struct pfs_file *f = my_block_data
			+ pfs_shs[sh]->element->handle.element_place.pos;
	if (f->file_length <= pfs_shs[sh]->pos) {
		return 0;
	}
	struct pfs_place place = find_place(f->first_block, pfs_shs[sh]->pos);
	for (; len > 0;) {
		i64 block_num = pfs_shs[sh]->is_file ? place.block : f->first_block;
		i32 pos_in_block =
				pfs_shs[sh]->is_file ?
						place.pos : ((struct pfs_pipe*) f)->start_offset;
		void *block_data = pfs->get(pfs, block_num);
		if (!block_data) {
			pfs->set(pfs, pfs_shs[sh]->element->handle.element_place.block);
			return reat;
		}
		i64 cpy = pfs->block_size - pos_in_block - 8;
		if (cpy > len) {
			cpy = len;
		}
		if ((f->file_length - pfs_shs[sh]->pos) < (cpy + pos_in_block)) {
			cpy = f->file_length - pfs_shs[sh]->pos;
		}
		memcpy(buffer, block_data + pos_in_block, cpy);
		len -= cpy;
		buffer += cpy;
		reat += cpy;
		if (pfs_shs[sh]->is_file) {
			pfs_shs[sh]->pos += cpy;
			if (cpy < pfs->block_size - pos_in_block - 8) {
				place.pos += cpy;
			} else {
				place.block = *(i64*) (block_data + pfs->block_size - 8);
				place.pos = 0;
				if (place.block == -1) {
					if (pfs_shs[sh]->pos < f->file_length) {
						abort();
					}
					len = 0;
					place.block = block_num;
					place.pos = pfs->block_size - 8;
				}
			}
		} else if (cpy < pfs->block_size - pos_in_block - 8) {
			((struct pfs_pipe*) f)->start_offset += cpy;
		} else {
			((struct pfs_pipe*) f)->start_offset = 0;
			f->file_length -= pfs->block_size - 8;
			f->first_block = *(i64*) (block_data + pfs->block_size - 8);
		}
		pfs->unget(pfs, block_num);
	}
	pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
	return reat;
}

extern i64 pfs_stream_get_pos(int sh) {
	sh(-1)
	if (!pfs_shs[sh]->is_file) {
		(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return -1L;
	}
	return pfs_shs[sh]->pos;
}

extern int pfs_stream_set_pos(int sh, i64 pos) {
	sh(0)
	if (!pfs_shs[sh]->is_file) {
		(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return 0;
	}
	if (pos < 0L) {
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	pfs_shs[sh]->pos = pos;
	return 1;
}

extern i64 pfs_stream_add_pos(int sh, i64 add) {
	sh(-1)
	if (!pfs_shs[sh]->is_file) {
		(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return -1;
	}
	i64 new = pfs_shs[sh]->pos + add;
	if (new < 0L) {
		(*pfs_err_loc) = PFS_ERRNO_ILLEGAL_ARG;
		return -1;
	}
	return pfs_shs[sh]->pos = new;
}

extern i64 pfs_stream_seek_eof(int sh) {
	sh(-1)
	if (!pfs_shs[sh]->is_file) {
		(*pfs_err_loc) = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return -1;
	}
	void *block_data = pfs->get(pfs,
			pfs_shs[sh]->element->handle.element_place.block);
	if (!block_data) {
		return -1;
	}
	struct pfs_file *f = block_data
			+ pfs_shs[sh]->element->handle.element_place.pos;
	pfs_shs[sh]->pos = f->file_length;
	pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
	return pfs_shs[sh]->pos;
}
