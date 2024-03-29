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

#include "patr-file-sys.h"
#include <stdint.h>

/*
 * do a typedef pfs_uint128_t
 * first try to redirect to a type from the compiler
 *   if this is done PFS_COMPILER_INT128 will be defined
 * otherwise create a struct pfs_uint128_t { uint64_t low; uint64_t high; }
 */
#if !defined PFS_PORTABLE_BUILD && defined __SIZEOF_INT128__
/*
 * defined if the pfs_uint128_t supports arithmetic operations
 */
#define RND_COMPILER_INT128
/*
 * use the __int128 built-in from GCC if available
 */
typedef unsigned __int128 pfs_uint128_t;
#else // __SIZEOF_INT128__
#include <endian.h>
struct pfs_uint128_t {
#if BYTE_ORDER == LITTLE_ENDIAN
	uint64_t low;
	uint64_t high;
#else // everything except for LITTLE_ENDIAN gets BIG_ENDIAN but it
	uint64_t high; // should not matter how they are ordered anyway
	uint64_t low;
#endif
};
typedef struct pfs_uint128_t pfs_uint128_t;
#endif // __SIZEOF_INT128__

#ifdef I_AM_RANDOM
void (*random_free)(void *__ptr);
#else
extern void (*random_free)(void *__ptr);
#endif

/*
 * if the generator is already initialized, the old seed will be free'd
 * (with a call to random_free()) and overwritten
 *
 * Initialize the ACORN (Additive Congruential Random Number)
 *
 * note that the seed array will be used directly by the PRNG, so it
 * MUST NOT be modified or free'd.
 *
 * this function also sets random_free to free, so if random_init is
 * called again and random_free is not set to an other value the free
 * function will be called.
 *
 * because the seed is used to save the complete state, one can use multiple
 * random instances by setting random_free to NULL after each call to
 * random_init and using the same addresses as seed (with the same length)
 *
 * note that no function which uses the global state is thread safe.
 * to be thread save, use the random_state_* functions and ensure that the
 * state is synchronized with all other threads and not currently used by any
 * other thread
 *
 * - the seed must have at least 2 entries (at least 11 are recommended)
 *   - if the seed is to small the program will abort()
 * - the first value of the seed should be odd
 *   - otherwise a warning may be displayed and the value will be made odd
 * - all entries of the given array are truncated to values below (2^120)-1
 *   - if at least one value needs to be truncated a warning may be displayed
 */
void random_init(uint64_t seed_entries, pfs_uint128_t *seed);

/*
 * validates and corrects the given state (this should be called before
 * a state is first used)
 *
 * if suppress_warnings has a non-zero value, the same warnings are printed
 * like calling random_init().
 *
 * if the seed is to small the program will abort()
 */
void random_state_init(uint64_t state_entries, pfs_uint128_t *state,
		_Bool suppress_warnings);

/*
 * if already initialized this function just returns
 * otherwise it will try to get a random seed and than
 * use it for initializing
 */
void random_ensure_init();

/*
 * creates and initializes a new state
 * if state_entries points to zero it will be replaced by a default value
 * the returned state will have *state_entries 128-bit entries
 */
pfs_uint128_t* random_init_new_state(uint64_t *state_entries);

/*
 * returns an integer with random bits
 */
uint64_t random_num();

/*
 * like random_num() but uses the given state
 */
uint64_t random_state_num(uint64_t state_entries, pfs_uint128_t *state);

/*
 * returns a random value from 0 to 1 (zero inclusive, one exclusive)
 */
double random_num_d();

/*
 * like random_num_d() but uses the given state
 */
double random_state_num_d(uint64_t state_entries, pfs_uint128_t *state);

/*
 * fills the given array with random values
 */
void random_data(void *data, size_t len);

/*
 * like random_data, but it uses the given state and not the global seed
 */
void random_state_data(uint64_t state_entries, pfs_uint128_t *state, void *data,
		size_t len);

#endif /* SRC_INCLUDE_PFS_RANDOM_H_ */
