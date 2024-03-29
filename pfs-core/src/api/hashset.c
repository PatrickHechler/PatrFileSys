//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.

/*
 * hashset.c
 *
 *  Created on: 04.11.2021
 *      Author: Patrick
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stddef.h>

#include "../pfs/hashset.h"

struct hs_list {
	void *datam1[0];
	uint64_t len;
	void *data[];
};

_Static_assert(offsetof(struct hs_list, data) == sizeof(void*), "Error!");

#define hs_one_bit_set(val) ((val & (val - 1)) == 0)

extern void* hashset_get(const struct hashset *set, uint64_t hash,
		const void *equalto) {
	int mi = set->maxi;
	if (!mi) {
		return NULL;
	}
	hash &= mi;
	int64_t entry = ((int64_t*) set->entries)[hash];
	if (entry > 0) {
		if (set->equalizer((void*) entry, equalto)) {
			return (void*) entry;
		}
		return NULL;
	} else if (entry < 0) {
		struct hs_list *list = (struct hs_list*) ~entry;
		for (uint64_t i = list->len; i; i--) { // possibly search the new elements more often
			if (set->equalizer(list->datam1[i], equalto)) {
				return list->datam1[i];
			}
		}
		return NULL;
	} else {
		return NULL;
	}
}

struct no_check_put_arg {
	uint64_t (*hashmaker)(const void*);
	int64_t *elements;
	uint64_t mi;
};

static int hs_no_check_put(void *arg0, void *element) {
	struct no_check_put_arg *arg = arg0;
	uint64_t hash = arg->hashmaker(element);
	hash &= arg->mi;
	int64_t es = arg->elements[hash];
	// no need to check for equal here
	if (es > 0) {
		struct hs_list *list = malloc(sizeof(void*) * 4);
		list->len = 2;
		list->data[0] = (void*) es;
		list->data[1] = element;
		arg->elements[hash] = ~(int64_t) list;
	} else if (es < 0) {
		struct hs_list *list = (struct hs_list*) ~es;
		uint64_t i = ++list->len;
		if (hs_one_bit_set(i)) {
			list = realloc(list, sizeof(void*) * 2 * list->len);
			arg->elements[hash] = ~(int64_t) list;
		}
		list->datam1[i] = element;
	} else {
		arg->elements[hash] = (int64_t) element;
	}
	return 1;
}

static inline void hs_free_old(struct hashset *set) {
	if (!set->maxi) {
		return;
	}
	for (uint64_t i = set->maxi + 1; i-- > 0;) {
		int64_t es = ((int64_t*) set->entries)[i];
		if (es < 0) {
			free((void*) ~es);
		}
	}
	free(set->entries);
}

static inline void* hashset_add_put(struct hashset *set, uint64_t hash,
		void *newvalue, _Bool also_replace) {
	if (set->maxi >> 1 <= set->entrycount) {
		uint64_t nmi = (set->maxi << 1) | 1;
		void *newEntries = calloc(nmi + 1, sizeof(void*));
		struct no_check_put_arg arg = { //
				/*	  */.elements = newEntries, //
						.hashmaker = set->hashmaker, //
						.mi = nmi //
				};
		hashset_for_each(set, hs_no_check_put, &arg);
		hs_free_old(set);
		set->entries = newEntries;
		set->maxi = nmi;
	}
	hash &= set->maxi;
	int64_t es = ((int64_t*) set->entries)[hash];
	if (es > 0) {
		if (set->equalizer((void*) es, newvalue)) {
			if (also_replace) {
				((void**) set->entries)[hash] = newvalue;
			}
			return (void*) es;
		}
		struct hs_list *list = malloc(sizeof(void*) * 4);
		list->len = 2;
		list->data[0] = (void*) es;
		list->data[1] = newvalue;
		((void**) set->entries)[hash] = (void*) ~(int64_t) list;
		set->entrycount++;
		return NULL;
	} else if (es < 0) {
		struct hs_list *list = (struct hs_list*) ~es;
		for (uint64_t i = list->len; i; i--) {
			if (set->equalizer(list->datam1[i], newvalue)) {
				void *res = list->datam1[i];
				if (also_replace) {
					list->datam1[i] = newvalue;
				}
				return res;
			}
		}
		uint64_t i = ++list->len;
		if (hs_one_bit_set(i)) {
			list = realloc(list, sizeof(void*) * 2 * list->len);
			((void**) set->entries)[hash] = (void*) ~(int64_t) list;
		}
		list->datam1[i] = newvalue;
		set->entrycount++;
		return NULL;
	} else {
		((void**) set->entries)[hash] = newvalue;
		set->entrycount++;
		return NULL;
	}
}

