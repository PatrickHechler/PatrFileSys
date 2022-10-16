/*
 * pfs-folder.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_FOLDER_H_
#define SRC_INCLUDE_PFS_FOLDER_H_

#include "patr-file-sys.h"

/*
 * opens a new folder iterator handle for the given folder
 *
 * returns on success the folder iterator handle and on error -1
 */
extern int pfs_folder_open_iter(int eh, int show_hidden);

/*
 * returns the child count of the given folder
 * on error -1 is returned
 */
extern i64 pfs_folder_child_count(int eh);

/*
 * opens a new element handle for the child element with the given name
 * returns on success the new element handle and on error -1
 */
extern int pfs_folder_child(int eh, const char *name);

/*
 * this function works like pfs_folder_child, but fails when the child is no folder
 *
 * this function works like:
 *   int ceh = pfs_folder_child(eh, name);
 *   if (ceh == -1) return -1;
 *   int flags = pfs_element_get_flags(ceh);
 *   if ((flags == -1) || ((ceh & PFS_F_FOLDER) == 0)) {
 *     pfs_element_close(ceh);
 *     return -1;
 *   }
 *   return ceh;
 */
extern int pfs_folder_child_folder(int eh, const char *name);

/*
 * this function works like pfs_folder_child, but fails when the child is no file
 *
 * this function works like:
 *   int ceh = pfs_folder_child(eh, name);
 *   if (ceh == -1) return -1;
 *   int flags = pfs_element_get_flags(ceh);
 *   if ((flags == -1) || ((ceh & PFS_F_FILE) == 0)) {
 *     pfs_element_close(ceh);
 *     return -1;
 *   }
 *   return ceh;
 */
extern int pfs_folder_child_file(int eh, const char *name);

/*
 * this function works like pfs_folder_child, but fails when the child is no pipe
 *
 * this function works like:
 *   int ceh = pfs_folder_child(eh, name);
 *   if (ceh == -1) return -1;
 *   int flags = pfs_element_get_flags(ceh);
 *   if ((flags == -1) || ((ceh & PFS_F_PIPE) == 0)) {
 *     pfs_element_close(ceh);
 *     return -1;
 *   }
 *   return ceh;
 */
extern int pfs_folder_child_pipe(int eh, const char *name);

/*
 * creates a new folder with the given name and adds it to the given folder as a child element
 */
extern int pfs_folder_create_folder(int eh, const char *name);

/*
 * creates a new file with the given name and adds it to the given folder as a child element
 */
extern int pfs_folder_create_file(int eh, const char *name);

/*
 * creates a new pipe with the given name and adds it to the given folder as a child element
 */
extern int pfs_folder_create_pipe(int eh, const char *name);

#endif /* SRC_INCLUDE_PFS_FOLDER_H_ */
