/*
 * hashmap.h
 *
 *  Created on: 04.11.2021
 *      Author: Patrick
 */

#ifndef HASHSET_H_
#define HASHSET_H_

#ifndef I_AM_HASH_SET
extern
#endif
/*
 * used to mark an removed entry
 * when iterating over all entries a non-NULL
 * check and a non-illegal check has to be made
 */
char illegal;

struct hashset {
	int entrycount;
	int setsize;
	int (*equalizer)(const void*, const void*);
	unsigned int (*hashmaker)(const void*);
	void **entries;
};

_Static_assert(sizeof(struct hashset) == 32);

extern void* hashset_get(const struct hashset *set, unsigned int hash, const void *other);

extern void* hashset_put(struct hashset *set, unsigned int hash, void *newval);

extern void* hashset_remove(struct hashset *set, unsigned int hash, void *oldval);

#endif /* HASHSET_H_ */
