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

int __REDIRECT(pfsc_pipe_append, (pfs_eh p, void *data, i64 length), pfsc_file_append);

i64 pfs_pipe_length(pfs_eh p);

#endif /* SRC_CORE_PFS_PIPE_H_ */
