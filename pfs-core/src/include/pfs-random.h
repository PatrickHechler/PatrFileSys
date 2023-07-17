/*
 * pfs-random.h
 * 
 * ACORN
 * see http://acorn.wikramaratna.org/concept.html
 * 
 *  Created on: Jul 17, 2023
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_RANDOM_H_
#define SRC_INCLUDE_PFS_RANDOM_H_

#include <stdint.h>

/*
 * this file typedefs a 128 bit type (pfs_uint128_t)
 *
 */

/*
 * do a typedef pfs_uint128_t
 * first try to redirect to a type from the compiler
 *   if this is done PFS_COMPILER_INT128 will be defined
 * otherwise create a struct pfs_uint128_t { uint64_t low; uint64_t high; }
 */
#ifdef __SIZEOF_INT128__
/*
 * defined if the pfs_uint128_t supports arithmetic operations
 */
#define PFS_COMPILER_INT128
/*
 * use the __int128 built-in from GCC if available
 */
typedef unsigned __int128 pfs_uint128_t;
#else // __SIZEOF_INT128__
struct pfs_uint128_t {
	uint64_t low;
	uint64_t high;
};
typedef struct pfs_uint128_t pfs_uint128_t;
#endif // __SIZEOF_INT128__

/*
 * if the generator is already initialized, the old seed will be free'd
 * (with a call to free()) and overwritten
 *
 * Initialize the ACORN (Additive Congruential Random Number)
 *
 * note that the seed array will be used directly by the PRNG, so it
 * MUST NOT be modified or free'd.
 *
 * - the seed must have at least 2 entries (at least 11 are recommended)
 * - if the first value of the seed should be odd
 *     otherwise a warning may be displayed and the value will be made odd
 * - all entries of the given array are truncated to values below (2^120)-1
 *   - if at least one value needs to be truncated a warning may be displayed
 */
void init(uint64_t seed_entries, pfs_uint128_t *seed);

/*
 * if already initialized this function just returns
 * otherwise it will try to get a random seed and than
 * use it for initializing
 */
void ensure_init();

/*
 * returns an integer with random bits
 */
uint64_t random_num();

/*
 * returns a random value from 0 to 1 (0 inclusive, 1 exclusive)
 */
long double random_num_ld();

/*
 * returns a random value from 0 to 1 (0 inclusive, 1 exclusive)
 */
double random_num_d();

#endif /* SRC_INCLUDE_PFS_RANDOM_H_ */
