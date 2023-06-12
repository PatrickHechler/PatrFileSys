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
	if (!pfs_shs[sh]->element) {
		if (pfs_shs[sh]->delegate->write) {
			return pfs_shs[sh]->delegate->write(pfs_shs[sh]->delegate, data,
					len);
		}
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return 0;
	} else if ((pfs_shs[sh]->flags & (PFS_SO_APPEND | PFS_SO_WRITE)) == 0) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (len <= 0) {
		if (len < 0) {
			pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		}
		return 0;
	}
	void *my_block_data = pfs->get(pfs,
			pfs_shs[sh]->element->handle.element_place.block);
	if (!my_block_data) {
		return 0;
	}
	struct pfs_file *f = my_block_data
			+ pfs_shs[sh]->element->handle.element_place.pos;
	if ((!pfs_shs[sh]->is_file) || (pfs_shs[sh]->flags & PFS_SO_APPEND)) {
		i64 appended = pfsc_file_append(&pfs_shs[sh]->element->handle, data,
				len);
		pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
		return appended;
	} else if (f->file_length < pfs_shs[sh]->pos) {
		if (!pfsc_file_truncate_grow(&pfs_shs[sh]->element->handle,
				pfs_shs[sh]->pos)) {
			pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
			return -1;
		}
	}
	i64 overwrite_place = f->file_length - pfs_shs[sh]->pos;
	if (overwrite_place > 0) {
		i64 write_len;
		if (overwrite_place <= len) {
			write_len = overwrite_place;
		} else {
			write_len = len;
		}
		if (!pfsc_file_write(&pfs_shs[sh]->element->handle, pfs_shs[sh]->pos,
				data, write_len)) {
			pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
			return -1;
		}
		i64 remain = len - write_len;
		if (remain <= 0) {
			pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
			return write_len;
		}
		i64 appended = pfsc_file_append(&pfs_shs[sh]->element->handle,
				data + write_len, remain);
		pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
		i64 sum = write_len + appended;
		pfs_shs[sh]->pos += sum;
		return sum;
	} else if (overwrite_place < 0) {
		abort(); // should be covered earlier
	} else {
		i64 appended = pfsc_file_append(&pfs_shs[sh]->element->handle, data,
				len);
		pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
		pfs_shs[sh]->pos += appended;
		return appended;
	}
}

extern i64 pfs_stream_read(int sh, void *buffer, i64 len) {
	sh(0)
	if (!pfs_shs[sh]->element) {
		if (pfs_shs[sh]->delegate->read) {
			return pfs_shs[sh]->delegate->read(pfs_shs[sh]->delegate, buffer, len);
		}
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return 0;
	} else if ((pfs_shs[sh]->flags & PFS_SO_READ) == 0) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	if (len <= 0) {
		if (len < 0) {
			pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		}
		return 0;
	}
	void *my_block_data = pfs->get(pfs,
			pfs_shs[sh]->element->handle.element_place.block);
	if (!my_block_data) {
		return 0;
	}
	struct pfs_file *f = my_block_data
			+ pfs_shs[sh]->element->handle.element_place.pos;
	if (!pfs_shs[sh]->is_file) {
		if (len > (f->file_length - ((struct pfs_pipe*) f)->start_offset)) {
			len = (f->file_length - ((struct pfs_pipe*) f)->start_offset);
		}
		if (!pfsc_pipe_read(&pfs_shs[sh]->element->handle, buffer, len)) {
			pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
			return -1;
		}
		pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
		return len;
	}
	if (f->file_length <= pfs_shs[sh]->pos) {
		pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
		return 0;
	}
	if (len > f->file_length - pfs_shs[sh]->pos) {
		len = f->file_length - pfs_shs[sh]->pos;
	}
	if (!pfsc_file_read(&pfs_shs[sh]->element->handle, pfs_shs[sh]->pos, buffer,
			len)) {
		pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
		return 0;
	}
	pfs_shs[sh]->pos += len;
	pfs->unget(pfs, pfs_shs[sh]->element->handle.element_place.block);
	return len;
}

extern i64 pfs_stream_get_pos(int sh) {
	sh(-1)
	if (!pfs_shs[sh]->element) {
		if (pfs_shs[sh]->delegate->get_pos) {
			return pfs_shs[sh]->delegate->get_pos(pfs_shs[sh]->delegate);
		}
		if ((pfs_shs[sh]->flags & PFS_SO_FILE) == 0) {
			pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			return -1L;
		}
		i64 sek = lseek(pfs_shs[sh]->is_file, 0, SEEK_CUR);
		if (sek == -1) {
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		}
		return sek;
	} else if (!pfs_shs[sh]->is_file) {
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return -1L;
	}
	return pfs_shs[sh]->pos;
}

extern int pfs_stream_set_pos(int sh, i64 pos) {
	sh(0)
	if (!pfs_shs[sh]->element) {
		if ((pfs_shs[sh]->flags & PFS_SO_FILE) == 0) {
			pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			return -1L;
		}
		i64 sek = lseek(pfs_shs[sh]->is_file, pos, SEEK_SET);
		if (sek == -1) {
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		}
		return sek;
	} else if (!pfs_shs[sh]->is_file) {
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return 0;
	}
	if (pos < 0L) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	pfs_shs[sh]->pos = pos;
	return 1;
}

extern i64 pfs_stream_add_pos(int sh, i64 add) {
	sh(-1)
	if (!pfs_shs[sh]->element) {
		if ((pfs_shs[sh]->flags & PFS_SO_FILE) == 0) {
			pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			return -1L;
		}
		i64 sek = lseek(pfs_shs[sh]->is_file, add, SEEK_CUR);
		if (sek == -1) {
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		}
		return sek;
	} else if (!pfs_shs[sh]->is_file) {
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
		return -1;
	}
	i64 new = pfs_shs[sh]->pos + add;
	if (new < 0L) {
		pfs_err = PFS_ERRNO_ILLEGAL_ARG;
		return -1;
	}
	return pfs_shs[sh]->pos = new;
}

extern i64 pfs_stream_seek_eof(int sh) {
	sh(-1)
	if (!pfs_shs[sh]->element) {
		if ((pfs_shs[sh]->flags & PFS_SO_FILE) == 0) {
			pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
			return -1L;
		}
		i64 sek = lseek(pfs_shs[sh]->is_file, 0, SEEK_END);
		if (sek == -1) {
			pfs_err = PFS_ERRNO_UNKNOWN_ERROR;
		}
		return sek;
	} else if (!pfs_shs[sh]->is_file) {
		pfs_err = PFS_ERRNO_ELEMENT_WRONG_TYPE;
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
