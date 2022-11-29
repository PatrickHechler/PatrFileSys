/*
 * pfs-driver.c
 *
 *  Created on: Nov 22, 2022
 *      Author: pat
 */

#include <linux/fs.h>
#include <linux/module.h>
MODULE_LICENSE("GPL");
MODULE_AUTHOR("Patrick");

static int patr_fs_init(void) {
	printk(KERN_ALERT "Hello, world, I am the patrFS-driver\n");
	return 0;
}

static void patr_fs_exit(void) {
	printk(KERN_ALERT "Goodbye, cruel world\n");
}

module_init(patr_fs_init);
module_exit(patr_fs_exit);

//static struct dentry* patr_fs_mount(struct file_system_type*, int, const char*,
//		void*);
//static void patr_fs_kill_sb(struct super_block*);
//
//static inline void init_patr_fs_type(struct file_system_type *patr_fs) {
//	patr_fs->name = "patrfs";
//	patr_fs->fs_flags = FS_REQUIRES_DEV;
//	patr_fs->mount = patr_fs_mount;
//	patr_fs->kill_sb = patr_fs_kill_sb;
//	patr_fs->owner = THIS_MODULE;
//	patr_fs->next = NULL;
//	patr_fs->fs_supers.first = NULL;
//}
//
//static struct dentry* patr_fs_mount(struct file_system_type*, int, const char*,
//		void*) {
//
//}
//static void patr_fs_kill_sb(struct super_block*) {
//
//}
