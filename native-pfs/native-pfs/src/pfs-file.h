/*
 * pfs-file.h
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#ifndef NATIVE_PFS_SRC_PFS_FILE_H_
#define NATIVE_PFS_SRC_PFS_FILE_H_

#include "pfs-fs.h"

/**
 * read some data from the file
 *
 * if position + length is greater than the files length this operation will fail
 */
extern int pfs_file_read(int fh, i64 position, void *buffer, i64 length);

/**
 * overwrite some data of the file
 *
 * if position + length is greater than the files length this operation will fail
 */
extern int pfs_file_write(int fh, i64 position, void *data, i64 length);

/**
 * append some data to the file
 *
 * returns the number of actually appended bytes on success
 * (may be lower than the requested bytes if not enough blocks could be allocated)
 * if this operation fails -1 is returned
 */
extern i64 pfs_file_append(int fh, void *data, i64 length);

/**
 * change the length of the file
 *
 * if the new_length is larger than the current length zeros are appended to the
 * file until the files-length is equal to new_length
 *
 * if the new_length is lower than the current length the files content at the
 * end of the file is removed
 */
extern int pfs_file_truncate(int fh, i64 new_length);

/**
 * get the length of a file
 *
 * if this operation fails -1 is returned
 */
extern i64 pfs_file_length(int fh);

/*
 * creates a new stream
 */
extern int pfs_file_stream(int fh, int stream_flags);

#endif /* NATIVE_PFS_SRC_PFS_FILE_H_ */
