/*
 * pfs.h
 *
 *  Created on: Jul 6, 2022
 *      Author: pat
 */

#ifndef PFS_H_
#define PFS_H_

#include "patr-file-sys.h"
#include "pfs-constants.h"
#include "bm.h"
#include <stddef.h>

extern void pfs_init();

#define pfs_new_ram_fs(block_count, block_size) \
	struct pfs_file_sys *pfs = malloc(sizeof(struct pfs_file_sys));\
	if (pfs == NULL) {\
		abort();\
	}\
	pfs->bm = bm_new_ram_block_manager(block_count, block_size);\
	pfs->lock = 0;\
	pfs->block = 0;

#define pfs_new_file_fs(file, block_size) \
	struct pfs_file_sys *pfs = malloc(sizeof(struct pfs_file_sys));\
	if (pfs == NULL) {\
		abort();\
	}\
	pfs->bm = bm_new_file_block_manager(file, block_size);\
	pfs->lock = 0;\
	pfs->block = 0;

struct pfs_file_sys {
	/**
	 * the block manager
	 */
	struct bm_block_manager *bm;
	/**
	 * the lock used when working with the file system
	 */
	i64 lock;
	/**
	 * for internal use
	 */
	i32 block;
};

struct pfs_element {
	/**
	 * the id of the pfs-element
	 */
	i64 id;
	/**
	 * the lock to be used when working with the pfs element
	 */
	i64 lock;
	/**
	 * for internal use
	 */
	i64 block;
	/**
	 * for internal use
	 */
	i32 pos;
};

/**
 * creates a new patr-file system.
 *
 * the block size will be read from the block manager
 *
 * returns 1 on success and zero if it fails
 */
extern int pfs_fs_format(struct pfs_file_sys *pfs, i64 block_count);

/**
 * changes the block count
 *
 * returns 1 on success and zero if there are blocks in use which would be outside of the new boundary
 */
extern int pfs_fs_set_block_count(struct pfs_file_sys *pfs, i64 block_count);

/**
 * locks the patr-file-system with the given lock data
 */
extern void pfs_fs_lock_fs(struct pfs_file_sys *pfs, i64 lock_data);

/**
 * reads the block count from the patr-file-system
 */
extern i64 pfs_fs_block_count(struct pfs_file_sys *pfs);

/**
 * reads the block size from the patr-file-system
 */
extern i32 pfs_fs_block_size(struct pfs_file_sys *pfs);

/**
 * sets the pfs-element to its parent folder
 *
 * returns 1 on success and 0 when the given element is the root folder
 */
extern int pfs_element_get_parent(struct pfs_element *element);

/**
 * moves the given element
 *
 * if new_name is NULL the name is not changed
 *
 * if new_parent_ID is INVALID_ID the parent is not changed
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_element_move(struct pfs_element *element, wchar_t *new_name,
		i64 new_parent_ID);

/**
 * returns the flags of this element
 *
 * returns the flags on success
 * returns -1 on error
 */
extern i32 pfs_element_get_flags(struct pfs_element *element);

/**
 * changes the flags of this element
 *
 * the add and rem flags are not allowed to contain the flags PFS_FLAG_FOLDER, PFS_FLAG_FILE and PFS_FLAG_LINK
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_element_get_flag(struct pfs_element *element, i32 add, i32 rem);

/**
 * gets the create time of the given element
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_element_get_create(struct pfs_element *element, i64 *time);

/**
 * gets the last modification time of the given element
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_element_get_las_mod(struct pfs_element *element, i64 *time);

/**
 * gets the last meta modification time of the given element
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_element_get_last_meta_mod(struct pfs_element *element, i64 *time);

/**
 * sets the create time of the given element
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_element_set_create(struct pfs_element *element, i64 time);

/**
 * sets the last modification time of the given element
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_element_set_las_mod(struct pfs_element *element, i64 time);

/**
 * sets the last meta modification time of the given element
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_element_set_last_meta_mod(struct pfs_element *element, i64 time);

/**
 * returns the lock data of the given element or PFS_NO_LOCK if the is currently no lock
 *
 * returns the lock data on success
 * returns -1 on error
 */
extern i64 pfs_element_get_lock_data(struct pfs_element *element);
/**
 * gets the time this element was locked or PFS_NO_TIME if the element is not locked
 *
 * returns 1 success
 * returns 0 on error
 */
