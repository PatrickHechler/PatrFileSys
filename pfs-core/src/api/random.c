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
 * random.c
 *
 * ACORN
 * see http://acorn.wikramaratna.org/concept.html
 *
 *  Created on: Jul 17, 2023
 *      Author: pat
 */
#define I_AM_RANDOM
#include "../include/random.h"

#include <endian.h>
#include <time.h>
#include <stdlib.h>
#include <stdio.h>

#if !defined PFS_PORTABLE_BUILD
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#endif // half- or non-portable

#if !defined PFS_PORTABLE_BUILD && !defined PFS_HALF_PORTABLE_BUILD
#include <sys/random.h>
#endif // non-portable

static uint64_t k;
static pfs_uint128_t *Y = NULL;

// M is 2^120
// Mm1 holds the high bits of 2^120-1
static const uint64_t Mm1 = UINT64_C(0x00FFFFFFFFFFFFFF); //2^56-1 (interpreted as 2^120-1)

#define DEFAULT_ENTRY_COUNT 32

void random_ensure_init() {
	if (Y) {
		return;
	}
	void *data = malloc(DEFAULT_ENTRY_COUNT * sizeof(pfs_uint128_t));
	/*
	 * 1: try getrandom()
	 * 2: try /dev/urandom
	 * 3: use srandom() and random()
	 */
	void *p = data;
	size_t remain = DEFAULT_ENTRY_COUNT * sizeof(pfs_uint128_t);
#if !defined PFS_PORTABLE_BUILD && !defined PFS_HALF_PORTABLE_BUILD
	while (remain) {
		ssize_t len = getrandom(p, remain, 0);
		if (len == -1) {
			int e = errno;
			errno = 0;
			switch (e) {
			case EINTR:
				continue;
			default:
				fprintf(stderr,
						"[WARN]: unknown/impossible errno value after getrandom: %s (%d)\n"
								"  I will treat this like ENOSYS\n",
						strerror(e), e);
				/* no break */
			case ENOSYS:
#endif // non-portable // now also allow half portable
#if !defined PFS_PORTABLE_BUILD
				int fd = open("/def/urandom", O_RDONLY);
				if (fd == -1) {
					portable_rnd: ;
#endif // half- or non-portable
					srandom(time(NULL));
					while (remain--) {
						*(char*) p++ = random();
					}
#if !defined PFS_PORTABLE_BUILD
					goto after_rnd_data;
				}
				while (remain) {
					ssize_t reat = read(fd, p, remain);
					if (reat == -1) {
						int e = errno;
						errno = 0;
						switch (e) {
						case EAGAIN:
#if EAGAIN != EWOULDBLOCK
						case EWOULDBLOCK:
#endif
						case EINTR:
							continue;
						default:
							goto portable_rnd;
						}
					} else if (reat) {
						remain -= reat;
						p += reat;
					} else {
						fprintf(stderr, "I got EOF on /dev/urandom!\n");
						abort();
					}
				}
				goto after_rnd_data;
#endif // half-portable or non-portable
#if !defined PFS_PORTABLE_BUILD && !defined PFS_HALF_PORTABLE_BUILD
			}
		}
		remain -= len;
		p += len;
	}
#endif // non-portable
	after_rnd_data: ; // unused when portable
	*(char*) data |= 1;
	for (int i = 1; i < (DEFAULT_ENTRY_COUNT << 1); i += 2) {
		if (((int64_t*) data)[i] & ~Mm1) {
			((int64_t*) data)[i] &= Mm1;
		}
	}
	Y = data;
	k = DEFAULT_ENTRY_COUNT - 1;
}

