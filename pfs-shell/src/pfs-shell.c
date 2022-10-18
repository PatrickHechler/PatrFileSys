/*
 * pfs-shell.c
 *
 *  Created on: Oct 16, 2022
 *      Author: pat
 */

#include <stdio.h>
#include <stddef.h>
#include <stdlib.h>
#include <pfs.h>
#include <unistd.h>
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
		last_exit = execute(child_args);
	}
}

static inline int execute(char **args) {
	if (!strcmp("help", *args)) {
		buildin_info("help");

	} else if (!strcmp("mkfs.pfs", *args)) {
		buildin_info("mkfs.pfs");

	} else if (!strcmp("mount.pfs", *args)) {
		buildin_info("mount.pfs");

	} else if (!strcmp("umount.pfs", *args)) {
		buildin_info("umount.pfs");

	} else if (!strcmp("ls", *args)) {
		buildin_info("ls");

	} else if (!strcmp("cat", *args)) {
		buildin_info("cat");

	} else if (!strcmp("cp", *args)) {
		buildin_info("cp");

	} else if (!strcmp("mkdir", *args)) {
		buildin_info("mkdir");

	} else if (!strcmp("rmdir", *args)) {
		buildin_info("rmdir");

	} else if (!strcmp("rm", *args)) {
		buildin_info("rm");

	} else {
		int argc = 1;
		while (args[args]) {
			argc++;
		}
		pid_t cpid = vfork();
		if (cpid) {
			int cen;
			waitpid(cpid, &cen, 0);
			return cen;
		}
		execv(*args, args);
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
