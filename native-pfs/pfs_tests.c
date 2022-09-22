/*
 * pfs_tests.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#include "bm.h"
#include "pfs.h"
#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <errno.h>

static int checks();

static int simple_check();
static int file_check();
static int read_write_file_check();
static int append_file_check();
static int folder_check();
static int deep_folder_check();

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
		printf("%s    [%d]: %d..%d [5]\n", start, (i64) (table_start + i) - (i64) block_data,
		        table_start[i], table_start[i + 1]);
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

#include "pfs-intern.h"

static int print_folder(struct pfs_place fp, const char *name, struct pfs_place real_parent,
        struct pfs_place folder_entry, int is_helper) {
	const char *start = "[[…].print_pfs.print_folder]:                           ";
	void *block_data = pfs->get(pfs, fp.block);
	if (block_data == NULL) {
		printf("%s[WARN]: block_data is NULL (block=%ld)! [0]\n", start, fp.block);
		return 1;
	}
	int res = 0;
	struct pfs_folder *f = block_data + fp.pos;
	printf("%sblock=%ld; pos=%d [1]\n"
			"%schild_count=%d [2]\n"
			"%shelper: %d [3]\n", start, fp.block, fp.pos, start, f->direct_child_count, start,
	        f->helper_index);
	if (f->real_parent.block != real_parent.block) {
		printf("%sreal_parent.block does not match (expected: %ld got: %ld) [4]\n", start,
		        real_parent.block, f->real_parent.block);
		res++;
	}
	if (f->real_parent.pos != real_parent.pos) {
		printf("%sreal_parent.pos does not match (expected: %d got: %d) [5]\n", start,
		        real_parent.pos, f->real_parent.pos);
		res++;
	}
	if (f->folder_entry.block != folder_entry.block) {
		printf("%sfolder_entry.block does not match (expected: %ld got: %ld) [6]\n", start,
		        folder_entry.block, f->folder_entry.block);
		res++;
	}
	if (f->folder_entry.pos != folder_entry.pos) {
		printf("%sfolder_entry.pos does not match (expected: %d got: %d) [7]\n", start,
		        folder_entry.pos, f->folder_entry.pos);
		res++;
	}
	i64 my_size = sizeof(struct pfs_folder)
	        + (f->direct_child_count * sizeof(struct pfs_folder_entry));
	for (i32 *table = block_data + *(i32*) (block_data + pfs->block_size - 4); 1; table += 2) {
		if (table >= (i32*) (block_data + pfs->block_size - 4)) {
			printf("%spos not in table! (pos=%d) [8]\n", start, fp.pos);
			res++;
		} else if (fp.pos > *table) {
			continue;
		} else if (fp.pos < *table) {
			printf("%spos not in table! (pos=%d) [9]\n", start, fp.pos);
			res++;
		} else {
			i32 allocated = table[1] - table[0];
			if (allocated != my_size) {
				printf("%smy_size (%ld) and my_allocated_sizes (%d) does not match! [A]\n", start,
				        my_size, allocated);
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
		} else {
			name = block_data + f->entries[i].name_pos;
			for (i32 *table = block_data + *(i32*) (block_data + pfs->block_size - 4); 1; table +=
			        2) {
				if (table >= (i32*) (block_data + pfs->block_size - 4)) {
					printf("%sname not in table! (name_pos=%d) [B]\n", start, name_pos);
					res++;
					res += print_block_table0(fp.block, 1);
				} else if (f->entries[i].name_pos > *table) {
					continue;
				} else if (f->entries[i].name_pos < *table) {
					printf("%sname not in table! (name_pos=%d) [C]\n", start, name_pos);
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
		printf("%s  [%d]: %c%s%c [D]\n"
				"%s    flags: %lu : %s [E]\n"
				"%s    create_time=%ld [F]\n", start, i, name_pos == -1 ? '<' : '"', name,
		        name_pos == -1 ? '>' : '"', start, f->entries[i].flags,
		        (PFS_FLAGS_FOLDER & f->entries[i].flags) ? "folder" :
		                ((PFS_FLAGS_FILE & f->entries[i].flags) ? "file" : "invalid"), start,
		        f->entries[i].create_time);
		if ((f->entries[i].flags & (PFS_FLAGS_FILE | PFS_FLAGS_FOLDER)) == 0) {
			res++;
		}
		if ((f->entries[i].flags & (PFS_FLAGS_FILE | PFS_FLAGS_FOLDER))
		        == (PFS_FLAGS_FILE | PFS_FLAGS_FOLDER)) {
			res++;
		}
		if ((f->entries[i].flags & (PFS_FLAGS_FILE | PFS_FLAGS_HELPER_FOLDER))
		        == (PFS_FLAGS_FILE | PFS_FLAGS_HELPER_FOLDER)) {
			res++;
		}
		if ((f->entries[i].flags & PFS_FLAGS_HELPER_FOLDER) != 0) {
			if (f->entries[i].name_pos != -1) {
				res++;
			}
		} else if (f->entries[i].name_pos == -1) {
			res++;
		}
		if ((f->entries[i].flags & PFS_FLAGS_FOLDER) != 0) {
			struct pfs_place cur_pos;
			cur_pos.block = fp.block;
			cur_pos.pos = fp.pos + sizeof(struct pfs_folder) + i * sizeof(struct pfs_folder_entry);
			res += print_folder(f->entries[i].child_place, name, is_helper ? real_parent : fp,
			        cur_pos, name_pos == -1);
		}
	}
	pfs->unget(pfs, fp.block);
	printf("%sfinished print of folder %s [12]\n", start, name);
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
	int res = print_folder(rp, "<ROOT>", np, np, 0);
	printf("%sfinished printing pfs (found %d errors) [3]\n", start, res);
	return res;
}

int main(int argc, char **argv) {
	const char *start = "[main]:                                               ";
	printf("%sstart checks with a ram block manager [0]\n", start);
	pfs = bm_new_ram_block_manager(512L, 1024);
	int res = checks();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	pfs->close_bm(pfs);
	printf("%sstart checks with a file block manager [1]\n", start);
	const char *test_file = "./testout/testfile.pfs";
	int open_mode = O_RDWR | O_CREAT | O_TRUNC;
	if (argc > 1) {
		if (argc != 2) {
			printf("%sinvalid argument count (Usage: %s [OPTIONAL_TESTFILE]) [2]\n", start,
			        argv[0]);
			return EXIT_FAILURE;
		}
		test_file = argv[1];
		open_mode = O_RDWR | O_CREAT;
	}
	int fd = open(test_file, open_mode, 0666);
	if (fd == -1) {
		printf("%scould not open testfile ('%s') [3]\n", start, test_file);
		return EXIT_FAILURE;
	}
	pfs = bm_new_file_block_manager(fd, 1024);
	res = checks();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	pfs->close_bm(pfs);
	printf("%sstart checks with a file block manager (again) [4]\n", start);
	fd = open(test_file, O_RDWR, 0666);
	if (fd == -1) {
		printf("%scould not open testfile ('%s') [5]\n", start, test_file);
		return EXIT_FAILURE;
	}
	new_file_pfs(fd, return EXIT_FAILURE;)
	res = checks();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	pfs->close_bm(pfs);
	printf("%sstart checks with block-flaggable ram block manager [6]\n", start);
	pfs = bm_new_ram_block_manager(512L, 1024);
	res = checks();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	pfs->close_bm(pfs);
	printf("%sFINISH [7]\n", start);
	return EXIT_SUCCESS;
}

static int checks() {
	const char *start = "[main.checks]:                                        ";
	if (!pfs_format(512L)) {
		printf("%scould not format the file system! [0]\n", start);
		return EXIT_FAILURE;
	}
	print_pfs();
	printf("%sstart simple_check [1]\n", start);
	int res = simple_check();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	printf("%sstart file_check [2]\n", start);
	res = file_check();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	printf("%sstart read_write_file_check [3]\n", start);
	res = read_write_file_check();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	printf("%sstart append_file_check [4]\n", start);
	res = append_file_check();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	printf("%sstart folder_check [5]\n", start);
	res = folder_check();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	printf("%sstart deep_folder_check [6]\n", start);
	res = deep_folder_check();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	// TODO check with real file sys
	printf("%sall checks executed [7]\n", start);
	return EXIT_SUCCESS;
}

static int simple_check() {
	const char *start = "[main.checks.simple_check]:                           ";
	pfs_eh root = pfs_root();
	if (root == NULL) {
		printf("%scould not get the root element [0]\n", start);
		return EXIT_FAILURE;
	}
	i64 child_count = pfs_folder_child_count(root);
	if (child_count != 0) {
		printf("%sroot child count != 0 (%ld) [1]\n", start, child_count);
		return EXIT_FAILURE;
	}
	pfs_eh other_root = malloc(PFS_EH_SIZE);
	if (other_root == NULL) {
		printf("%scould not allocate a element handle [2]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_fill_root(other_root)) {
		printf("%scould not fill root element [3]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(root, other_root, PFS_EH_SIZE) != 0) {
		printf("%sthe two root elements differ [4]\n", start);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}

/**
 * this function exits on failure and returns never a NULL pointer
 */
