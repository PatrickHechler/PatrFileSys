/*
 * hashset.c
 *
 *  Created on: 04.11.2021
 *      Author: Patrick
 */

#include <stdlib.h>
#include <stdio.h>

#include "hashset.h"

#define INIT_MAP_SIZE 17

static void* put0(struct hashset*, unsigned int, void*);

void* hashset_get(const struct hashset *set, unsigned int hash,
		const void *equalto) {
	if (!set->setsize) {
		return NULL;
	}
	int i = hash % set->setsize;
	while (1) {
		if (!set->entries[i]) {
			return NULL;
		}
		if (set->hashmaker(set->entries[i]) == hash) {
			if (set->equalizer(set->entries[i], equalto)) {
				return set->entries[i];
			}
		}
		i++;
		if (i >= set->setsize) {
			i = 0;
		}
	}
}

void* hashset_put(struct hashset *set, unsigned int hash, void *newvalue) {
	set->entrycount++;
	if (set->setsize <= set->entrycount * 1.5) {
		const int oldlen = set->setsize;
		const int newsize = set->setsize =
				set->setsize ? set->setsize * 2 : INIT_MAP_SIZE;
		void **oldentries = set->entries;
		void *freePNTR = set->entries;
		void **newentries = set->entries = malloc(newsize * sizeof(void*));
		for (int i = 0; i < newsize; i++) {
			newentries[i] = NULL; //NULL entries are not permitted
		}
		for (int i = 0; i < oldlen; i++) {
			if (oldentries[i]) {
				put0(set, set->hashmaker(oldentries[i]), oldentries[i]);
			}
		}
		if (freePNTR) {
			free(freePNTR);
		}
	}
	void *old = put0(set, hash, newvalue);
	if (old) {
		set->entrycount--;
	}
	return old;
}

void* hashset_remove(struct hashset *set, unsigned int hash, void *oldvalue) {
	int i = hash % (unsigned int) set->setsize;
	while (1) {
		if (set->entries[i] == NULL) {
			return NULL;
		}
		if (set->hashmaker(set->entries[i]) == hash) {
			if (set->equalizer(set->entries[i], oldvalue)) {
				set->entrycount--;
				void *old = set->entries[i];
				for (i++; i < set->setsize; i++) {
					if (set->entries[i] == NULL) {
						set->entries[i - 1] = NULL;
						break;
					}
					if ((set->hashmaker(set->entries[i]) % set->setsize) < i) {
						set->entries[i - 1] = set->entries[i];
					}
				}
				return old;
			}
		}
		i++;
		if (i >= set->setsize) {
			i = 0;
		}
	}
}

static void* put0(struct hashset *set, unsigned int hash, void *newvalue) {
	int i = hash % (unsigned int) set->setsize;
	while (1) {
		if (set->entries[i] == NULL) {
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
