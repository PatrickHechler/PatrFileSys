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
	i64 load_count;
	struct hashset children;
};

struct stream_handle {
	struct element_handle *element;
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
#define INIT(val)
#else
#define EXT
#define INIT(val) = val
#endif

EXT struct element_handle **ehs INIT(NULL);
EXT struct stream_handle **shs INIT(NULL);
EXT struct iter_handle **ihs INIT(NULL);

EXT i64 eh_len INIT(0L);
EXT i64 sh_len INIT(0L);
EXT i64 ih_len INIT(0L);

EXT struct element_handle *root INIT(NULL);
EXT struct element_handle *cwd INIT(NULL);

#undef EXT
#undef INIT

#endif /* SRC_API_PFS_H_ */
