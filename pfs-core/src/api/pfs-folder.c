/*
 * pfs-folder.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#include "pfs.h"
#include "../include/pfs-folder.h"

#define ch(err_ret) \
	eh(err_ret) \
	c_h(pfs_ehs[eh], err_ret, PFS_F_FOLDER)

#define cr(err_ret) c_r(pfs_ehs[eh], err_ret)

extern i64 pfs_folder_child_count(int eh) {
	ch(-1)
	int res = pfsc_folder_child_count(&pfs_ehs[eh]->handle);
	cr(-1)
	return res;
}

#define get_child \
	{ \
		struct element_handle *oc = hashset_get(&pfs_ehs[eh]->children, eh_hash(c), c); \
		if (oc) { \
			free(c); \
			oc->load_count++; \
			c = oc; \
		} else { \
			c->children.entrycount = 0; \
			c->children.setsize = 0; \
			c->children.equalizer = childset_equal; \
			c->children.hashmaker = childset_hash; \
			c->children.entries = NULL; \
			c->load_count = 1; \
			c->parent = pfs_ehs[eh]; \
			hashset_put(&pfs_ehs[eh]->children, eh_hash(c), c); \
		} \
	}

#define pfs_folder_child_impl(type) \
	ch(-1) \
	struct element_handle *c = malloc(sizeof(struct element_handle)); \
	if (!c) { \
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY; \
		cr(-1) \
		return -1; \
	} \
	c->handle = pfs_ehs[eh]->handle; \
	if (!pfsc_folder_##type##child_from_name(&c->handle, name)) { \
		cr(-1) \
		return -1; \
	} \
	get_child \
	cr(-1) \
	return_handle(pfs_eh_len, pfs_ehs, c)

extern int pfs_folder_child(int eh, const char *name) {
	pfs_folder_child_impl()
}

extern int pfs_folder_child_folder(int eh, const char *name) {
	pfs_folder_child_impl(folder_)
}

extern int pfs_folder_child_file(int eh, const char *name) {
	pfs_folder_child_impl(file_)
}

extern int pfs_folder_child_pipe(int eh, const char *name) {
	pfs_folder_child_impl(pipe_)
}

#define pfs_folder_create(type) \
	ch(-1) \
	struct element_handle *c = malloc(sizeof(struct element_handle)); \
	if (!c) { \
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY; \
		cr(-1) \
		return -1; \
	} \
	c->handle = pfs_ehs[eh]->handle; \
	if (!pfsc_folder_create_##type(&c->handle, &pfs_ehs[eh]->handle, name)) { \
		cr(-1) \
		return -1; \
	} \
	get_child \
	cr(-1) \
	return_handle(pfs_eh_len, pfs_ehs, c)

extern int pfs_folder_create_folder(int eh, const char *name) {
	pfs_folder_create(folder)
}

extern int pfs_folder_create_file(int eh, const char *name) {
	pfs_folder_create(file)
}

extern int pfs_folder_create_pipe(int eh, const char *name) {
	pfs_folder_create(pipe)
}
