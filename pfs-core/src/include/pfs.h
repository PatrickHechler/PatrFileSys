/*
 * pfs.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_H_
#define SRC_INCLUDE_PFS_H_

extern int pfs_load(struct bm_block_manager *bm);

extern int pfs_load_and_format(struct bm_block_manager *bm, i64 block_count);

extern int pfs_format(i64 block_count);

extern i64 pfs_block_count();

extern i32 pfs_block_size();

extern int pfs_delete(const char *path);

extern int pfs_open_handle(const char *path);

extern int pfs_open_file_stream(const char *path, i32 stream_flags);

extern int pfs_open_pipe_stream(const char *path, i32 stream_flags);

/*
 * the flags PFS_S_CREATE_* are not allowed
 */
extern int pfs_open_stream(const char *path, i32 stream_flags);

extern int pfs_open_iter(const char *path, int show_hidden);

#endif /* SRC_INCLUDE_PFS_H_ */
