/*
 * pfs-shell.c
 *
 *  Created on: Oct 16, 2022
 *      Author: pat
 */

#include <pfs.h>
#include <errno.h>
#include <linux/sched.h>
#include <sched.h>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <stdio.h>
#include <stddef.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <readline/readline.h>
#include <readline/history.h>

static enum debug_lebel_enum {
	dl_none, dl_warn, dl_default = dl_warn, dl_all,
} debug_level;
static int color;

#define invalid_args fprintf(stderr, "invalid arg: '%s'\n%s", *argv, help_msg); exit(1);

static void setup(int argc, char **argv) {
	const char const *help_msg =
	/*            */"Usage: pfs-shell [OPTIONS]\n"
	/*            */"\n"
			/*            */"Options:\n"
			/*    */"  --help\n"
			/*    */"      print a simple help message and exit\n"
			/*    */"  --version\n"
			/*    */"      print the version and exit\n"
			/*    */"  --color\n"
			/*    */"      enable console coloring\n"
			/*    */"      (overwrites previous --no-color)\n"
			/*    */"  --no-color\n"
			/*    */"      disable console coloring\n"
			/*    */"      (overwrites previous --color)\n"
			/*    */"  --debug=[none|warn|all]\n"
			/*    */"      set the debug level\n"
			/*    */"      (overwrites previous --debug=)\n";
	const char const *vers_msg = "pfs-shell 1.0.0\n";
	debug_level = dl_default;
	color = 1;
	int do_exit = 0;
	for (argv++; *argv; argv++) {
		char *arg = *argv;
		if ('-' != (*arg)) {
			invalid_args
		}
		arg++;
		if ('-' == (*arg)) {
			arg++;
			switch (*arg) {
			case 'h':
				if (strcmp("elp", arg + 1)) {
					invalid_args
				}
				printf(help_msg);
				do_exit = 1;
				break;
			case 'v':
				if (strcmp("ersion", arg + 1)) {
					invalid_args
				}
				printf(vers_msg);
				do_exit = 1;
				break;
			case 'c':
				if (strcmp("olor", arg + 1)) {
					invalid_args
				}
				color = 1;
				break;
			case 'n':
				if (strcmp("o-color", arg + 1)) {
					invalid_args
				}
				color = 0;
				break;
			case 'd':
				if (memcmp("ebug=", arg + 1, 5)) {
					invalid_args
				}
				arg += 6;
				switch (*arg) {
				case 'n':
					if (strcmp("one", arg + 1)) {
						invalid_args
					}
					debug_level = dl_none;
					break;
				case 'w':
					if (strcmp("arn", arg + 1)) {
						invalid_args
					}
					debug_level = dl_warn;
					break;
				case 'a':
					if (strcmp("ll", arg + 1)) {
						invalid_args
					}
					debug_level = dl_all;
					break;
				default:
					invalid_args
				}
				break;
			default:
				invalid_args
			}
		} else { // currently all valid arguments start with --
			invalid_args
		}
	}
	if (do_exit) {
		exit(0);
	}
}

#undef invalid_args

const char *cd;
const char *name;

static inline void execute(void);
static inline void prompt(void);
static inline void buildin_info(const char *name);
static inline char** args(void);

int main(int argc, char **argv) {
	setup(argc, argv);
	int last_exit = 0;
	for (prompt(); 1; prompt()) {
		char **child_args = args();
		if (!strcmp("exit", *args)) {
			printf("goodbye\n");
			return last_exit;
		}
		int cpid;
		void *stack = mmap(NULL, 1024L, PROT_EXEC,
		/*		*/MAP_ANONYMOUS | MAP_STACK | MAP_SHARED, -1, 0);
		struct clone_args ca = { //
				/*			*/.flags = CLONE_VFORK | CLONE_PARENT_SETTID /* may want to comment CLONE_VM out*/
				/*			*/| CLONE_VM //
				/*			*/,//
						/*	*/.parent_tid = &cpid, //
						/*	*/.exit_signal = SIGCHLD, //
						/*	*/.stack = stack, //
						/*	*/.stack_size = 1024L, //
				};
		long l = syscall(SYS_clone3, &ca, sizeof(struct clone_args));
		if (l == -1) {
			perror("clone");
			last_exit = 1;
		}
		if (l) {
			waitpid(l, &last_exit, 0);
		} else {
			execute(child_args);
		}
	}
}

