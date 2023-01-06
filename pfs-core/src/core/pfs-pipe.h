/*
 * pfs-pipe.h
 *
 *  Created on: Sep 23, 2022
 *      Author: pat
 */

#ifndef SRC_CORE_PFS_PIPE_H_
#define SRC_CORE_PFS_PIPE_H_

#include "pfs.h"

int pfsc_pipe_read(pfs_eh p, void *buffer, i64 length);

// note, that when pipe append becomes its own function the code sometimes rely on it to be a redirect to file
// (it is not checked if it is a file or pipe and just calls append)
// also note that functions like truncate are also assumed to allow pipes
int __REDIRECT(pfsc_pipe_append, (pfs_eh p, void *data, i64 length), pfsc_file_append);

i64 pfsc_pipe_length(pfs_eh p);

#endif /* SRC_CORE_PFS_PIPE_H_ */
