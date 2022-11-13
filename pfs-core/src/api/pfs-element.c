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
	return_handle(pfs_eh_len, pfs_ehs, pfs_ehs[eh]->parent)
}

extern int pfs_element_delete(int eh) {
	eh(0)
	int res = pfsc_element_delete(&pfs_ehs[eh]->handle);
	if (res) {
		struct element_handle *e = pfs_ehs[eh];
		for (int i = 0; i < pfs_eh_len; i++) {
			if (!pfs_ehs[i]) {
				continue;
			}
			if (e == pfs_ehs[i]) {
				pfs_ehs[i] = NULL;
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
	return pfsc_element_get_flags(&pfs_ehs[eh]->handle);
}

extern int pfs_element_modify_flags(int eh, i32 add_flags, i32 rem_flags) {
	eh(0)
	return pfsc_element_modify_flags(&pfs_ehs[eh]->handle, add_flags, rem_flags);
}

extern i64 pfs_element_get_create_time(int eh) {
	eh(-1)
	return pfsc_element_get_create_time(&pfs_ehs[eh]->handle);
}

extern i64 pfs_element_get_last_modify_time(int eh) {
	eh(-1)
	return pfsc_element_get_last_mod_time(&pfs_ehs[eh]->handle);
}

extern int pfs_element_set_create_time(int eh, i64 ct) {
	eh(0)
	return pfsc_element_set_create_time(&pfs_ehs[eh]->handle, ct);
}

extern int pfs_element_set_last_modify_time(int eh, i64 lmt) {
	eh(0)
	return pfsc_element_set_last_mod_time(&pfs_ehs[eh]->handle, lmt);
}

extern int pfs_element_get_name(int eh, char **name_buf, i64 *buf_len) {
	eh(0)
	return pfsc_element_get_name(&pfs_ehs[eh]->handle, name_buf, buf_len);
}

extern int pfs_element_set_name(int eh, char *name) {
	eh(0)
	return pfsc_element_set_name(&pfs_ehs[eh]->handle, name);
}

extern int pfs_element_set_parent(int eh, int parenteh) {
	eh(0)
	get_eh(0, parenteh)
	return pfsc_element_set_parent(&pfs_ehs[eh]->handle, &pfs_ehs[parenteh]->handle);
}

extern int pfs_element_move(int eh, int parenteh, char* name) {
	eh(0)
	get_eh(0, parenteh)
	return pfsc_element_move(&pfs_ehs[eh]->handle, &pfs_ehs[parenteh]->handle, name);
}

extern int pfs_element_same(int aeh, int beh) {
	get_eh(-1, aeh)
	get_eh(-1, beh)
	return (pfs_ehs[aeh] == pfs_ehs[beh]) ? 1 : 0;
}
