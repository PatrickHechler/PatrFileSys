/*
 * pfs-stream.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_STREAM_H_
#define SRC_INCLUDE_PFS_STREAM_H_

/*
 * closes the given stream handle
 *
 * the given stream handle ID may be reused by the system when
 * a new stream handle is opened
 */
extern int pfs_stream_close(int sh);

/*
 * writes some data to the stream
 *
 * returns the number of bytes actually written and on error -1
 *   - note that it is not considered an error when at least one
 *     byte is actually written to the stream
 */
extern i64 pfs_stream_write(int sh, void *data, i64 len);

/*
 * works like write, but sets the position of the stream to the
 * end of the file before writing to the file.
 *
 * for pipes the write and append functions works exactly the same
 *
 *
 * returns the number of bytes actually appended and on error -1
 *   - note that it is not considered an error when at least one
 *     byte is actually written to the stream
 */
extern i64 pfs_stream_append(int sh, void *data, i64 len);

/*
 * reads some data from the stream.
 *
 * if the stream is a pipe stream the pipe will remove the read
 * bytes from its content
 *
 * if this stream is a file stream the position will be incremented
 * by the number of bytes actually read
 *
 * returns the number of bytes actually read and -1 on error
 *   - note that reaching end of file/pipe is not considered an error
 *     so when zero is returned and len is not zero the end of the
 *     pipe/file has been reached
 *   - note also that nothing is not considered an error when at least
 *     one byte has been read
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
 * on success the new position and on error -1 is returned
 */
extern i64 pfs_stream_set_pos(int sh, i64 pos);

/*
 * adds the given value to the position of the given file stream
 *
 * when the given stream is a pipe stream this function fails
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
