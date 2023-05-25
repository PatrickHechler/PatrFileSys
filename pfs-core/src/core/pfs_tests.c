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
 * pfs_tests.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#define PRINT_PFS

#include "pfs.h"

#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/stat.h>

#define BLOCK_COUNT (1L << 20)

static void checks();

static void simple_check();
static void file_check();
static void read_write_file_check();
static void append_file_check();
static void folder_check();
static void deep_folder_check();
static void real_file_sys_check();
static void meta_check();
static void pipe_check();

#ifdef PRINT_PFS

void pfs_simple_print(i64 max_depth, pfs_eh folder, char **buf, i64 *len) {
	if (max_depth < 0) {
		return;
	}
	int is_root = 0;
	if (!folder) {
		is_root = 1;
		folder = pfsc_root();
		buf = malloc(sizeof(void*) * 2);
		len = ((void*) buf) + sizeof(void*);
		*buf = malloc(1024);
		*len = 1024;
		(*buf)[0] = '/';
		(*buf)[1] = '\0';
	}
	i64 nl = strlen(*buf);
	pfs_fi iter = pfsc_folder_iterator(folder, 1);
	while (1) {
		if (!pfsc_folder_iter_next(iter)) {
			(*buf)[nl] = '\0';
			if (is_root) {
				free(folder);
				free(*buf);
				free(buf);
			}
			if ((*pfs_err_loc) == PFS_ERRNO_NO_MORE_ELEMENTS) {
				free(iter);
				return;
			} else {
				pfs_perror("folder iter next");
				free(iter);
				return;
			}
		}
		i64 cnl = pfsc_element_get_name_length(folder);
		if (cnl == -1) {
			pfs_perror("get name length");
			free(iter);
			return;
		}
		while (*len - nl - 16 <= cnl) {
			*buf = realloc(*buf, *len += 1024);
		}
		char *cs = *buf + nl;
		if (!pfsc_element_get_name(folder, &cs, len)) {
			pfs_perror("get name");
			free(iter);
			return;
		}
		ui32 flags = pfsc_element_get_flags(folder);
		if (flags & PFS_F_FILE) {
			(*buf)[nl + cnl] = ' ';
			(*buf)[nl + cnl + 1] = '<';
			(*buf)[nl + cnl + 2] = 'F';
			(*buf)[nl + cnl + 3] = 'I';
			(*buf)[nl + cnl + 4] = 'L';
			(*buf)[nl + cnl + 5] = 'E';
			(*buf)[nl + cnl + 6] = '>';
			(*buf)[nl + cnl + 7] = '\n';
			(*buf)[nl + cnl + 8] = '\0';
		} else if (flags & PFS_F_PIPE) {
			(*buf)[nl + cnl] = ' ';
			(*buf)[nl + cnl + 1] = '<';
			(*buf)[nl + cnl + 2] = 'P';
			(*buf)[nl + cnl + 3] = 'I';
			(*buf)[nl + cnl + 4] = 'P';
			(*buf)[nl + cnl + 5] = 'E';
			(*buf)[nl + cnl + 6] = '>';
			(*buf)[nl + cnl + 7] = '\n';
			(*buf)[nl + cnl + 8] = '\0';
		} else if (flags & PFS_F_FOLDER) {
			(*buf)[nl + cnl] = '/';
			(*buf)[nl + cnl + 1] = ' ';
			(*buf)[nl + cnl + 2] = '<';
			(*buf)[nl + cnl + 3] = 'F';
			(*buf)[nl + cnl + 4] = 'O';
			(*buf)[nl + cnl + 5] = 'L';
			(*buf)[nl + cnl + 6] = 'D';
			(*buf)[nl + cnl + 7] = 'E';
			(*buf)[nl + cnl + 8] = 'R';
			(*buf)[nl + cnl + 9] = '>';
			(*buf)[nl + cnl + 10] = '\n';
			(*buf)[nl + cnl + 11] = '\0';
		} else {
			(*buf)[nl + cnl] = ' ';
			(*buf)[nl + cnl + 1] = '<';
			(*buf)[nl + cnl + 2] = 'U';
			(*buf)[nl + cnl + 3] = 'N';
			(*buf)[nl + cnl + 4] = 'K';
			(*buf)[nl + cnl + 5] = 'N';
			(*buf)[nl + cnl + 6] = 'O';
			(*buf)[nl + cnl + 7] = 'W';
			(*buf)[nl + cnl + 8] = 'N';
			(*buf)[nl + cnl + 9] = '>';
			(*buf)[nl + cnl + 10] = '\n';
			(*buf)[nl + cnl + 11] = '\0';
		}
		fputs(*buf, stdout);
		if (flags & PFS_F_FOLDER) {
			(*buf)[nl + cnl + 1] = '\0';
			pfs_simple_print(max_depth - 1, folder, buf, len);
		}
	}
}

static int print_block_table0(i64 block, int long_start) {
	const char *start = "[[…].print_block_table0]:                             ";
	if (long_start) {
		start = "[[…].print_pfs.print_folder.print_block_table0]:          ";
	}
	// may be called before pfs is initialized
	if (pfs == NULL) {
		printf("%spfs is NULL! [0]\n", start);
		return 1;
	}
	void *block_data = pfs->get(pfs, block);
	if (block_data == NULL) {
		printf("%scould not get the block [1]\n", start);
		return 1;
	}
	i32 *table_end = block_data + pfs->block_size - 4;
	printf("%sblock: %ld [2]\n"
			"%s  table_end: %d [3]\n"
			"%s  table: [4]\n", start, block, start, *table_end, start);
	i32 *table_start = block_data + *table_end;
	i32 last = 0;
	int res = 0;
	for (int i = 0; (table_start + i) < table_end; i += 2) {
		printf("%s    [%d]: %d..%d [5]\n", start,
				(i64) (table_start + i) - (i64) block_data, table_start[i],
				table_start[i + 1]);
		if (last > table_start[i]) {
			printf("%s      [WARN]: invalid table! [6]\n", start);
			res++;
		}
		if (table_start[i] > table_start[i + 1]) {
			printf("%s      [WARN]: invalid table! [7]\n", start);
			res++;
		}
		last = table_start[i + 1];
	}
	printf("%s    [%d]: %d.. [8]\n", start, pfs->block_size - 4, *table_end);
	pfs->unget(pfs, block);
	return res;
}

