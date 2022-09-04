/*
 * pfs-folder.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_FOLDER_H_
#define PFS_FOLDER_H_

#include "pfs-element.h"
#include "pfs-constants.h"

i64 pfs_folder_child_count(element *f);

int pfs_folder_child_from_index(element *f, i64 index);

int pfs_folder_child_from_name(element *f, char *name);

int pfs_folder_create_folder(element *f, char *name);

int pfs_folder_create_file(element *f, char *name);

#endif /* PFS_FOLDER_H_ */
