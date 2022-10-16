/*
 * pfs-pipe.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_PIPE_H_
#define SRC_INCLUDE_PFS_PIPE_H_

#include "patr-file-sys.h"

/*
 * opens a stream handle for the file/pipe
 *   note that this function works also for files
 *   add the PFS_SO_PIPE flag when no files are allowed
 *
 * on success the stream handle and on error -1 is returned
 */
extern int pfs_open_stream(int eh, i32 stream_flags);

/*
 * returns the pipe length
 * on error -1 is returned
 */
extern i64 pfs_pipe_length(int eh);

#endif /* SRC_INCLUDE_PFS_PIPE_H_ */