void random_init(uint64_t seed_entries, pfs_uint128_t *seed) {
	if (seed_entries <= 1) {
		fputs(
				"I need at least two 128-bit entries in the seed (a minimum of 11 is recommended)\n",
				stderr);
		abort();
	}
	if (seed_entries < 11) {
		fprintf(stderr,
				"[WARN]: at least 11 128-bit seed entries are recommended (there are %d)\n",
				(int) seed_entries);
	}
	if ((1 & (*seed)) == 0) {
		fputs(
				"[WARN]: the first number of the seed is even (I will or it with 1)\n",
				stderr);
		*seed |= 1;
	}
	// truncate all numbers in Y to max 2^120-1
	_Bool already_warned = 0;
#if BYTE_ORDER == LITTLE_ENDIAN || !defined  PFS_COMPILER_INT128
	for (int i = 1; i < (seed_entries << 1); i += 2)
#else
	for (int i = 0; i < seed_entries; i ++)
#endif
			{
#if BYTE_ORDER == LITTLE_ENDIAN || !defined  PFS_COMPILER_INT128
		if (((int64_t*) seed)[i] & ~Mm1) {
			if (already_warned) {
				fputs(
						"[WARN]: at least one number in the seed is not below 2^120\n",
						stderr);
			}
			already_warned = 1;
			((int64_t*) seed)[i] &= Mm1;
		}
#else
		if ( ( (uint64_t) (seed[i] >> 64) ) & ~Mm1) {
			if (already_warned) {
				fputs(
						"[WARN]: at least one number in the seed is not below 2^120\n",
						stderr);
			}
			already_warned = 1;
			res = ((uint64_t) seed[i]) | (((seed[i] >> 64) & Mm1) << 64);
		}
#endif
	}
	void *old = Y;
	void (*rf)(void*) = random_free;
	Y = seed;
	k = seed_entries;
	random_free = free;
	if (old) {
		if (rf) {
			rf(old);
		}
	}
}

pfs_uint128_t random_num0() {
	pfs_uint128_t res = Y[0];
	for (int_fast64_t i = 1; i < k; i++) {
#ifdef PFS_COMPILER_INT128
		res += Y[i];
		res = ((uint64_t) res) | (((res >> 64) & Mm1) << 64);
#else // res %= 2^120
		if (res.low & Y[i].low & INT64_MIN) {
			res.high++;
		}
		res.low += Y[i].low;
		res.high += Y[i].high;
		res.high &= Mm1;
#endif // PFS_COMPILER_INT128
		Y[i] = res;
	}
	return res;
}

void random_data(void *data, size_t len) {
	if (((long) data) & 7) { // align data
		if (len == 0) {
			return;
		}
		uint64_t val = random_num();
		do {
			*(char*) data = val;
			val >>= 8;
		} while ((((long) ++data) & 7) && --len);
	}
	for (; len >= 8; len -= 8, data += 8) {
		*(uint64_t*) data = random_num();
	}
	if (len) {
		uint64_t val = random_num();
		do {
			*(char*) data = val;
			val >>= 8;
			data++;
		} while (--len);
	}
}

uint64_t random_num() {
	// do a right shift because the lower bits are less random
#ifdef PFS_COMPILER_INT128
	return random_num0() >> 56;
#else
	pfs_uint128_t res = random_num0();
	return (res.low >> 56) | (res.high << 8);
#endif
}

long double random_num_ld() {
	pfs_uint128_t val = random_num0();
#ifdef PFS_COMPILER_INT128
	if (val == 0)
		return 1.0L; // prevent NaN
	return 1.0L / val;
#else
	if (val.low == 0 && val.high == 0) return 1.0L; // prevent NaN
	return (1.0L / val.low) + (1.0L / (0x10000000000000000.0L * val.high));
#endif
}

double random_num_d() {
	pfs_uint128_t val = random_num0();
#ifdef PFS_COMPILER_INT128
	if (val == 0)
		return 1.0; // prevent NaN
	return 1.0 / val;
#else
	if (val.low == 0 && val.high == 0) return 1.0; // prevent NaN
	return (1.0 / val.low) + (1.0 / (0x10000000000000000.0 * val.high));
#endif
}
