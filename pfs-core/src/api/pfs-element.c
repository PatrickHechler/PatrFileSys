/*
 * pfs-element.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#include "pfs.h"
#include "../include/pfs-element.h"

extern int pfs_element_parent(int eh) {
	eh(-1)
	return_handle(eh_len, ehs, ehs[eh]->parent)
}

extern int pfs_element_delete(int eh) {
	eh(0)
	int res = pfsc_element_delete(&ehs[eh]->handle);
	if (res) {
		struct element_handle *e = ehs[eh];
		for (int i = 0; i < eh_len; i++) {
			if (!ehs[i]) {
				continue;
			}
			if (e == ehs[i]) {
				ehs[i] = NULL;
			}
		}
		hashset_remove(&e->parent->children, eh_hash(e), e);
		for (struct element_handle *a = e->parent, *b; a != NULL && has_refs(a);
				a = b) {
			b = a->parent;
			hashset_remove(&b->children, eh_hash(a), a);
			free(a);
		}
		free(e);
	}
	return res;
}

extern i32 pfs_element_get_flags(int eh) {
	eh(-1)
	return pfsc_element_get_flags(&ehs[eh]->handle);
}

extern int pfs_element_modify_flags(int eh, i32 add_flags, i32 rem_flags) {
	eh(0)
	return pfsc_element_modify_flags(&ehs[eh]->handle, add_flags, rem_flags);
}

extern i64 pfs_element_get_create_time(int eh) {
	eh(-1)
	return pfsc_element_get_create_time(&ehs[eh]->handle);
}

extern i64 pfs_element_get_last_modify_time(int eh) {
	eh(-1)
	return pfsc_element_get_last_mod_time(&ehs[eh]->handle);
}

extern int pfs_element_set_create_time(int eh, i64 ct) {
	eh(0)
	return pfsc_element_set_create_time(&ehs[eh]->handle, ct);
}

extern int pfs_element_set_last_modify_time(int eh, i64 lmt) {
	eh(0)
	return pfsc_element_set_last_mod_time(&ehs[eh]->handle, lmt);
}
