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
 * pfs-driver.c
 *
 *  Created on: Nov 22, 2022
 *      Author: pat
 */

#include "patr-fs.h"
#include <linux/stddef.h>
#include <linux/blkdev.h>
#include <linux/cred.h>
#include <linux/uidgid.h>
#include <linux/types.h>
#include <linux/err.h>
#include <linux/fs.h>
#include <linux/buffer_head.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/fs_parser.h>

// for debugging
#include <linux/delay.h>

#define MY_NAME "patrfs"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Patrick");
MODULE_DESCRIPTION("A file system module for the Patr-File-System.");
MODULE_VERSION("00.00.01");
MODULE_ALIAS_FS(MY_NAME);

struct patr_fs_info {
	_Bool read_only;
	_Bool force_deep_read_write;
};

struct patrfs_options {
	_Bool always_read_only;
	_Bool no_allow_read_only;
	_Bool ignore_read_only_flag;
	_Bool deep_ignore_read_only_flag;
	_Bool no_options;
};

static inline int patrfs_parse_options(struct patrfs_options *opts, char *data) {
	printk(KERN_DEBUG MY_NAME ": fill super: enter parse options options: '%s'\n", data);
	if (!data || *data == '\0') {
		opts->no_options = 1;
		return 0;
	}
	while (1) {
		if (*data == 'r') {
			if (data[1] == 'o') {
				data += 2;
			} else if (memcmp(data + 1, "ead-only", 8)) {
				return -EINVAL;
			} else {
				data += 9;
			}
			opts->always_read_only = 1;
		} else if (*data == 'n') {
			if (memcmp(data + 1, "o-allow-read-only", 14)) {
				return -EINVAL;
			}
			data += 18;
			opts->no_allow_read_only = 1;
		} else if (*data == 'i') {
			if (memcmp(data + 1, "gnore-read-only", 15)) {
				return -EINVAL;
			}
			data += 16;
			opts->ignore_read_only_flag = 1;
		} else if (*data == 'd') {
			if (memcmp(data + 1, "eep-ignore-read-only", 20)) {
				return -EINVAL;
			}
			data += 21;
			opts->deep_ignore_read_only_flag = 1;
			opts->ignore_read_only_flag = 1;
		} else {
			return -EINVAL;
		}
		if (*data == '\0') {
			return 0;
		} else if (*data == ',') {
			data++;
		} else {
			return -EINVAL;
		}
	}
}

static int patr_fs_fill_super(struct super_block *sb, void *data, int silent) {
	printk(KERN_DEBUG MY_NAME ": enter fill super\n");
	struct patr_fs_info *fsi = kzalloc(sizeof(struct patr_fs_info), GFP_KERNEL);
	sb->s_fs_info = fsi;
	if (!fsi) {
		return -ENOMEM;
	}
	struct patrfs_options opts = { };
	int err = patrfs_parse_options(&opts, data);
	if (err) {
		kfree(fsi);
		return err;
	}
	if (opts.no_options) {
		printk(KERN_DEBUG MY_NAME ": fill super: no opts set -> return -EINVAL (%d)\n", -EINVAL);
		return -EINVAL;
	}
	printk(KERN_DEBUG MY_NAME ": fill super: no opts not set\n"
			MY_NAME ":             always_read_only: %d\n"
			MY_NAME ":             no_allow_read_only: %d\n"
			MY_NAME ":             ignore_read_only_flag: %d\n"
			MY_NAME ":             deep_ignore_read_only_flag: %d\n"
			, (int) opts.always_read_only
			, (int) opts.no_allow_read_only
			, (int) opts.ignore_read_only_flag
			, (int) opts.deep_ignore_read_only_flag
			);
	sb->s_maxbytes = 0x7FFFFFFFFFFFFFFFLL;
	unsigned minbs = PATRFS_MIN_BLOCK_SIZE;
	{
		unsigned lbs = bdev_logical_block_size(sb->s_bdev);
		if (minbs < lbs) {
			minbs = lbs;
		}
		printk(KERN_DEBUG MY_NAME ": fill super: logical block size: %u, min block size: %s\n", lbs, minbs);
	}
	if (sb_set_blocksize(sb, minbs) != minbs) {
		kfree(fsi);
		return -EIO;
	}
	printk(KERN_DEBUG MY_NAME ": fill super: call sb_getblk_gfp(sb=%p, 0U, 0)\n", sb);
	struct buffer_head *bh = sb_getblk_gfp(sb, 0U, 0);
	printk(KERN_DEBUG MY_NAME ": fill super: sb_getblk_gfp(sb=%p, 0U, 0) returned: %p : %u\n", sb, bh, IS_ERR(bh));
	if (IS_ERR(bh)) {
		kfree(fsi);
		return PTR_ERR(bh);
	}
	struct patrfs_b0 *b0 = (void*) bh->b_data;
	printk(KERN_DEBUG MY_NAME ": fill super: b0=%p\n", b0);
	printk(KERN_DEBUG MY_NAME ": fill super: b0->MAGIC0=%llu\n", b0->MAGIC0);
	if (b0->MAGIC0 != PATRFS_MAGIC_START0 || b0->MAGIC1 != PATRFS_MAGIC_START1
			|| sb_set_blocksize(sb, b0->block_size) != b0->block_size) {
		printk(KERN_DEBUG MY_NAME ": fill super FAIL: call kfree(fsi=%p)\n", fsi);
		kfree(fsi);
		printk(KERN_DEBUG MY_NAME ": fill super FAIL: kfree(fsi=%p) returned\n", fsi);
		printk(KERN_DEBUG MY_NAME ": fill super FAIL: call brelse(bh=%p)\n", bh);
		brelse(bh);
		printk(KERN_DEBUG MY_NAME ": fill super FAIL: brelse(bh=%p) returned\n", bh);
		return -EINVAL;
	}
	if (opts.ignore_read_only_flag) {
		if (opts.no_allow_read_only || opts.always_read_only) {
			kfree(fsi);
			brelse(bh);
			return -EINVAL;
		}
		kuid_t id = current_uid();
		if (id.val != 0) {
			kfree(fsi);
			brelse(bh);
			return -EPERM;
		}
		if (opts.deep_ignore_read_only_flag) {
			fsi->force_deep_read_write = 1;
		}
	} else if (opts.always_read_only) {
		fsi->read_only = 1;
	} else if ((b0->flags & PATRFS_B0_FLAG_READ_ONLY) != 0) {
		if (opts.no_allow_read_only) {
			kfree(fsi);
			brelse(bh);
			return -EROFS;
		} else {
			fsi->read_only = 1;
		}
	}
	printk(KERN_DEBUG MY_NAME ": fill super: call brelse(bh=%p)\n", bh);
	brelse(bh);
	printk(KERN_DEBUG MY_NAME ": fill super: brelse(bh=%p) returned\n", bh);
	return 0;
}