static int print_folder(struct pfs_place fp, const char *name,
		struct pfs_place real_parent, struct pfs_place folder_entry,
		int is_helper, int deep) {
	const char *start =
			"[[…].print_pfs.print_folder]:                           [DEEP=";
	void *block_data = pfs->get(pfs, fp.block);
	if (block_data == NULL) {
		printf("%s%d]: [WARN]: block_data is NULL (block=%ld)! [0]\n", start,
				deep, fp.block);
		return 1;
	}
	int res = 0;
	struct pfs_folder *f = block_data + fp.pos;
	printf("%s%d]: block=%ld; pos=%d [1]\n"
			"%s%d]: child_count=%d [2]\n"
			"%s%d]: helper: %d [3]\n", start, deep, fp.block, fp.pos, start,
			deep, f->direct_child_count, start, deep, f->helper_index);
	if (f->real_parent.block != real_parent.block) {
		printf(
				"%s%d]: real_parent.block does not match (expected: %ld got: %ld) [4]\n",
				start, deep, real_parent.block, f->real_parent.block);
		res++;
	}
	if (f->real_parent.pos != real_parent.pos) {
		printf(
				"%s%d]: real_parent.pos does not match (expected: %d got: %d) [5]\n",
				start, deep, real_parent.pos, f->real_parent.pos);
		res++;
	}
	if (f->folder_entry.block != folder_entry.block) {
		printf(
				"%s%d]: folder_entry.block does not match (expected: %ld got: %ld) [6]\n",
				start, deep, folder_entry.block, f->folder_entry.block);
		res++;
	}
	if (f->folder_entry.pos != folder_entry.pos) {
		printf(
				"%s%d]: folder_entry.pos does not match (expected: %d got: %d) [7]\n",
				start, deep, folder_entry.pos, f->folder_entry.pos);
		res++;
	}
	i64 my_size = sizeof(struct pfs_folder)
			+ (f->direct_child_count * sizeof(struct pfs_folder_entry));
	for (i32 *table = block_data + *(i32*) (block_data + pfs->block_size - 4);
			1; table += 2) {
		if (table >= (i32*) (block_data + pfs->block_size - 4)) {
			printf("%s%d]: pos not in table! (pos=%d) [8]\n", start, deep,
					fp.pos);
			res++;
		} else if (fp.pos > *table) {
			continue;
		} else if (fp.pos < *table) {
			printf("%s%d]: pos not in table! (pos=%d) [9]\n", start, deep,
					fp.pos);
			res++;
		} else {
			i32 allocated = table[1] - table[0];
			if (allocated != my_size) {
				printf(
						"%s%d]: my_size (%ld) and my_allocated_sizes (%d) does not match! [A]\n",
						start, deep, my_size, allocated);
				res++;
			}
		}
		break;
	}
	res += print_block_table0(fp.block, 1);
	for (int i = 0; i < f->direct_child_count; i++) {
		i32 name_pos = f->entries[i].name_pos;
		const char *name;
		if (name_pos == -1) {
			name = "help-folder/no-name";
			if (i != f->helper_index) {
				res++;
				printf("non helper has no name!");
			}
		} else {
			if (i == f->helper_index) {
				res++;
				printf("helper has a name!");
			}
			name = block_data + f->entries[i].name_pos;
			for (i32 *table = block_data
					+ *(i32*) (block_data + pfs->block_size - 4); 1; table +=
					2) {
				if (table >= (i32*) (block_data + pfs->block_size - 4)) {
					printf("%s%d]: name not in table! (name_pos=%d) [B]\n",
							start, deep, name_pos);
					res++;
					res += print_block_table0(fp.block, 1);
				} else if (f->entries[i].name_pos > *table) {
					continue;
				} else if (f->entries[i].name_pos < *table) {
					printf("%s%d]: name not in table! (name_pos=%d) [C]\n",
							start, deep, name_pos);
					res++;
					res += print_block_table0(fp.block, 1);
				} else {
					char *new_name = malloc(table[1] - table[0] + 1);
					if (new_name != NULL) {
						memcpy(new_name, name, table[1] - table[0]);
						new_name[table[1] - table[0]] = '\0';
						name = new_name;
					}
				}
				break;
			}
		}
		printf("%s%d]:   [%d]: %c%s%c [D]\n"
				"%s%d]:     flags: %lu : %s [E]\n"
				"%s%d]:     create_time=%ld [F]\n", start, deep, i,
				name_pos == -1 ? '<' : '"', name, name_pos == -1 ? '>' : '"',
				start, deep, f->entries[i].flags,
				(PFS_F_FOLDER & f->entries[i].flags) ?
						"folder" :
						((PFS_F_FILE & f->entries[i].flags) ?
								"file" :
								((PFS_F_PIPE & f->entries[i].flags) ?
										"pipe" : "invalid")), start, deep,
				f->entries[i].create_time);
		if ((f->entries[i].flags & (PFS_F_FILE | PFS_F_FOLDER)) == 0) {
			res++;
		}
		if ((f->entries[i].flags & (PFS_F_FILE | PFS_F_FOLDER))
				== (PFS_F_FILE | PFS_F_FOLDER)) {
			res++;
		}
		if ((f->entries[i].flags & (PFS_F_FILE | PFS_F_HELPER_FOLDER))
				== (PFS_F_FILE | PFS_F_HELPER_FOLDER)) {
			res++;
		}
		if ((f->entries[i].flags & PFS_F_HELPER_FOLDER) != 0) {
			if (f->entries[i].name_pos != -1) {
				res++;
			}
		} else if (f->entries[i].name_pos == -1) {
			res++;
		}
		if ((f->entries[i].flags & PFS_F_FOLDER) != 0) {
			struct pfs_place cur_pos;
			cur_pos.block = fp.block;
			cur_pos.pos = fp.pos + sizeof(struct pfs_folder)
					+ i * sizeof(struct pfs_folder_entry);
			res += print_folder(f->entries[i].child_place, name,
					is_helper ? real_parent : fp, cur_pos, name_pos == -1,
					deep + 1);
		}
	}
	pfs->unget(pfs, fp.block);
	printf("%s%d]: finished print of folder %s [12]\n", start, deep, name);
	return res;
}

static int print_pfs() {
	const char *start = "[[…].print_pfs]:                                      ";
	if (pfs == NULL) {
		printf("%spfs is NULL! [0]\n", start);
		return 1;
	}
	struct pfs_b0 *b0 = pfs->get(pfs, 0);
	if (b0 == NULL) {
		printf("%sb0 is NULL! [1]\n", start);
		return 1;
	}
	struct pfs_place rp = b0->root;
	pfs->unget(pfs, 0);
	printf("%sprint now pfs [2]\n", start);
	struct pfs_place np;
	np.block = -1L;
	np.pos = -1;
	int res = print_folder(rp, "<ROOT>", np, np, 0, 0);
	printf("%sfinished printing pfs (found %d errors) [3]\n", start, res);
	return res;
}

#endif // PRINT_PFS

static ui64 flag_and;

static int (*other_set_flags)(struct bm_block_manager *bm, i64 block,
		ui64 flags);

static int my_set_flags(struct bm_block_manager *bm, i64 block, ui64 flags) {
	return other_set_flags(bm, block, flags & flag_and);
}

int main(int argc, char **argv) {
	const char *start = "[main]:                                               ";
	printf("%sstart checks with a ram block manager [0]\n", start);
	pfs = bm_new_ram_block_manager(BLOCK_COUNT, 1024);
	checks();
	pfs->close_bm(pfs);
	printf("%sstart checks with a file block manager [1]\n", start);
	const char *test_file = "./testout/testfile.pfs";
	if (argc > 1) {
		if (argc != 2) {
			printf(
					"%sinvalid argument count (Usage: %s [OPTIONAL_TESTFILE]) [2]\n",
					start, argv[0]);
			exit(EXIT_FAILURE);
		}
		test_file = argv[1];
	}
	int fd = bm_fd_open_rw(test_file);
	if (fd == -1) {
		printf("%scould not open testfile ('%s') [3]\n", start, test_file);
		exit(EXIT_FAILURE);
	}
	pfs = bm_new_file_block_manager(fd, 1024);
	checks();
	pfs->close_bm(pfs);
	printf("%sstart checks with a file block manager (again) [4]\n", start);
	fd = bm_fd_open_rw(test_file);
	if (fd == -1) {
		printf("%scould not open testfile ('%s') [5]\n", start, test_file);
		exit(EXIT_FAILURE);
	}
	new_file_pfs(fd, exit(EXIT_FAILURE);)
	checks();
	pfs->close_bm(pfs);
	printf("%sstart checks with block-flaggable ram block manager [6]\n",
			start);
	pfs = bm_new_flaggable_ram_block_manager(BLOCK_COUNT, 1024);
	checks();
	pfs->close_bm(pfs);
	printf(
			"%sstart checks with block-one-bit-flaggable ram block manager [7]\n",
			start);
	pfs = bm_new_flaggable_ram_block_manager(BLOCK_COUNT, 1024);
	other_set_flags = pfs->set_flags;
	*((int (**)(struct bm_block_manager*, i64, ui64)) &pfs->set_flags) =
			my_set_flags;
	*((int*) &pfs->block_flag_bits) = 1;
	flag_and = 1;
	checks();
	pfs->close_bm(pfs);
	printf(
			"%sstart checks with block-two-bit-flaggable ram block manager [8]\n",
			start);
	pfs = bm_new_flaggable_ram_block_manager(BLOCK_COUNT, 1024);
	other_set_flags = pfs->set_flags;
	*((int (**)(struct bm_block_manager*, i64, ui64)) &pfs->set_flags) =
			my_set_flags;
	*((int*) &pfs->block_flag_bits) = 2;
	flag_and = 3;
	checks();
	pfs->close_bm(pfs);
	printf(
			"%sstart checks with block-three-bit-flaggable ram block manager [9]\n",
			start);
	pfs = bm_new_flaggable_ram_block_manager(BLOCK_COUNT, 1024);
	other_set_flags = pfs->set_flags;
	*((int (**)(struct bm_block_manager*, i64, ui64)) &pfs->set_flags) =
			my_set_flags;
	*((int*) &pfs->block_flag_bits) = 3;
	flag_and = 7;
	checks();
	pfs->close_bm(pfs);
	printf("%sFINISH [A]\n", start);
	return EXIT_SUCCESS;
}

