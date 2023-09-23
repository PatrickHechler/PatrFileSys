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

#include <linux/fs.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/kernel.h>

#define MY_NAME "patr_fs"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Patrick");
MODULE_DESCRIPTION("A file system module for the Patr-File-System.");
MODULE_VERSION("00.01.01");

static int patr_fs_fill_super(struct super_block *sb, void *data, int silent) {

}

static struct dentry* patr_fs_mount(struct file_system_type *fs_type, int flags,
		const char *dev_name, void *data) {
	return mount_bdev(fs_type, flags, dev_name, data, patr_fs_fill_super);
}

void patr_fs_kill_super(struct super_block*) {
}

//static int patr_fs_init_fs_context(struct fs_context *);

//const struct fs_parameter_spec patr_fs_param_spec = {
//
//};

static struct file_system_type patr_fs_type = {
//    patr_fs_read_super, "patr-fs", 1, NULL
		.name = MY_NAME,                            //
		.fs_flags = FS_REQUIRES_DEV,                //
		.mount = patr_fs_mount,                     //
		.kill_sb = patr_fs_kill_super,              //
		.owner = THIS_MODULE,                       //
		};

MODULE_ALIAS_FS(MY_NAME);

static int __init patr_fs_init(void) {
	int res = register_filesystem(&patr_fs_type);
	printk(KERN_NOTICE MY_NAME ": init\n");
	if (res) {
		printk(KERN_ERR MY_NAME "FS: could not register the file system: %d\n",
				res);
	}
	return res;
}

static void __exit patr_fs_exit(void) {
	printk(KERN_NOTICE "PatrFS: exit\n");
	unregister_filesystem(&patr_fs_type);
}

module_init(patr_fs_init);
module_exit(patr_fs_exit);

