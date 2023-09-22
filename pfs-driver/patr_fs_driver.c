/*
 * pfs-driver.c
 *
 *  Created on: Nov 22, 2022
 *      Author: pat
 */

//#include <linux/fs.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/kernel.h>

MODULE_LICENSE("GPL");
MODULE_AUTHOR("Patrick");
MODULE_DESCRIPTION("A file system Linux module for the Patr-File-System.");
MODULE_VERSION("00.01.01");

static int __init patr_fs_init(void) {
	printk(KERN_INFO "Hello, world, I am the patrFS-driver\n");
	return 0;
}

static void __exit patr_fs_exit(void) {
	printk(KERN_ALERT "Goodbye, cruel world\n");
}

module_init(patr_fs_init);
module_exit(patr_fs_exit);