static void checks() {
	const char *start = "[main.checks]:                                        ";
	if (!pfsc_format(BLOCK_COUNT)) {
		printf("%scould not format the file system! [0]\n", start);
		exit(EXIT_FAILURE);
	}
#ifdef PRINT_PFS
	print_pfs();
#endif // PRINT_PFS
	printf("%sstart simple_check [1]\n", start);
	fflush(NULL);
	simple_check();
	printf("%sstart file_check [2]\n", start);
	fflush(NULL);
	file_check();
	printf("%sstart read_write_file_check [3]\n", start);
	fflush(NULL);
	read_write_file_check();
	printf("%sstart append_file_check [4]\n", start);
	fflush(NULL);
	append_file_check();
	printf("%sstart folder_check [5]\n", start);
	fflush(NULL);
	folder_check();
	printf("%sstart deep_folder_check [6]\n", start);
	fflush(NULL);
	deep_folder_check();
	printf("%sstart real_file_sys_check [7]\n", start);
	fflush(NULL);
	real_file_sys_check();
	printf("%sstart meta_check [8]\n", start);
	fflush(NULL);
	meta_check();
	printf("%sstart pipe_check [9]\n", start);
	fflush(NULL);
	pipe_check();
	printf("%sall checks executed [A]\n", start);
	fflush(NULL);
}

static void simple_check() {
	const char *start = "[main.checks.simple_check]:                           ";
	pfs_eh root = pfsc_root();
	if (root == NULL) {
		printf("%scould not get the root element [0]\n", start);
		exit(EXIT_FAILURE);
	}
	i64 child_count = pfsc_folder_child_count(root);
	if (child_count != 0) {
		printf("%sroot child count != 0 (%ld) [1]\n", start, child_count);
		exit(EXIT_FAILURE);
	}
	pfs_eh other_root = malloc(sizeof(struct pfs_element_handle));
	if (other_root == NULL) {
		printf("%scould not allocate a element handle [2]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_fill_root(other_root)) {
		printf("%scould not fill root element [3]\n", start);
		exit(EXIT_FAILURE);
	}
	if (memcmp(root, other_root, sizeof(struct pfs_element_handle)) != 0) {
		printf("%sthe two root elements differ [4]\n", start);
		exit(EXIT_FAILURE);
	}
}

/**
 * this function exits on failure and returns never a NULL pointer
 */
static void* random_data(const char *start, i64 size) {
	void *data = malloc(size);
	if (data == NULL) {
		printf("%could not allocate enough memory (size=%ld) [0]\n", start,
				size);
		exit(EXIT_FAILURE);
	}
	int urnd = open("/dev/urandom", O_RDONLY);
	if (urnd == -1) {
		printf("%could not open a read stream of file /dev/urandom [1]\n",
				start);
		exit(EXIT_FAILURE);
	}
	for (i64 remain = size, reat; remain > 0;) {
		i64 reat = read(urnd, data, remain);
		if (reat == -1) {
			if (errno == EAGAIN) {
				continue;
			}
			perror("read");
			printf("%serror at reading from /dev/urandom errno=%d [2]\n", start,
			/*			*/errno);
			fflush(NULL);
			exit(1);
		} else if (reat == 0) {
			printf("%swarning: reached EOF at file /dev/urandom [3]\n", start);
			break;
		}
		remain -= reat;
	}
	close(urnd);
	return data;
}

int debug_print(void *arg0, void *element) {
	const char *identy = arg0;
	printf("debug print [0]: %s pointer=%p\n", identy, element);
	fflush(NULL);
	printf("debug print [1]: %s block=%lX\n", identy, *(uint64_t*) element);
	fflush(NULL);
	return 1;
}

void print_blocks(char *arg0) {
	printf("print blocks [0]: %s start entrycnt=%ld maxi=%ld\n", arg0,
			pfs->loaded.entrycount, pfs->loaded.maxi);
	fflush(NULL);
	hashset_for_each(&pfs->loaded, debug_print, arg0);
	printf("print blocks [1]: %s finish\n", arg0);
	fflush(NULL);
}

// should actually be named truncate_file_check
static void file_check() {
	print_blocks("[:6:]");
	const char *start = "[main.checks.file_check]:                             ";
	const char *rd_start =
			"[main.checks.file_check.random_data]:                 ";
	pfs_eh file = pfsc_root();
	if (file == NULL) {
		printf("%scould not get the root element [0]\n", start);
		exit(EXIT_FAILURE);
	}
	i64 child_count = pfsc_folder_child_count(file);
	if (child_count != 0) {
		printf("%sroot child count != 0 (%ld) [1]\n", start, child_count);
		exit(EXIT_FAILURE);
	}
	int res = pfsc_folder_create_file(file, NULL, "file-name");
	if (!res) {
		printf("%scould not create the file [2]\n", start);
		exit(EXIT_FAILURE);
	}
	i64 length = pfsc_file_length(file);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [3]\n", start, length);
		exit(EXIT_FAILURE);
	}
	void *data = random_data(rd_start, 1016);
	res = pfsc_file_append(file, data, 1016);
	if (res != 1016) {
		printf("%scould not append to the file (appended: %ld) [4]\n", start,
				res);
		exit(EXIT_FAILURE);
	}
	length = pfsc_file_length(file);
	if (length != 1016L) {
		printf("%sfile length != 1016 (%ld) [5]\n", start, length);
		exit(EXIT_FAILURE);
	}
	print_blocks("[:5:]");
	res = pfsc_file_truncate(file, 10);
	if (!res) {
		printf("%scould not truncate the file to length 10 [6]\n", start);
		exit(EXIT_FAILURE);
	}
	print_blocks("[:4:]");
	length = pfsc_file_length(file);
	if (length != 10L) {
		printf("%sfile length != 10 (%ld) [7]\n", start, length);
		exit(EXIT_FAILURE);
	}
	print_blocks("[:3:]");
	res = pfsc_file_truncate(file, 1500);
	if (!res) {
		printf("%scould not truncate the file to length 1500 [8]\n", start);
		exit(EXIT_FAILURE);
	}
	print_blocks("[:2:]");
	length = pfsc_file_length(file);
	if (length != 1500L) {
		printf("%sfile length != 1500 (%ld) [9]\n", start, length);
		exit(EXIT_FAILURE);
	}
	print_blocks("[:1:]");
	res = pfsc_file_truncate(file, 0);
	if (!res) {
		printf("%scould not truncate the file to length 0 [A]\n", start);
		exit(EXIT_FAILURE);
	}
	print_blocks("[:0:]");
	length = pfsc_file_length(file);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [B]\n", start, length);
		exit(EXIT_FAILURE);
	}
	print_blocks("[:-1:]");
	res = pfsc_file_truncate(file, 10000);
	if (!res) {
		printf("%scould not truncate the file to length 10000 [C]\n", start);
		exit(EXIT_FAILURE);
	}
	print_blocks("[:-2:]");
	length = pfsc_file_length(file);
	if (length != 10000L) {
		printf("%sfile length != 10000 (%ld) [D]\n", start, length);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_append(file, data, 1016);
	if (res != 1016) {
		printf("%scould not append to the file (appended: %ld) [E]\n", start,
				res);
		exit(EXIT_FAILURE);
	}
	length = pfsc_file_length(file);
	if (length != 11016L) {
		printf("%sfile length != 11016 (%ld) [F]\n", start, length);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_truncate(file, 0);
	if (!res) {
		printf("%scould not truncate the file to length 0 [10]\n", start);
		exit(EXIT_FAILURE);
	}
	length = pfsc_file_length(file);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [11]\n", start, length);
		exit(EXIT_FAILURE);
	}
}

