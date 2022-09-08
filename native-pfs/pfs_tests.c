/*
 * pfs_tests.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

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

int main(int argc, char **argv) {
	const char *start = "[main]:                                               ";
	printf("%sstart checks with a ram block manager [0]\n", start);
	pfs = bm_new_ram_block_manager(512L, 1024);
	int res = checks();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	pfs->close(pfs);
	printf("%sstart checks with a file block manager [1]\n", start);
	int fd = open("./testout/testfile.pfs", O_RDWR | O_CREAT | O_TRUNC, 0666);
	pfs = bm_new_file_block_manager(fd, 1024);
	res = checks();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	pfs->close(pfs);
	printf("%sstart checks with block-flaggable ram block manager [2]\n",
			start);
	pfs = bm_new_ram_block_manager(512L, 1024);
	res = checks();
	if (res != EXIT_SUCCESS) {
		return res;
	}
	pfs->close(pfs);
	printf("%sFINISH [3]\n", start);
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
	printf("%sall checks executed [4]\n", start);
	return EXIT_SUCCESS;
}

static int simple_check() {
	const char *start = "[main.checks.simple_check]:                           ";
	element root = pfs_root();
	i64 child_count = pfs_folder_child_count(&root);
	if (child_count != 0) {
		printf("%sroot child count != 0 (%ld) [0]\n", start, child_count);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}

static void* random_data(const char *start, i64 size) {
	void *data = malloc(size);
	if (data == NULL) {
		printf("%could not allocate enough memory (size=%ld) [0]\n", start,
				size);
		fflush(NULL);
		exit(EXIT_FAILURE);
	}
	int urnd = open("/dev/urandom", O_RDONLY);
	if (urnd == -1) {
		printf("%could not open a read stream of file /dev/urandom [1]\n",
				start);
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

static int file_check() {
	const char *start = "[main.checks.file_check]:                             ";
	const char *rd_start =
			"[main.checks.file_check.random_data]:                 ";
	element e = pfs_root();
	i64 child_count = pfs_folder_child_count(&e);
	if (child_count != 0) {
		printf("%sroot child count != 0 (%ld) [0]\n", start, child_count);
		return EXIT_FAILURE;
	}
	int res = pfs_folder_create_file(&e, "file-name");
	if (!res) {
		printf("%scould not create the file [1]\n", start);
		return EXIT_FAILURE;
	}
	i64 length = pfs_file_length(&e);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [2]\n", start, length);
		return EXIT_FAILURE;
	}
	void *data = random_data(rd_start, 1016);
	res = pfs_file_append(&e, data, 1016);
	if (res != 1016) {
		printf("%scould not append to the file (appended: %ld) [7]\n", start,
				res);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(&e);
	if (length != 1016L) {
		printf("%sfile length != 1016 (%ld) [8]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(&e, 10);
	if (!res) {
		printf("%scould not truncate the file to length 10 [9]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(&e);
	if (length != 10L) {
		printf("%sfile length != 10 (%ld) [A]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(&e, 1500);
	if (!res) {
		printf("%scould not truncate the file to length 1500 [B]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(&e);
	if (length != 1500L) {
		printf("%sfile length != 1500 (%ld) [C]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(&e, 0);
	if (!res) {
		printf("%scould not truncate the file to length 0 [D]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(&e);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [E]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(&e, 10000);
	if (!res) {
		printf("%scould not truncate the file to length 10000 [F]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(&e);
	if (length != 10000L) {
		printf("%sfile length != 10000 (%ld) [10]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_append(&e, data, 1016);
	if (res != 1016) {
		printf("%scould not append to the file (appended: %ld) [11]\n", start,
				res);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(&e);
	if (length != 11016L) {
		printf("%sfile length != 11016 (%ld) [12]\n", start, length);
		return EXIT_FAILURE;
	}
	res = pfs_file_truncate(&e, 0);
	if (!res) {
		printf("%scould not truncate the file to length 0 [13]\n", start);
		return EXIT_FAILURE;
	}
	length = pfs_file_length(&e);
	if (length != 0L) {
		printf("%sfile length != 0 (%ld) [14]\n", start, length);
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}

static int read_write_file_check() {
	const char *start = "[main.checks.read_write_file_check]:                  ";
	const char *rd_start =
			"[main.checks.read_write_file_check.random_data]:      ";
	element file = pfs_root();
	if (!pfs_folder_create_file(&file, "read_write_file")) {
		printf("%scould not create the file [0]\n", start);
		return EXIT_FAILURE;
	}
	void *rnd = random_data(rd_start, 4098);
	i64 res = pfs_file_append(&file, rnd, 4098);
	if (res != 4098) {
		printf("%scould not append the random data file (appended=%ld) [0]\n", res, start);
		return EXIT_FAILURE;
	}
	//TODO check read/write
	return EXIT_SUCCESS;
}
