/*
 * pfs-pipe.h
 *
 *  Created on: Sep 23, 2022
 *      Author: pat
 */

#ifndef PFS_PIPE_H_
#define PFS_PIPE_H_

#include "pfs.h"

extern int pfs_pipe_read(pfs_eh p, void *buffer, i64 length);

extern int __REDIRECT(pfs_pipe_append, (pfs_eh p, void *data, i64 length), pfs_file_append);

extern i64 pfs_pipe_length(pfs_eh p);

#endif /* PFS_PIPE_H_ */
