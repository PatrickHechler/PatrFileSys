/*
 * patr-file-sys.h
 *
 *  Created on: Jul 6, 2022
 *      Author: pat
 */

#ifndef PATR_FILE_SYS_H_
#define PATR_FILE_SYS_H_

#define _LARGEFILE64_SOURCE
#define _FILE_OFFSET_BITS 64
//#define __USE_TIME_BITS64

#include <stddef.h>
#include <limits.h>
#include <stdlib.h>
#include <stdint.h>
#include <time.h>
#include <assert.h>

typedef int64_t i64;
typedef int32_t i32;
typedef uint64_t ui64;
typedef uint32_t ui32;
typedef uint8_t ui8;

static_assert(CHAR_BIT == 8, "Error!");
static_assert(sizeof(__time_t) == 8, "Error!");
static_assert(sizeof(i64) == 8, "Error!");
static_assert(sizeof(i32) == 4, "Error!");
static_assert(sizeof(ui64) == 8, "Error!");
static_assert(sizeof(ui32) == 4, "Error!");
static_assert(sizeof(char) == 1, "Error!");

#endif /* PATR_FILE_SYS_H_ */
