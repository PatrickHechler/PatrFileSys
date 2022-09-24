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

struct set_entry {
	struct pfs_element_handle eh;
	jobject object;
	jobject parent;
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

static inline jobject fill_java_stuff(JNIEnv *env, jclass cls, jobject result, struct hashset *set,
        struct set_entry *root_entry, struct bm_block_manager *bm) {
	hashset_put(set, hash(root_entry), root_entry);
	jclass folder_cls = (*env)->FindClass(env,
	        "de/hechler/patrick/pfs/folder/impl/NativePatrFileSysFolder");
	jmethodID new_folder_ID = (*env)->GetMethodID(env, folder_cls, "<init>", "(J)V");
	jobject root_obj = (*env)->NewObject(env, folder_cls, new_folder_ID);
	jobject root_ref = (*env)->NewWeakGlobalRef(env, root_obj);
	root_entry->object = root_ref;
	root_entry->parent = NULL;
	jfieldID bmid = (*env)->GetFieldID(env, cls, "bm", "J");
	jfieldID setid = (*env)->GetFieldID(env, cls, "set", "J");
	jfieldID rootid = (*env)->GetFieldID(env, cls, "root",
	        "Lde/hechler/patrick/pfs/folder/impl/NativePatrFileSysFolder;");
	jfieldID rvid = (*env)->GetFieldID(env, cls, "value", "J");
	(*env)->SetLongField(env, result, bmid, (jlong) bm);
	(*env)->SetLongField(env, result, setid, (jlong) set);
	(*env)->SetObjectField(env, result, rootid, root_ref);
	(*env)->SetLongField(env, root_obj, rvid, (jlong) root_entry);
	return result;
}

static inline jobject enter_monitor(JNIEnv *env, jclass cls) {
	jfieldID lid = (*env)->GetStaticFieldID(env, cls, "LOCK", "Ljava/lang/Object;");
	jobject lock_obj = (*env)->GetStaticObjectField(env, cls, lid);
	(*env)->MonitorEnter(env, lock_obj);
	return lock_obj;
}

static inline struct hashset* init_set(JNIEnv *env) {
	struct hashset *set = malloc(sizeof(struct hashset));
	if (set == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		throwError(env, "malloc");
		return NULL;
	}
	set->entrycount = 0;
	set->setsize = 0;
	set->equalizer = equal;
	set->hashmaker = hash;
	set->entries = NULL;
	return set;
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
	struct hashset *set = init_set(env);
	if (set == NULL) {
		return NULL;
	}
	struct set_entry *root_entry = malloc(sizeof(struct set_entry));
	if (root_entry == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		bm->close_bm(bm);
		free(set);
		free(bm);
		throwError(env, "formatting the file system");
		return NULL;
	}
	jobject lock_obj = enter_monitor(env, cls);
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
	return fill_java_stuff(env, cls, result, set, root_entry, bm);
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
	struct hashset *set = init_set(env);
	if (set == NULL) {
		return NULL;
	}
	struct set_entry *root_entry = malloc(sizeof(struct set_entry));
	if (root_entry == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		free(set);
		throwError(env, "formatting the file system");
		return NULL;
	}
	struct bm_block_manager *bm = bm_new_file_block_manager(fd, block_size);
	if (bm == NULL) {
		free(set);
		free(root_entry);
		throwError(env, "creating the block manager");
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
	return fill_java_stuff(env, cls, result, set, root_entry, bm);
}

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    createRamFs
 * Signature: (JI)Lde/hechler/patrick/pfs/fs/impl/NativePatrFileSys;
 */
JNIEXPORT jobject JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_createRamFs(
        JNIEnv *env, jclass cls, jlong block_size, jint block_count) {
	if (block_size <= 0 || block_count <= 0L) {
		pfs_errno = PFS_ERRNO_ILLEGAL_ARG;
		throwError(env, "negative/zero block_count/block_size");
		return NULL;
	}
	struct hashset *set = init_set(env);
	struct set_entry *root_entry = malloc(sizeof(struct set_entry));
	if (root_entry == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		free(set);
		throwError(env, "maloc");
	}
	struct bm_block_manager *bm = bm_new_ram_block_manager(block_count, block_size);
	if (bm == NULL) {
		free(set);
		free(root_entry);
		throwError(env, "creating the block manager");
		return NULL;
	}
	jobject lock = enter_monitor(env, cls);
	pfs = bm;
	if (!pfs_format(block_count)) {
		(*env)->MonitorExit(env, lock);
		bm->close_bm(bm);
		free(set);
		free(root_entry);
		free(bm);
		throwError(env, "formatting the file system");
		return NULL;
	}
	if (!pfs_fill_root(&root_entry->eh)) {
		(*env)->MonitorExit(env, lock);
		bm->close_bm(bm);
		free(set);
		free(root_entry);
		free(bm);
		throwError(env, "getting the root folder");
		return NULL;
	}
	(*env)->MonitorExit(env, lock);
	jmethodID methodID = (*env)->GetMethodID(env, cls, "<init>", "()V");
	jobject result = (*env)->NewObject(env, cls, methodID);
	return fill_java_stuff(env, cls, result, set, root_entry, bm);
}

#define setVal(type_pntr, struct_member, value) \
		*(type_pntr) (((void*) bm) + offsetof (struct bm_block_manager, struct_member)) = value;

struct java_block_manager {
	struct bm_block_manager bm;
	jobject java_impl;
	JNIEnv *env;
};

static_assert(offsetof(struct java_block_manager, bm) == 0);

static void* java_bm_get(struct bm_block_manager *bm, i64 block);
static int java_bm_unget(struct bm_block_manager *bm, i64 block);
static int java_bm_set(struct bm_block_manager *bm, i64 block);
static int java_bm_sync_bm(struct bm_block_manager *bm);
static int java_bm_close_bm(struct bm_block_manager *bm);
static i64 java_bm_get_flags(struct bm_block_manager *bm, i64 block);
static int java_bm_set_flags(struct bm_block_manager *bm, i64 block, i64 flags);
static i64 java_bm_first_zero_flagged_block(struct bm_block_manager *bm);
static int java_bm_delete_all_flags(struct bm_block_manager *bm);

static const struct bm_block_manager java_init_bm = {
/*		*/.loaded = {
/*			*/.entrycount = 0,
/*			*/.setsize = 0,
/*			*/.equalizer = equal,
/*			*/.hashmaker = hash,
/*			*/.entries = NULL
/*		*/},
/*		*/.get = java_bm_get,
/*		*/.unget = java_bm_unget,
/*		*/.set = java_bm_set,
/*		*/.sync_bm = java_bm_sync_bm,
/*		*/.close_bm = java_bm_close_bm,
/*		*/.block_size = -1,
/*		*/.block_flag_bits = -1,
/*		*/.get_flags = java_bm_get_flags,
/*		*/.set_flags = java_bm_set_flags,
/*		*/.first_zero_flagged_block = java_bm_first_zero_flagged_block,
/*		*/.delete_all_flags = java_bm_delete_all_flags
/*	*/};

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    create
 * Signature: (Lde/hechler/patrick/pfs/bm/BlockManager;)Lde/hechler/patrick/pfs/fs/impl/NativePatrFileSys;
 */
JNIEXPORT jobject JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_create__Lde_hechler_patrick_pfs_bm_BlockManager_2(
        JNIEnv *env, jclass cls, jobject block_manager) {
	struct hashset *set = init_set(env);
	struct set_entry *root_entry = malloc(sizeof(struct set_entry));
	if (root_entry == NULL) {
		pfs_errno = PFS_ERRNO_OUT_OF_MEMORY;
		free(set);
		throwError(env, "maloc");
	}
	struct java_block_manager *bm = malloc(sizeof(struct java_block_manager));
	if (bm == NULL) {
		free(set);
		free(root_entry);
		throwError(env, "creating the block manager");
		return NULL;
	}
	memcpy(&bm->bm, &java_init_bm, sizeof(struct bm_block_manager));
	bm->java_impl = (*env)->NewGlobalRef(env, block_manager);
	bm->env = env;
	jclass bm_java_cls = (*env)->FindClass(env, "de/hechler/patrick/pfs/bm/BlockManager");
	jmethodID block_size_ID = (*env)->GetMethodID(env, bm_java_cls, "blockSize", "()I");
	jint bs = (*env)->CallIntMethod(env, block_manager, block_size_ID);
	jmethodID flags_per_block_ID = (*env)->GetMethodID(env, bm_java_cls, "flagsPerBlock", "()I");
	jint flag_cnt = (*env)->CallIntMethod(env, block_manager, flags_per_block_ID);
	setVal(i32*, block_size, (i32 ) bs)
	setVal(i32*, block_flag_bits, (i32 ) flag_cnt)
	jobject lock = enter_monitor(env, cls);
	pfs = &bm->bm;
	if (!pfs_fill_root(&root_entry->eh)) {
		(*env)->MonitorExit(env, lock);
		free(set);
		free(root_entry);
		free(bm);
		throwError(env, "getting the root folder");
		return NULL;
	}
	(*env)->MonitorExit(env, lock);
	jmethodID methodID = (*env)->GetMethodID(env, cls, "<init>", "()V");
	jobject result = (*env)->NewObject(env, cls, methodID);
	return fill_java_stuff(env, cls, result, set, root_entry, &bm->bm);
}

#undef setVal

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    format
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_format(JNIEnv *env,
        jobject caller, jlong block_cnt) {
	jclass cls = (*env)->GetObjectClass(env, caller);
	jfieldID bmID = (*env)->GetFieldID(env, cls, "bm", "J");
	jfieldID rootID = (*env)->GetFieldID(env, cls, "root",
	        "Lde/hechler/patrick/pfs/folder/impl/NativePatrFileSysFolder;");
	jfieldID setID = (*env)->GetFieldID(env, cls, "set", "J");
	struct bm_block_manager *bm = (void*) (*env)->GetLongField(env, caller, bmID);
	struct hashset *set = (void*) (*env)->GetLongField(env, caller, setID);
	jobject root_obj = (*env)->GetObjectField(env, caller, rootID);
	jclass folder_cls = (*env)->FindClass(env,
	        "de/hechler/patrick/pfs/folder/impl/NativePatrFileSysFolder");
	jfieldID valueID = (*env)->GetFieldID(env, folder_cls, "value", "J");
	struct set_entry *root_entry = (void*) (*env)->GetLongField(env, root_obj, valueID);
	(*env)->DeleteLocalRef(env, root_obj);
	jobject lock = enter_monitor(env, cls);
	if (!pfs_format(block_cnt)) {
		(*env)->MonitorExit(env, lock);
		throwError(env, "format");
		return;
	}
	if (!pfs_fill_root(&root_entry->eh)) {
		(*env)->MonitorExit(env, lock);
		throwError(env, "get the root");
		return;
	}
	(*env)->MonitorExit(env, lock);
	for (int i = 0; i < set->setsize; i++) {
		if (!set->entries[i]) {
			continue;
		}
		if (set->entries[i] == &illegal) {
			set->entries[i] = NULL;
			continue;
		}
		struct set_entry *entry = set->entries[i];
		jobject local_ref = (*env)->NewLocalRef(env, entry->object);
		(*env)->DeleteWeakGlobalRef(env, entry->object);
		if (local_ref == NULL) {
			free(entry);
			set->entries[i] = NULL;
			continue;
		}
		jclass cls = (*env)->GetObjectClass(env, local_ref);
		jfieldID valID = (*env)->GetFieldID(env, cls, "value", "J");
		(*env)->SetLongField(env, local_ref, valID, (jlong) 0L);
		if (entry != root_entry) {
			free(entry);
		}
		(*env)->DeleteLocalRef(env, local_ref);
		set->entries[i] = NULL;
	}
	set->entrycount = 0;
	jmethodID const_ID = (*env)->GetMethodID(env, folder_cls, "<init>", "(Lde/hechler/patrick/pfs/fs/impl/NativePatrFileSys;J)V");
	root_obj = (*env)->NewObject(env, folder_cls, const_ID, caller, (jlong) root_entry);
	root_entry->object = (*env)->NewWeakGlobalRef(env, root_obj);
	hashset_put(set, hash(root_entry), root_entry);
}

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    blockCount
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_blockCount(
        JNIEnv *env, jobject caller) {

}

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    blockSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_blockSize(JNIEnv *env,
        jobject caller) {

}

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_close(JNIEnv *env,
        jobject caller) {

}

/*
 * Class:     de_hechler_patrick_pfs_fs_impl_NativePatrFileSys
 * Method:    finalize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_de_hechler_patrick_pfs_fs_impl_NativePatrFileSys_finalize(JNIEnv *env,
        jobject caller) {

}

static void* java_bm_get(struct bm_block_manager *bm, i64 block) {
	struct java_block_manager *jbm = (void*) bm;
	jclass cls = (*jbm->env)->GetObjectClass(jbm->env, jbm->java_impl);
	(*jbm->env)->GetMethodID(jbm->env, cls, "get", "()[");
}
static int java_bm_unget(struct bm_block_manager *bm, i64 block) {

}
static int java_bm_set(struct bm_block_manager *bm, i64 block) {

}
static int java_bm_sync_bm(struct bm_block_manager *bm) {

}
static int java_bm_close_bm(struct bm_block_manager *bm) {

}
static i64 java_bm_get_flags(struct bm_block_manager *bm, i64 block) {

}
static int java_bm_set_flags(struct bm_block_manager *bm, i64 block, i64 flags) {

}
static i64 java_bm_first_zero_flagged_block(struct bm_block_manager *bm) {

}
static int java_bm_delete_all_flags(struct bm_block_manager *bm) {

}
