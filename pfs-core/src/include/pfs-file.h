/*
 * pfs-file.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_FILE_H_
#define SRC_INCLUDE_PFS_FILE_H_

#include "patr-file-sys.h"

/*
 * opens a stream handle for the file/pipe
 *   note that this function works also for pipes
 *   add the PFS_SO_FILE flag when no pipes are allowed
 *
 * on success the stream handle and on error -1 is returned
 */
extern int pfs_open_stream(int eh, i32 stream_flags);

/*
 * returns the file length
 * on error -1 is returned
 */
extern i64 pfs_file_length(int eh);

#endif /* SRC_INCLUDE_PFS_FILE_H_ */
