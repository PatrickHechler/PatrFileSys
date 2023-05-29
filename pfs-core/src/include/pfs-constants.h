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
 * pfs-constants.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_CONSTANTS_H_
#define PFS_CONSTANTS_H_
/*
 * the magic should be changed when the version changes,
 * so old version does not load a new version which is
 * not backwards compatible
 */
#define PFS_MAGIC_START 0x31756539FC422698UL

#define	PFS_INTERN_ENTRY_FLAGS (PFS_F_HELPER_FOLDER) /* entries with one of these flags are not allowed to be passed to the outside */
#define PFS_ESSENTIAL_FLAGS (PFS_F_FILE | PFS_F_FOLDER | PFS_F_PIPE)
#define PFS_UNMODIFIABLE_FLAGS  0x000000FFU /* these flags are not allowed to be changed */
#define PFS_F_FOLDER            0x00000001U
#define PFS_F_FILE              0x00000002U
#define PFS_F_PIPE              0x00000004U
/* not yet supported
 #define PFS_FLAGS_SYM_LINK        0x00000008U
 */
#define	PFS_F_HELPER_FOLDER     0x00000080U /* folders with this flag should not be passed to the outside */

#define	PFS_F_EXECUTABLE        0x00000100U
#define	PFS_F_USER_ENCRYPTED    0x00000200U /* ignored */
#define	PFS_F_HIDDEN            0x01000000U

#define PFS_SO_ONLY_CREATE  0x00000001U /* fail if the file/pipe exist already */
#define PFS_SO_ALSO_CREATE  0x00000002U /* create the file/pipe if it does not exist, but do not fail if the file/pipe exist already (overwritten by PFS_SO_ONLY_CREATE) */
#define PFS_SO_FILE         0x00000004U /* fail if the element is a pipe and if CREATE is set create a file */
#define PFS_SO_PIPE         0x00000008U /* fail if the element is a file and if CREATE is set create a pipe */
#define PFS_SO_READ         0x00000100U /* open the stream for read access */
#define PFS_SO_WRITE        0x00000200U /* open the stream for write access */
#define PFS_SO_APPEND       0x00000400U /* open the stream for append access (before every write operation the position is set to the end of the file) */
#define PFS_SO_FILE_TRUNC   0x00010000U /* truncate the files content */
#define PFS_SO_FILE_EOF     0x00020000U /* set the position initially to the end of the file not the start */

#endif /* PFS_CONSTANTS_H_ */
