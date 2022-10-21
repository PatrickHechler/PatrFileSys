/*
 * pfs-pipe.c
 *
 *  Created on: Oct 7, 2022
 *      Author: pat
 */
#include "pfs.h"
#include "../include/pfs-pipe.h"

#define ch(err_ret) \
	eh(err_ret) \
	c_h(pfs_ehs[eh], err_ret, PFS_F_PIPE)

#define cr(err_ret) c_r(pfs_ehs[eh], err_ret)

extern i64 pfs_pipe_length(int eh) {
	ch(-1)
	int res = pfsc_pipe_length(&pfs_ehs[eh]->handle);
	cr(-1)
	return res;
}