static void* random_data(const char *start, i64 size) {
	void *data = malloc(size);
	if (data == NULL) {
		printf("%could not allocate enough memory (size=%ld) [0]\n", start, size);
		fflush(NULL);
		exit(EXIT_FAILURE);
	}
	int urnd = open("/dev/urandom", O_RDONLY);
	if (urnd == -1) {
		printf("%could not open a read stream of file /dev/urandom [1]\n", start);
		fflush(NULL);
		exit(EXIT_FAILURE);
	}
	for (i64 remain = size, reat; remain > 0;) {
		i64 reat = read(urnd, data, remain);
		if (reat == -1) {
			if (errno == EAGAIN) {
				continue;
			}
			printf("%serror at reading from /dev/urandom errno=%d [2]\n", start,
			errno);
			abort();
		} else if (reat == 0) {
			printf("%swarning: reached EOF at file /dev/urandom [3]\n", start);
			break;
		}
		remain -= reat;
	}
	close(urnd);
	return data;
}

// should actually be named truncate_file_check
static int file_check() {
	const char *start = "[main.checks.file_check]:                             ";
	const char *rd_start = "[main.checks.file_check.random_data]:                 ";
	pfs_eh file = pfs_root();
	if (file == NULL) {
		printf("%scould not get the root element [0]\n", start);
		return EXIT_FAILURE;
	}
	i64 child_count = pfs_folder_child_count(file);
	if (child_count != 0) {
		printf("%sroot child count != 0 (%ld) [1]\n", start, child_count);
		return EXIT_FAILURE;
	}
	int res = pfs_folder_create_file(file, NULL, "file-name");
	if (!res) {
		printf("%scould not create the file [2]\n", start);
		return EXIT_FAILURE;
	}
	i64 length = pfs_file_length(file);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [3]\n", start, length);
		return EXIT_FAILURE;
	}
	void *data = random_data(rd_start, 1016);
	res = pfs_file_append(file, data, 1016);
	if (res != 1016) {
		printf("%scould not append to the file (appended: %ld) [4]\n", start, res);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 1016L) {
		printf("%sfile length != 1016 (%ld) [5]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 10);
	if (!res) {
		printf("%scould not truncate the file to length 10 [6]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 10L) {
		printf("%sfile length != 10 (%ld) [7]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 1500);
	if (!res) {
		printf("%scould not truncate the file to length 1500 [8]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 1500L) {
		printf("%sfile length != 1500 (%ld) [9]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 0);
	if (!res) {
		printf("%scould not truncate the file to length 0 [A]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [B]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 10000);
	if (!res) {
		printf("%scould not truncate the file to length 10000 [C]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 10000L) {
		printf("%sfile length != 10000 (%ld) [D]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data, 1016);
	if (res != 1016) {
		printf("%scould not append to the file (appended: %ld) [E]\n", start, res);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 11016L) {
		printf("%sfile length != 11016 (%ld) [F]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 0);
	if (!res) {
		printf("%scould not truncate the file to length 0 [10]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [11]\n", start, length);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}

static int read_write_file_check() {
	const char *start = "[main.checks.read_write_file_check]:                  ";
	const char *rd0_start = "[main.checks.read_write_file_check.random_data[0]:   ";
	const char *rd1_start = "[main.checks.read_write_file_check.random_data[1]:   ";
	pfs_eh file = pfs_root();
	if (file == NULL) {
		printf("%scould not get the root element [0]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_file(file, NULL, "read_write_file")) {
		printf("%scould not create the file [1]\n", start);
		return EXIT_FAILURE;
	}
	void *data = malloc(4098);
	void *rnd = random_data(rd0_start, 4098);
	memcpy(data, rnd, 4098);
	i64 res = pfs_file_append(file, rnd, 4098);
	if (res != 4098) {
		printf("%scould not append the random data file (appended=%ld) [2]\n", start, res);
		return EXIT_FAILURE;
	}
	void *reat = malloc(4098);
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [3]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [4]\n", start);
		return EXIT_FAILURE;
	}
	memset(reat, 0, 4098);
	if (!pfs_file_read(file, 100L, reat, 3998)) {
		printf("%sfailed to read from the file [5]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data + 100, reat, 3998) != 0) {
		printf("%sdid not reat the expected data! [6]\n", start);
		return EXIT_FAILURE;
	}
	memset(reat, 0, 3998);
	if (!pfs_file_read(file, 1000L, reat, 2098)) {
		printf("%sfailed to read from the file [7]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data + 1000, reat, 2098) != 0) {
		printf("%sdid not reat the expected data! [8]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [9]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [A]\n", start);
		return EXIT_FAILURE;
	}
	const i64 start1 = 0, len1 = 100;
	void *rnd2 = random_data(rd1_start, 2048);
	memcpy(data + start1, rnd2, len1);
	if (!pfs_file_write(file, start1, rnd2, len1)) {
		printf("%sfailed to write to the file [B]\n", start);
		return EXIT_FAILURE;
	}
	memset(reat, 0, 4098);
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [C]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the data expected data! [D]\n", start);
		return EXIT_FAILURE;
	}
	const i64 start2 = 1000, len2 = 412;
	memcpy(data + start2, rnd2 + len1, len2);
	if (!pfs_file_write(file, start2, rnd2 + len1, len2)) {
		printf("%sfailed to write to the file [E]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [F]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [10]\n", start);
		return EXIT_FAILURE;
	}
	const i64 len3 = 2048 - len1 - len2, start3 = 4098 - len3;
	memcpy(data + start3, rnd2 + (len1 + len2), len3);
	if (!pfs_file_write(file, start3, rnd2 + (len1 + len2), len3)) {
		printf("%sfailed to write to the file [11]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [12]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [13]\n", start);
		for (int i = 0; i < 4098; i++) {
			if ((*(unsigned char*) (data + i)) != (*(unsigned char*) (reat + i))) {
				if (i != 0
				        && ((*(unsigned char*) (data + i - 1)) != (*(unsigned char*) (reat + i - 1)))) {
					continue;
				}
				printf("%sdiff at index=%d [14]\n", start, i);
			} else if (i != 0
			        && ((*(unsigned char*) (data + i - 1)) != (*(unsigned char*) (reat + i - 1)))) {
				printf("%ssame at index=%d [15]\n", start, i);
			}
		}
		return EXIT_FAILURE;
	}
	static_assert(start1 == 0, "error");
	static_assert((start3 + len3) == 4098, "error");
	return EXIT_SUCCESS;
}

static int append_file_check() {
	const char *start = "[main.checks.append_file_check]:                      ";
	const char *rd0_start = "[main.checks.append_file_check.random_data[0]:       ";
	pfs_eh file = pfs_root();
	if (file == NULL) {
		printf("%scould not get the root element [0]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_file(file, NULL, "append_file")) {
		printf("%scould not create the file [1]\n", start);
		return EXIT_FAILURE;
	}
	void *data = random_data(rd0_start, 10000);
	void *data2 = random_data(rd0_start, 1000);
	void *orig;
	i64 res = pfs_file_append(file, data, 10);
	if (res != 10) {
		printf("%scould not append to the file [2]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 10) {
		printf("%sfile length is not 10 (%ld) [3]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data + 10, 2000);
	if (res != 2000) {
		printf("%scould not append to the file [4]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 2010) {
		printf("%sfile length is not 2010 (%ld) [5]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data + 2010, 0);
	if (res != 0) {
		printf("%scould not append to the file [6]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 2010) {
		printf("%sfile length is not 2010 (%ld) [7]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data + 2010, 1990);
	if (res != 1990) {
		printf("%scould not append to the file [8]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 4000) {
		printf("%sfile length is not 4000 (%ld) [9]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data + 4000, 6000);
	if (res != 6000) {
		printf("%scould not append to the file [A]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 10000) {
		printf("%sfile length is not 10000 (%ld) [B]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data2, 1000);
	if (res != 1000) {
		printf("%scould not append to the file [C]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 11000) {
		printf("%sfile length is not 11000 (%ld) [D]\n", start, res);
		return EXIT_FAILURE;
	}
	void *buffer = malloc(11000);
	if (buffer == NULL) {
		printf("%scould not allocate to buffer [E]\n", start, res);
		return EXIT_FAILURE;
	}
	pfs_file_read(file, 0, buffer, 11000);
	if (memcmp(data, buffer, 10000) != 0) {
		printf("%smemcmp != 0 [F]\n", start, res);
		return EXIT_FAILURE;
	}
	if (memcmp(data2, buffer + 10000, 1000) != 0) {
		printf("%smemcmp != 0 [10]\n", start, res);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}

static int folder_check() {
	const char *start = "[main.checks.folder_check]:                           ";
	if (!pfs_format(1024)) {
		printf("%scould not format the file system! [0]\n");
		return EXIT_FAILURE;
	}
	pfs_eh root = pfs_root();
	if (root == NULL) {
		printf("%scould not get the root element [1]\n", start);
		return EXIT_FAILURE;
	}
	const char *n0 = "first_folder", *n1 = "the-file";
	pfs_duplicate_handle0(root, child,
	        printf("%scould not duplicate the handle! [2]\n", start); return EXIT_FAILURE;);
	if (!pfs_folder_create_folder(child, root, n0)) {
		printf("%scould not create the folder %s [3]\n", start, n0);
		return EXIT_FAILURE;
	}
	memcpy(child, root, PFS_EH_SIZE);
	if (!pfs_folder_create_file(child, root, n1)) {
		printf("%scould not create the folder %s [4]\n", start, n1);
		return EXIT_FAILURE;
	}
	memcpy(child, root, PFS_EH_SIZE);
	pfs_fi fi = pfs_folder_iterator(child, 0);
	if (fi == NULL) {
		printf("%scould not get the folder-iter! [5]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_iter_next(fi)) {
		printf("%scould not get the next element from the iter! [6]\n", start);
		return EXIT_FAILURE;
	}
	char *name;
	i64 len = 0;
	if (!pfs_element_get_name(child, &name, &len)) {
		printf("%scould not get the name! [7]\n", start);
		return EXIT_FAILURE;
	}
	if (strcmp(n0, name) != 0) {
		printf("%sgot not the expected element from the iter! (name=%s) [8]\n", start, name);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_iter_next(fi)) {
		printf("%scould not get the next element from the iter! [8]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_element_get_name(child, &name, &len)) {
		printf("%scould not get the name! [9]\n", start);
		return EXIT_FAILURE;
	}
	if (strcmp(n1, name) != 0) {
		printf("%sgot not the expected element from the iter! (name=%s) [A]\n", start, name);
		return EXIT_FAILURE;
	}
	if (pfs_folder_iter_next(fi)) {
		printf("%sgot more elements from the iter than expected! [B]\n", start);
		return EXIT_FAILURE;
	}
	if (pfs_errno != PFS_ERRNO_NO_MORE_ELEMNETS) {
		printf("%spfs_errno has not the expected value (no-more-elements)! [C]\n", start);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}

static int deep_folder_check() {
	const char *start = "[main.checks.deep_folder_check]:                      ";
	pfs_eh parent = pfs_root();
	if (parent == NULL) {
		printf("%scould not get the root element [0]\n", start);
		return EXIT_FAILURE;
	}
	pfs_duplicate_handle0(parent, child,
	        printf("%scould not duplicate the handle! [1]\n", start); return EXIT_FAILURE;);
	if (!pfs_folder_create_folder(child, NULL, "folder-name")) {
		printf("%scould not create a child folder [2]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, NULL, "folder-name")) {
		printf("%scould not create a child folder [3]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, NULL, "folder-name")) {
		printf("%scould not create a child folder [4]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, NULL, "folder-name")) {
		printf("%scould not create a child folder [5]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, parent, "folder-name")) {
		printf("%scould not create a child folder [7]\n", start);
		return EXIT_FAILURE;
	}
	pfs_element_get_parent(child);
	if (memcmp(child, parent, PFS_EH_SIZE) != 0) {
		printf("%sdid not get the expected handle [8]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, parent, "folder-name2")) {
		printf("%scould not create a child folder [9]\n", start);
		return EXIT_FAILURE;
	}
	pfs_element_get_parent(child);
	if (memcmp(child, parent, PFS_EH_SIZE) != 0) {
		printf("%sdid not get the expected handle [A]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, parent, "folder-name3")) {
		printf("%scould not create a child folder [B]\n", start);
		return EXIT_FAILURE;
	}
	pfs_element_get_parent(child);
	if (memcmp(child, parent, PFS_EH_SIZE) != 0) {
		printf("%sdid not get the expected handle [C]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, parent, "folder-name4")) {
		printf("%scould not create a child folder [D]\n", start);
		return EXIT_FAILURE;
	}
	pfs_element_get_parent(child);
	if (memcmp(child, parent, PFS_EH_SIZE) != 0) {
		printf("%sdid not get the expected handle [E]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, parent, "folder-name5")) {
		printf("%scould not create a child folder [F]\n", start);
		return EXIT_FAILURE;
	}
	pfs_element_get_parent(child);
	if (memcmp(child, parent, PFS_EH_SIZE) != 0) {
		printf("%sdid not get the expected handle [10]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, parent, "folder-name6")) {
		printf("%scould not create a child folder [11]\n", start);
		return EXIT_FAILURE;
	}
	pfs_element_get_parent(child);
	if (memcmp(child, parent, PFS_EH_SIZE) != 0) {
		printf("%sdid not get the expected handle [12]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, parent, "folder-name7")) {
		printf("%scould not create a child folder [13]\n", start);
		return EXIT_FAILURE;
	}
	pfs_element_get_parent(child);
	if (memcmp(child, parent, PFS_EH_SIZE) != 0) {
		printf("%sdid not get the expected handle [14]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_create_folder(child, parent, "folder-name8")) {
		printf("%scould not create a child folder [15]\n", start);
		return EXIT_FAILURE;
	}
	pfs_element_get_parent(child);
	if (memcmp(child, parent, PFS_EH_SIZE) != 0) {
		printf("%sdid not get the expected handle [16]\n", start);
		return EXIT_FAILURE;
	}
	if (pfs_folder_create_folder(child, NULL, "folder-name6")) {
		printf("%scould create a child folder [17]\n", start);
		return EXIT_FAILURE;
	}
	if (pfs_errno != PFS_ERRNO_ELEMENT_ALREADY_EXIST) {
		printf("%spfs_errno has not the expected value! (pfs_errno=%lu) [18]\n", start, pfs_errno);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}
