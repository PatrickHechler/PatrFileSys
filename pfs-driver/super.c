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

#include "patrfs.h"
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

#define MY_NAME "patrfs"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Patrick");
MODULE_DESCRIPTION("A file system module for the Patr-File-System.");
MODULE_VERSION("00.01.01");
MODULE_ALIAS_FS(MY_NAME);

struct patr_fs_info {
	bool read_only;
	bool force_deep_read_write;
};

struct patrfs_options {
	bool always_read_only;
	bool allow_read_only;
	bool ignore_read_only_flag;
	bool deep_ignore_read_only_flag;
};

static inline int patrfs_parse_options(struct patrfs_options *opts, char *data) {
	if (!data || *data == '\0') {
		return 0;
	}
	while (1) {
		if (*data == 'r') {
			if (data[1] == 'o') {
				opts += 2;
			} else if (memcmp(data + 1, "ead-only", 8)) {
				return -EINVAL;
			} else {
				data += 9;
			}
			opts->always_read_only = 1;
		} else if (*data == 'a') {
			if (memcmp(data + 1, "llow-read-only", 14)) {
				return -EINVAL;
			}
			data += 15;
			opts->allow_read_only = 1;
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
	sb->s_maxbytes = 0x7FFFFFFFFFFFFFFFLL;
	sb->s_blocksize = PATRFS_MIN_BLOCK_SIZE;
	sb->s_blocksize_bits = PATRFS_MIN_BLOCK_SIZE_SHIFT;
	struct buffer_head *bh = sb_getblk_gfp(sb, 0U, 0);
	if (IS_ERR(bh)) {
		kfree(fsi);
		return PTR_ERR(bh);
	}
	struct patrfs_b0 *b0 = (void*) bh->b_data;
	if (b0->MAGIC0 != PATRFS_MAGIC_START0 || b0->MAGIC1 != PATRFS_MAGIC_START1
			|| sb_set_blocksize(sb, b0->block_size) != b0->block_size) {
		kfree(fsi);
		brelse(bh);
		return -EINVAL;
	}
	if (opts.ignore_read_only_flag) {
		if (opts.allow_read_only || opts.always_read_only) {
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
		if (opts.allow_read_only) {
			fsi->read_only = 1;
		} else {
			kfree(fsi);
			brelse(bh);
			return -EROFS;
		}
	}
	brelse(bh);
	return 0;
}

static struct dentry* patr_fs_mount(struct file_system_type *fs_type, int flags,
		const char *dev_name, void *data) {
	printk(KERN_DEBUG MY_NAME ": mount now %s\n", dev_name);
	struct dentry *res = mount_bdev(fs_type, flags, dev_name, data,
			patr_fs_fill_super);
	if (res) {
		printk(KERN_DEBUG MY_NAME ": mounted " MY_NAME ": %s: %s\n", dev_name,
				res->d_sb->s_id);
	}
	return res;
}

void patr_fs_kill_super(struct super_block *sb) {
	kill_block_super(sb);
	kfree(sb->s_fs_info);
}

const struct fs_parameter_spec patrfs_fs_parameters = fsparam_flag("read-only", 0);



static struct file_system_type patr_fs_type = {      //
		/*	  */.name = MY_NAME,                     //
				.fs_flags = FS_REQUIRES_DEV,         //
				.mount = patr_fs_mount,              //
				.kill_sb = patr_fs_kill_super,       //
				.owner = THIS_MODULE,                //
				.parameters = &patrfs_fs_parameters, //
		};

static int __init patr_fs_init(void) {
	int res = register_filesystem(&patr_fs_type);
	printk(KERN_NOTICE MY_NAME ": init\n");
	printk(KERN_DEBUG MY_NAME ": type.name: %s\n", patr_fs_type.name);
	printk(KERN_DEBUG MY_NAME ": type.parameters: %p\n", patr_fs_type.parameters);
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
