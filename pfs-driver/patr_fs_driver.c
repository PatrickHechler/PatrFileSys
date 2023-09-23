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

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Patrick");
MODULE_DESCRIPTION("A file system Linux module for the Patr-File-System.");
MODULE_VERSION("00.01.01");

static struct dentry* patr_fs_read_super(struct file_system_type*, int, const char*,
		void*){
	return NULL;
}

//static int patr_fs_init_fs_context(struct fs_context *);

//const struct fs_parameter_spec patr_fs_param_spec = {
//
//};

static struct file_system_type patr_fs_type = {
//    patr_fs_read_super, "patr-fs", 1, NULL
		.name = "patrfs",                           //
		.fs_flags = FS_REQUIRES_DEV,                //
//		.init_fs_context = patr_fs_init_fs_context, //
//		.parameters = &patr_fs_param_spec,          //
		.mount = patr_fs_read_super,                //

};

static int __init patr_fs_init(void) {
	int res = register_filesystem(&patr_fs_type);
	printk(KERN_NOTICE "PatrFS: init val=%d\n", res);
	if (!res) {
		printk(KERN_ERR "PatrFS: could not register the file system\n");
	}
	return res;
}

static void __exit patr_fs_exit(void) {
	printk(KERN_NOTICE "PatrFS: exit\n");
	unregister_filesystem(&patr_fs_type);
}

module_init( patr_fs_init);
module_exit( patr_fs_exit);

