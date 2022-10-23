/*
 * pfs-shell.c
 *
 *  Created on: Oct 16, 2022
 *      Author: pat
 */

#include <pfs.h>
#include <pfs-err.h>
#include <pfs-constants.h>
#include <pfs-stream.h>
#include <pfs-element.h>
#include <pfs-file.h>
#include <pfs-folder.h>
#include <pfs-iter.h>
#include <fcntl.h>
#include <stdlib.h>
#include <errno.h>
#include <dirent.h>
#include <sched.h>
#include <unistd.h>
#include <stdio.h>
#include <stddef.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <sys/wait.h>
#include <sys/syscall.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <linux/sched.h>
#include <readline/readline.h>
#include <readline/history.h>

#include "buildins.h"

static enum debug_lebel_enum {
	dl_none, dl_warn, dl_default = dl_warn, dl_all,
} debug_level;
static int color;

char *mount = NULL;

char *cwd;
char *name;

static inline void execute(char **args) __attribute__ ((__noreturn__));
//static inline char* prompt(void);
static inline char** read_args(void);

#define invalid_args fprintf(stderr, "invalid arg: '%s'\n%s", *argv, help_msg); exit(1);

static inline void setup(int argc, char **argv) {
	const char const *help_msg =
	/*            */"Usage: pfs-shell [OPTIONS]\n"
	/*            */"\n"
			/*    */"Options:\n"
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
	{
		set_signal_handlers();
		char *pwd = getenv("PWD");
		if (!pwd) {
			pwd = getenv("HOME");
			if (!pwd) {
				pwd = "/";
			}
		}
		i64 len = strlen(pwd);
		if (pwd[len - 1] == '/') {
			len--;
		}
		cwd = malloc((len + 128) & ~127);
		memcpy(cwd, pwd, len);
		cwd[len] = '\0';
		name = strrchr(cwd, '/');
		if (!name) {
			name = cwd;
		} else {
			name++;
		}
	}
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
static int cpid = -1;

void ps_child_delegate_handler(int sig) {
	if (cpid != -1) {
		kill(cpid, sig);
	} else {
		char *p = gen_prompt();
		fprintf(stdout, "\n%s", p);
		fflush(stdout);
	}
}

static inline void set_signal_handlers(void) {
	struct sigaction act = { .sa_flags = 0, .sa_mask = 1 << SIGINT
			| 1 << SIGTERM | 1 << SIGQUIT, };
	act.sa_handler = ps_child_delegate_handler;
	sigaction(SIGINT, &act, NULL);
	sigaction(SIGTERM, &act, NULL);
}

// for debugging
static volatile int debug_child = 0;

int main(int argc, char **argv) {
	setup(argc, argv);
	for (; 1;) {
		char **child_args = read_args();
		if (!child_args) {
			return 0;
		}
		// exit, cd, mount and umount has to be made in the same process
		// all other buildins are made after the fork
		if (!strcmp("exit", *child_args)) {
			bc_exit((char**) child_args);
		}
		if (!strcmp("cd", *child_args)) {
			bc_cd((char**) child_args);
			continue;
		}
		if (!strcmp("mount.pfs", *child_args)) {
			bc_mount((char**) child_args);
			continue;
		}
		if (!strcmp("umount.pfs", *child_args)) {
			bc_umount((char**) child_args);
			continue;
		}
		cpid = fork();
		if (cpid == -1) {
			perror("fork");
			last_exit = 1;
		} else if (cpid) {
			waitpid(cpid, &last_exit, 0);
		} else {
			if (debug_child) {
				printf("cpid=%d\n", getpid());
				fflush(stdout);
				sleep(5);
			}
			execute(child_args);
		}
		free(child_args);
		if (errno != 0) {
			perror(NULL);
			errno = 0;
		}
	}
}

#define buildin_info(name) fputs("buildin " #name " (use /bin/" #name " for the non-buildin (if there is one))\n", stdout);

#ifdef NO_COLOR

#define ANSI(act) ""

#else

#define ANSI(act) (color ? (ANSI_ESC act ANSI_END) : "")

#define ANSI_ESC "\033["
#define ANSI_SEP ";"
#define ANSI_END "m"

#define ANSI_RESET              "0"
#define ANSI_UNDERLINE          "4"
#define ANSI_BLINK_ON           "5"
#define ANSI_REVERSE_COLORS_ON  "7"
#define ANSI_LETTER_RESET       "22" /* reset bold or faint */
#define ANSI_BLINK_OFF          "25"
#define ANSI_REVERSE_COLORS_OFF "27"
#define ANSI_F_RESET            "39"
#define ANSI_B_RESET            "49"
#define ANSI_F_BLACK            "30"
#define ANSI_F_RED              "31"
#define ANSI_F_GREEN            "32"
#define ANSI_F_YELLOW           "33"
#define ANSI_F_BLUE             "34"
#define ANSI_F_MAGENTA          "35"
#define ANSI_F_CYAN             "36"
#define ANSI_F_WHITE            "37"
#define ANSI_B_BLACK            "40"
#define ANSI_B_RED              "41"
#define ANSI_B_GREEN            "42"
#define ANSI_B_YELLOW           "43"
#define ANSI_B_BLUE             "44"
#define ANSI_B_MAGENTA          "45"
#define ANSI_B_CYAN             "46"
#define ANSI_B_WHITE            "47"
#define ANSI_F_B_BLACK          "90"
#define ANSI_F_B_RED            "91"
#define ANSI_F_B_GREEN          "92"
#define ANSI_F_B_YELLOW         "93"
#define ANSI_F_B_BLUE           "94"
#define ANSI_F_B_MAGENTA        "95"
#define ANSI_F_B_CYAN           "96"
#define ANSI_F_B_WHITE          "97"
#define ANSI_B_B_BLACK          "100"
#define ANSI_B_B_RED            "101"
#define ANSI_B_B_GREEN          "102"
#define ANSI_B_B_YELLOW         "103"
#define ANSI_B_B_BLUE           "104"
#define ANSI_B_B_MAGENTA        "105"
#define ANSI_B_B_CYAN           "106"
#define ANSI_B_B_WHITE          "107"
#define ANSI_F_RGB(r, g, b)     "38;2;" r ";" g ";" b
#define ANSI_B_RGB(r, g, b)     "48;2;" r ";" g ";" b

#endif // !defined(NO_COLOR)

static inline void execute(char **args) {
	if (!strcmp("help", *args)) {
		buildin_info(help);
		bc_help((char**) args);
	} else if (!strcmp("mkfs.pfs", *args)) {
		buildin_info(mkfs.pfs);
		bc_mkfs((char**) args);
	} else if (!strcmp("lsm.pfs", *args)) {
		buildin_info(lsm.pfs);
		bc_lsm((char**) args);
	} else if (!strcmp("ls", *args)) {
		buildin_info(ls);
		bc_ls((char**) args);
	} else if (!strcmp("cat", *args)) {
		buildin_info(cat);
		bc_cat((char**) args);
	} else if (!strcmp("cp", *args)) {
		buildin_info(cp);
		bc_cp((char**) args);
	} else if (!strcmp("mkdir", *args)) {
		buildin_info(mkdir);
		bc_mkdir((char**) args);
	} else if (!strcmp("rm", *args)) {
		buildin_info(rm);
		bc_rm((char**) args);
	} else if (!strcmp("rmdir", *args)) {
		buildin_info(rmdir);
		bc_rmdir((char**) args);
	} else {
		char *binary = (char*) *args;
		switch (*binary) {
		case '/':
		case '~':
		case '.':
			break;
		default:
			i64 arglen = strlen(*args);
			binary = malloc(arglen + 6);
			binary[0] = '/';
			binary[1] = 'b';
			binary[2] = 'i';
			binary[3] = 'n';
			binary[4] = '/';
			memcpy(binary + 5, *args, arglen + 1); // also copy '\0'
		}
		execv((const char*) binary, args);
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
			fprintf(stderr, "segmentation fault an execv!\n");
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
			fprintf(stderr, "binary not found! (%s)\n", binary);
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
		default:
			fprintf(stderr, "error on execv call (cmd='%s')\n", *args);
			perror("execv");
			exit(1);
		}
	}
}

