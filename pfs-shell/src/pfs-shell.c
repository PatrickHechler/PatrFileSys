/*
 * pfs-shell.c
 *
 *  Created on: Oct 16, 2022
 *      Author: pat
 */

#include <pfs.h>
#include <stdlib.h>
#include <sys/wait.h>
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

const char *cwd;
const char *name;

static inline void execute(char **args);
//static inline char* prompt(void);
static inline void buildin_info(const char *name);
static inline char** read_args(void);

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

static int last_exit = 0;

int main(int argc, char **argv) {
	setup(argc, argv);
	for (; 1;) {
		char **child_args = read_args();
		if (!strcmp("exit", *child_args)) {
			printf("goodbye\n");
			return last_exit;
		}
		int cpid = vfork();
		if (cpid == -1) {
			perror("vfork");
			last_exit = 1;
		}
		if (cpid) {
			waitpid(cpid, &last_exit, 0);
		} else {
			execute(child_args);
		}
	}
}

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
			fprintf(stderr, "could not execute the process!\n"
					"the environment is too large!\n");
			exit(1);
		case EACCES:
			fprintf(stderr,
					"could access the file to execute! (or the file is not executable)\n");
			exit(1);
		case EAGAIN:
			fprintf(stderr, "resource limit reached!\n");
			exit(1);
		case EFAULT:
			fprintf(stderr, "segmentation fault an execve!\n");
			fflush(NULL);
			abort();
		case EINVAL:
			fprintf(stderr, "the ELF has too many interpreters!\n");
			exit(1);
		case EIO:
			fprintf(stderr, "an io error occured on starting the process!\n");
			exit(1);
		case EISDIR:
			fprintf(stderr, "directories ar no interpreters!\n");
			exit(1);
		case ELIBBAD:
			fprintf(stderr, "unknown ELF interpreter!\n");
			exit(1);
		case ELOOP:
			fprintf(stderr,
					"too many symlinks in the path of the executable or too many interpreters should interpret each other!\n");
			exit(1);
		case EMFILE:
			fprintf(stderr,
					"too many open files (this may be a bug (or is the limit set to a very low number))!\n");
			exit(1);
		case ENAMETOOLONG:
			fprintf(stderr, "the (path)name of the executable is too long!\n");
			exit(1);
		case ENFILE:
			fprintf(stderr, "too many open files on the system!\n");
			exit(1);
		case ENOENT:
			fprintf(stderr, "interpreter not found!\n");
			exit(1);
		case ENOEXEC:
			fprintf(stderr,
					"the system could not execute the executable (wrong arch or whatever)!\n");
			exit(1);
		case ENOMEM:
			fprintf(stderr, "the kernel runs out of memory!\n");
			exit(1);
		case ENOTDIR:
			fprintf(stderr,
					"there was a non-directory in the path which was expected to be a directory!\n");
			exit(1);
		case EPERM:
			fprintf(stderr, "permission things!\n");
			exit(1);
		case ETXTBSY:
			fprintf(stderr, "there writes someone to my executable!\n");
			exit(1);
		}
	}
}

static inline char* gen_prompt(void) {
	i64 addlen = strlen(name);
	char *result = malloc(16 + addlen);
	memcpy(result, "[pfs-shell: ", 12);
	memcpy(result + 12, name, addlen);
	result[12 + addlen] = ']';
	result[12 + addlen + 1] = '$';
	result[12 + addlen + 2] = ' ';
	result[12 + addlen + 3] = '\0';
	return result;
}

static inline void buildin_info(const char *name) {
	printf("buildin %s (use /bin/%s for the non-builtin)\n", name, name);
	fflush(stdout);
}

static inline char** read_args(void) {
	int argc = 1;
	char *p = gen_prompt();
	char *line = readline(p);
	free(p);
	char **args = malloc((argc + 1) * sizeof(void*));
	*args = malloc(128);
	for (int li = 0, al = 128, an = 0, ai = 0; 1;) {
		switch (line[li]) {
		case '\0':
			args[an] = realloc(args[an], ai + 1);
			args[an][ai] = '\0';
			return args;
		case '\\':
			if (al <= ai) {
				args[an] = realloc(args[an], al += 128);
			}
			li++;
			if (line[li] == '\0') {
				args[an][ai++] = '\n';
				li = 0;
				line = readline("> ");
			} else {
				args[an][ai++] = line[li++];
			}
			break;
		case '$': {
			i64 len = 128;
			char *c = malloc(128);
			int i = 0;
			for (; 1;) {
				if (line[li] < 'A') {
					inval: ;
					if (isspace(line[li])) {
						li++;
						if (i == 3 && c[0] == 'P' && c[1] == 'W'
								&& c[2] == 'D') {
							free(c);
							c = cwd;
						} else {
							if (i >= len) {
								c = realloc(c, len = i + 1);
							}
							c[i] = '\0';
							char *cs = getenv(c);
							free(c);
							c = cs;
						}
						len = strlen(c);
						while (al - len <= ai) {
							args[an] = realloc(args[an], al += 128);
						}
						memcpy(args[an] + ai, c, len);
						ai += len;
					} else if (line[li] == '_') {
						goto good;
					} else {
						fprintf(stderr, "invalid character %c at char %d\n",
								line[li], li);
						return NULL;
					}
				}
				if (line[li] <= 'Z') {
					good: ;
					if (i >= len) {
						c = realloc(c, len += 128);
					}
					c[i++] = line[li++];
				}
				if (line[li] < 'a') {
					goto inval;
				}
				if (line[li] <= 'z') {
					goto good;
				}
				goto inval;
			}
			break;
		}
		case '\'':
#define read_string(end) \
			for (; 1;) { \
				if (line[li] == end) { \
					li++; \
					break; \
				} else if (line[li] == '\0') { \
					li = 0; \
					line = readline("> "); \
					continue; \
				} \
				if (al <= ai) { \
					args[an] = realloc(args[an], al += 128); \
				} \
				args[an][ai++] = line[li++]; \
			}
			read_string('\'')
			break;
		case '"':
			read_string('"')
			break;
		case ' ':
		case '\t':
			space: ;
			args[an] = realloc(args[an], ai + 1);
			args[an][ai] = '\0';
			argc++;
			args = realloc(args, (argc + 1) * sizeof(void*));
			break;
		default:
			if (isspace(line[li])) {
				goto space;
			}
			if (al <= ai) {
				args[an] = realloc(args[an], al += 128);
			}
			args[an][ai++] = line[li++];
		}
	}
}
