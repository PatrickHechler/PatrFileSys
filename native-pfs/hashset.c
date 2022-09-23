/*
 * hashset.c
 *
 *  Created on: 04.11.2021
 *      Author: Patrick
 */

#include <stdlib.h>
#include <stdio.h>

#define I_AM_HASH_SET
#include "hashset.h"

#define INIT_MAP_SIZE 17

static void* put0(struct hashset*, unsigned int, void*);
static void grow(struct hashset*);

extern void* hashset_get(const struct hashset *set, unsigned int hash, const void *equalto) {
	if (set->entrycount == 0) {
		return NULL;
	}
	int i = hash % set->setsize;
	const int origI = i;
	while (1) {
		if (!set->entries[i]) {
			return NULL;
		}
		if (set->entries[i] != &illegal) {
			if (set->hashmaker(set->entries[i]) == hash) {
				if (set->equalizer(set->entries[i], equalto)) {
					return set->entries[i];
				}
			}
		}
		i++;
		if (i >= set->setsize) {
			i = 0;
		}
		if (i == origI) {
			return NULL;
		}
	}
}

extern void* hashset_put(struct hashset *set, unsigned int hash, void *newvalue) {
	set->entrycount++;
	if (set->setsize <= set->entrycount * 1.5) {
		grow(set);
	}
	void *old = put0(set, hash, newvalue);
	if (old) {
		set->entrycount--;
	}
	return old;
}

extern void* hashset_remove(struct hashset *set, unsigned int hash, void *oldvalue) {
	if (set->setsize == 0) {
		return NULL;
	}
	int i = hash % (unsigned int) set->setsize;
	const int origI = i;
	while (1) {
		if (set->entries[i] == NULL) {
			return NULL;
		}
		if (set->hashmaker(set->entries[i]) == hash) {
			if (set->equalizer(set->entries[i], oldvalue)) {
				set->entrycount--;
				void *old = set->entries[i];
				if (set->entries[i + 1 == set->setsize ? 0 : i + 1] == NULL) {
					set->entries[i] = NULL;
				} else {
					set->entries[i] = &illegal;
				}
				return old;
			}
		}
		i++;
		if (i >= set->setsize) {
			i = 0;
		}
		if (i == origI) {
			grow(set);
			return NULL;
		}
	}
}

static void* put0(struct hashset *set, unsigned int hash, void *newvalue) {
	int i = hash % (unsigned int) set->setsize;
	while (1) {
		if ((set->entries[i] == NULL) || (set->entries[i] == &illegal)) {
			set->entries[i] = newvalue;
			return NULL;
		}
		if (set->hashmaker(set->entries[i]) == hash) {
			if (set->equalizer(set->entries[i], newvalue)) {
				void *old = set->entries[i];
				set->entries[i] = newvalue;
				return old;
			}
		}
		i++;
		if (i >= set->setsize) {
			i = 0;
		}
	}
}

static void grow(struct hashset *set) {
	const int oldlen = set->setsize;
	const int newsize = set->setsize = set->setsize ? set->setsize * 2 : INIT_MAP_SIZE;
	void **oldentries = set->entries;
	void *freePNTR = set->entries;
	void **newentries = set->entries = malloc(newsize * sizeof(void*));
	for (int i = 0; i < newsize; i++) {
		newentries[i] = NULL; //NULL entries are not permitted
	}
	for (int i = 0; i < oldlen; i++) {
		if (!oldentries[i]) {
			continue;
		}
		if (oldentries[i] == &illegal) {
			continue;
		}
		put0(set, set->hashmaker(oldentries[i]), oldentries[i]);
	}
	if (freePNTR) {
		free(freePNTR);
	}
}
