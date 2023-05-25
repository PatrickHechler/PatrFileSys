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
 * hashmap.h
 *
 *  Created on: 04.11.2021
 *      Author: Patrick
 */

#ifndef HASHSET_H_
#define HASHSET_H_

#include <stdint.h>

struct hashset {
	void *entries;
	uint64_t maxi; // if set to zero the set size is zero
	int (*equalizer)(const void*, const void*);
	uint64_t (*hashmaker)(const void*);
	uint64_t entrycount;
};

_Static_assert(sizeof(uint64_t) == sizeof(void*), "Error!");

/**
 * returns the entry which is equal to the given entry and in the given set
 *
 * if the set contains no such entry NULL is returned
 */
extern void* hashset_get(const struct hashset *set, uint64_t hash,
		const void *other);

/**
 * adds/overwrites the entry with the given hash and value
 *
 * this function returns the previous entry (equal to the given entry) or NULL if there was no such entry perviusly
 */
extern void* hashset_put(struct hashset *set, uint64_t hash, void *newval);

/**
 * removes the entry with the given hash and value
 *
 * this function returns the previous entry (equal to the given entry) or NULL if no such entry was found
 */
extern void* hashset_remove(struct hashset *set, uint64_t hash,
		void *oldval);

/**
 * execute the given function with each argument in this set.
 *
 * this operation will pass the elements in the set to the function until
 * the function returns zero/false or all elements were passed to it
 */
extern void hashset_for_each(const struct hashset *set,
		int (*do_stuff)(void *arg0, void *element), void *arg0);

#endif /* HASHSET_H_ */
