/*
 * help-msg.h
 *
 *  Created on: Oct 19, 2022
 *      Author: pat
 */

#ifdef SRC_BUILDINS_H_ // this should never happen
#error
#endif /* SRC_BUILDINS_H_ */

#define SRC_BUILDINS_H_

static const char const *msg_help_all = //
		/*	  */"exit [EXIT_NUM]\n"//
				"  exits with the given exit number\n"//
				"  if no exit number is set exit with the\n"//
				"  exit number of the last command\n"//
				"  (when no command has been executed 0)\n"//
				"  if the exit number can not be parsed\n"//
				"  exit with 1 instead\n"//
				"\n"
				"help [BUILDIN]\n"//
				"  if buildin is set a help message for the\n"//
				"  given buildin command is printed.\n"//
				"  otherwise a help message for all buildin\n"//
				"  commands is printed\n"//
				"\n"//
				"mkfs.pfs [OPTIONS] [FILE]\n"//
				"  makes a new patr-file-system\n"//
				"\n"//
				"  Options:\n"//
				"    --block-size=[BLOCK_SIZE]\n"//
				"        set the size of the blocks\n"//
				"        default=1024\n"//
				"    --block-count=[BLOCK_COUNT]\n"//
				"        set the number of available blocks\n"//
				"        WARN: default=I64_MAX_VALUE\n"//
				"\n"//
				"mount.pfs [FILE] [FOLDER]\n"//
				"  mounts a patr-file-system from file to\n"//
				"  the given folder\n"//
				"\n"//
				"umount.pfs [FILE|FOLDER]\n"//
				"  unmounts a patr-file-system from the given\n"//
				"  mountpoint or mounted device\n"//
				"\n"//
				"ls\n"//
				"  prints the content of the current working\n"//
				"  directory.\n"//
				"\n"//
				"cat [FILE]\n"//
				"  prints the content of the given file\n"//
				"\n"//
				"cp [SOURCE_FILE] [TARGET_FILE]\n"//
				"  creates TARGET_FILE and copies the content\n"//
				"  of SOURCE_FILE to TARGET_FILE\n"//
				"  cp fails if TARGET_FILE already exists\n"//
				"\n"//
				"rm [FILE]\n"//
				"  deletes the given file\n"//
				"  rm will not fail if the file is not empty\n"//
				"\n"//
				"rmdir [FOLDER]\n"//
				"  deletes the given folder\n"//
				"  rmdir fails if the given folder is not empty\n"//
;
static const char const *msg_help_exit = //
		/*	  */"exit [EXIT_NUM]\n"//
				"  exits with the given exit number\n"//
				"  if no exit number is set exit with the\n"//
				"  exit number of the last command\n"//
				"  (when no command has been executed 0)\n"//
				"  if the exit number can not be parsed\n"//
				"  exit with 1 instead\n"//
;
static const char const *msg_help_help = //
		/*	  */"help [BUILDIN]\n"//
				"  if buildin is set a help message for the\n"//
				"  given buildin command is printed.\n"//
				"  otherwise a help message for all buildin\n"//
				"  commands is printed\n"//
;
static const char const *msg_help_mkfs = //
		/*	  */"mkfs.pfs [OPTIONS] [FILE]\n"//
				"  makes a new patr-file-system\n"//
				"\n"//
				"  Options:\n"//
				"    --block-size=[BLOCK_SIZE]\n"//
				"        set the size of the blocks\n"//
				"        default=1024\n"//
				"    --block-count=[BLOCK_COUNT]\n"//
				"        set the number of available blocks\n"//
				"        WARN: default=I64_MAX_VALUE\n"//
;
static const char const *msg_help_mount = //
		/*	  */"mount.pfs [FILE] [FOLDER]\n"//
				"  mounts a patr-file-system from file to\n"//
				"  the given folder\n"//
;
static const char const *msg_help_umount = //
		/*	  */"umount.pfs [FILE|FOLDER]\n"//
				"  unmounts a patr-file-system from the given\n"//
				"  mountpoint or mounted device\n"//
;
static const char const *msg_help_ls = //
		/*	  */"ls\n"//
				"  prints the content of the current working\n"//
				"  directory.\n"//
;
static const char const *msg_help_cat = //
		/*	  */"cat [FILE]\n"//
				"  prints the content of the given file\n"//
;
static const char const *msg_help_cp = //
		/*	  */"cp [SOURCE_FILE] [TARGET_FILE]\n"//
				"  creates TARGET_FILE and copies the content\n"//
				"  of SOURCE_FILE to TARGET_FILE\n"//
				"  cp fails if TARGET_FILE already exists\n"//
;
static const char const *msg_help_mkdir = //
		/*	  */"mkdir [FOLDER]\n"//
				"  creates the given folder\n"//
				"  mkdir fails if the parent folder does not\n"//
				"  already exists or the given folder already\n"//
				"  exists\n"//
;
static const char const *msg_help_rm = //
		/*	  */"rm [FILE]\n"//
				"  deletes the given file\n"//
				"  rm will not fail if the file is not empty\n"//
;
static const char const *msg_help_rmdir = //
		/*	  */"rmdir [FOLDER]\n"//
				"  deletes the given folder\n"//
				"  rmdir fails if the given folder is not empty\n"//
;

static inline void bc_help(char **args);
static inline void bc_mkfs(char **args);
static inline void bc_mount(char **args);
static inline void bc_umount(char **args);
static inline void bc_ls(char **args);
static inline void bc_cat(char **args);
static inline void bc_cp(char **args);
static inline void bc_mkdir(char **args);
static inline void bc_rm(char **args);
static inline void bc_rmdir(char **args);

