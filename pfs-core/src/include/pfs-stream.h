/*
 * pfs-stream.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_STREAM_H_
#define SRC_INCLUDE_PFS_STREAM_H_

extern int pfs_stream_close(int sh);

extern i64 pfs_stream_write(int sh, void *data, i64 len);

extern i64 pfs_stream_append(int sh, void *data, i64 len);

extern i64 pfs_stream_read(int sh, void *buffer, i64 len);

extern i64 pfs_stream_get_pos(int sh);

extern i64 pfs_stream_set_pos(int sh, i64 pos);

extern i64 pfs_stream_add_pos(int sh, i64 add);

extern i64 pfs_stream_seek_eof(int sh);

#endif /* SRC_INCLUDE_PFS_STREAM_H_ */
