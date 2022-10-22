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
				"\n"//
				"help [BUILDIN]\n"//
				"  if buildin is set a help message for the\n"//
				"  given buildin command is printed.\n"//
				"  otherwise a help message for all buildin\n"//
				"  commands is printed\n"//
				"\n"//
				"mkfs.pfs [OPTIONS] [FILE]\n"//
				"  makes a new patr-file-system\n"//
				"  this command will fail if there is already a PFS mount-point\n"//
				"  Options:\n"//
				"    --block-size=[BLOCK_SIZE]\n"//
				"        set the size of the blocks\n"//
				"        default=1024\n"//
				"    --block-count=[BLOCK_COUNT]\n"//
				"        set the number of available blocks\n"//
				"        WARN: default=9223372036854775807\n"//
				"            Set the value when using  a device\n"//
				"            with limited size\n"//
				"    --force\n"//
				"        do not fail if FILE already exists\n"//
				"    --no-auto-mount\n"//
				"        do not automatically mount the created PFS\n"//
				"\n"//
				"mount.pfs [FILE] [FOLDER]\n"//
				"  mounts a patr-file-system from file to\n"//
				"  the given folder\n"//
				"  currently there is only one mount-point\n"//
				"  at a time allowed. to change it use\n"//
				"  umount.pfs before\n"//
				"\n"//
				"umount.pfs [FOLDER]\n"//
				"  un-mounts a patr-file-system from the given\n"//
				"  PFS-mount-point\n"//
				"  when the mount-point is not given this command\n"//
				"  un-mounts all mounted patr-file-systems\n"//
				"\n"//
				"lsm.pfs\n"//
				"  list all patr-file-system mount points\n"//
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
				"  this command will fail if there is already a PFS mount-point\n"//
				"\n"//
				"  Options:\n"//
				"    --block-size=[BLOCK_SIZE]\n"//
				"        set the size of the blocks\n"//
				"        default=1024\n"//
				"    --block-count=[BLOCK_COUNT]\n"//
				"        set the number of available blocks\n"//
				"        WARN: default=9223372036854775807\n"//
				"            Set the value when using  a device\n"//
				"            with limited size\n"//
				"    --force\n"//
				"        do not fail if FILE already exists\n"//
				"    --auto-mount=[FOLDER]\n"//
				"        automatically mount the created PFS to FOLDER\n"//
;
static const char const *msg_help_mount = //
		/*	  */"mount.pfs [FILE] [FOLDER]\n"//
				"  mounts a patr-file-system from file to\n"//
				"  the given folder\n"//
				"  currently there is only one mount-point\n"//
				"  at a time allowed. to change it use\n"//
				"  umount.pfs before\n"//
;
static const char const *msg_help_umount = //
		/*	  */"umount.pfs [FOLDER]\n"//
				"  un-mounts a patr-file-system from the given\n"//
				"  PFS-mount-point\n"//
				"  when the mount-point is not given this command\n"//
				"  un-mounts all mounted patr-file-systems"//
;
static const char const *msg_help_lsm = //
		/*	  */"lsm.pfs\n"//
				"  list all patr-file-system mount points\n"//
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

static inline void set_signal_handlers(void);
static inline char* gen_prompt(void);

static inline void bc_exit(char **args)__attribute__ ((__noreturn__));
static inline void bc_cd(char **args);
static inline void bc_help(char **args)__attribute__ ((__noreturn__));
static inline void bc_mkfs(char **args)__attribute__ ((__noreturn__));
static inline void bc_mount(char **args);
static inline void bc_umount(char **args);
static inline void bc_lsm(char **args)__attribute__ ((__noreturn__));
static inline void bc_ls(char **args)__attribute__ ((__noreturn__));
static inline void bc_cat(char **args)__attribute__ ((__noreturn__));
static inline void bc_cp(char **args)__attribute__ ((__noreturn__));
static inline void bc_mkdir(char **args)__attribute__ ((__noreturn__));
static inline void bc_rm(char **args)__attribute__ ((__noreturn__));
static inline void bc_rmdir(char **args)__attribute__ ((__noreturn__));
