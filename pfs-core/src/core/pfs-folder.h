/*
 * pfs-folder.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef SRC_CORE_PFS_FOLDER_H_
#define SRC_CORE_PFS_FOLDER_H_

#include "pfs.h"

/**
 * set the given folder iterator to the given index
 */
int pfsc_folder_fill_iterator_index(pfs_eh f, pfs_fi iter, i64 index);

/**
 * creates a new folder iterator
 *
 * if show_hidden is zero hidden elements will be skipped
 *
 * if show_hidden has a non-zero value hidden elements will not be skipped
 *
 * the returned iterator will use the given pfs-element-handle
 *
 * if this operation fails NULL is returned
 */
pfs_fi pfsc_folder_iterator(pfs_eh f, int show_hidden);

/**
 * creates a new folder iterator
 *
 * if show_hidden is zero hidden elements will be skipped
 *
 * if show_hidden has a non-zero value hidden elements will not be skipped
 *
 * the returned iterator will use the given pfs-element-handle
 *
 * if this operation fails 0 is returned otherwise 1
 */
int pfsc_folder_fill_iterator(pfs_eh f, pfs_fi iter, int show_hidden);

/**
 * get the next element of the given iterator
 *
 * note that the pfs-element-handle which was used to create this iterator will
 * be used to return the elements of the iterator
 *
 * returns 1 on success, 0 if no more elements are there or an error occurred
 *
 * if there are no more elements pfs_errno will be set to PFS_ERRNO_NO_MORE_ELEMENTS
 */
int pfsc_folder_iter_next(pfs_fi fi);

/**
 * get the number of child elements from this folder
 *
 * of error -1 is returned
 */
i64 pfsc_folder_child_count(pfs_eh f);

/**
 * get the child element with the given name
 *
 * on success a non-zero value will be returned and on error zero
 */
int pfsc_folder_child_from_name(pfs_eh f, const char *name);

/**
 * get the child folder with the given name
 *
 * if there is a child entry with the given name, but the entry
 * is no folder this operation will fail
 *
 * on success a non-zero value will be returned and on error zero
 */
int pfsc_folder_folder_child_from_name(pfs_eh f, const char *name);

/**
 * get the child file with the given name
 *
 * if there is a child entry with the given name, but the entry
 * is no file this operation will fail
 *
 * on success a non-zero value will be returned and on error zero
 */
int pfsc_folder_file_child_from_name(pfs_eh f, const char *name);

/**
 * get the child pipe with the given name
 *
 * if there is a child entry with the given name, but the entry
 * is no pipe this operation will fail
 *
 * on success a non-zero value will be returned and on error zero
 */
int pfsc_folder_pipe_child_from_name(pfs_eh f, const char *name);

/**
 * adds a new child folder to this folder
 *
 * note that the pfs-element-handle will be a handle of the child folder after this
 * call (only when successful, when not successfully the handle will remain valid)
 *
 * if parent is not NULL parent will be made to an element-handle of the parent
 * folder, if parent is NULL it is ignored. this can be used when the parent will
 * later be needed, because a duplicated handle of the parent may become invalid
 * with this function call. note that the folder may even be moved even if the
 * call fails. in all cases parent will, when it has a non-NULL value, be made to
 * an element handle of the element which is currently handled by f
 *
 * if the function fails f will remain a valid element handle
 *
 * on success a non-zero value will be returned and on error zero
 */
int pfsc_folder_create_folder(pfs_eh f, pfs_eh parent, const char *name);

/**
 * adds a new child file to this folder
 *
 * note that the pfs-element-handle will be a handle of the child file after this
 * call (only when successful, when not successfully the handle will remain valid)
 *
 * if parent is not NULL parent will be made to an element-handle of the parent
 * folder, if parent is NULL it is ignored. this can be used when the parent will
 * later be needed, because a duplicated handle of the parent may become invalid
 * with this function call. note that the folder may even be moved even if the
 * call fails. in all cases parent will, when it has a non-NULL value, be made to
 * an element handle of the element which is currently handled by f
 *
 * if the function fails f will remain a valid element handle
 *
 * on success a non-zero value will be returned and on error zero
 */
int pfsc_folder_create_file(pfs_eh f, pfs_eh parent, const char *name);

/**
 * adds a new child pipe to this folder
 *
 * note that the pfs-element-handle will be a handle of the child pipe after this
 * call (only when successful, when not successfully the handle will remain valid)
 *
 * if parent is not NULL parent will be made to an element-handle of the parent
 * folder, if parent is NULL it is ignored. this can be used when the parent will
 * later be needed, because a duplicated handle of the parent may become invalid
 * with this function call. note that the folder may even be moved even if the
 * call fails. in all cases parent will, when it has a non-NULL value, be made to
 * an element handle of the element which is currently handled by f
 *
 * if the function fails f will remain a valid element handle
 *
 * on success a non-zero value will be returned and on error zero
 */
int pfsc_folder_create_pipe(pfs_eh f, pfs_eh parent, const char *name);

#endif /* SRC_CORE_PFS_FOLDER_H_ */