static inline char* gen_prompt(void) {
	i64 addlen = strlen(name);
	if (last_exit) {
		addlen += 128;
		char *result = malloc(addlen);
		int len;
		while (1) {
			if (WIFEXITED(last_exit)) {
				len = snprintf(result, addlen, "%s%.2X%s): [pfs-shell: %s]$ ",
						ANSI(ANSI_F_B_RED), WEXITSTATUS(last_exit),
						ANSI(ANSI_F_RESET), name);
			} else if (WIFSIGNALED(last_exit)) {
				len = snprintf(result, addlen, "%s%d:%s%s): [pfs-shell: %s]$ ",
						ANSI(ANSI_F_B_RED), WTERMSIG(last_exit),
						sigabbrev_np(WTERMSIG(last_exit)), ANSI(ANSI_F_RESET),
						name);
			} else {
				len = snprintf(result, addlen, "<%s%.4X%s> [pfs-shell: %s]$ ",
						ANSI(ANSI_F_B_RED), (ui32) last_exit,
						ANSI(ANSI_F_RESET), name);
			}
			if (len >= addlen) {
				addlen = len + 1L;
				result = realloc(result, addlen);
				continue;
			}
			return result;
		}
	} else {
		char *result = malloc(16 + addlen);
		memcpy(result, "[pfs-shell: ", 12);
		memcpy(result + 12, name, addlen);
		result[12 + addlen] = ']';
		result[12 + addlen + 1] = '$';
		result[12 + addlen + 2] = ' ';
		result[12 + addlen + 3] = '\0';
		return result;
	}
}

