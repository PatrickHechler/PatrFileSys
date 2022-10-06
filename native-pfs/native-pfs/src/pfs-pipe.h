/*
 * pfs-pipe.h
 *
 *  Created on: Sep 23, 2022
 *      Author: pat
 */

#ifndef NATIVE_PFS_SRC_PFS_PIPE_H_
#define NATIVE_PFS_SRC_PFS_PIPE_H_

#include "pfs-fs.h"

/*
 * reads some data from the pipe
 */
extern int pfs_pipe_read(int ph, void *buffer, i64 length);

/*
 * writes some data to the pipe
 */
extern int pfs_pipe_append(int ph, void *data, i64 length);

/*
 * get the current length of the pipe (the amount of bytes which can be read)
 */
extern i64 pfs_pipe_length(int ph);

/*
 * opens a stream for the pipe.
 *
 * the returned stream will only support read/write/append
 * (write and append will work exactly the same for pipes)
 */
extern int pfs_pipe_open(int ph);

#endif /* NATIVE_PFS_SRC_PFS_PIPE_H_ */
