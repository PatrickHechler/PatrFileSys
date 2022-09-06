/*
 * pfs-file.h
 *
 *  Created on: Sep 4, 2022
 *      Author: pat
 */

#ifndef PFS_FILE_H_
#define PFS_FILE_H_

#include "pfs.h"

extern int pfs_file_read(element *f, i64 position, void *buffer, i64 length);

extern int pfs_file_write(element *f, i64 position, void *data, i64 length);

extern i64 pfs_file_append(element *f, void *data, i64 length);

extern int pfs_file_truncate(element *f, i64 new_length);

extern i64 pfs_file_length(element *f);

#endif /* PFS_FILE_H_ */