extern int pfs_element_get_lock_time(struct pfs_element *element, i64 *time);

/**
 * locks from the given element
 *
 * returns 1 success
 * returns 0 on error
 */
extern int pfs_element_lock(struct pfs_element *element, i64 lock);

/**
 * removes the lock from the given element
 *
 * returns 1 success
 * returns 0 on error
 */
extern int pfs_element_remove_lock(struct pfs_element *element, i64 lock);

/**
 * deletes the given element
 *
 * a non empty folder can not be deleted
 *
 * returns 1 success
 * returns 0 on error
 */
extern int pfs_element_delete(struct pfs_element *element);

/**
 * gets the name of the given element
 *
 * if the elements-name needs more bytes than size, name is reallocated
 *
 * if size is zero name is allocated
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_element_get_name(struct pfs_element *element, i32 size, wchar_t **name);

/**
 * reads the files content from off len bytes
 *
 * returns the number of bytes actually read on success
 * returns -1 on error and 0 on EOF (except len is also 0)
 */
extern i64 pfs_file_read(struct pfs_element *file, i64 off, void *data, i64 len);

/**
 * writes the files content from off len bytes
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_file_write(struct pfs_element *file, i64 off, void *data, i64 len);

/**
 * appends len bytes to the file
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_file_append(struct pfs_element *file, void *data, i64 len);

/**
 * truncates the files content.
 *
 * if the file is currently smaller than new_len, this function will fail
 *
 * if the file has the same length than new_len, this function has no effect
 *
 * if the file is currently larger than new_len, this function will remove bytes
 * from the end of the file, until the files length is new_len matches
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_file_truncate(struct pfs_element *file, i64 new_len);

/**
 * adds a child folder to this folder
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_folder_add_folder(struct pfs_element *folder, wchar_t *name);

/**
 * adds a child file to this folder
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_folder_add_file(struct pfs_element *folder, wchar_t *name);

/**
 * adds a child link to this folder
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_folder_add_link(struct pfs_element *folder, wchar_t *name, i64 targetID);

/**
 * returns the child count of this folder
 *
 * returns the child count on success
 * returns -1 on error
 */
extern i32 pfs_folder_child_count(struct pfs_element *folder);

/**
 * changes the given folder to its child with the given name
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_folder_get_child_name(struct pfs_element *folder, wchar_t *name);

/**
 * changes the given folder to its child with the given index in the child list
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_folder_get_child_index(struct pfs_element *folder, i32 index);

/**
 * changes the given link to its target
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_link_get_target(struct pfs_element *link);

/**
 * sets the links target
 *
 * returns 1 on success
 * returns 0 on error
 */
extern int pfs_link_set_target(struct pfs_element *link, i64 targetID);

#define __pfs_element_check_file_or_folder(element, file_or_folder_flag, error){\
	i32 __flags = pfs_element_get_flags(element);\
	if (__flags == -1) {\
		error\
	}\
	if (__flags & file_or_folder_flag) {\
		if (flags & PFS_FLAG_LINK) {\
			if (!pfs_link_get_target(element)) {\
				error\
			}\
		}\
	} else {\
		error\
	}\
}

/**
 * checks that the given element is a folder
 *
 * this macro will dereference a link if the given element is a link to a folder
 *
 * executes error if an error occurs or the element is no file (or folder-link)
 */
#define pfs_element_check_folder(element, error) __pfs_element_check_file_or_folder(element, PFS_FLAG_FOLDER, error)

/**
 * checks that the given element is a file
 *
 * this macro will dereference a link if the given element is a link to a file
 *
 * executes error if an error occurs or the element is no file (or file-link)
 */
#define pfs_element_check_file(element, error) __pfs_element_check_file_or_folder(element, PFS_FLAG_FILE, error)

/**
 * checks that the given element is a file
 *
 * this macro will dereference a link if the given element is a link to a file
 *
 * executes error if an error occurs or the element is no link
 */
#define pfs_element_check_link(element, error) {\
	i32 __flags = pfs_element_get_flags(element);\
	if (__flags == -1) {\
		error\
	}\
	if ((flags & PFS_FLAG_LINK) == 0) {\
		error\
	}\
}

#endif /* PFS_H_ */