static void read_write_file_check() {
	const char *start = "[main.checks.read_write_file_check]:                  ";
	const char *rd0_start =
			"[main.checks.read_write_file_check.random_data[0]:   ";
	const char *rd1_start =
			"[main.checks.read_write_file_check.random_data[1]:   ";
	pfs_eh file = pfsc_root();
	if (file == NULL) {
		printf("%scould not get the root element [0]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_file(file, NULL, "read_write_file")) {
		printf("%scould not create the file [1]\n", start);
		exit(EXIT_FAILURE);
	}
	void *data = malloc(4098);
	void *rnd = random_data(rd0_start, 4098);
	memcpy(data, rnd, 4098);
	i64 res = pfsc_file_append(file, rnd, 4098);
	if (res != 4098) {
		printf("%scould not append the random data file (appended=%ld) [2]\n",
				start, res);
		exit(EXIT_FAILURE);
	}
	void *reat = malloc(4098);
	if (!pfsc_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [3]\n", start);
		exit(EXIT_FAILURE);
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [4]\n", start);
		exit(EXIT_FAILURE);
	}
	memset(reat, 0, 4098);
	if (!pfsc_file_read(file, 100L, reat, 3998)) {
		printf("%sfailed to read from the file [5]\n", start);
		exit(EXIT_FAILURE);
	}
	if (memcmp(data + 100, reat, 3998) != 0) {
		printf("%sdid not reat the expected data! [6]\n", start);
		exit(EXIT_FAILURE);
	}
	memset(reat, 0, 3998);
	if (!pfsc_file_read(file, 1000L, reat, 2098)) {
		printf("%sfailed to read from the file [7]\n", start);
		exit(EXIT_FAILURE);
	}
	if (memcmp(data + 1000, reat, 2098) != 0) {
		printf("%sdid not reat the expected data! [8]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [9]\n", start);
		exit(EXIT_FAILURE);
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [A]\n", start);
		exit(EXIT_FAILURE);
	}
	const i64 start1 = 0, len1 = 100;
	void *rnd2 = random_data(rd1_start, 2048);
	memcpy(data + start1, rnd2, len1);
	if (!pfsc_file_write(file, start1, rnd2, len1)) {
		printf("%sfailed to write to the file [B]\n", start);
		exit(EXIT_FAILURE);
	}
	memset(reat, 0, 4098);
	if (!pfsc_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [C]\n", start);
		exit(EXIT_FAILURE);
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the data expected data! [D]\n", start);
		exit(EXIT_FAILURE);
	}
	const i64 start2 = 1000, len2 = 412;
	memcpy(data + start2, rnd2 + len1, len2);
	if (!pfsc_file_write(file, start2, rnd2 + len1, len2)) {
		printf("%sfailed to write to the file [E]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [F]\n", start);
		exit(EXIT_FAILURE);
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [10]\n", start);
		exit(EXIT_FAILURE);
	}
	const i64 len3 = 2048 - len1 - len2, start3 = 4098 - len3;
	memcpy(data + start3, rnd2 + (len1 + len2), len3);
	if (!pfsc_file_write(file, start3, rnd2 + (len1 + len2), len3)) {
		printf("%sfailed to write to the file [11]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [12]\n", start);
		exit(EXIT_FAILURE);
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [13]\n", start);
		for (int i = 0; i < 4098; i++) {
			if ((*(unsigned char*) (data + i))
					!= (*(unsigned char*) (reat + i))) {
				if (i != 0
						&& ((*(unsigned char*) (data + i - 1))
								!= (*(unsigned char*) (reat + i - 1)))) {
					continue;
				}
				printf("%sdiff at index=%d [14]\n", start, i);
			} else if (i != 0
					&& ((*(unsigned char*) (data + i - 1))
							!= (*(unsigned char*) (reat + i - 1)))) {
				printf("%ssame at index=%d [15]\n", start, i);
			}
		}
		exit(EXIT_FAILURE);
	}
	static_assert(start1 == 0, "error");
	static_assert((start3 + len3) == 4098, "error");
}

static void append_file_check() {
	const char *start = "[main.checks.append_file_check]:                      ";
	const char *rd0_start =
			"[main.checks.append_file_check.random_data[0]:       ";
	pfs_eh file = pfsc_root();
	if (file == NULL) {
		printf("%scould not get the root element [0]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_file(file, NULL, "append_file")) {
		printf("%scould not create the file [1]\n", start);
		exit(EXIT_FAILURE);
	}
	void *data = random_data(rd0_start, 10000);
	void *data2 = random_data(rd0_start, 1000);
	void *orig;
	i64 res = pfsc_file_append(file, data, 10);
	if (res != 10) {
		printf("%scould not append to the file [2]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_length(file);
	if (res != 10) {
		printf("%sfile length is not 10 (%ld) [3]\n", start, res);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_append(file, data + 10, 2000);
	if (res != 2000) {
		printf("%scould not append to the file [4]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_length(file);
	if (res != 2010) {
		printf("%sfile length is not 2010 (%ld) [5]\n", start, res);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_append(file, data + 2010, 0);
	if (res != 0) {
		printf("%scould not append to the file [6]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_length(file);
	if (res != 2010) {
		printf("%sfile length is not 2010 (%ld) [7]\n", start, res);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_append(file, data + 2010, 1990);
	if (res != 1990) {
		printf("%scould not append to the file [8]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_length(file);
	if (res != 4000) {
		printf("%sfile length is not 4000 (%ld) [9]\n", start, res);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_append(file, data + 4000, 6000);
	if (res != 6000) {
		printf("%scould not append to the file [A]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_length(file);
	if (res != 10000) {
		printf("%sfile length is not 10000 (%ld) [B]\n", start, res);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_append(file, data2, 1000);
	if (res != 1000) {
		printf("%scould not append to the file [C]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_file_length(file);
	if (res != 11000) {
		printf("%sfile length is not 11000 (%ld) [D]\n", start, res);
		exit(EXIT_FAILURE);
	}
	void *buffer = malloc(11000);
	if (buffer == NULL) {
		printf("%scould not allocate to buffer [E]\n", start, res);
		exit(EXIT_FAILURE);
	}
	pfsc_file_read(file, 0, buffer, 11000);
	if (memcmp(data, buffer, 10000) != 0) {
		printf("%smemcmp != 0 [F]\n", start, res);
		exit(EXIT_FAILURE);
	}
	if (memcmp(data2, buffer + 10000, 1000) != 0) {
		printf("%smemcmp != 0 [10]\n", start, res);
		exit(EXIT_FAILURE);
	}
}

static void folder_check() {
	const char *start = "[main.checks.folder_check]:                           ";
	if (!pfsc_format(BLOCK_COUNT)) {
		printf("%scould not format the file system! [0]\n");
		exit(EXIT_FAILURE);
	}
	pfs_eh root = pfsc_root();
	if (root == NULL) {
		printf("%scould not get the root element [1]\n", start);
		exit(EXIT_FAILURE);
	}
	const char *n0 = "first_folder", *n1 = "the-file";
	pfs_duplicate_handle0(root, child,
			printf("%scould not duplicate the handle! [2]\n", start); exit(EXIT_FAILURE););
	if (!pfsc_folder_create_folder(child, root, n0)) {
		printf("%scould not create the folder %s [3]\n", start, n0);
		exit(EXIT_FAILURE);
	}
	memcpy(child, root, sizeof(struct pfs_element_handle));
	if (!pfsc_folder_create_file(child, root, n1)) {
		printf("%scould not create the folder %s [4]\n", start, n1);
		exit(EXIT_FAILURE);
	}
	memcpy(child, root, sizeof(struct pfs_element_handle));
	pfs_fi fi = pfsc_folder_iterator(child, 0);
	if (fi == NULL) {
		printf("%scould not get the folder-iter! [5]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_iter_next(fi)) {
		printf("%scould not get the next element from the iter! [6]\n", start);
		exit(EXIT_FAILURE);
	}
	char *name;
	i64 len = 0;
	if (!pfsc_element_get_name(child, &name, &len)) {
		printf("%scould not get the name! [7]\n", start);
		exit(EXIT_FAILURE);
	}
	if (strcmp(n0, name) != 0) {
		printf("%sgot not the expected element from the iter! (name=%s) [8]\n",
				start, name);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_iter_next(fi)) {
		printf("%scould not get the next element from the iter! [8]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_element_get_name(child, &name, &len)) {
		printf("%scould not get the name! [9]\n", start);
		exit(EXIT_FAILURE);
	}
	if (strcmp(n1, name) != 0) {
		printf("%sgot not the expected element from the iter! (name=%s) [A]\n",
				start, name);
		exit(EXIT_FAILURE);
	}
	if (pfsc_folder_iter_next(fi)) {
		printf("%sgot more elements from the iter than expected! [B]\n", start);
		exit(EXIT_FAILURE);
	}
	if ((*pfs_err_loc) != PFS_ERRNO_NO_MORE_ELEMENTS) {
		printf(
				"%s(*pfs_err_loc) has not the expected value (no-more-elements, but: %s)! [C]\n",
				start, pfs_error());
		exit(EXIT_FAILURE);
	}
	(*pfs_err_loc) = PFS_ERRNO_NONE;
}

static void deep_folder_check() {
	const char *start = "[main.checks.deep_folder_check]:                      ";
	pfs_eh parent = pfsc_root();
	if (parent == NULL) {
		printf("%scould not get the root element [0]\n", start);
		exit(EXIT_FAILURE);
	}
	pfs_duplicate_handle0(parent, child,
			printf("%scould not duplicate the handle! [1]\n", start); exit(EXIT_FAILURE););
	if (!pfsc_folder_create_folder(child, NULL, "folder-name")) {
		printf("%scould not create a child folder [2]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, NULL, "folder-name")) {
		printf("%scould not create a child folder [3]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, NULL, "folder-name")) {
		printf("%scould not create a child folder [4]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, NULL, "folder-name")) {
		printf("%scould not create a child folder [5]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, parent, "folder-name")) {
		printf("%scould not create a child folder [7]\n", start);
		exit(EXIT_FAILURE);
	}
	pfsc_element_get_parent(child);
	if (memcmp(child, parent, sizeof(struct pfs_element_handle)) != 0) {
		printf("%sdid not get the expected handle [8]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, parent, "folder-name2")) {
		printf("%scould not create a child folder [9]\n", start);
		exit(EXIT_FAILURE);
	}
	pfsc_element_get_parent(child);
	if (memcmp(child, parent, sizeof(struct pfs_element_handle)) != 0) {
		printf("%sdid not get the expected handle [A]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, parent, "folder-name3")) {
		printf("%scould not create a child folder [B]\n", start);
		exit(EXIT_FAILURE);
	}
	pfsc_element_get_parent(child);
	if (memcmp(child, parent, sizeof(struct pfs_element_handle)) != 0) {
		printf("%sdid not get the expected handle [C]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, parent, "folder-name4")) {
		printf("%scould not create a child folder [D]\n", start);
		exit(EXIT_FAILURE);
	}
	pfsc_element_get_parent(child);
	if (memcmp(child, parent, sizeof(struct pfs_element_handle)) != 0) {
		printf("%sdid not get the expected handle [E]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, parent, "folder-name5")) {
		printf("%scould not create a child folder [F]\n", start);
		exit(EXIT_FAILURE);
	}
	pfsc_element_get_parent(child);
	if (memcmp(child, parent, sizeof(struct pfs_element_handle)) != 0) {
		printf("%sdid not get the expected handle [10]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, parent, "folder-name6")) {
		printf("%scould not create a child folder [11]\n", start);
		exit(EXIT_FAILURE);
	}
	pfsc_element_get_parent(child);
	if (memcmp(child, parent, sizeof(struct pfs_element_handle)) != 0) {
		printf("%sdid not get the expected handle [12]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, parent, "folder-name7")) {
		printf("%scould not create a child folder [13]\n", start);
		exit(EXIT_FAILURE);
	}
	pfsc_element_get_parent(child);
	if (memcmp(child, parent, sizeof(struct pfs_element_handle)) != 0) {
		printf("%sdid not get the expected handle [14]\n", start);
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(child, parent, "folder-name8")) {
		printf("%scould not create a child folder [15]\n", start);
		exit(EXIT_FAILURE);
	}
	pfsc_element_get_parent(child);
	if (memcmp(child, parent, sizeof(struct pfs_element_handle)) != 0) {
		printf("%sdid not get the expected handle [16]\n", start);
		exit(EXIT_FAILURE);
	}
	if (pfsc_folder_create_folder(child, NULL, "folder-name6")) {
		printf("%scould create a child folder [17]\n", start);
		exit(EXIT_FAILURE);
	}
	if ((*pfs_err_loc) != PFS_ERRNO_ELEMENT_ALREADY_EXIST) {
		printf(
				"%s(*pfs_err_loc) has not the expected value! ((*pfs_err_loc)=%s) [18]\n",
				start, pfs_error());
		exit(EXIT_FAILURE);
	}
	(*pfs_err_loc) = PFS_ERRNO_NONE;
}

static void ensurelen(char **name, i64 *len, i64 min_len, int from_read_dir) {
	const char *start = "[main.checks.real_file_sys_check.read_dir.ensurelen]: ";
	if (!from_read_dir) {
		start = "[main.checks.real_file_sys_check.write_to.ensurelen]: ";
	}
	if (min_len <= ((*len) - 1)) {
		*name = realloc(*name, min_len + 128);
		if (*name == NULL) {
			printf(
					"%scould not realloc the name buffer! len=%ld needed=%ld [0]\n",
					start, *len, min_len);
		}
	}
}

static void read_from(DIR *dir, pfs_eh f, char **name, i64 *name_size) {
	const char *start =
			"[main.checks.real_file_sys_check.read_from]:            ";
	if (errno != 0) {
		perror("I don't know");
		printf("%serrno has not the expected value! [0]\n", start);
		exit(1);
	}
	i64 cur_len = strlen(*name);
	(*name)[cur_len++] = '/';
	pfs_duplicate_handle(f, f2)
	while (1) {
		struct dirent *entry = readdir(dir);
		if (entry == NULL) {
			if (errno == 0) {
				closedir(dir);
				free(f2);
				(*name)[--cur_len] = '\0';
				printf("%sfinish with readdir name_len=%ld name=%s [0.5]\n",
						start, cur_len, *name);
				fflush(NULL);
				return;
			}
			perror("readdir");
			printf("%scould not read the next directory entry! [1]\n", start);
			exit(1);
		}
		ensurelen(name, name_size, cur_len + strlen(entry->d_name), 1);
		strcpy((*name) + cur_len, entry->d_name);
		printf("%s  name_len=%ld, name=%s [1.5]\n", start, strlen(*name),
				*name);
		fflush(NULL);
		switch (entry->d_type) {
		case DT_UNKNOWN: {
			struct stat buf;
			if (lstat(*name, &buf) == -1) {
				printf("%scould not get the file type! [2]\n", start);
				exit(1);
			}
			switch (buf.st_mode) {
			case S_IFREG:
				goto reg_file_entry;
			case S_IFDIR:
				goto dir_entry;
			default:
				printf("%sdir-entry has an unknown type! [3]\n", start);
				exit(1);
			}
			break;
		}
		case DT_REG: {
			reg_file_entry: ;
			if (!pfsc_folder_create_file(f2, f, entry->d_name)) {
				printf("%scould not create the child file '%s'! [4]\n", start,
						*name);
				exit(1);
			}
			int fd = open64(*name, O_RDONLY);
			if (fd == -1) {
				perror("open64");
				printf("%scould not open the file (%s) [5]\n", start, *name);
				exit(1);
			}
			void *buf = malloc(1L << 15);
			if (buf == NULL) {
				printf("%scould not allocate the copy buffer [6]\n", start);
				exit(1);
			}
			int cnt = 0;
			while (1) {
				i64 reat = read(fd, buf, 1L << 15);
				if (reat == 0) {
					break;
				} else if (reat == -1) {
					switch (errno) {
					case EAGAIN:
					case EINTR:
						errno = 0;
						continue;
					default:
						perror("read");
						printf("%serror while reading from the file [7]\n",
								start);
						exit(1);
					}
				}
				i64 appended = pfsc_file_append(f2, buf, reat);
				cnt++;
				if (appended != reat) {
					printf(
							"%scould not append to the file ((*pfs_err_loc)=%s, cnt=%d) [8]\n",
							start, pfs_error(), cnt);
					exit(1);
				}
			}
			memcpy(f2, f, sizeof(struct pfs_element_handle));
			close(fd);
			free(buf);
			break;
		}
		case DT_DIR: {
			dir_entry: ;
			if ((strcmp(".", entry->d_name) == 0)
					|| (strcmp("..", entry->d_name) == 0)) {
				break;
			}
			if (!pfsc_folder_create_folder(f2, f, entry->d_name)) {
				printf("%scould not create the child folder '%s'! [9]\n", start,
						*name);
				exit(1);
			}
			DIR *sub = opendir(*name);
			if (sub == NULL) {
				printf(
						"%scould not open a dir stream for the child folder! [A]\n",
						start, *name);
				exit(1);
			}
			read_from(sub, f2, name, name_size);
			memcpy(f2, f, sizeof(struct pfs_element_handle));
			break;
		}
		default:
			printf("%sdir-entry has an unknown type! [B]\n", start);
			exit(1);
		}
	}
}

static void write_to(pfs_eh f, char **name, i64 *name_size) {
	const char *start = "[main.checks.real_file_sys_check.write_to]:           ";
	pfs_fi iter = pfsc_folder_iterator(f, 0);
	i64 cur_len = strlen(*name);
	if (iter == NULL) {
		printf(
				"%sfailed to get a iterator for the folder! ((*pfs_err_loc)=%s) [0]\n",
				start, pfs_error());
		exit(1);
	}
	(*name)[cur_len++] = '/';
	while (1) {
		if (!pfsc_folder_iter_next(iter)) {
			if ((*pfs_err_loc) != PFS_ERRNO_NO_MORE_ELEMENTS) {
				printf(
						"%sfailed to get the next element from the folder iter! ((*pfs_err_loc)=%s) [1]\n",
						start, pfs_error);
				exit(1);
			}
			free(iter);
			(*name)[--cur_len] = '\0';
			(*pfs_err_loc) = PFS_ERRNO_NONE;
			return;
		}
		*name_size -= cur_len;
		*name += cur_len;
		if (!pfsc_element_get_name(f, name, name_size)) {
			*name_size += cur_len;
			*name -= cur_len;
			printf(
					"%scould not get the name of the element ((*pfs_err_loc)=%s) [2]\n",
					start, pfs_error);
			exit(1);
		}
		*name_size += cur_len;
		*name -= cur_len;
		ui64 flags = pfsc_element_get_flags(f);
		if (flags & PFS_F_FILE) {
			void *buf = malloc(1 << 15);
			bm_fd fd = bm_fd_open_rw(*name);
			if (fd <= 0) {
				perror("open");
				printf("%scould not open the file %s [3]\n", start, *name);
				exit(1);
			}
			for (i64 len = pfsc_file_length(f), pos = 0; len > 0;) {
				i64 cpy = 1 << 15;
				if (len < cpy) {
					cpy = len;
				}
				if (!pfsc_file_read(f, pos, buf, cpy)) {
					printf(
							"%scould not read from my file %s ((*pfs_err_loc)=%s) [3]\n",
							start, *name, pfs_error);
					exit(1);
				}
				cpy = bm_fd_write(fd, buf, cpy);
				if (cpy == -1) {
					switch (errno) {
					case EAGAIN:
					case EINTR:
						errno = 0;
						continue;
					default:
						printf("%scould not write to the file %s [4]\n", start,
								*name);
						exit(1);
					}
				}
				len -= cpy;
				pos += cpy;
			}
			bm_fd_close(fd);
			free(buf);
		} else if (flags & PFS_F_FOLDER) {
			if (mkdir(*name, 0777) == -1) {
				switch (errno) {
				case EEXIST:
					if (rmdir(*name) == -1) {
						switch (errno) {
						case ENOTEMPTY:
							errno = 0;
							break;
						default:
							perror("rmdir");
							printf("%scould not remove dir %s [5]\n", start,
									*name);
							exit(1);
						}
						break;
					} else {
						errno = 0;
						if (mkdir(*name, 0777) == -1) {
							perror("mkdir");
							printf("%scould not mkdir %s [6]\n", start, *name);
							exit(1);
						}
						break;
					}
				default:
					perror("mkdir");
					printf("%scould not mkdir %s [7]\n", start, *name);
					exit(1);
				}
			}
			write_to(f, name, name_size);
		} else {
			printf("%sinvalid flags: %lu [8]\n", start, flags);
			exit(1);
		}
	}
}

static void compare(DIR *dir, DIR *dir2, char *name, char *name2) {
	const char *start = "[main.checks.real_file_sys_check.compare]:            ";
	if (errno != 0) {
		perror("I don't know");
		printf("%serrno has not the expected value! [0]\n", start);
		exit(1);
	}
	i64 cur_len = strlen(name);
	i64 cur2_len = strlen(name2);
	name[cur_len++] = '/';
	name2[cur2_len++] = '/';
	while (1) {
		struct dirent *entry = readdir(dir);
		if (entry == NULL) {
			if (errno == 0) {
				closedir(dir);
				if ((entry = readdir(dir2)) != NULL) {
					printf(
							"%scould read the next directory entry in dir2! (%s/%s) [1]\n",
							start, name, entry->d_name);
					exit(1);
				}
				if (errno == 0) {
					closedir(dir2);
					name[cur_len] = '\0';
					name2[cur2_len] = '\0';
					return;
				}
			}
			perror("readdir");
			printf("%scould not read the next directory entry! [2]\n", start);
			exit(1);
		}
		printf("%sread dir: %s [3]\n", start, entry->d_name);
		struct dirent *entry2 = readdir(dir2);
		if (entry2 == NULL) {
			perror("readdir");
			printf("%scould not read the next directory entry! [4]\n", start);
			exit(1);
		}
		printf("%sread dir: %s [5]\n", start, entry2->d_name);
		if (strcmp(entry->d_name, entry2->d_name) != 0) {
			printf("%sgot different entries '%s' and '%s'! [6]\n", start,
					entry->d_name, entry2->d_name);
			exit(1);
		}
		strcpy(name2 + cur2_len, entry->d_name);
		strcpy(name + cur_len, entry->d_name);
		if (entry->d_type != entry2->d_type) {
			printf("%sgot different entries [7]\n", start);
			exit(1);
		}
		switch (entry->d_type) {
		case DT_UNKNOWN: {
			struct stat buf;
			if (lstat(name, &buf) == -1) {
				printf("%scould not get the file type! [8]\n", start);
				exit(1);
			}
			struct stat buf2;
			if (lstat(name2, &buf2) == -1) {
				printf("%scould not get the file type! [9]\n", start);
				exit(1);
			}
			if (buf.st_mode != buf2.st_mode) {
				printf("%sgot different entries [A]\n", start);
				exit(1);
			}
			switch (buf.st_mode) {
			case S_IFREG:
				goto reg_file;
			case S_IFDIR:
				goto dir;
			default:
				printf("%sdir-entry has an unknown type! [B]\n", start);
				exit(1);
			}
		}
		case DT_REG:
			reg_file: ;
			void *buf = malloc(1 << 15);
			void *buf2 = malloc(1 << 15);
			int fd = open64(name, O_RDONLY);
			int fd2 = open64(name2, O_RDONLY);
			if (fd == -1 || fd2 == -1) {
				perror("open64");
				printf("%scould not open the files (%d:%s | %d:%s)! [C]\n",
						start, fd, name, fd2, name2);
				exit(1);
			}
			while (1) {
				i64 reat = read(fd, buf, 1 << 15);
				if (reat == -1) {
					switch (errno) {
					case EAGAIN:
					case EINTR:
						errno = 0;
						continue;
					default:
						perror("read");
						printf("%scould not read! [D]\n", start);
						exit(1);
					}
				}
				i64 reat2 = read(fd2, buf2, 1 << 15);
				if (reat != reat2) {
					perror("read");
					printf(
							"%sread retuned different values! (%ld and %ld) [E]\n",
							start, reat, reat2);
					exit(1);
				}
				if (reat == 0) {
					break;
				}
				if (memcmp(buf, buf2, reat)) {
					printf("%sread different data! [F]\n", start);
					exit(1);
				}
			}
			close(fd);
			close(fd2);
			free(buf);
			free(buf2);
			break;
		case DT_DIR:
			dir: ;
			if ((strcmp(".", entry->d_name) == 0)
					|| (strcmp("..", entry->d_name) == 0)) {
				break;
			}
			DIR *d = opendir(name);
			DIR *d2 = opendir(name2);
			if (d == NULL || d2 == NULL) {
				printf("%scould not open the dir-stream! [10]\n", start);
				exit(1);
			}
			compare(d, d2, name, name2);
			break;
		default:
			printf("%sgot an unknown dir-entry type! [11]\n", start);
			exit(1);
		}
	}
}

static void real_file_sys_check() {
	const char *start = "[main.checks.real_file_sys_check]:                    ";
	if (!pfsc_format(BLOCK_COUNT)) {
		printf("%scould not format the pfs! [0]\n", start);
		exit(EXIT_FAILURE);
	}
	i64 name_size = 128;
	char *name = malloc(name_size);
	strcpy(name, "./testin");
	i64 name2_size = 128;
	char *name2 = malloc(name_size);
	strcpy(name2, "./testout/fs-root");
	DIR *dir = opendir("./testin/");
	if (dir == NULL) {
		printf("%scould not open the ./testin/ directory! [1]\n", start);
		exit(EXIT_FAILURE);
	}
	pfs_eh root = pfsc_root();
	if (root == NULL) {
		printf("%scould not get the root! [2]\n", start);
		exit(EXIT_FAILURE);
	}
	read_from(dir, root, &name, &name_size);
#ifdef PRINT_PFS
	pfs_simple_print(0, NULL, NULL, NULL);
#endif // PRINT_PFS
	fflush(NULL);
	if (!pfsc_fill_root(root)) {
		printf("%scould not get the root! ((*pfs_err_loc)=%s) [3]\n", start,
				pfs_error);
		exit(EXIT_FAILURE);
	}
#ifdef  PRINT_PFS
	print_pfs();
#endif // PRINT_PFS
	write_to(root, &name2, &name2_size);
	fflush(NULL);
	dir = opendir("./testin/");
	if (dir == NULL) {
		printf("%scould not open the ./testin/ directory! [4]\n", start);
		exit(EXIT_FAILURE);
	}
	DIR *dir2 = opendir("./testout/fs-root/");
	if (dir2 == NULL) {
		printf("%scould not open the ./testout/fs-root/ directory! [5]\n",
				start);
		exit(EXIT_FAILURE);
	}
	compare(dir, dir2, name, name2);
}

void sub_meta_check(pfs_eh e, int is_not_root) {
	const char *start = "[main.checks.meta_check.sub_meta_check]:              ";
	i64 ct = pfsc_element_get_create_time(e);
	if ((*pfs_err_loc) != 0) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%scould not get the create time! (%s) [0]\n", start,
					pfs_error());
			exit(EXIT_FAILURE);
		}
		(*pfs_err_loc) = 0;
	} else if (!is_not_root) {
		printf("%scould get the create time! (%s) [1]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	}
	i64 t1 = time(NULL);
	if (ct > t1) {
		printf("%screate time lies in the future! [2]\n", start);
		exit(EXIT_FAILURE);
	}
	i64 lmt = pfsc_element_get_last_mod_time(e);
	if ((*pfs_err_loc) != 0) {
		printf("%scould not get the last modified time! (%s) [3]\n", start,
				pfs_error());
		exit(EXIT_FAILURE);
	}
	if (ct > lmt) {
		printf("%screate time is greater than last modified time! [4]\n",
				start);
		exit(EXIT_FAILURE);
	}
	if (lmt > t1) {
		printf("%slast modified time lies in the future! [5]\n", start);
		exit(EXIT_FAILURE);
	}
	ct ^= rand() | ((ui64) rand() << 31) | ((ui64) rand() << 62);
	if (!pfsc_element_set_create_time(e, ct)) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%scould not set the create time! [6]\n", start);
			exit(EXIT_FAILURE);
		}
		(*pfs_err_loc) = 0;
	} else if (!is_not_root) {
		printf("%scould set the create time! [7]\n", start);
		exit(EXIT_FAILURE);
	}
	if (ct != pfsc_element_get_create_time(e)) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%screate time has an unexpected value! [8]\n", start);
			exit(EXIT_FAILURE);
		}
		(*pfs_err_loc) = 0;
	} else if (!is_not_root) {
		printf("%scould get the create time! [9]\n", start);
		exit(EXIT_FAILURE);
	}
	lmt ^= rand() | ((ui64) rand() << 31) | ((ui64) rand() << 62);
	if (!pfsc_element_set_last_mod_time(e, lmt)) {
		printf("%scould not set the create time! [A]\n", start);
		exit(EXIT_FAILURE);
	}
	if (lmt != pfsc_element_get_last_mod_time(e)) {
		printf("%slast mod time has an unexpected value! [B]\n", start);
		exit(EXIT_FAILURE);
	}
	if (ct != pfsc_element_get_create_time(e)) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%screate time has the wrong value! [C]\n", start);
			exit(EXIT_FAILURE);
		}
		(*pfs_err_loc) = 0;
	} else if (!is_not_root) {
		printf("%scould get the create time! [D]\n", start);
		exit(EXIT_FAILURE);
	}
	if ((pfsc_element_get_flags(e) & PFS_F_USER_ENCRYPTED) != 0) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%sthe flags have an unexpected value! (%s) [E]\n", start,
					pfs_error());
			exit(EXIT_FAILURE);
		}
	} else if (!is_not_root) {
		printf("%scould get the flags! (%s) [E.2]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	}
	if (!pfsc_element_modify_flags(e, PFS_F_USER_ENCRYPTED, 0)) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%scould not modify the flags! [F]\n", start);
			exit(EXIT_FAILURE);
		}
		(*pfs_err_loc) = 0;
	} else if (!is_not_root) {
		printf("%scould get the flags! [10]\n", start);
		exit(EXIT_FAILURE);
	}
	if ((pfsc_element_get_flags(e) & PFS_F_USER_ENCRYPTED) == 0) {
		printf("%sthe flags have an unexpected value! [11]\n", start);
		exit(EXIT_FAILURE);
	} else if ((!is_not_root && (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER)
			|| (is_not_root && (*pfs_err_loc) != PFS_ERRNO_NONE)) {
		printf("%serror! (%s) [11.2]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	}
	(*pfs_err_loc) = 0;
	if ((pfsc_element_get_flags(e) & PFS_F_USER_ENCRYPTED) == 0) {
		printf("%sthe flags have an unexpected value! [13]\n", start);
		exit(EXIT_FAILURE);
	} else if ((!is_not_root && (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER)
			|| (is_not_root && (*pfs_err_loc) != PFS_ERRNO_NONE)) {
		printf("%serror! (%s) [14]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	}
	if (!pfsc_element_modify_flags(e, 0, PFS_F_USER_ENCRYPTED)) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%scould not modify the flags! [15]\n", start);
			exit(EXIT_FAILURE);
		}
		(*pfs_err_loc) = 0;
	} else if (!is_not_root) {
		printf("%scould get the flags! [16]\n", start);
		exit(EXIT_FAILURE);
	}
	if ((pfsc_element_get_flags(e) & PFS_F_USER_ENCRYPTED) != 0
			&& pfsc_element_get_flags(e) != -1) {
		printf("%scould not modify the flags! (%s) [17]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	} else if ((!is_not_root && (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER)
			|| (is_not_root && (*pfs_err_loc) != PFS_ERRNO_NONE)) {
		printf("%serror! (%s) [18]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	}
	if (pfsc_element_modify_flags(e, PFS_F_FILE, 0)) {
		printf("%scould modify the flags! [19]\n", start);
		exit(EXIT_FAILURE);
	} else if ((*pfs_err_loc) != PFS_ERRNO_ILLEGAL_ARG) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%s(*pfs_err_loc) has an unexpected value (%s)! [1A]\n",
					start, pfs_error());
			exit(EXIT_FAILURE);
		}
	}
	(*pfs_err_loc) = 0;
	if (pfsc_element_modify_flags(e, 0, PFS_F_FOLDER)) {
		printf("%scould modify the flags! [1B]\n", start);
		exit(EXIT_FAILURE);
	} else if ((*pfs_err_loc) != PFS_ERRNO_ILLEGAL_ARG) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%s(*pfs_err_loc) has an unexpected value (%s)! [1C]\n",
					start, pfs_error());
			exit(EXIT_FAILURE);
		}
	}
	(*pfs_err_loc) = 0;
	if (pfsc_element_modify_flags(e, PFS_F_EXECUTABLE, PFS_F_EXECUTABLE)) {
		printf("%scould modify the flags! [1D]\n", start);
		exit(EXIT_FAILURE);
	} else if ((*pfs_err_loc) != PFS_ERRNO_ILLEGAL_ARG) {
		if (is_not_root || (*pfs_err_loc) != PFS_ERRNO_ROOT_FOLDER) {
			printf("%s(*pfs_err_loc) has an unexpected value (%s)! [1E]\n",
					start, pfs_error());
			exit(EXIT_FAILURE);
		}
	}
	(*pfs_err_loc) = 0;
}

static void meta_check() {
	const char *start = "[main.checks.meta_check]:                             ";
	pfs_eh e = pfsc_root();
	if (e == NULL) {
		printf("%scould not get the root directory! (%s) [0]\n", start,
				pfs_error());
		exit(EXIT_FAILURE);
	}
	if (!pfsc_format(BLOCK_COUNT)) {
		printf("%scould not format the PFS! (%s) [0.3333333333]\n", start,
				pfs_error());
		exit(EXIT_FAILURE);
	}
	if (!pfsc_fill_root(e)) {
		printf("%scould not fill the root! (%s) [0.6666666666]\n", start,
				pfs_error());
		exit(EXIT_FAILURE);
	}
	sub_meta_check(e, 0);
	if (!pfsc_folder_create_file(e, NULL, "meta-checking-file")) {
		printf("%scould not create the folder! (%s) [1]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	}
	sub_meta_check(e, 1);
	if (!pfsc_fill_root(e)) {
		printf("%scould not get the root directory! (%s) [2]\n", start,
				pfs_error());
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_folder(e, NULL, "meta-checking-folder")) {
		printf("%scould not create the folder! (%s) [3]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	}
	sub_meta_check(e, 1);
	if (!pfsc_fill_root(e)) {
		printf("%scould not get the root directory! (%s) [4]\n", start,
				pfs_error());
		exit(EXIT_FAILURE);
	}
}

static void pipe_check() {
	const char *start = "[main.checks.pipe_check]:                             ";
	const char *rnd_start =
			"[main.checks.pipe_check.random_data]:                 ";
	pfs_eh p = pfsc_root();
	if (p == NULL) {
		printf("%scould not get the root directory! (%s) [0]\n", start,
				pfs_error());
		exit(EXIT_FAILURE);
	}
	if (!pfsc_folder_create_pipe(p, NULL, "check-pipe")) {
		printf("%scould not create the folder! (%s) [1]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	}
	if (pfsc_pipe_length(p) != 0) {
		printf("%spipe is no empty! (%s) [2]\n", start, pfs_error());
		exit(EXIT_FAILURE);
	}
	void *write_buf = random_data(rnd_start, 10000);
	void *read_buf = malloc(10000);
	if (read_buf == NULL) {
		printf("%scould not allocate the read buffer! [3]\n", start);
		exit(EXIT_FAILURE);
	}
	i64 res = pfsc_pipe_append(p, write_buf, 1000);
	if (!res) {
		printf("%scould not append to the pipe! [4]\n", start);
		exit(EXIT_FAILURE);
	}
	// pipe: write[0..1000]
	res = pfsc_pipe_length(p);
	if (res != 1000) {
		printf("%spipe has not the expected length! (len=%ld (%s)) [5]\n",
				start, pfsc_pipe_length(p), pfs_error());
		exit(EXIT_FAILURE);
	}
	res = pfsc_pipe_read(p, read_buf, 500);
	if (!res) {
		printf("%scould not read form the pipe! [6]\n", start);
		exit(EXIT_FAILURE);
	}
	// read: write[0..500]
	// pipe: write[500..1000]
	res = pfsc_pipe_length(p);
	if (res != 500) {
		printf("%spipe has not the expected length! (len=%ld (%s)) [7]\n",
				start, pfsc_pipe_length(p), pfs_error());
		exit(EXIT_FAILURE);
	}
	res = memcmp(write_buf, read_buf, 500);
	if (res != 0) {
		printf("%sdid not read the same data! [8]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_pipe_read(p, read_buf, 256);
	// read: [500..756],[256..500]
	// pipe: [756..1000]
	if (!res) {
		printf("%scould not read form the pipe! [9]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_pipe_length(p);
	if (res != 244) {
		printf("%spipe has not the expected length! [A]\n", start);
		exit(EXIT_FAILURE);
	}
	res = memcmp(write_buf + 500, read_buf, 256);
	if (res != 0) {
		printf("%sdid not read the same data! [B]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_pipe_append(p, write_buf + 1500, 1024);
	// read: [500..756],[256..500]
	// pipe: [756..1000][1500..2524]
	if (res != 1024) {
		printf("%scould not append tothe pipe! [C]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_pipe_read(p, read_buf, 512);
	// read: [756..1000],[1500..1512]
	// pipe: [1512..2524]
	if (!res) {
		printf("%scould not read form the pipe! [D]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_pipe_length(p);
	if (res != 756) {
		printf("%spipe has not the expected length! [E]\n", start);
		exit(EXIT_FAILURE);
	}
	res = pfsc_pipe_read(p, read_buf, 757);
	if (res) {
		printf("%scould read form the pipe! [F]\n", start);
		exit(EXIT_FAILURE);
	} else if ((*pfs_err_loc) != PFS_ERRNO_ILLEGAL_ARG) {
		printf("%s(*pfs_err_loc) has not the expected value (%s)! [10]\n",
				start, pfs_error());
		exit(EXIT_FAILURE);
	}
	(*pfs_err_loc) = PFS_ERRNO_NONE;
	res = pfsc_pipe_read(p, read_buf + 1024, 756);
	if (!res) {
		printf("%scould not read form the pipe! [11]\n", start);
		exit(EXIT_FAILURE);
	}
	res = memcmp(read_buf, write_buf + 756, 1000 - 756);
	if (res != 0) {
		printf("%sdid not read the expected data! [12]\n", start);
		exit(EXIT_FAILURE);
	}
	res = memcmp(read_buf + 1000 - 756, write_buf + 1500, 267);
	if (res != 0) {
//		for (int i = 0; i < 2524 - 1500; i++) {
//			if (*(char*) (read_buf + i + 1000 - 756) != *(char*) (write_buf + 1500 + i)) {
//				printf("  [%d]\n", i);
//				break;
//			}
//		}
		printf("%sdid not read the expected data! [13]\n", start);
		exit(EXIT_FAILURE);
	}
}