static inline void read_string(char **line, char **args, int *li, int an,
		int *ai, int *al, char end) {
	for (; 1;) {
		if ((*line)[(*li)] == end) {
			(*li)++;
			break;
		} else if ((*line)[(*li)] == '\\') {
			if ((*al) <= (*ai)) {
				args[an] = realloc(args[an], (*al) += 128);
			}
			if ((*line)[++(*li)] == '\0') {
				goto new_line;
			}
			args[an][(*ai)++] = (*line)[(*li)++];
		} else if ((*line)[(*li)] == '\0') {
			if ((*al) <= (*ai)) {
				args[an] = realloc(args[an], (*al) += 128);
			}
			new_line: args[an][(*ai)++] = '\n';
			(*li) = 0;
			(*line) = readline("> ");
			if (!(*line)) {
				fprintf(stderr,
						"reached end of file in the middle of a command!");
				exit(1);
			}
			add_history((*line));
			continue;
		}
		if ((*al) <= (*ai)) {
			args[an] = realloc(args[an], (*al) += 128);
		}
		args[an][(*ai)++] = (*line)[(*li)++];
	}
}

static inline char** read_args(void) {
	char *prompt = gen_prompt();
	char *line;
	while (1) {
		line = readline(prompt);
		if (!line) {
			return NULL;
		} else if (*line) {
			break;
		}
	}
//	free(prompt); // why?
	add_history(line);
	char **args = malloc(2 * sizeof(void*));
	*args = NULL;
	for (int li = 0, al = 0, an = 0, ai = 0; 1;) {
		switch (line[li]) {
		case '\0':
			if (args[an]) {
				args[an] = realloc(args[an], al + 1);
				args[an][ai] = '\0';
			}
			args[an + 1] = NULL;
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
				if (!line) {
					fprintf(stderr,
							"reached end of file in the middle of a command!");
					exit(1);
				}
				add_history(line);
			} else {
				args[an][ai++] = line[li++];
			}
			break;
		case '$': {
			i64 len = 128;
			char *c = malloc(128);
			int i = 0;
			for (li++; 1;) {
				if (line[li] < 'A') {
					inval: ;
					switch (line[li]) {
					default: {
						end: ;
						if (i >= len) {
							c = realloc(c, len = i + 1);
						}
						c[i] = '\0';
						char *cs = getenv(c);
						free(c);
						if (cs) {
							c = cs;
						} else {
							c = "";
						}
						end0: ;
						len = strlen(c);
						while (al - len <= ai) {
							args[an] = realloc(args[an], al += 128);
						}
						memcpy(args[an] + ai, c, len);
						ai += len;
						goto big_break;
					}
					case '_': {
						goto good;
					}
					case '?': {
						if (i == 0) {
							li++;
							c = realloc(c, 64);
							int wrote = snprintf(c, 64, "%d",
									(((ui32) last_exit) >> 8)
											| (((ui32) last_exit) << 8));
							if (wrote == -1 || wrote >= 64) {
								perror("error on snprintf");
								return NULL;
							}
							goto end0;
						} else {
							goto end;
						}
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
				big_break: ;
				break;
			}
			case '\'':
			{
				read_string(&line, args, &li, an, &ai, &al, '\'');
				break;
			}
			case '"':
			{
				read_string(&line, args, &li, an, &ai, &al, '"');
				break;
			}
			case ' ':
			case '\t':
			space: ;
			if (args[an]) {
				args[an] = realloc(args[an], (ai + 1) * sizeof(char*));
				args[an][ai] = '\0';
				args = realloc(args, (an + 2) * sizeof(void*));
				an++;
				args[an] = NULL;
				ai = 0;
				al = 0;
			}
			li++;
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
}

static char* extract_path(const char *p) {
	char *result = malloc(128);
	i64 len = 128;
	i64 index;
	switch (p[0]) {
	case '~':
		if (p[1] != '/' && p[1] != '\0') {
			index = 0;
			break;
		}
		char *home = getenv("HOME");
		if (home) {
			i64 slen = strlen(home);
			while (len < slen) {
				result = realloc(result, len += 128);
			}
			if (home[slen - 1] == '/') {
				slen--;
			}
			memcpy(result, home, slen);
			index = slen;
		} else {
			index = 0;
		}
		p++;
		break;
	case '/':
		index = 0;
		p++;
		break;
	case '.':
	default:
		index = strlen(cwd);
		while (index >= len) {
			result = realloc(result, len += 128);
		}
		memcpy(result, cwd, index);
		break;
	}
	while (1) {
		char *end = strchrnul(p, '/');
		switch (end - p) {
		case 0:
			if (*end) { // *end == '/' && p == end
				goto end0;
			}
			goto rend;
		case 1:
			if (p[0] == '.') {
				goto end0;
			}
			break;
		case 2:
			if (p[0] == '.' && p[1] == '.') {
				char *lst = strrchr(result, '/');
				if (!lst) {
					fprintf(stderr, "on my shell the root has no parent!");
					fflush(stderr);
					free(result);
					return NULL;
				}
				*lst = '\0';
				index = lst - result;
				goto end0;
			}
			break;
		}
		i64 cpy = end - p;
		while (cpy >= len - index) {
			result = realloc(result, len += 128);
		}
		result[index++] = '/';
		memcpy(result + index, p, cpy);
		index += cpy;
		end0: p = end + 1;
		rend: ;
		if (!*end) {
			break;
		}
	}
	if (index == 0) {
		result = realloc(result, 2);
		result[index++] = '/';
	} else {
		result = realloc(result, index + 1);
	}
	result[index] = '\0';
	return result;
}

static char* extract_pfs_path(char *path) {
	if (!mount) {
		return NULL;
	}
	int i;
	for (i = 0; path[i] && mount[i]; i++) {
		if (path[i] != mount[i]) {
			return NULL;
		}
	}
	return path + i;
}

static inline void bc_exit(char **args) {
	if (args[1]) {
		char *end;
		last_exit = strtol(args[1], &end, 10);
		if (*end || errno != 0) {
			errno = 0;
			fprintf(stderr, "could not parse argument to a number: '%s'\n",
					args[1]);
			fflush(stderr);
			if (last_exit == 0) {
				last_exit = 1;
			}
		}
	}
	exit(last_exit);
}

static inline void bc_cd(char **args) {
	if (!args[1]) {
		fprintf(stderr, "not enugh args for cd\n");
		last_exit = 1;
		return;
	}
	if (args[2]) {
		fprintf(stderr, "too many args for cd\n");
		last_exit = 1;
		return;
	}
	char *path = extract_path(args[1]);
	chdir(path);
	setenv("PWD", path, 1);
	free(cwd);
	cwd = path;
	name = strrchr(path, '/');
	if (name == NULL) {
		name = cwd;
	} else if (name[1]) {
		name++;
	}
	last_exit = 0;
}

static inline void bc_help(char **args) {
	if (!args[1]) {
		fputs(msg_help_all, stdout);
	} else {
		for (args++; *args; args++) {
			if (!strcmp("all", *args)) {
				fputs(msg_help_all, stdout);
			} else if (!strcmp("exit", *args)) {
				fputs(msg_help_exit, stdout);
			} else if (!strcmp("help", *args)) {
				fputs(msg_help_help, stdout);
			} else if (!strcmp("mkfs.pfs", *args)) {
				fputs(msg_help_mkfs, stdout);
			} else if (!strcmp("mount.pfs", *args)) {
				fputs(msg_help_mount, stdout);
			} else if (!strcmp("umount.pfs", *args)) {
				fputs(msg_help_umount, stdout);
			} else if (!strcmp("lsm.pfs", *args)) {
				fputs(msg_help_lsm, stdout);
			} else if (!strcmp("ls", *args)) {
				fputs(msg_help_ls, stdout);
			} else if (!strcmp("cat", *args)) {
				fputs(msg_help_cat, stdout);
			} else if (!strcmp("cp", *args)) {
				fputs(msg_help_cp, stdout);
			} else if (!strcmp("mkdir", *args)) {
				fputs(msg_help_mkdir, stdout);
			} else if (!strcmp("rm", *args)) {
				fputs(msg_help_rm, stdout);
			} else if (!strcmp("rmdir", *args)) {
				fputs(msg_help_rmdir, stdout);
			} else {
				fprintf(stderr, "unknown buildin-command: '%s'\n", *args);
				fflush(stderr);
				exit(1);
			}
		}
	}
	fflush(stdout);
	exit(0);
}

static inline void bc_mkfs(char **args) {
	i32 block_size = 1024;
	i64 block_count = LONG_MAX;
	int overwrite = 0;
	char *auto_mount = NULL;
	char *device = NULL;
	if (mount) {
		fprintf(stderr, "there is already a PFS-mount-point\n"
				"(I don't care if --no-auto-mount is set)\n", *args);
		exit(1);
	}
	for (; *args; args++) {
		if (**args != '-') {
			break;
		}
		if (!memcmp("--block-size=", *args, 13)) {
			char *end;
			long val = strtol(*args + 13, &end, 0);
			if (*end || errno != 0 || val < 0) {
				if (errno) {
					perror("strtol");
				}
				errno = 0;
				fprintf(stderr, "could not parse block size: '%s'\n", *args);
				exit(1);
			}
			if (val > INT32_MAX) {
				fprintf(stderr, "block size out of range: '%s'\n", *args);
				exit(1);
			}
		} else if (!memcmp("--block-count=", *args, 14)) {
			char *end;
			long val = strtol(*args + 13, &end, 0);
			if (*end || errno != 0 || val < 0) {
				if (errno) {
					perror("strtol");
				}
				errno = 0;
				fprintf(stderr, "could not parse block count: '%s'\n", *args);
				exit(1);
			}
			block_count = val;
		} else if (!memcmp("--auto-mount=", *args, 13)) {
			auto_mount = extract_path(*args + 13);
		} else if (!memcmp("--force", *args, 8 /*(also compare the '\0') */)) {
			overwrite = 1;
		} else {
			fprintf(stderr, "unknown option: '%s'\n", *args);
			exit(1);
		}
	}
	if (!*args) {
		fprintf(stderr, "not enugh args!\n");
		exit(1);
	}
	device = extract_path(*args);
	if (extract_pfs_path(device)) {
		fprintf(stderr, "nesting a pfs does not make any sense!\n");
		exit(1);
	}
	int fd;
	while (1) {
		fd = open(device,
		/*		*/O_CLOEXEC | O_RDWR | O_CREAT | (overwrite ? 0 : (O_EXCL)),
		/*		*/S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH);
		if (fd == -1) {
			switch (errno) {
			case EACCES:
				fprintf(stderr, "access error while opening the device file\n");
				break;
			case EDQUOT:
				fprintf(stderr, "the user has no more blocks\n");
				break;
			case EEXIST:
				fprintf(stderr, "the target file already exists\n");
				break;
			case EINTR:
				continue;
			case EINVAL:
				fprintf(stderr, "the target file name seems to be invalid (%s)",
						device);
				break;
			case EISDIR:
				fprintf(stderr, "the target file is a folder (%s)", device);
				break;
			case ELOOP:
				fprintf(stderr, "too many symlinks (%s)", device);
				break;
			case ENAMETOOLONG:
				fprintf(stderr, "filename to long (%s)", device);
				break;
			case ENOENT:
				fprintf(stderr, "parent folder does not exist (%s)", device);
				break;
			case EPERM:
				fprintf(stderr, "permission was denied\n");
				break;
			case EROFS:
				fprintf(stderr, "the file is on a read only file-system\n");
				break;
			case ETXTBSY:
				fprintf(stderr,
						"the file seems to be currently executed (%s)\n",
						device);
				break;
			}
			perror("open");
			exit(1);
		}
		break;
	}
	struct bm_block_manager *bm = bm_new_file_block_manager(fd, block_size);
	if (!bm) {
		fprintf(stderr, "the block manager could not be created (%s)\n",
				pfs_error());
		pfs_errno = 0;
		exit(1);
	}
	if (!pfs_load_and_format(bm, block_count)) {
		fprintf(stderr, "PFS-format failed (%s)\n", pfs_error());
		pfs_errno = 0;
		exit(1);
	}
	if (auto_mount) {
		printf("mount the new PFS to %s", auto_mount);
		mount = auto_mount;
	} else {
		pfs_close();
	}
	exit(0);
}

static inline void bc_mount(char **args) {
	if (mount) {
		fprintf(stderr, "there is already a PFS-mount-point\n", *args);
		last_exit = 1;
		return;
	}
	if (!args[1] || !args[2]) {
		fprintf(stderr, "not enugh args\n");
		last_exit = 1;
		return;
	}
	if (args[3]) {
		fprintf(stderr, "too many args\n");
		last_exit = 1;
		return;
	}
	char *mount_device = extract_path(args[1]);
	char *mount_folder = extract_path(args[2]);
	int fd;
	while (1) {
		fd = open(mount_device, O_CLOEXEC | O_RDWR,
		/*		*/S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH);
		free(mount_device);
		if (fd == -1) {
			switch (errno) {
			case EACCES:
				fprintf(stderr, "access error while opening the device file\n");
				break;
			case EEXIST:
				fprintf(stderr, "the target file already exists\n");
				break;
			case EINTR:
				continue;
			case EINVAL:
				fprintf(stderr, "the target file name seems to be invalid (%s)",
						mount_device);
				break;
			case EISDIR:
				fprintf(stderr, "the target file is a folder (%s)",
						mount_device);
				break;
			case ELOOP:
				fprintf(stderr, "too many symlinks (%s)", mount_device);
				break;
			case ENAMETOOLONG:
				fprintf(stderr, "filename to long (%s)", mount_device);
				break;
			case ENOENT:
				fprintf(stderr, "parent folder does not exist (%s)",
						mount_device);
				break;
			case EPERM:
				fprintf(stderr, "permission was denied\n");
				break;
			case EROFS:
				fprintf(stderr, "the file is on a read only file-system\n");
				break;
			case ETXTBSY:
				fprintf(stderr,
						"the file seems to be currently executed (%s)\n",
						mount_device);
				break;
			}
			perror("open");
			errno = 0;
			last_exit = 1;
			return;
		}
		break;
	}
	if (lseek(fd, PFS_B0_OFFSET_BLOCK_SIZE,
	/*		*/SEEK_SET) != PFS_B0_OFFSET_BLOCK_SIZE) {
		perror("lseek");
		last_exit = 1;
		return;
	}
	i32 block_size;
	sread(fd, &block_size, 4,
			printf("error while reading the block size!"); exit(1);)
	struct bm_block_manager *bm = bm_new_file_block_manager(fd, block_size);
	if (!bm) {
		fprintf(stderr, "the block manager could not be created (%s)\n",
				pfs_error());
		pfs_errno = 0;
		last_exit = 1;
		return;
	}
	if (!pfs_load(bm, NULL)) {
		fprintf(stderr, "PFS-load failed (%s)\n", pfs_error());
		bm->close_bm(bm);
		pfs_errno = 0;
		last_exit = 1;
		return;
	}
	mount = mount_folder;
	last_exit = 0;
	return;
}

static inline void bc_umount(char **args) {
	if (!args[1]) {
		fprintf(stderr, "not enugh args\n");
		last_exit = 1;
		return;
	}
	if (args[2]) {
		fprintf(stderr, "too many args\n");
		last_exit = 1;
		return;
	}
	char *unmount = extract_path(args[1]);
	if (strcmp(unmount, mount)) {
		fprintf(stderr, "there is no such PFS-mountpoint (%s)\n", unmount);
		last_exit = 1;
		return;
	}
	free(unmount);
	if (!pfs_close()) {
		fprintf(stderr, "failed to close the PFS (%s)\n", pfs_error());
		pfs_errno = 0;
		last_exit = 1;
		return;
	}
	free(mount);
	mount = NULL;
	last_exit = 0;
	return;
}

static inline void bc_lsm(char **args) {
	if (mount) {
		printf("PFS-mount: %s\n", mount);
	} else {
		printf("there are currently no PFS-mounts\n");
	}
	exit(0);
}

static inline void bc_ls(char **args) {
	char *path;
	if (!args[1]) {
		path = extract_path(".");
	} else if (args[2]) {
		fprintf(stderr, "too many args\n");
		exit(1);
	} else {
		path = extract_path(args[1]);
	}
	char *pfs_path = extract_pfs_path(path);
	if (pfs_path) {
		int iter = pfs_iter(*pfs_path ? pfs_path : "/", 1);
		if (iter == -1) {
			fprintf(stderr, "could not open the pfs-iter (%s)\n", pfs_error());
			pfs_errno = 0;
			exit(1);
		}
		char *name_buf = NULL;
		i64 buf_len = 0;
		while (1) {
			int e = pfs_iter_next(iter);
			if (e == -1) {
				if (pfs_errno != PFS_ERRNO_NO_MORE_ELEMNETS) {
					fprintf(stderr,
							"could not get the next element handle (%s)\n",
							pfs_error());
					pfs_errno = 0;
					exit(1);
				}
				pfs_errno = 0;
				break;
			}
			if (!pfs_element_get_name(e, &name_buf, &buf_len)) {
				fprintf(stderr, "could not get the elements name (%s)\n",
						pfs_error());
				pfs_errno = 0;
				exit(1);
			}
			ui32 flags = pfs_element_get_flags(e);
			if (flags == (ui32) -1) {
				fprintf(stderr, "could not get the elements flags (%s)\n",
						pfs_error());
				pfs_errno = 0;
				exit(1);
			}
			printf("<%s%s%s%s> %s\n",
					flags & PFS_F_FILE ?
							"FILE" :
							(flags & PFS_F_FOLDER ?
									"FOLDER" :
									(flags & PFS_F_PIPE ? "PIPE" : "UNKNOWN")) //
							, flags & PFS_F_ENCRYPTED ? " ENC" : "" //
							, flags & PFS_F_EXECUTABLE ? " EXE " : "" //
							, flags & PFS_F_HIDDEN ? " HID" : "" //
							, name_buf);
			if (!pfs_element_close(e)) {
				fprintf(stderr, "could not close the element handle (%s)\n",
						pfs_error());
				pfs_errno = 0;
				exit(1);
			}
		}
		if (!pfs_iter_close(iter)) {
			fprintf(stderr, "could not close the pfs-iter (%s)\n", pfs_error());
			pfs_errno = 0;
			exit(1);
		}
	} else {
		DIR *dir = opendir(path);
		while (1) {
			struct dirent *c = readdir(dir);
			if (!c) {
				if (errno) {
					perror("readdir");
					errno = 0;
				}
				break;
			}
			char *type;
			char *padding;
			switch (c->d_type) {
			case DT_BLK:
				type = "BLOCK-DEVICE";
				padding = "    ";
				break;
			case DT_CHR:
				type = "CHARACTER-DEVICE";
				padding = "";
				break;
			case DT_DIR:
				type = "FOLDER";
				padding = "          ";
				break;
			case DT_FIFO:
				type = "PIPE";
				padding = "            ";
				break;
			case DT_LNK:
				type = "SYM-LINK";
				padding = "        ";
				break;
			case DT_REG:
				type = "FILE";
				padding = "            ";
				break;
			case DT_SOCK:
				type = "SOCKET";
				padding = "          ";
				break;
			default:
				printf("unknown type=%d", c->d_type);
				/* no break */
			case DT_UNKNOWN: // do not call fstat
				type = "UNKNOWN";
				break;
			}
			printf("<%s>%s %s\n", type, padding, c->d_name);
		}
		closedir(dir);
	}
	exit(0);
}

static inline void LFS_write(int fd, void *buf, i64 cnt) {
	for (; cnt;) {
		i64 wrote = write(fd, buf, cnt);
		if (wrote == -1) {
			switch (errno) {
			case EAGAIN:
			case EINTR:
				errno = 0;
				continue;
			default:
				perror("write");
				fprintf(stderr, "error while writing!\n");
				errno = 0;
				exit(1);
			}
		}
		cnt -= wrote;
		buf += wrote;
	}
}

static inline void PFS_write(int s, void *buf, i64 cnt) {
	i64 wrote = pfs_stream_write(s, buf, cnt);
	if (wrote < cnt) {
		fprintf(stderr, "error while writing! (%s)\n", pfs_error());
		pfs_errno = 0;
		exit(1);
	}
}

static inline i64 LFS_read(int fd, void *buf, i64 cnt) {
	i64 reat;
	for (reat = 0; cnt;) {
		i64 r = read(fd, buf, cnt);
		if (r == -1) {
			switch (errno) {
			case EAGAIN:
			case EINTR:
				errno = 0;
				continue;
			default:
				perror("read");
				fprintf(stderr, "error while reading!\n");
				errno = 0;
				exit(1);
			}
		} else if (r) {
			cnt -= r;
			buf += r;
			reat += r;
		} else {
			return reat;
		}
	}
	return reat;
}

static inline int PFS_read(int s, void *buf, i64 cnt) {
	i64 reat = pfs_stream_read(s, buf, cnt);
	if (reat < cnt && pfs_errno) {
		fprintf(stderr, "error while reading! (%s)\n", pfs_error());
		pfs_errno = 0;
		exit(1);
	}
	return reat;
}

static inline int PFS_open(char *path, i32 flags) {
	int s = pfs_stream(path, flags);
	if (s == -1) {
		fprintf(stderr, "error while opening the stream (%s)! (%s)\n", path,
				pfs_error());
		pfs_errno = 0;
		exit(1);
	}
	return s;
}

static inline int LFS_open(char *path, int flags, int mode) {
	int fd = open(path, flags, mode);
	if (fd == -1) {
		perror("open");
		fprintf(stderr, "could not open the file!\n");
		errno = 0;
		exit(1);
	}
	return fd;
}

static inline void bc_cat(char **args) {
	if (!args[1]) {
		fprintf(stderr, "not enough args\n");
		exit(1);
	}
	if (args[2]) {
		fprintf(stderr, "too many args\n");
		exit(1);
	}
	char *path = extract_path(args[1]);
	char *pfs_dir = extract_pfs_path(path);
	void *buf = malloc(1024);
	if (pfs_dir) {
		int s = PFS_open(pfs_dir, PFS_SO_READ);
		free(path);
		while (1) {
			i64 r = PFS_read(s, buf, 1024);
			LFS_write(STDOUT_FILENO, buf, r);
			if (r < 1024) {
				break;
			}
		}
	} else {
		int fd = LFS_open(path, O_RDONLY, 0);
		free(path);
		while (1) {
			int r = LFS_read(fd, buf, 1024);
			LFS_write(STDOUT_FILENO, buf, r);
			if (r < 1024) {
				break;
			}
		}
	}
	exit(0);
}

static inline void bc_cp(char **args) {
	if (!args[1] || !args[2]) {
		fprintf(stderr, "not enough args\n");
		exit(1);
	}
	if (args[3]) {
		fprintf(stderr, "too many args\n");
		exit(1);
	}
	void *buf = malloc(1024);
	char *source_path = extract_path(args[1]);
	char *target_path = extract_path(args[2]);
	char *source_pfs_path = extract_pfs_path(source_path);
	char *target_pfs_path = extract_pfs_path(target_path);
	if (target_pfs_path) {
		int ts = PFS_open(target_pfs_path,
		PFS_SO_WRITE | PFS_SO_ALSO_CREATE | PFS_SO_FILE_TRUNC);
		free(target_path);
		if (source_pfs_path) {
			int ss = PFS_open(source_pfs_path, PFS_SO_READ);
			free(source_path);
			while (1) {
				i64 r = PFS_read(ss, buf, 1024);
				PFS_write(ts, buf, r);
				if (r < 1024) {
					break;
				}
			}
			pfs_stream_close(ss);
		} else {
			int sfd = PFS_open(source_path, O_RDONLY);
			free(source_path);
			while (1) {
				i64 r = LFS_read(sfd, buf, 1024);
				PFS_write(ts, buf, r);
				if (r < 1024) {
					break;
				}
			}
			close(sfd);
		}
		pfs_stream_close(ts);
	} else {
		int tfd = LFS_open(target_path, O_WRONLY | O_CREAT | O_TRUNC,
		/*  		*/S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH);
		free(target_path);
		if (source_pfs_path) {
			int ss = PFS_open(source_pfs_path, PFS_SO_READ);
			free(source_path);
			while (1) {
				i64 r = PFS_read(ss, buf, 1024);
				LFS_write(tfd, buf, r);
				if (r < 1024) {
					break;
				}
			}
			pfs_stream_close(ss);
		} else {
			int sfd = PFS_open(source_path, O_RDONLY);
			free(source_path);
			while (1) {
				i64 r = LFS_read(sfd, buf, 1024);
				LFS_write(tfd, buf, r);
				if (r < 1024) {
					break;
				}
			}
			close(sfd);
		}
		close(tfd);
	}
	exit(0);
}

static inline void bc_mkdir(char **args) {
	if (!args[1]) {
		fprintf(stderr, "not enough args\n");
		exit(1);
	}
	if (args[2]) {
		fprintf(stderr, "too many args\n");
		exit(1);
	}
	char *path = extract_path(args[1]);
	char *pfs_path = extract_pfs_path(path);
	if (pfs_path) {
		char *last = strrchr(pfs_path, '/');
		if (last == NULL) {
			fprintf(stderr, "invalid argument path! (%s)\n", args[1]);
			exit(1);
		}
		int f;
		if (last == pfs_path) {
			f = pfs_handle_folder("/");
		} else {
			*last = '\0';
			f = pfs_handle_folder(pfs_path);
		}
		if (f == -1) {
			fprintf(stderr,
					"could not open the handle for the parent folder (%s)\n",
					pfs_error());
			pfs_errno = 0;
			exit(1);
		}
		if (!pfs_folder_create_folder(f, last + 1)) {
			fprintf(stderr, "could not create the child folder (%s)\n",
					pfs_error());
			pfs_errno = 0;
			exit(1);
		}
		pfs_element_close(f);
	} else {
		if (mkdir(path, S_IRWXU | S_IRGRP | S_IWGRP | S_IROTH) == -1) {
			perror("mkdir");
			fprintf(stderr, "mkdir failed\n");
			exit(1);
		}
	}
	free(path);
	exit(0);
}

static inline void bc_rm(char **args) {
	if (!args[1]) {
		fprintf(stderr, "not enough args\n");
		exit(1);
	}
	if (args[2]) {
		fprintf(stderr, "too many args\n");
		exit(1);
	}
	char *path = extract_path(args[1]);
	char *pfs_path = extract_pfs_path(path);
	if (pfs_path) {
		int e = pfs_handle_folder(pfs_path);
		free(path);
		if (e == -1) {
			fprintf(stderr,
					"could not open the handle for the file/pipe (%s)\n",
					pfs_error());
			pfs_errno = 0;
			exit(1);
		}
		ui32 flags = pfs_element_get_flags(e);
		if (flags == -1 || (flags & (PFS_F_FILE | PFS_F_PIPE))) {
			if (flags == -1) {
				fprintf(stderr, "could not get the flags (%s)\n", pfs_error());
				pfs_errno = 0;
			} else {
				fprintf(stderr, "I won't delete a folder!\n");
			}
			exit(1);
		}
		pfs_element_delete(e);
	} else {
		struct stat s;
		if (stat(path, &s) == -1) {
			perror("stat");
			errno = 0;
			exit(1);
		}
		switch (s.st_mode) {
		case S_IFREG: // file
		case S_IFIFO: // fi-fo
		case S_IFLNK: // sym-link
			break;
		case S_IFDIR:
			fprintf(stderr, "I won't delete a folder!\n");
			exit(1);
		case S_IFSOCK:
			fprintf(stderr, "I won't delete a socket!\n");
			exit(1);
		case S_IFBLK:
			fprintf(stderr, "I won't delete a block device!\n");
			exit(1);
		case S_IFCHR:
			fprintf(stderr, "I won't delete a character device!\n");
			exit(1);
		default:
			fprintf(stderr, "I don't even know what this is!\n");
			exit(1);
		}
		if (unlink(path) == -1) {
			perror("unlink");
			fprintf(stderr, "error on unlink!\n");
			exit(1);
		}
	}
	exit(0);
}

static inline void bc_rmdir(char **args) {
	if (!args[1]) {
		fprintf(stderr, "not enough args\n");
		exit(1);
	}
	if (args[2]) {
		fprintf(stderr, "too many args\n");
		exit(1);
	}
	char *path = extract_path(args[1]);
	char *pfs_path = extract_pfs_path(path);
	if (pfs_path) {
		int e = pfs_handle_folder(*pfs_path ? pfs_path : "/");
		free(path);
		if (e == -1) {
			fprintf(stderr,
					"could not open the handle for the file/pipe (%s)\n",
					pfs_error());
			pfs_errno = 0;
			exit(1);
		}
		ui32 flags = pfs_element_get_flags(e);
		if (flags == -1 || (flags & (PFS_F_FOLDER))) {
			if (flags == -1) {
				fprintf(stderr, "could not get the flags (%s)\n", pfs_error());
				pfs_errno = 0;
			} else {
				fprintf(stderr, "this is no folder!\n");
			}
			exit(1);
		}
		pfs_element_delete(e);
	} else {
		struct stat s;
		if (stat(path, &s) == -1) {
			perror("stat");
			errno = 0;
			exit(1);
		}
		if (s.st_mode != S_IFDIR) {
			fprintf(stderr, "this is no folder!\n");
			exit(1);
		}
		if (rmdir(path) == -1) {
			perror("rmdir");
			errno = 0;
			fprintf(stderr, "error on rmdir!\n");
			exit(1);
		}
	}
	exit(0);
}

