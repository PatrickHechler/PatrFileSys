/*
 * pfs_tests.c
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#include <stdlib.h>
#include <stdio.h>
#include "pfs.h"

int main(int argc, char **argv) {
	printf("[PFS-TESTS]: start\n");
	fflush(NULL);
	pfs = bm_new_ram_block_manager(64L, 1024);
	printf("[PFS_TESTS]: pfs=%p\n", pfs);
	printf("[PFS-TESTS]: finish\n");
	fflush(NULL);
	return EXIT_SUCCESS;
}
