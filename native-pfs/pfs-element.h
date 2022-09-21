/*
 * pfs-element.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_ELEMENT_H_
#define PFS_ELEMENT_H_

#include "pfs.h"

/**
 * get the flags of a patr-file-system-element
 *
 * when the operation fails (ui64) -1 is returned and pfs_errno will be set
 */
extern ui64 pfs_element_get_flags(pfs_eh e);

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
extern int pfs_element_modify_flags(pfs_eh e, ui64 add_flags, ui64 rem_flags);

/**
 * get the length of the name in the UTF-8 charset without the '\0' terminating character
 *
 * if the operation fails -1 is returned
 *
 * if a handle for the root folder is given zero is returned
 */
extern i64 pfs_element_get_name_length(pfs_eh e);

/**
 * get the name of the element
 *
 * if buffer_len is zero a new buffer is allocated with malloc
 *
 * if the operation fails 0 is returned and on success 1
 */
extern int pfs_element_get_name(pfs_eh e, char **buffer, i64 *buffer_len);

/**
 * set the name of the given element
 *
 * if the operation fails 0 is returned and on success 1
 */
extern int pfs_element_set_name(pfs_eh e, char *name);

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
extern i64 pfs_element_get_create_time(pfs_eh e);

/**
 * set the create time of the given pfs-element
 */
extern int pfs_element_set_create_time(pfs_eh e, i64 new_time);

/**
 * get the last modify time of the given pfs-element
 *
 * when the operation fails (ui64) -1 is returned and pfs_errno will be set
 *
 * note that -1 may also be the last modify time of the pfs-element, so ensure that
 * pfs_errno is set to zero before calling this and than check with the pfs_errno
 */
extern i64 pfs_element_get_last_mod_time(pfs_eh e);

/**
 * set the last modify time of the given pfs-element
 */
extern int pfs_element_set_last_mod_time(pfs_eh e, i64 new_time);

// note that this function is implemented in pfs.c and not in pfs-element.c
/**
 * deletes the given pfs-element
 *
 * note that the pfs-element-handle needs to be manually freed
 */
extern int pfs_element_delete(pfs_eh e);

// note that this function is implemented in pfs-folder.c and not in pfs-element.c
/**
 * get the parent folder of the given element
 *
 * when the operation fails 0 is returned and
 * pfs_errno will be set, otherwise 1 is returned
 */
extern int pfs_element_get_parent(pfs_eh e);

#endif /* PFS_ELEMENT_H_ */
