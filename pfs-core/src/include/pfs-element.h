/*
 * pfs-element.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_ELEMENT_H_
#define SRC_INCLUDE_PFS_ELEMENT_H_

#include "patr-file-sys.h"

/*
 * closes the given element handle
 *
 * after this operation the eh-ID may be reused by the system when a new handle is opened
 *
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_close(int eh);

/*
 * returns a handle for the parent folder of the given handle
 * on error -1 is returned
 */
extern int pfs_element_parent(int eh);

/*
 * deletes the element of the given handle
 *
 * this operation automatically closes the element handle (even if this function fails)
 *
 * after this operation the eh-ID may be reused by the system when a new handle is opened
 *
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_delete(int eh);

/*
 * returns the flags of the element
 * on error -1 is returned
 */
extern ui32 pfs_element_get_flags(int eh);

/*
 * modifies the flags of the given element
 *
 * - rem_flags and add_flags are not allowed to contain common bits
 *     (rem_flags & add_flags) has to be zero
 * - rem_flags and add_flags are not allowed to contain unmodifiable bits
 *     ((rem_flags | add_flags) & PFS_UNMODIFIABLE_FLAGS) has to be zero
 *
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_modify_flags(int eh, ui32 add_flags, ui32 rem_flags);

/*
 * returns the create time of the given element
 * on error -1 is returned
 */
extern i64 pfs_element_get_create_time(int eh);

/*
 * returns the last modify time of the given element
 * on error -1 is returned
 */
extern i64 pfs_element_get_last_modify_time(int eh);

/*
 * sets the create time of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_set_create_time(int eh, i64 ct);

/*
 * sets the last modify time of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_set_last_modify_time(int eh, i64 lmt);

/*
 * get the name of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_get_name(int eh, char **name_buf, i64 *buf_len);

/*
 * set the name of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_set_name(int eh, char *name);

/*
 * set the parent of the given element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_set_parent(int eh, int parenteh);

/*
 * sets the name and the parent of the element
 * on success 1 and on error 0 is returned
 */
extern int pfs_element_move(int eh, int parenteh, char *name);

/*
 * returns 1 if both element handles are valid and refer to the same element
 * returns 0 if both element handles are valid, but do not refer to the same element
 * returns -1 if at least one not a valid element handle
 */
extern int pfs_element_same(int aeh, int beh);

#endif /* SRC_INCLUDE_PFS_ELEMENT_H_ */
