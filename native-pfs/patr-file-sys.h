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

#include <stdint.h>
#include <time.h>
#include  <assert.h>

typedef int64_t i64;
typedef int32_t i32;
typedef uint64_t ui64;
typedef uint32_t ui32;
typedef uint16_t u16;

static_assert(sizeof(__time_t) == 8, "Error!");

#endif /* PATR_FILE_SYS_H_ */
