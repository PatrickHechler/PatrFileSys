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
	if ((shs[sh]->flags & (PFS_SO_APPEND | PFS_SO_WRITE)) == 0) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (len <= 0) {
		if (len < 0) {
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		}
		return 0;
	}
	void *my_block_data = pfs->get(pfs,
			shs[sh]->element->handle.element_place.block);
	if (!my_block_data) {
		return 0;
	}
	i64 wrote = 0;
	struct pfs_file *f = my_block_data
			+ shs[sh]->element->handle.element_place.pos;
	if (shs[sh]->pos == -1) {
		shs[sh]->place = find_place(f->first_block, f->file_length);
		shs[sh]->pos = f->file_length;
	} else if ((shs[sh]->flags & PFS_SO_APPEND) || (!shs[sh]->is_file)) {
		if (shs[sh]->is_file) {
			if (shs[sh]->pos >= f->file_length) {
				shs[sh]->pos = 0;
				shs[sh]->place.block = f->first_block;
				shs[sh]->place.pos = 0;
			}
		}
		shs[sh]->place = find_place(shs[sh]->place.block,
				f->file_length - shs[sh]->pos + shs[sh]->place.pos);
	} else if (f->file_length < shs[sh]->pos) {
		void *write_block_data = pfs->get(pfs, shs[sh]->place.block);
		i64 last_block_num = -1L;
		i64 len2 = shs[sh]->place.pos - f->file_length;
		for (; 1;) {
			if (!write_block_data) {
				break;
			}
			i64 cpy = pfs->block_size - shs[sh]->pos - 8;
			if (cpy > len2) {
				cpy = len2;
			}
			memset(write_block_data + shs[sh]->place.pos, 0, cpy);
			last_block_num = shs[sh]->place.block;
			len2 -= cpy;
			f->file_length += cpy;
			shs[sh]->place.pos = 0;
			if (len2 <= 0) {
				break;
			}
			i64 last_block_num = shs[sh]->place.block;
			if (-1 != *(i64*) (write_block_data + pfs->block_size - 8)) {
				shs[sh]->place.block = *(i64*) (write_block_data
						+ pfs->block_size - 8);
			} else {
				shs[sh]->place.block = allocate_block(
				/*			*/BLOCK_FLAG_USED | BLOCK_FLAG_FILE_DATA);
				*(i64*) (write_block_data + pfs->block_size - 8) =
						shs[sh]->place.block;
			}
			pfs->set(pfs, last_block_num);
			write_block_data = pfs->get(pfs, shs[sh]->place.block);
		}
		pfs->set(pfs, shs[sh]->place.block);
		if (f->file_length != shs[sh]->pos) {
			abort();
		}
	}
	void *write_block_data = pfs->get(pfs, shs[sh]->place.block);
	for (; 1;) { // write and append
		if (!write_block_data) {
			break;
		}
		i64 cpy = pfs->block_size - shs[sh]->pos - 8;
		if (cpy > len) {
			cpy = len;
		}
		memcpy(write_block_data + shs[sh]->place.pos, data, cpy);
		len -= cpy;
		wrote += cpy;
		shs[sh]->pos += cpy;
		f->file_length += cpy;
		shs[sh]->place.pos = 0;
		if (len <= 0) {
			break;
		}
		i64 last_block_num = shs[sh]->place.block;
		if (-1 != *(i64*) (write_block_data + pfs->block_size - 8)) {
			shs[sh]->place.block = *(i64*) (write_block_data + pfs->block_size
					- 8);
		} else {
			shs[sh]->place.block = allocate_block(
			/*			*/BLOCK_FLAG_USED | BLOCK_FLAG_FILE_DATA);
			*(i64*) (write_block_data + pfs->block_size - 8) =
					shs[sh]->place.block;
		}
		pfs->set(pfs, last_block_num);
		write_block_data = pfs->get(pfs, shs[sh]->place.block);
	}
	pfs->set(pfs, shs[sh]->place.block);
	pfs->set(pfs, shs[sh]->element->handle.element_place.block);
	if (len < 0) {
		abort();
	}
	return wrote;
}

extern i64 pfs_stream_read(int sh, void *buffer, i64 len) {
	sh(0)
	if ((shs[sh] & PFS_SO_READ) == 0) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (len <= 0) {
		if (len < 0) {
			pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		}
		return 0;
	}
	void *my_block_data = pfs->get(pfs,
			shs[sh]->element->handle.element_place.block);
	if (!my_block_data) {
		return 0;
	}
	i64 reat = 0L;
	struct pfs_file *f = my_block_data
			+ shs[sh]->element->handle.element_place.pos;
}