static const char const *msg_help_all = "";
static const char const *msg_help_exit = "";
static const char const *msg_help_help = "";
static const char const *msg_help_mkfs = "";
static const char const *msg_help_mount = "";
static const char const *msg_help_umount = "";
static const char const *msg_help_ls = "";
static const char const *msg_help_cat = "";
static const char const *msg_help_cp = "";
static const char const *msg_help_mkdir = "";
static const char const *msg_help_rm = "";
static const char const *msg_help_rmdir = "";

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

static inline void execute(char **args) {
	if (!strcmp("help", *args)) {
		buildin_info("help");
		bc_help(args);
	} else if (!strcmp("mkfs.pfs", *args)) {
		buildin_info("mkfs.pfs");
		bc_mkfs(args);
	} else if (!strcmp("mount.pfs", *args)) {
		buildin_info("mount.pfs");
		bc_mount(args);
	} else if (!strcmp("umount.pfs", *args)) {
		buildin_info("umount.pfs");
		bc_umount(args);
	} else if (!strcmp("ls", *args)) {
		buildin_info("ls");
		bc_ls(args);
	} else if (!strcmp("cat", *args)) {
		buildin_info("cat");
		bc_cat(args);
	} else if (!strcmp("cp", *args)) {
		buildin_info("cp");
		bc_cp(args);
	} else if (!strcmp("mkdir", *args)) {
		buildin_info("mkdir");
		bc_mkdir(args);
	} else if (!strcmp("rm", *args)) {
		buildin_info("rm");
		bc_rm(args);
	} else if (!strcmp("rmdir", *args)) {
		buildin_info("rmdir");
		bc_rmdir(args);
	} else {
		execv(*args, args);
		switch (errno) {
		case E2BIG:
			fpritnf(stderr, "could not execute the process!\n"
					"the environment is too large!\n");
			exit(1);
		case EACCES:
			fpritnf(stderr,
					"could access the file to execute! (or the file is not executable)\n");
			exit(1);
		case EAGAIN:
			fpritnf(stderr, "resource limit reached!\n");
			exit(1);
		case EFAULT:
			fpritnf(stderr, "segmentation fault an execve!\n");
			fflush(NULL);
			abort();
		case EINVAL:
			fpritnf(stderr, "the ELF has too many interpreters!\n");
			exit(1);
		case EIO:
			fpritnf(stderr, "an io error occured on starting the process!\n");
			exit(1);
		case EISDIR:
			fpritnf(stderr, "directories ar no interpreters!\n");
			exit(1);
		case ELIBBAD:
			fpritnf(stderr, "unknown ELF interpreter!\n");
			exit(1);
		case ELOOP:
			fpritnf(stderr,
					"too many symlinks in the path of the executable or too many interpreters should interpret each other!\n");
			exit(1);
		case EMFILE:
			fpritnf(stderr,
					"too many open files (this may be a bug (or is the limit set to a very low number))!\n");
			exit(1);
		case ENAMETOOLONG:
			fpritnf(stderr, "the (path)name of the executable is too long!\n");
			exit(1);
		case ENFILE:
			fpritnf(stderr, "too many open files on the system!\n");
			exit(1);
		case ENOENT:
			fpritnf(stderr, "interpreter not found!\n");
			exit(1);
		case ENOEXEC:
			fpritnf(stderr,
					"the system could not execute the executable (wrong arch or whatever)!\n");
			exit(1);
		case ENOMEM:
			fpritnf(stderr, "the kernel runs out of memory!\n");
			exit(1);
		case ENOTDIR:
			fpritnf(stderr,
					"there was a non-directory in the path which was expected to be a directory!\n");
			exit(1);
		case EPERM:
			fpritnf(stderr, "permission things!\n");
			exit(1);
		case ETXTBSY:
			fpritnf(stderr, "there writes someone to my executable!\n");
			exit(1);
		}
	}
}

static inline void prompt(void) {
	printf("[pfs-shell: %s]$ ", name);
	fflush(stderr);
}

static inline void buildin_info(const char *name) {
	printf("buildin %s (use /bin/%s for the non-builtin)\n", name, name);
	fflush(stdout);
}

static inline char** args(void) {
	char *line = readline(NULL);
// TODO parse args
}
