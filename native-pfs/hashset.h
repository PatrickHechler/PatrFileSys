/*
 * hashmap.h
 *
 *  Created on: 04.11.2021
 *      Author: Patrick
 */

#ifndef HASHSET_H_
#define HASHSET_H_

struct hashset {
	int entrycount;
	int setsize;
	int (*equalizer)(const void*, const void*);
	int (*hashmaker)(const void*);
	void **entries;
};

void* hashset_get(const struct hashset *set, unsigned int hash, const void *other);

void* hashset_put(struct hashset *set, unsigned int hash, void *newval);

void* hashset_remove(struct hashset *set, unsigned int hash, void *newval);

#endif /* HASHSET_H_ */
