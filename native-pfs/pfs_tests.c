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
static int folder_check();
static int file_check();
static int read_write_file_check();
static int append_file_check();

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
	if (argc > 1) {
		if (argc != 2) {
			printf("%sinvalid argument count (Usage: pfs_tests [OPTIONAL_TESTFILE]) [2]\n", start);
			return EXIT_FAILURE;
		}
		test_file = argv[1];
	}
	int fd = open(test_file, O_RDWR | O_CREAT | O_TRUNC, 0666);
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
	printf("%sall checks executed [6]\n", start);
	return EXIT_SUCCESS;
}

static int simple_check() {
	const char *start = "[main.checks.simple_check]:                           ";
	pfs_eh root = pfs_root();
	i64 child_count = pfs_folder_child_count(root);
	if (child_count != 0) {
		printf("%sroot child count != 0 (%ld) [0]\n", start, child_count);
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
	pfs_duplicate_handle0(root, child, printf("%scould not duplicate the handle! [1]\n", start); return EXIT_FAILURE;);
	pfs_folder_create_folder(child, "first_folder");
	pfs_duplicate_handle0(root, child2, printf("%scould not duplicate the handle! [2]\n", start); return EXIT_FAILURE;);
	pfs_folder_create_file(child2, "second-folder");
	pfs_duplicate_handle0(root, iter, printf("%scould not duplicate the handle! [3]\n", start); return EXIT_FAILURE;);
	pfs_fi fi = pfs_folder_iterator(iter, 0);
	if (fi == NULL) {
		printf("%scould not get the folder-iter! [4]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_iter_next(fi)) {
		printf("%scould not get the next element from the iter! [5]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(iter, child, PFS_EH_SIZE) != 0) {
		printf("%sgot not the expected element from the iter! [6]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_folder_iter_next(fi)) {
		printf("%scould not get the next element from the iter! [7]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(iter, child2, PFS_EH_SIZE) != 0) {
		printf("%sgot not the expected element from the iter! [8]\n", start);
		return EXIT_FAILURE;
	}
	if (pfs_folder_iter_next(fi)) {
		printf("%sgot more elements from the iter than expected! [9]\n", start);
		return EXIT_FAILURE;
	}
	if (pfs_errno != PFS_ERRNO_NO_MORE_ELEMNETS) {
		printf("%spfs_errno has not the expected value (no-more-elements)! [A]\n", start);
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
	i64 child_count = pfs_folder_child_count(file);
	if (child_count != 0) {
		printf("%sroot child count != 0 (%ld) [0]\n", start, child_count);
		return EXIT_FAILURE;
	}
	int res = pfs_folder_create_file(file, "file-name");
	if (!res) {
		printf("%scould not create the file [1]\n", start);
		return EXIT_FAILURE;
	}
	i64 length = pfs_file_length(file);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [2]\n", start, length);
		return EXIT_FAILURE;
	}
	void *data = random_data(rd_start, 1016);
	res = pfs_file_append(file, data, 1016);
	if (res != 1016) {
		printf("%scould not append to the file (appended: %ld) [3]\n", start, res);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 1016L) {
		printf("%sfile length != 1016 (%ld) [4]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 10);
	if (!res) {
		printf("%scould not truncate the file to length 10 [5]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 10L) {
		printf("%sfile length != 10 (%ld) [6]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 1500);
	if (!res) {
		printf("%scould not truncate the file to length 1500 [7]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 1500L) {
		printf("%sfile length != 1500 (%ld) [8]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 0);
	if (!res) {
		printf("%scould not truncate the file to length 0 [9]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [A]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 10000);
	if (!res) {
		printf("%scould not truncate the file to length 10000 [B]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 10000L) {
		printf("%sfile length != 10000 (%ld) [C]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data, 1016);
	if (res != 1016) {
		printf("%scould not append to the file (appended: %ld) [D]\n", start, res);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 11016L) {
		printf("%sfile length != 11016 (%ld) [E]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(file, 0);
	if (!res) {
		printf("%scould not truncate the file to length 0 [F]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(file);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [10]\n", start, length);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}

static int read_write_file_check() {
	const char *start = "[main.checks.read_write_file_check]:                  ";
	const char *rd0_start = "[main.checks.read_write_file_check.random_data[0]:   ";
	const char *rd1_start = "[main.checks.read_write_file_check.random_data[1]:   ";
	pfs_eh file = pfs_root();
	if (!pfs_folder_create_file(file, "read_write_file")) {
		printf("%scould not create the file [0]\n", start);
		return EXIT_FAILURE;
	}
	void *data = malloc(4098);
	void *rnd = random_data(rd0_start, 4098);
	memcpy(data, rnd, 4098);
	i64 res = pfs_file_append(file, rnd, 4098);
	if (res != 4098) {
		printf("%scould not append the random data file (appended=%ld) [1]\n", start, res);
		return EXIT_FAILURE;
	}
	void *reat = malloc(4098);
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [2]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [3]\n", start);
		return EXIT_FAILURE;
	}
	memset(reat, 0, 4098);
	if (!pfs_file_read(file, 100L, reat, 3998)) {
		printf("%sfailed to read from the file [4]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data + 100, reat, 3998) != 0) {
		printf("%sdid not reat the expected data! [5]\n", start);
		return EXIT_FAILURE;
	}
	memset(reat, 0, 3998);
	if (!pfs_file_read(file, 1000L, reat, 2098)) {
		printf("%sfailed to read from the file [6]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data + 1000, reat, 2098) != 0) {
		printf("%sdid not reat the expected data! [7]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [8]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [9]\n", start);
		return EXIT_FAILURE;
	}
	const i64 start1 = 0, len1 = 100;
	void *rnd2 = random_data(rd1_start, 2048);
	memcpy(data + start1, rnd2, len1);
	if (!pfs_file_write(file, start1, rnd2, len1)) {
		printf("%sfailed to write to the file [A]\n", start);
		return EXIT_FAILURE;
	}
	memset(reat, 0, 4098);
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [B]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the data expected data! [C]\n", start);
		return EXIT_FAILURE;
	}
	const i64 start2 = 1000, len2 = 412;
	memcpy(data + start2, rnd2 + len1, len2);
	if (!pfs_file_write(file, start2, rnd2 + len1, len2)) {
		printf("%sfailed to write to the file [D]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [E]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [F]\n", start);
		return EXIT_FAILURE;
	}
	const i64 len3 = 2048 - len1 - len2, start3 = 4098 - len3;
	memcpy(data + start3, rnd2 + (len1 + len2), len3);
	if (!pfs_file_write(file, start3, rnd2 + (len1 + len2), len3)) {
		printf("%sfailed to write to the file [10]\n", start);
		return EXIT_FAILURE;
	}
	if (!pfs_file_read(file, 0L, reat, 4098)) {
		printf("%sfailed to read from the file [11]\n", start);
		return EXIT_FAILURE;
	}
	if (memcmp(data, reat, 4098) != 0) {
		printf("%sdid not reat the expected data! [12]\n", start);
		for (int i = 0; i < 4098; i++) {
			if ((*(unsigned char*) (data + i)) != (*(unsigned char*) (reat + i))) {
				if (i != 0
				        && ((*(unsigned char*) (data + i - 1)) != (*(unsigned char*) (reat + i - 1)))) {
					continue;
				}
				printf("%sdiff at index=%d\n", start, i);
			} else if (i != 0
			        && ((*(unsigned char*) (data + i - 1)) != (*(unsigned char*) (reat + i - 1)))) {
				printf("%ssame at index=%d\n", start, i);
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
	if (!pfs_folder_create_file(file, "append_file")) {
		printf("%scould not create the file [0]\n", start);
		return EXIT_FAILURE;
	}
	void *data = random_data(rd0_start, 10000);
	void *data2 = random_data(rd0_start, 1000);
	void *orig;
	i64 res = pfs_file_append(file, data, 10);
	if (res != 10) {
		printf("%scould not append to the file [1]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 10) {
		printf("%sfile length is not 10 (%ld) [2]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data + 10, 2000);
	if (res != 2000) {
		printf("%scould not append to the file [3]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 2010) {
		printf("%sfile length is not 2010 (%ld) [4]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data + 2010, 0);
	if (res != 0) {
		printf("%scould not append to the file [5]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 2010) {
		printf("%sfile length is not 2010 (%ld) [6]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data + 2010, 1990);
	if (res != 1990) {
		printf("%scould not append to the file [7]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 4000) {
		printf("%sfile length is not 4000 (%ld) [8]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data + 4000, 6000);
	if (res != 6000) {
		printf("%scould not append to the file [9]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 10000) {
		printf("%sfile length is not 10000 (%ld) [A]\n", start, res);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(file, data2, 1000);
	if (res != 1000) {
		printf("%scould not append to the file [B]\n", start);
		return EXIT_FAILURE;
	}
	res = pfs_file_length(file);
	if (res != 11000) {
		printf("%sfile length is not 11000 (%ld) [C]\n", start, res);
		return EXIT_FAILURE;
	}
	void *buffer = malloc(11000);
	if (buffer == NULL) {
		printf("%scould not allocate to buffer [D]\n", start, res);
		return EXIT_FAILURE;
	}
	pfs_file_read(file, 0, buffer, 11000);
	if (memcmp(data, buffer, 10000) != 0) {
		printf("%smemcmp != 0 [E]\n", start, res);
		return EXIT_FAILURE;
	}
	if (memcmp(data2, buffer + 10000, 1000) != 0) {
		printf("%smemcmp != 0 [F]\n", start, res);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}
