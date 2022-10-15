/*
 * pfs.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_API_PFS_H_
#define SRC_API_PFS_H_

#include "../core/pfs.h"

struct element_handle {
	struct pfs_element_handle handle;
	struct element_handle *parent;
	long load_count;
};

struct stream_handle {
	struct element_handle *file;
	i64 pos;
	struct pfs_place place;
	int is_file;
	i32 flags;
};

struct iter_handle {
	struct pfs_folder_iter handle;
	struct element_handle *folder;
};

#ifndef I_AM_API_PFS
#define EXT extern
#else
#define EXT
#endif

EXT struct element_handle *ehs;
EXT struct stream_handle *shs;
EXT struct iter_handle *ihs;

EXT i64 eh_size;
EXT i64 sh_size;
EXT i64 ih_size;

#endif /* SRC_API_PFS_H_ */
