/*
 * pfs-element.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_ELEMENT_H_
#define SRC_INCLUDE_PFS_ELEMENT_H_

extern int pfs_element_close(int eh);

extern int pfs_element_parent(int eh);

extern int pfs_element_delete(int eh);

extern i32 pfs_element_get_flags(int eh);

extern i32 pfs_element_modify_flags(int eh, i32 rem_flags, i32 add_flags);

extern i64 pfs_element_get_create_time(int eh);

extern i64 pfs_element_get_last_modify_time(int eh);

extern int pfs_element_set_create_time(int eh, i64 ct);

extern int pfs_element_set_last_modify_time(int eh, i64 lmt);

#endif /* SRC_INCLUDE_PFS_ELEMENT_H_ */
