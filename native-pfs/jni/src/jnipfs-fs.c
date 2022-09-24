/*
 * jnipfs.c
 *
 *  Created on: Sep 24, 2022
 *      Author: pat
 */

#include <pfs.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include "de_hechler_patrick_pfs_fs_impl_NativePatrFileSys.h"

struct pfs_element_handle {
	int :32;
	int :32;
	int :32;
	int :32;
	int :32;
	int :32;
	int :32;
	int :32;
	int :32;
	int :32;
	int :32;
};

static_assert(sizeof(struct pfs_element_handle) == PFS_EH_SIZE, "error!");

struct set_etntry {
	struct pfs_element_handle eh;
	jobject object;
};

static int equal(const void *a_, const void *b_) {
	const ui32 *a = a_, *b = b_;
	return (a[0] == b[0]) && (a[1] == b[1]) && (a[2] == b[2]) && (a[3] == b[3]) && (a[4] == b[4])
	        && (a[5] == b[5]) && (a[6] == b[6]) && (a[7] == b[7]) && (a[8] == b[8])
	        && (a[9] == b[9]) && (a[10] == b[10]);
}

static unsigned int hash(const void *pntr) {
	const ui32 *e = pntr;
	return e[0] ^ e[1] ^ e[2] ^ e[3] ^ e[4] ^ e[5] ^ e[6] ^ e[7] ^ e[8] ^ e[9] ^ e[10];
}

void throwError(JNIEnv *env, const char *msg) {
	jclass cls = (*env)->FindClass(env, "de/hechler/patrick/exceptions/PatrFileSysException");
	if (cls == NULL) {
		(*env)->FatalError(env, msg);
	}
	jmethodID methodId;
	if (msg == NULL) {
		methodId = (*env)->GetMethodID(env, cls, "<init>", "(I)V");
	} else {
		methodId = (*env)->GetMethodID(env, cls, "<init>", "(ILjava/lang/String;)V");
	}
	if (cls == NULL) {
		(*env)->FatalError(env, msg);
	}
	jthrowable err;
	if (msg == NULL) {
		err = (*env)->NewObject(env, cls, methodId, (jint) pfs_errno);
	} else {
		jstring java_msg = (*env)->NewStringUTF(env, msg);
		err = (*env)->NewObject(env, cls, methodId, (jint) pfs_errno, java_msg);
	}
	(*env)->Throw(env, err);
}

jobject fill_java_stuff(jclass cls, jobject result, struct hashset *set,
        struct set_etntry *root_entry, JNIEnv *env, struct bm_block_manager *bm) {
	hashset_put(set, hash(root_entry), root_entry);
	jclass folder_cls = (*env)->FindClass(env,
	        "de/hechler/patrick/pfs/folder/impl/NativePatrFileSysFolder");
	jmethodID new_folder_ID = (*env)->GetMethodID(env, folder_cls, "<init>", "(J)V");
	jobject root_obj = (*env)->NewObject(env, folder_cls, new_folder_ID);
	jobject glob_root_ref = (*env)->NewGlobalRef(env, root_obj);
	root_entry->object = glob_root_ref;
	jfieldID v1id = (*env)->GetFieldID(env, cls, "val1", "J");
	jfieldID v2id = (*env)->GetFieldID(env, cls, "val2", "J");
	jfieldID rtid = (*env)->GetFieldID(env, cls, "root",
	        "Lde/hechler/patrick/pfs/folder/impl/NativePatrFileSysFolder;");
	jfieldID rvid = (*env)->GetFieldID(env, cls, "val3", "J");
	(*env)->SetLongField(env, result, v1id, (jlong) bm);
	(*env)->SetLongField(env, result, v2id, (jlong) set);
	(*env)->SetObjectField(env, result, rtid, glob_root_ref);
	(*env)->SetLongField(env, root_obj, rvid, (jlong) root_entry);
	return result;
}

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    create
 * Signature: (Ljava/lang/String;)Lde/hechler/patrick/pfs/fs/impl/NativePatrFileSys;
 */
JNIEXPORT jobject JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_create__Ljava_lang_String_2(
        JNIEnv *env, jclass cls, jstring pfs_file) {
	if (pfs_file == NULL) {
		jclass exep_cls = (*env)->FindClass(env, "java/lang/NullPointerException");
		(*env)->ThrowNew(env, exep_cls, "pfs_file is null");
		return NULL;
	}
	const char *utf8_pfs_file = (*env)->GetStringUTFChars(env, pfs_file, NULL);
	int fd = open64(utf8_pfs_file, O_RDWR);
	if (fd == -1) {
		if (errno == EIO) {
			pfs_errno = PFS_ERRNO_IO_ERR;
		} else {
			pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		}
		throwError(env, "open the file");
		return NULL;
	}
	(*env)->ReleaseStringUTFChars(env, pfs_file, utf8_pfs_file);
	jmethodID methodID = (*env)->GetMethodID(env, cls, "<init>", "()V");
	jobject result = (*env)->NewObject(env, cls, methodID);
	struct bm_block_manager *bm;
	i64 block_size;
	new_file_bm0(bm, fd,
	        pfs_errno = PFS_ERRNO_ILLEGAL_ARG; throwError(env, "illegal magic"); return NULL;,
	        pfs_errno = PFS_ERRNO_IO_ERR; throwError(env, NULL); return NULL;)
	if (bm == NULL) {
		throwError(env, "creating the block manager");
		return NULL;
	}
	struct hashset *set = malloc(sizeof(struct hashset));
	if (set == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		bm->close_bm(bm);
		free(bm);
		throwError(env, "malloc");
		return NULL;
	}
	set->entrycount = 0;
	set->setsize = 0;
	set->equalizer = equal;
	set->hashmaker = hash;
	set->entries = NULL;
	struct set_etntry *root_entry = malloc(sizeof(struct set_etntry));
	if (root_entry == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		bm->close_bm(bm);
		free(set);
		free(bm);
		throwError(env, "formatting the file system");
		return NULL;
	}
	jfieldID lid = (*env)->GetStaticFieldID(env, cls, "LOCK", "Ljava/lang/Object;");
	jobject lock_obj = (*env)->GetStaticObjectField(env, cls, lid);
	(*env)->MonitorEnter(env, lock_obj);
	pfs = bm;
	if (!pfs_fill_root(&root_entry->eh)) {
		(*env)->MonitorExit(env, lock_obj);
		bm->close_bm(bm);
		free(set);
		free(root_entry);
		free(bm);
		throwError(env, "getting the root");
		return NULL;
	}
	(*env)->MonitorExit(env, lock_obj);
	return fill_java_stuff(cls, result, set, root_entry, env, bm);
}

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    create
 * Signature: (Ljava/lang/String;JIII)Lde/hechler/patrick/pfs/fs/impl/NativePatrFileSys;
 */
