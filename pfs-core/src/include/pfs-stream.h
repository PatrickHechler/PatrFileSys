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
 * pfs-stream.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_STREAM_H_
#define SRC_INCLUDE_PFS_STREAM_H_

#include "patr-file-sys.h"

/*
 * closes the given stream handle
 *
 * the given stream handle ID may be reused by the system when
 * a new stream handle is opened
 */
extern int pfs_stream_close(int sh);

/**
 * creates a delegate stream for the given file descriptor and with the given flags
 */
extern int pfs_stream_open_delegate_fd(bm_fd fd, i32 stream_flags);

struct delegate_stream {
	i64 (*write)(struct delegate_stream*,void*,i64);
	i64 (*read)(struct delegate_stream*,void*,i64);
	i64 (*get_pos)(struct delegate_stream*);
	int (*set_pos)(struct delegate_stream*,i64);
	i64 (*add_pos)(struct delegate_stream*,i64);
	i64 (*seek_eof)(struct delegate_stream*);
};

/**
 * creates a delegate stream for the given stream and with the given flags
 *
 * all unsupported operations are symbolized by a NULL pointer
 */
extern int pfs_stream_open_delegate(struct delegate_stream *stream);

/*
 * writes some data to the stream
 *
 * returns the number of bytes actually written
 *
 * on success len bytes will be written and thus len will be returned
 * on error less then len (potentially 0) bytes will be written and (pfs_err) will be set
 */
extern i64 pfs_stream_write(int sh, void *data, i64 len);

/*
 * reads some data from the stream.
 *
 * if the stream is a pipe stream the pipe will remove the read
 * bytes from its content
 *
 * if this stream is a file stream the position will be incremented
 * by the number of bytes actually read
 *
 * returns the number of bytes actually read
 *   - note that reaching end of file/pipe is not considered an error
 *     so when a value less than len is returned the end of the
 *     pipe/file has been reached
 */
extern i64 pfs_stream_read(int sh, void *buffer, i64 len);

/*
 * returns the position of the given file stream
 *
 * when the given stream is a pipe stream this function fails
 *
 * on error -1 is returned
 */
extern i64 pfs_stream_get_pos(int sh);

/*
 * sets the position of the given file stream
 *
 * when the given stream is a pipe stream this function fails
 *
 * note that it is allowed to set the position behind the end of
 * the file. pfs_stream_write will append zeros to the file when the
 * position is greater then the file length.
 *
 * on success 1 on error 0 is returned
 */
extern int pfs_stream_set_pos(int sh, i64 pos);

/*
 * adds the given value to the position of the given file stream
 *
 * when the given stream is a pipe stream this function fails
 *
 * this function works like:
 *   i64 pos = pfs_stream_get_pos(sh);
 *   if (pfs_stream_set_pos(sh, pos + add)) {
 *     return pos + add;
 *   } else {
 *     return -1;
 *   }
 *
 * on success the new position and on error -1 is returned
 */
extern i64 pfs_stream_add_pos(int sh, i64 add);

/*
 * sets the position of the given file stream to the end of the file
 *
 * when the given stream is a pipe stream this function fails
 *
 * on success the new position and on error -1 is returned
 */
extern i64 pfs_stream_seek_eof(int sh);

#endif /* SRC_INCLUDE_PFS_STREAM_H_ */
