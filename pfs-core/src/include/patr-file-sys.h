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
 * patr-file-sys.h
 *
 *  Created on: Jul 6, 2022
 *      Author: pat
 */

#ifndef PATR_FILE_SYS_H_
#define PATR_FILE_SYS_H_

#define _GNU_SOURCE
#define _LARGEFILE64_SOURCE
#define _FILE_OFFSET_BITS 64
//#define __USE_TIME_BITS64

#include <stddef.h>
#include <limits.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <time.h>
#include <assert.h>
#include <string.h>

typedef int64_t i64;
typedef int32_t i32;
typedef uint64_t ui64;
typedef uint32_t ui32;
typedef uint8_t ui8;

#define export __attribute__ ((visibility ("default"))) export

static_assert(CHAR_BIT == 8, "Error!");
static_assert(sizeof(__time_t) == 8, "Error!");
static_assert(sizeof(i64) == 8, "Error!");
static_assert(sizeof(i32) == 4, "Error!");
static_assert(sizeof(ui64) == 8, "Error!");
static_assert(sizeof(ui32) == 4, "Error!");
static_assert(sizeof(char) == 1, "Error!");

#endif /* PATR_FILE_SYS_H_ */