JNIEXPORT jobject JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_create__Ljava_lang_String_2JIII(
        JNIEnv *env, jclass cls, jstring pfs_file, jlong block_count, jint block_size,
        jint open_mode, jint open_permissions) {
	if (pfs_file == NULL) {
		jclass exep_cls = (*env)->FindClass(env, "java/lang/NullPointerException");
		(*env)->ThrowNew(env, exep_cls, "pfs_file is null");
		return NULL;
	}
	const char *utf8_pfs_file = (*env)->GetStringUTFChars(env, pfs_file, NULL);
	int fd = open64(utf8_pfs_file, open_mode | O_RDWR, open_permissions);
	if (fd == -1) {
		if (errno == EIO) {
			pfs_errno = PFS_ERRNO_IO_ERR;
		} else {
			pfs_errno = PFS_ERRNO_UNKNOWN_ERROR;
		}
		throwError(env, "open the file");
		return NULL;
	}
	(*env)->ReleaseStringUTFChars(env, pfs_file, utf8_pfs_file);
	jmethodID methodID = (*env)->GetMethodID(env, cls, "<init>", "()V");
	jobject result = (*env)->NewObject(env, cls, methodID);
	struct bm_block_manager *bm = bm_new_file_block_manager(fd, block_size);
	if (bm == NULL) {
		throwError(env, "creating the block manager");
		return NULL;
	}
	struct hashset *set = malloc(sizeof(struct hashset));
	if (set == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		bm->close_bm(bm);
		free(bm);
		throwError(env, "malloc");
		return NULL;
	}
	set->entrycount = 0;
	set->setsize = 0;
	set->equalizer = equal;
	set->hashmaker = hash;
	set->entries = NULL;
	struct set_etntry *root_entry = malloc(sizeof(struct set_etntry));
	if (root_entry == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		bm->close_bm(bm);
		free(set);
		free(bm);
		throwError(env, "formatting the file system");
		return NULL;
	}
	jfieldID lid = (*env)->GetStaticFieldID(env, cls, "LOCK", "Ljava/lang/Object;");
	jobject lock_obj = (*env)->GetStaticObjectField(env, cls, lid);
	(*env)->MonitorEnter(env, lock_obj);
	pfs = bm;
	if (!pfs_format(block_count)) {
		(*env)->MonitorExit(env, lock_obj);
		bm->close_bm(bm);
		free(set);
		free(root_entry);
		free(bm);
		throwError(env, "formatting the file system");
		return NULL;
	}
	if (!pfs_fill_root(&root_entry->eh)) {
		(*env)->MonitorExit(env, lock_obj);
		bm->close_bm(bm);
		free(set);
		free(root_entry);
		free(bm);
		throwError(env, "getting the root");
		return NULL;
	}
	(*env)->MonitorExit(env, lock_obj);
	return fill_java_stuff(cls, result, set, root_entry, env, bm);
}

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    createRamFs
 * Signature: (JI)Lde/hechler/patrick/pfs/fs/impl/NativePatrFileSys;
 */
JNIEXPORT jobject JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_createRamFs(JNIEnv*,
        jclass, jlong, jint);


#define setVal(type_pntr, struct_member, value) \
		*(type_pntr) (((void*) bm) + offsetof (struct bm_block_manager, struct_member)) = value;

struct java_block_manager {
	struct bm_block_manager bm;
	jobject java_impl;
};

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    create
 * Signature: (Lde/hechler/patrick/pfs/bm/BlockManager;)Lde/hechler/patrick/pfs/fs/impl/NativePatrFileSys;
 */
JNIEXPORT jobject JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_create__Lde_hechler_patrick_pfs_bm_BlockManager_2(
        JNIEnv*, jclass, jobject);

#undef setVal

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    format
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_format(JNIEnv*,
        jobject, jlong);

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    root
 * Signature: ()Lde/hechler/patrick/pfs/folder/PFSFolder;
 */
JNIEXPORT jobject JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_root(JNIEnv*,
        jobject);

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    blockCount
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_blockCount(JNIEnv*,
        jobject);

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    blockSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_blockSize(JNIEnv*,
        jobject);

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_close(JNIEnv*, jobject);

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    finalize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_finalize(JNIEnv*,
        jobject);
