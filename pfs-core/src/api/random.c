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
#include "../include/patr-file-sys.h"
#include "../include/pfs-random.h"

#include <endian.h>

#if !defined PFS_PORTABLE_BUILD
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

void ensure_init() {
	void *data = malloc(DEFAULT_ENTRY_COUNT * sizeof(pfs_uint128_t));
	/*
	 * 1: try getrandom()
	 * 2: try /dev/urandom
	 * 3: use srandom() and random()
	 */
#if !defined PFS_PORTABLE_BUILD && !defined PFS_HALF_PORTABLE_BUILD
	void *p = data;
	for (size_t remain = DEFAULT_ENTRY_COUNT * sizeof(pfs_uint128_t); remain;) {
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
#if defined PFS_HALF_PORTABLE_BUILD || defined PFS_PORTABLE_BUILD /* no need to do this three times */
				void *p = data;
				size_t remain = DEFAULT_ENTRY_COUNT * sizeof(pfs_uint128_t);
#endif // half-portable
#if !defined PFS_PORTABLE_BUILD
				int fd = open("/def/urandom", O_RDONLY);
				if (fd == -1) {
					portable_rnd: ;
#endif // half- or non-portable
					srandom(time());
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
	init(DEFAULT_ENTRY_COUNT, data);
	Y = data;
	k = DEFAULT_ENTRY_COUNT - 1;
	*(char*) data |= 1;
	for (int i = 1; i < (DEFAULT_ENTRY_COUNT << 1); i += 2) {
		if (((int64_t*) data)[i] & ~Mm1) {
			((int64_t*) data)[i] &= Mm1;
		}
	}
}

void init(uint64_t seed_entries, pfs_uint128_t *seed) {
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
	void *old = Y;
	Y = seed;
	if (old) {
		free(old);
	}
	k = seed_entries;
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
}

pfs_uint128_t random_num0() {
	pfs_uint128_t res = Y[0];
	for (int_fast64_t i = 1; i < k; i++) {
		res += Y[i];
#ifdef PFS_COMPILER_INT128
		res = ((uint64_t) res) | (((res >> 64) & Mm1) << 64);
#else // res %= 2^120
		res.high &= Mm1;
#endif // PFS_COMPILER_INT128
		Y[i] = res;
	}
	return res;
}

int64_t random_num() {
	return random_num0();
}

long double random_num_ld() {
	return 1.0L / random_num0();
}

double random_num_d() {
	return 1.0 / random_num0();
}
