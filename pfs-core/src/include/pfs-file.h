/*
 * pfs-file.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_FILE_H_
#define SRC_INCLUDE_PFS_FILE_H_

extern int pfs_open_stream(int eh, i32 stream_flags);

extern i64 pfs_file_length(int eh);

#endif /* SRC_INCLUDE_PFS_FILE_H_ */
