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
		char *home = getenv("HOME");
		if (!home) {
			home = "/";
		}
		i64 len = strlen(home);
		cwd = malloc((len + 128) & ~127);
		memcpy(cwd, home, len);
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

int main(int argc, char **argv) {
	setup(argc, argv);
	set_signal_handlers();
	for (; 1;) {
		char **child_args = read_args();
		if (!child_args) {
			continue;
		}
		if (!*child_args) {
			continue;
		}
		if (!strcmp("exit", *child_args)) {
			bc_exit((char**) child_args);
		}
		if (!strcmp("cd", *child_args)) {
			bc_cd((char**) child_args);
			continue;
		}
		cpid = vfork();
		if (cpid == -1) {
			perror("vfork");
			last_exit = 1;
		} else if (cpid) {
			waitpid(cpid, &last_exit, 0);
		} else {
			execute(child_args);
		}
		for (int i = 0; child_args[i]; i++) {
			free(child_args[i]);
		}
		free(child_args);
		if (errno != 0) {
			perror(NULL);
			errno = 0;
		}
	}
}

static inline void execute(char **args) {
	if (!strcmp("help", *args)) {
		buildin_info("help");
		bc_help((char**) args);
	} else if (!strcmp("mkfs.pfs", *args)) {
		buildin_info("mkfs.pfs");
		bc_mkfs((char**) args);
	} else if (!strcmp("mount.pfs", *args)) {
		buildin_info("mount.pfs");
		bc_mount((char**) args);
	} else if (!strcmp("umount.pfs", *args)) {
		buildin_info("umount.pfs");
		bc_umount((char**) args);
	} else if (!strcmp("lsm.pfs", *args)) {
		buildin_info("lsm.pfs");
		bc_lsm((char**) args);
	} else if (!strcmp("ls", *args)) {
		buildin_info("ls");
		bc_ls((char**) args);
	} else if (!strcmp("cat", *args)) {
		buildin_info("cat");
		bc_cat((char**) args);
	} else if (!strcmp("cp", *args)) {
		buildin_info("cp");
		bc_cp((char**) args);
	} else if (!strcmp("mkdir", *args)) {
		buildin_info("mkdir");
		bc_mkdir((char**) args);
	} else if (!strcmp("rm", *args)) {
		buildin_info("rm");
		bc_rm((char**) args);
	} else if (!strcmp("rmdir", *args)) {
		buildin_info("rmdir");
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
	char *result;
	if (last_exit != 0) {
		result = malloc(23 + addlen);
		sprintf(result, "%d%s%s%s", last_exit, "): [pfs-shell: ", name, "]$ ");
	} else {
		result = malloc(16 + addlen);
		memcpy(result, "[pfs-shell: ", 12);
		memcpy(result + 12, name, addlen);
		result[12 + addlen] = ']';
		result[12 + addlen + 1] = '$';
		result[12 + addlen + 2] = ' ';
		result[12 + addlen + 3] = '\0';
	}
	return result;
}

static inline void buildin_info(const char *name) {
	printf("buildin %s (use /bin/%s for the non-builtin)\n", name, name);
	fflush(stdout);
}

static inline char** read_args(void) {
	char *prompt = gen_prompt();
	char *line = readline(prompt);
	if (!line) {
		exit(0);
	}
	free(prompt);
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
							if (cs) {
								c = cs;
							} else {
								c = "";
							}
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
							int wrote = snprintf(c, 64, "%d", last_exit);
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
				break;
			}
			case '\'':
#define read_string(identy, end) \
			for (; 1;) { \
				if (line[li] == end) { \
					li++; \
					break; \
				} else if (line[li] == '\\') { \
					if (al <= ai) { \
						args[an] = realloc(args[an], al += 128); \
					} \
					if (line[++li] == '\0') { \
						goto identy##_new_line; \
					} \
					args[an][ai++] = line[li++]; \
				} else if (line[li] == '\0') { \
					if (al <= ai) { \
						args[an] = realloc(args[an], al += 128); \
					} \
					identy##_new_line: \
					args[an][ai++] = '\n'; \
					li = 0; \
					line = readline("> "); \
					if (!line) { \
						fprintf(stderr, "reached end of file in the middle of a command!"); \
						exit(1); \
					} \
					add_history(line); \
					continue; \
				} \
				if (al <= ai) { \
					args[an] = realloc(args[an], al += 128); \
				} \
				args[an][ai++] = line[li++]; \
			}
			read_string(rs0, '\'')
			break;
			case '"':
			read_string(rs1, '"')
			break;
			case ' ':
			case '\t':
			space: ;
			if (args[an]) {
				args[an] = realloc(args[an], (an + 1) * sizeof(char*));
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
		big_break: ;
	}
}

