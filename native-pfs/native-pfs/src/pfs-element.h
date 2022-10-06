/*
 * pfs-element.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef NATIVE_PFS_SRC_PFS_ELEMENT_H_
#define NATIVE_PFS_SRC_PFS_ELEMENT_H_

#include "pfs-fs.h"

/**
 * get the flags of a patr-file-system-element
 *
 * when the operation fails (ui32) -1 is returned and pfs_errno will be set
 */
extern ui32 pfs_element_get_flags(int eh);

/**
 * modify the flags of a patr-file-system-element
 *
 * the add_flags will be set on the flags of the element
 *
 * the rem_flags will be cleared from the flags of the element
 *
 * the add_flags and rem_flags are not allowed to contain common bits (add_flags & rem_flags) must be zero
 *
 * the add_flags and rem_flags are not allowed to contain reserved bits ((add_flags | rem_flags) & PFS_FLAGS_RESERVED) must be zero
 *
 * when the operation succeeds 1 is returned
 *
 * when the operation fails 0 is returned and pfs_errno will be set
 */
extern int pfs_element_modify_flags(int eh, ui32 add_flags, ui32 rem_flags);

/**
 * get the length of the name in the UTF-8 charset without the '\0' terminating character
 *
 * if the operation fails -1 is returned
 *
 * if a handle for the root folder is given zero is returned
 */
extern i64 pfs_element_get_name_length(int eh);

/**
 * get the name of the element
 *
 * if buffer_len is zero a new buffer is allocated with malloc
 *
 * if the operation fails 0 is returned and on success 1
 */
extern int pfs_element_get_name(int eh, char **buffer, i64 *buffer_len);

/**
 * set the name of the given element
 *
 * if the operation fails 0 is returned and on success 1
 */
extern int pfs_element_set_name(int eh, char *name);

/**
 * get the create time of the given pfs-element
 *
 * when the operation fails PFS_NO_TIME will be
 * returned and pfs_errno will be set
 *
 * note that PFS_NO_TIME may also be the create
 * time of the pfs-element, so ensure that pfs_errno
 * is set to zero before calling this and than check
 * with the pfs_errno and not with the return value
 */
extern i64 pfs_element_get_create_time(int eh);

/**
 * set the create time of the given pfs-element
 */
extern int pfs_element_set_create_time(int eh, i64 new_time);

/**
 * get the last modify time of the given pfs-element
 *
 * when the operation fails (ui64) -1 is returned and pfs_errno will be set
 *
 * note that -1 may also be the last modify time of the pfs-element, so ensure that
 * pfs_errno is set to zero before calling this and than check with the pfs_errno
 */
extern i64 pfs_element_get_last_mod_time(int eh);

/**
 * set the last modify time of the given pfs-element
 */
extern int pfs_element_set_last_mod_time(int eh, i64 new_time);

/*
 * note that the following functions are implemented in pfs-folder.c
 * and not in pfs-element.c.
 *
 * this is because they need to access the folder structure.
 */

/**
 * deletes the given pfs-element
 *
 * note that the pfs-element-handle needs to be manually freed
 */
extern int pfs_element_delete(int eh);

/**
 * get the parent folder of the given element
 *
 * when the operation fails 0 is returned, otherwise 1 is returned
 */
extern int pfs_element_get_parent(int eh);

/**
 * set the parent folder of the given element
 * to new_parent_handle
 *
 * when the operation fails 0 is returned and
 * pfs_errno will be set, otherwise 1 is returned
 */
extern int pfs_element_set_parent(int eh, int new_parent_handle);

/**
 * set the parent folder of the given element
 * to new_parent and also sets the name to name
 *
 * this operation is very similar to
 * pfs_element_set_parent(eh, new_parent_handle) and
 * pfs_element_set_name(eh, name) together. This operation
 * can be used when the new_parent_hanlde maybe/definitely
 * contains a child with the old name
 *
 * when the operation fails 0 is returned and
 * pfs_errno will be set, otherwise 1 is returned
 */
extern int pfs_element_move(int eh, int new_parent_handle, char *name);

#endif /* NATIVE_PFS_SRC_PFS_ELEMENT_H_ */
