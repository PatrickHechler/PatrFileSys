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
extern int pfs_stream_open_delegate(int fd, i32 stream_flags);

/*
 * writes some data to the stream
 *
 * returns the number of bytes actually written
 *
 * on success len bytes will be written and thus len will be returned
 * on error less then len (potentially 0) bytes will be written and (*pfs_err_loc) will be set
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