extern void* hashset_put(struct hashset *set, uint64_t hash, void *newvalue) {
	return hashset_add_put(set, hash, newvalue, 1);
}

extern void* hashset_add(struct hashset *set, uint64_t hash, void *addvalue) {
	return hashset_add_put(set, hash, addvalue, 0);
}

static inline void hs_shrink(struct hashset *set) {
	if (set->maxi >> 2 >= --set->entrycount) {
		if (set->entrycount) {
			uint64_t nmi = set->maxi >> 1;
			void *newEntries = calloc(nmi + 1, sizeof(void*));
			struct no_check_put_arg arg = { //
					/*	  */.elements = newEntries, //
							.hashmaker = set->hashmaker, //
							.mi = nmi //
					};
			hashset_for_each(set, hs_no_check_put, &arg);
			hs_free_old(set);
			set->entries = newEntries;
			set->maxi = nmi;
		} else {
			free(set->entries);
			set->entries = NULL;
			set->maxi = 0;
		}
	}
}

extern void* hashset_remove(struct hashset *set, uint64_t hash, void *oldvalue) {
	if (!set->entrycount) {
		return NULL;
	}
	hash &= set->maxi;
	int64_t es = ((int64_t*) set->entries)[hash];
	if (es > 0) {
		if (set->equalizer((void*) es, oldvalue)) {
			((void**) set->entries)[hash] = NULL;
			hs_shrink(set);
			return (void*) es;
		}
		return NULL;
	} else if (es < 0) {
		struct hs_list *list = (struct hs_list*) ~es;
		for (uint64_t i = list->len; i; i--) {
			if (set->equalizer(list->datam1[i], oldvalue)) {
				void *ov = list->datam1[i];
				if (list->len <= 2) {
					if (list->len == 1) {
						fputs("this should never happen\n", stderr);
						((int64_t*) set->entries)[hash] = 0;
					} else {
						((int64_t*) set->entries)[hash] =
								(int64_t) list->datam1[i == 1 ? 2 : 1];
					}
					free(list);
				} else {
					memmove(list->datam1 + i, list->datam1 + 1 + i,
							(list->len - i - 1) * sizeof(void*));
					if (hs_one_bit_set(list->len)) {
						list = realloc(list, sizeof(void*) * list->len);
					}
					list->len--;
				}
				hs_shrink(set);
				return ov;
			}
		}
		return NULL;
	} else {
		return NULL;
	}
}

extern void hashset_for_each(const struct hashset *set,
		int (*do_stuff)(void *arg0, void *element), void *arg0) {
	if (!set->entrycount) {
		return;
	}
	for (uint64_t i = set->maxi + 1; i-- > 0;) {
		int64_t val = ((int64_t*) set->entries)[i];
		if (val > 0) {
			if (!do_stuff(arg0, (void*) val)) {
				return;
			}
		} else if (val < 0) {
			struct hs_list *list = (struct hs_list*) ~val;
			for (uint64_t i = list->len; i > 0; i--) {
				if (!do_stuff(arg0, list->datam1[i])) {
					return;
				}
			}
		}
	}
}
