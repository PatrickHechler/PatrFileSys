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
