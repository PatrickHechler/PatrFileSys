/*
 * pfs-element.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_ELEMENT_H_
#define PFS_ELEMENT_H_

#include "pfs-constants.h"

typedef struct pfs_place element;

ui32 pfs_element_get_flags(element *e);

int pfs_element_modify_flags(element *e, ui32 add_flags, ui32 rem_flags);

i64 pfs_element_get_create_time(element *e);

i64 pfs_element_get_last_mod_time(element *e);

i64 pfs_element_get_last_meta_mod_time(element *e);

int pfs_element_set_create_time(element *e, i64 new_time);

int pfs_element_set_last_mod_time(element *e, i64 new_time);

int pfs_element_set_last_meta_mod_time(element *e, i64 new_time);

#endif /* PFS_ELEMENT_H_ */