static struct dentry* patr_fs_mount(struct file_system_type *fs_type, int flags,
		const char *dev_name, void *data) {
	printk(KERN_DEBUG MY_NAME ": mount now %s\n", dev_name);
	struct dentry *res = mount_bdev(fs_type, flags, dev_name, data,
			patr_fs_fill_super);
	if (IS_ERR(res)) {
		printk(KERN_ALERT MY_NAME ": mount of %s failed: %ld\n", dev_name, PTR_ERR(res));
	} else {
		printk(KERN_DEBUG MY_NAME ": mounted %s: %s\n", dev_name, res->d_sb->s_id);
	}
	return res;
}

void patr_fs_kill_super(struct super_block *sb) {
	kill_block_super(sb);
	kfree(sb->s_fs_info);
}

static struct file_system_type patr_fs_type = {      //
		/*	  */.name = MY_NAME,                     //
				.fs_flags = FS_REQUIRES_DEV,         //
				.mount = patr_fs_mount,              //
				.kill_sb = patr_fs_kill_super,       //
				.owner = THIS_MODULE,                //
		};

static long once;

static int __init patr_fs_init(void) {
	printk(KERN_DEBUG MY_NAME ": patr_fs_init sleep now\n");
//	usleep_range(10000000, 10000001);
//	printk(KERN_DEBUG MY_NAME ": patr_fs_init finished sleep\n");
	printk(KERN_DEBUG MY_NAME ": patr_fs_init skipped sleep\n");
	if (test_and_set_bit(0, &once)) {
		printk(KERN_DEBUG MY_NAME ": patr_fs_init called a second time owner: %p\n", patr_fs_type.owner);
		return 0;
	}
	int res = register_filesystem(&patr_fs_type);
	printk(KERN_NOTICE MY_NAME ": init\n");
	printk(KERN_DEBUG MY_NAME ": owner: %p\n", patr_fs_type.owner);
	printk(KERN_DEBUG MY_NAME ":         mount: %p\n", patr_fs_type.mount);
	printk(KERN_DEBUG MY_NAME ": patr_fs_mount: %p\n", patr_fs_mount);
	if (res) {
		printk(KERN_ERR MY_NAME ": could not register the file system: %d\n",
				res);
	}
	return res;
}

static void __exit patr_fs_exit(void) {
	printk(KERN_NOTICE MY_NAME ": exit\n");
	unregister_filesystem(&patr_fs_type);
}

module_init(patr_fs_init);
module_exit(patr_fs_exit);
