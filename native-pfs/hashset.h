/*
 * hashmap.h
 *
 *  Created on: 04.11.2021
 *      Author: Patrick
 */

#ifndef HASHSET_H_
#define HASHSET_H_

#ifdef I_AM_HASH_SET
char illegal;
#else
extern char illegal;
#endif

struct hashset {
	int entrycount;
	int setsize;
	int (*equalizer)(const void*, const void*);
	int (*hashmaker)(const void*);
	void **entries;
};

extern void* hashset_get(const struct hashset *set, unsigned int hash, const void *other);

extern void* hashset_put(struct hashset *set, unsigned int hash, void *newval);

extern void* hashset_remove(struct hashset *set, unsigned int hash, void *newval);

#endif /* HASHSET_H_ */