static char* extract_path(const char *p) {
	char *result = malloc(128);
	i64 len = 128;
	i64 index;
	switch (p[0]) {
	case '~':
		if (p[1] != '/') {
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
			p += slen;
		} else {
			index = 0;
			p++;
		}
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
		case 1:
			goto end0;
		case 2:
			if (p[1] == '.') {
				goto end0;
			}
			break;
		case 3:
			if (p[1] == '.' && p[2] == '.') {
				char *lst = strrchr(result, '/');
				if (!lst) {
					fprintf(stderr, "on my shell the root has no parent!");
					fflush(stderr);
					free(result);
					return NULL;
				}
				*lst = '\0';
				index = lst - result;
				if (!*end) {
					goto end0;
				}
				continue;
			}
		}
		i64 cpy = p - end + 1;
		while (cpy >= len - index) {
			result = realloc(result, len += 128);
		}
		result[index++] = '/';
		memcpy(result + index, p, cpy);
		end0: p = end + 1;
		if (!*end) {
			break;
		}
	}
	result = realloc(result, index + 1);
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
	cwd = path;
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
		exit(1);
	}
	if (!args[1] || !args[2]) {
		fprintf(stderr, "not enugh args\n");
		exit(1);
	}
	if (args[3]) {
		fprintf(stderr, "too many args\n");
		exit(1);
	}
	char *mount_device = extract_path(args[1]);
	char *mount_folder = extract_path(args[2]);
	int fd;
	while (1) {
		fd = open(mount_device, O_CLOEXEC | O_RDWR,
		/*		*/S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH);
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
			exit(1);
		}
		break;
	}
	if (lseek(fd, PFS_B0_OFFSET_BLOCK_SIZE,
	SEEK_SET) != PFS_B0_OFFSET_BLOCK_SIZE) {
		perror("lseek");
		exit(1);
	}
	i32 block_size;
	sread(fd, &block_size, 4,
			printf("error while reading the block size!"); exit(1);)
	struct bm_block_manager *bm = bm_new_file_block_manager(fd, block_size);
	if (!bm) {
		fprintf(stderr, "the block manager could not be created (%s)\n",
				pfs_error());
		pfs_errno = 0;
		exit(1);
	}
	if (!pfs_load(bm, NULL)) {
		fprintf(stderr, "PFS-load failed (%s)\n", pfs_error());
		pfs_errno = 0;
		exit(1);
		bm->close_bm(bm);
	}
	mount = mount_folder;
	exit(0);
}

static inline void bc_umount(char **args) {
	if (!args[1]) {
		fprintf(stderr, "not enugh args\n");
		exit(1);
	}
	if (args[2]) {
		fprintf(stderr, "too many args\n");
		exit(1);
	}
	char *unmount = extract_path(args[1]);
	if (strcmp(unmount, mount)) {
		fprintf(stderr, "there is no such PFS-mountpoint (%s)\n", unmount);
		exit(1);
	}
	if (!pfs_close()) {
		fprintf(stderr, "failed to close the PFS (%s)\n", pfs_error());
		pfs_errno = 0;
		exit(1);
	}
	mount = NULL;
	exit(0);
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
	} else {
		path = extract_path(args[1]);
	}
	if (args[2]) {
		fprintf(stderr, "too many args\n");
		exit(1);
	}
	char *pfs_dir = extract_pfs_path(path);
	if (pfs_dir) {
		int iter = pfs_iter(pfs_dir, 1);
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
	fflush(stdout);
	void *buf = malloc(1024);
	char *pfs_dir = extract_pfs_path(path);
	if (pfs_dir) {
		int s = pfs_stream(pfs_dir, PFS_SO_READ);
		if (s == -1) {
			fprintf(stderr, "could not open the file (%s)!\n", pfs_error());
			pfs_errno = 0;
			exit(1);
		}
		while (1) {
			i64 reat = pfs_stream_read(s, buf, 1024);
			if (pfs_errno) {
				fprintf(stderr, "error while reading (%s)!\n", pfs_error());
				pfs_errno = 0;
				exit(1);
			}
			for (i64 remain = reat; remain;) {
				i64 wrote = write(STDOUT_FILENO, buf, remain);
				if (wrote == -1) {
					fprintf(stderr, "error while writing!\n");
					perror("write");
					errno = 0;
					exit(1);
				}
				remain -= wrote;
			}
			if (reat < 1024) {
				break;
			}
		}
	} else {
		int fd = open(path, O_RDONLY);
		if (fd == -1) {
			fprintf(stderr, "could not open the file!\n");
			perror("open");
			errno = 0;
			exit(1);
		}
		while (1) {
			i64 reat = read(fd, buf, 1024);
			if (reat == -1) {
				fprintf(stderr, "error while reading!\n");
				perror("read");
				errno = 0;
				exit(1);
			} else if (reat == 0) {
				break;
			}
			for (i64 remain = reat; remain;) {
				i64 wrote = write(STDOUT_FILENO, buf, remain);
				if (wrote == -1) {
					fprintf(stderr, "error while writing!\n");
					perror("write");
					errno = 0;
					exit(1);
				}
				remain -= wrote;
			}
		}
	}
	exit(0);
}

static inline void bc_cp(char **args) {
	abort();
}

static inline void bc_mkdir(char **args) {
	abort();
}

static inline void bc_rm(char **args) {
	abort();
}

static inline void bc_rmdir(char **args) {
	abort();
}

