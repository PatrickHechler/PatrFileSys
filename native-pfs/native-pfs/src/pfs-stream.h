/*
 * pfs-stream.h
 *
 *  Created on: Oct 6, 2022
 *      Author: pat
 */

#ifndef NATIVE_PFS_SRC_PFS_STREAM_H_
#define NATIVE_PFS_SRC_PFS_STREAM_H_

#define PFS_SO_NOT_CREATE   0x00000000 /* fail if the file/pipe does not exist already (this is the default create option) */
#define PFS_SO_ONLY_CREATE  0x00000100 /* fail if the file/pipe exist already */
#define PFS_SO_ALSO_CREATE  0x00000200 /* create the file/pipe if it does not exist, but do not fail if the file/pipe exist already */
#define PFS_SOF_TRUNC       0x00010000 /* (file only) truncate the files content */
#define PFS_SOF_EOF         0x00020000 /* (file only) set the position initially to the end of the file not the start */

/*
 * reads data from the stream
 */
extern i64 pfs_stream_read(int sh, void *buf, i64 len);

/*
 * writes data to the stream
 */
extern i64 pfs_stream_write(int sh, void *buf, i64 len);

/*
 * appends data to the stream.
 *
 * for pipe-streams it makes no difference to write.
 *
 * for file-streams it is like calling seek_end(sh, 0); write(sh, buf, len)
 */
extern i64 pfs_stream_append(int sh, void *buf, i64 len);

/*
 * sets the position of the file stream
 *
 * if the new position is behind the end of the file the length
 * will be changed with the next write call
 *
 * if the strea-handle is a pipe stream this operation will fail
 *
 * on success the new position will be returned (pos) and on failure -1
 */
extern i64 pfs_stream_seek_set(int sh, i64 pos);

/*
 * adds the given value to the position of the file stream
 *
 * if the new position is behind the end of the file the length
 * will be changed with the next write call
 *
 * to get the current position seek_add(sh, 0) can be used
 *
 * if the strea-handle is a pipe stream this operation will fail
 *
 * on success the new position will be returned and on failure -1
 */
extern i64 pfs_stream_seek_add(int sh, i64 pos);

/*
 * sets the position of the file stream relative to the file end
 *
 * if the new position is behind the end of the file the length
 * will be changed with the next write call
 *
 * if the strea-handle is a pipe stream this operation will fail
 *
 * on success the new position will be returned and on failure -1
 */
extern i64 pfs_stream_seek_end(int sh, i64 pos);

#endif /* NATIVE_PFS_SRC_PFS_STREAM_H_ */
