/*
 * pfs-file.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#include "pfs.h"
#include "../include/pfs-file.h"

#define ch(err_ret) \
	eh(err_ret) \
	c_h(pfs_ehs[eh], err_ret, PFS_F_FILE)

#define cr(err_ret) c_r(pfs_ehs[eh], err_ret)

extern i64 pfs_file_length(int eh) {
	ch(-1)
	int res = pfsc_file_length(&pfs_ehs[eh]->handle);
	cr(-1)
	return res;
}

extern int pfs_file_truncate(int eh, i64 length) {
	if (length < 0) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		return 0;
	}
	ch(0)
	int res = pfsc_file_truncate(&pfs_ehs[eh]->handle, length);
	cr(0)
	return res;
}
