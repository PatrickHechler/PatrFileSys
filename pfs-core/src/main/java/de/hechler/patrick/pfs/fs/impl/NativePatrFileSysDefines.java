package de.hechler.patrick.pfs.fs.impl;

import static jdk.incubator.foreign.ValueLayout.ADDRESS;
import static jdk.incubator.foreign.ValueLayout.JAVA_INT;
import static jdk.incubator.foreign.ValueLayout.JAVA_LONG;

import java.lang.invoke.MethodHandle;
import java.nio.file.Paths;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.SymbolLookup;

public class NativePatrFileSysDefines {
	
	static {
		System.load(Paths.get("../native-pfs/native-core/exports/libpfs.so").toAbsolutePath().normalize().toString());
		System.load(
			Paths.get("../native-pfs/java-helper/bin/libjava-helper.so").toAbsolutePath().normalize().toString());
	}
	
	public static final CLinker      LINKER  = CLinker.systemCLinker();;
	public static final SymbolLookup LOOCKUP = SymbolLookup.loaderLookup();;
	
	public static MethodHandle handle(String name, MemoryLayout returnType, MemoryLayout... argTypes) {
		return LINKER.downcallHandle(LOOCKUP.lookup(name).orElseThrow(), FunctionDescriptor.of(returnType, argTypes));
	}
	
	public static class Errno {
		
		/** if pfs_errno is not set/no error occurred */
		public static final int NONE                  = 0;
		/** if an operation failed because of an unknown/unspecified error */
		public static final int UNKNOWN_ERROR         = 1;
		/** if the iterator has no next element */
		public static final int NO_MORE_ELEMNETS      = 2;
		/**
		 * if an IO operation failed because the element is not of the correct type (file expected, but folder or reverse)
		 */
		public static final int ELEMENT_WRONG_TYPE    = 3;
		/** if an IO operation failed because the element does not exist */
		public static final int ELEMENT_NOT_EXIST     = 4;
		/** if an IO operation failed because the element already existed */
		public static final int ELEMENT_ALREADY_EXIST = 5;
		/**
		 * if an IO operation failed because there was not enough space in the file system
		 */
		public static final int OUT_OF_SPACE          = 6;
		/** if an unspecified IO error occurred */
		public static final int IO_ERR                = 7;
		/** if there was at least one invalid argument */
		public static final int ILLEGAL_ARG           = 8;
		/**
		 * if an IO operation failed because there was not enough space in the file system
		 */
		public static final int OUT_OF_MEMORY         = 9;
		/**
		 * if an IO operation failed because the root folder has some restrictions
		 */
		public static final int ROOT_FOLDER           = 10;
		/**
		 * if an folder can not be moved because the new child (maybe a deep/indirect child) is a child of the folder
		 */
		public static final int PARENT_IS_CHILD       = 11;
		
	}
	
	public static class BlockManager {
		
		/**
		 * declaration:<br>
		 * <code>
		 * void* (*const get)(struct bm_block_manager *bm, i64 block);
		 * </code>
		 */
		public static final long OFFSET_GET                      = 32;
		/**
		 * declaration:<br>
		 * <code>
		 * int (*const unget)(struct bm_block_manager *bm, i64 block);
		 * </code>
		 */
		public static final long OFFSET_UNGET                    = 40;
		/**
		 * declaration:<br>
		 * <code>
		 * int (*const set)(struct bm_block_manager *bm, i64 block);
		 * </code>
		 */
		public static final long OFFSET_SET                      = 48;
		/**
		 * declaration:<br>
		 * <code>
		 * int (*const sync_bm)(struct bm_block_manager *bm);
		 * </code>
		 */
		public static final long OFFSET_SYNC_BM                  = 56;
		/**
		 * declaration:<br>
		 * <code>
		 * int (*const close_bm)(struct bm_block_manager *bm);
		 * </code>
		 */
		public static final long OFFSET_CLOSE_BM                 = 64;
		/**
		 * declaration:<br>
		 * <code>
		 * const ui32 block_size;
		 * </code>
		 */
		public static final long OFFSET_BLOCK_SIZE               = 72;
		/**
		 * declaration:<br>
		 * <code>
		 * const int block_flag_bits;
		 * </code>
		 */
		public static final long OFFSET_BLOCK_FLAG_BITS          = 76;
		/**
		 * declaration:<br>
		 * <code>
		 * i64 (*const get_flags)(struct bm_block_manager *bm, i64 block);
		 * </code>
		 */
		public static final long OFFSET_GET_FLAGS                = 80;
		/**
		 * declaration:<br>
		 * <code>
		 * int (*const set_flags)(struct bm_block_manager *bm, i64 block, i64 flags);
		 * </code>
		 */
		public static final long OFFSET_SET_FLAGS                = 88;
		/**
		 * declaration:<br>
		 * <code>
		 * i64 (*const first_zero_flagged_block)(struct bm_block_manager *bm);
		 * </code>
		 */
		public static final long OFFSET_FIRST_ZERO_FLAGGED_BLOCK = 96;
		/**
		 * declaration:<br>
		 * <code>
		 * int (*const delete_all_flags)(struct bm_block_manager *bm);
		 * </code>
		 */
		public static final long OFFSET_DELETE_ALL_FLAGS         = 104;
		
		private static final String      S_NEW_RAM      = "bm_new_ram_block_manager";
		/**
		 * declaration:<br>
		 * <code>
		 * extern struct bm_block_manager* bm_new_ram_block_manager
		 * (i64 block_count, i32 block_size);
		 * </code>
		 */
		public static final MethodHandle NEW_RAM        = handle(S_NEW_RAM, ADDRESS, JAVA_LONG, JAVA_INT);
		private static final String      S_NEW_FILE     = "bm_new_file_block_manager";
		/**
		 * declaration:<br>
		 * <code>
		 * extern struct bm_block_manager* bm_new_file_block_manager
		 * (int file, i32 block_size);
		 * </code>
		 */
		public static final MethodHandle NEW_FILE       = handle(S_NEW_FILE, ADDRESS, JAVA_INT, JAVA_INT);
		private static final String      S_NEW_FLAG_RAM = "bm_new_flaggable_ram_block_manager";
		/**
		 * declaration:<br>
		 * <code>
		 * extern struct bm_block_manager* bm_new_flaggable_ram_block_manager
		 * (i64 block_count, i32 block_size);
		 * </code>
		 */
		public static final MethodHandle NEW_FLAG_RAM   = handle(S_NEW_FLAG_RAM, ADDRESS, JAVA_LONG, JAVA_INT);
		
	}
	
	public static class FileSys {
		
		/**
		 * declaration:<br>
		 * <code>
		 * extern struct bm_block_manager *pfs;
		 * </code>
		 */
		private static final String S_PFS = "pfs";
		/* named FS because of there is the interface PFS */
		public static final MemoryAddress FS      = LOOCKUP.lookup(S_PFS).orElseThrow().address();
		private static final String       S_ERRNO = "pfs_errno";
		/**
		 * declaration:<br>
		 * <code>
		 * extern ui32 pfs_errno;
		 * </code>
		 */
		public static final MemoryAddress ERRNO   = LOOCKUP.lookup(S_ERRNO).orElseThrow().address();
		
		private static final String      S_FORMAT      = "pfs_format";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_format(i64 block_count);
		 * </code>
		 */
		public static final MethodHandle FORMAT        = handle(S_FORMAT, JAVA_INT, JAVA_INT);
		private static final String      S_BLOCK_COUNT = "pfs_block_count";
		/**
		 * declaration:<br>
		 * <code>
		 * extern i64 pfs_block_count();
		 * </code>
		 */
		public static final MethodHandle BLOCK_COUNT   = handle(S_BLOCK_COUNT, JAVA_LONG);
		private static final String      S_BLOCK_SIZE  = "pfs_block_size";
		/**
		 * declaration:<br>
		 * <code>
		 * extern i32 pfs_block_size();
		 * </code>
		 */
		public static final MethodHandle BLOCK_SIZE    = handle(S_BLOCK_SIZE, JAVA_INT);
		private static final String      S_FILL_ROOT   = "pfs_fill_root";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_fill_root(pfs_eh overwrite_me);
		 * </code>
		 */
		public static final MethodHandle FILL_ROOT     = handle(S_FILL_ROOT, JAVA_INT, ADDRESS);
		private static final String      S_ROOT        = "pfs_root";
		/**
		 * declaration:<br>
		 * <code>
		 * extern pfs_eh pfs_root();
		 * </code>
		 */
		public static final MethodHandle ROOT          = handle(S_ROOT, JAVA_INT, ADDRESS);
		
	}
	
	public static class Constants {
		
		/**
		 * declaration:<br>
		 * <code>
		 * #define PFS_EH_SIZE 44
		 * </code>
		 */
		public static final long EH_SIZE = 44L;
		/**
		 * declaration:<br>
		 * <code>
		 * #define PFS_FI_SIZE 40
		 * </code>
		 */
		public static final long FI_SIZE = 40L;
		
		/**
		 * declaration:<br>
		 * <code>
		 * #define PFS_NO_TIME -1L
		 * </code>
		 */
		public static final long NO_TIME = -1L;
		
		/**
		 * declaration:<br>
		 * <code>
		 * #define PFS_MAGIC_START 0xF17565393C422698UL
		 * </code>
		 */
		public static final long MAGIC_START           = 0xF17565393C422698L;
		/**
		 * declaration:<br>
		 * <code>
		 * #define PFS_B0_OFFSET_BLOCK_COUNT 24
		 * </code>
		 */
		public static final long B0_OFFSET_BLOCK_COUNT = 24L;
		/**
		 * declaration:<br>
		 * <code>
		 * #define PFS_B0_OFFSET_BLOCK_SIZE 20
		 * </code>
		 */
		public static final long B0_OFFSET_BLOCK_SIZE  = 20L;
		
		/**
		 * declaration:<br>
		 * <code>
		 * #define PFS_FLAGS_ESSENTIAL_FLAGS (PFS_FLAGS_FILE | PFS_FLAGS_FOLDER | PFS_FLAGS_PIPE)
		 * </code>
		 */
		public static final int FLAGS_ESSENTIAL_FLAGS = 0x00000007;
		/**
		 * declaration:<br>
		 * <code>
		 * #define	PFS_UNMODIFIABLE_FLAGS    0x000000FFU
		 * </code>
		 */
		public static final int UNMODIFIABLE_FLAGS    = 0x000000FF;
		/**
		 * declaration:<br>
		 * <code>
		 * #define	PFS_FLAGS_FOLDER          0x00000001U
		 * </code>
		 */
		public static final int FLAGS_FOLDER          = 0x00000001;
		/**
		 * declaration:<br>
		 * <code>
		 * #define	PFS_FLAGS_FILE            0x00000002U
		 * </code>
		 */
		public static final int FLAGS_FILE            = 0x00000002;
		/**
		 * declaration:<br>
		 * <code>
		 * #define	PFS_FLAGS_PIPE            0x00000004U
		 * </code>
		 */
		public static final int FLAGS_PIPE            = 0x00000004;
		/**
		 * declaration:<br>
		 * <code>
		 * #define	PFS_FLAGS_HELPER_FOLDER   0x00000080U
		 * </code>
		 */
		public static final int FLAGS_HELPER_FOLDER   = 0x00000080;
		/**
		 * declaration:<br>
		 * <code>
		 * #define	PFS_FLAGS_TYPE_SPECIFIC   0x0000FF00U
		 * </code>
		 */
		public static final int FLAGS_TYPE_SPECIFIC   = 0x0000FF00;
		/**
		 * declaration:<br>
		 * <code>
		 * #define	PFS_FLAGS_FILE_EXECUTABLE 0x00000100U
		 * </code>
		 */
		public static final int FLAGS_FILE_EXECUTABLE = 0x00000100;
		/**
		 * declaration:<br>
		 * <code>
		 * #define	PFS_FLAGS_FILE_ENCRYPTED  0x00000200U
		 * </code>
		 */
		public static final int FLAGS_FILE_ENCRYPTED  = 0x00000200;
		/**
		 * declaration:<br>
		 * <code>
		 * #define	PFS_FLAGS_HIDDEN          0x01000000U
		 * </code>
		 */
		public static final int FLAGS_HIDDEN          = 0x01000000;
		
	}
	
	public static class Element {
		
		private static final String      S_GET_FLAGS         = "pfs_element_get_flags";
		/**
		 * declaration:<br>
		 * <code>
		 * extern ui32 pfs_element_get_flags(pfs_eh e);
		 * </code>
		 */
		public static final MethodHandle GET_FLAGS           = handle(S_GET_FLAGS, JAVA_INT, ADDRESS);
		private static final String      S_MODIFY_FLAGS      = "pfs_element_modify_flags";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_element_modify_flags(pfs_eh e, ui32 add_flags, ui32 rem_flags);
		 * </code>
		 */
		public static final MethodHandle MODIFY_FLAGS        = handle(S_MODIFY_FLAGS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT);
		private static final String      S_GET_NAME_LENGTH   = "pfs_element_get_name_length";
		/**
		 * declaration:<br>
		 * <code>
		 * extern i64 pfs_element_get_name_length(pfs_eh e);
		 * </code>
		 */
		public static final MethodHandle GET_NAME_LENGTH     = handle(S_GET_NAME_LENGTH, JAVA_LONG, ADDRESS);
		private static final String      S_GET_NAME          = "pfs_element_get_name";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_element_get_name(pfs_eh e, char **buffer, i64 *buffer_len);
		 * </code>
		 */
		public static final MethodHandle GET_NAME            = handle(S_GET_NAME, JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
		private static final String      S_SET_NAME          = "pfs_element_set_name";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_element_set_name(pfs_eh e, char *name);
		 * </code>
		 */
		public static final MethodHandle SET_NAME            = handle(S_SET_NAME, JAVA_INT, ADDRESS, ADDRESS);
		private static final String      S_GET_CREATE_TIME   = "pfs_element_get_create_time";
		/**
		 * declaration:<br>
		 * <code>
		 * extern i64 pfs_element_get_create_time(pfs_eh e);
		 * </code>
		 */
		public static final MethodHandle GET_CREATE_TIME     = handle(S_GET_CREATE_TIME, JAVA_LONG, ADDRESS);
		private static final String      S_SET_CREATE_TIME   = "pfs_element_set_create_time";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_element_set_create_time(pfs_eh e, i64 new_time);
		 * </code>
		 */
		public static final MethodHandle SET_CREATE_TIME     = handle(S_SET_CREATE_TIME, JAVA_INT, ADDRESS, JAVA_LONG);
		private static final String      S_GET_LAST_MOD_TIME = "pfs_element_get_last_mod_time";
		/**
		 * declaration:<br>
		 * <code>
		 * extern i64 pfs_element_get_last_mod_time(pfs_eh e);
		 * </code>
		 */
		public static final MethodHandle GET_LAST_MOD_TIME   = handle(S_GET_LAST_MOD_TIME, JAVA_LONG, ADDRESS);
		private static final String      S_SET_LAST_MOD_TIME = "pfs_element_set_last_mod_time";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_element_set_last_mod_time(pfs_eh e, i64 new_time);
		 * </code>
		 */
		public static final MethodHandle SET_LAST_MOD_TIME   = handle(S_SET_LAST_MOD_TIME, JAVA_INT, ADDRESS, JAVA_LONG);
		private static final String      S_DELETE            = "pfs_element_delete";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_element_delete(pfs_eh e);
		 * </code>
		 */
		public static final MethodHandle DELETE              = handle(S_DELETE, JAVA_INT, ADDRESS);
		private static final String      S_GET_PARENT        = "pfs_element_get_parent";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_element_get_parent(pfs_eh e);
		 * </code>
		 */
		public static final MethodHandle GET_PARENT          = handle(S_GET_PARENT, JAVA_INT, ADDRESS);
		
		private static final String      S_SET_PARENT = "pfs_element_set_parent";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_element_set_parent(pfs_eh e, pfs_eh new_parent);
		 * </code>
		 */
		public static final MethodHandle SET_PAREMT   = handle(S_SET_PARENT, JAVA_INT, ADDRESS, ADDRESS);
		private static final String      S_MOVE       = "pfs_element_move";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_element_move(pfs_eh e, pfs_eh new_parent, char *name);
		 * </code>
		 */
		public static final MethodHandle MOVE         = handle(S_MOVE, JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
		
	}
	
	public static class Folder {
		
		private static final String      S_ITERATOR               = "pfs_folder_iterator";
		/**
		 * declaration:<br>
		 * <code>
		 * extern pfs_fi pfs_folder_iterator(pfs_eh f, int show_hidden);
		 * </code>
		 */
		public static final MethodHandle ITERATOR                 = handle(S_ITERATOR, ADDRESS, ADDRESS, JAVA_INT);
		private static final String      S_FILL_ITERATOR          = "pfs_folder_fill_iterator";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_folder_fill_iterator(pfs_eh f, pfs_fi iter, int show_hidden);
		 * </code>
		 */
		public static final MethodHandle FILL_ITERATOR            = handle(S_FILL_ITERATOR, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);
		private static final String      S_ITER_NEXT              = "pfs_folder_iter_next";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_folder_iter_next(pfs_fi fi);
		 * </code>
		 */
		public static final MethodHandle ITER_NEXT                = handle(S_ITER_NEXT, JAVA_INT, ADDRESS);
		private static final String      S_CHILD_COUNT            = "pfs_folder_child_count";
		/**
		 * declaration:<br>
		 * <code>
		 * extern i64 pfs_folder_child_count(pfs_eh f);
		 * </code>
		 */
		public static final MethodHandle CHILD_COUNT              = handle(S_CHILD_COUNT, JAVA_LONG, ADDRESS);
		private static final String      S_CHILD_FROM_NAME        = "pfs_folder_child_from_name";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_folder_child_from_name(pfs_eh f, char *name);
		 * </code>
		 */
		public static final MethodHandle CHILD_FROM_NAME          = handle(S_CHILD_FROM_NAME, JAVA_INT, ADDRESS, ADDRESS);
		private static final String      S_FOLDER_CHILD_FROM_NAME = "pfs_folder_folder_child_from_name";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_folder_folder_child_from_name(pfs_eh f, char *name);
		 * </code>
		 */
		public static final MethodHandle FOLDER_CHILD_FROM_NAME   = handle(S_FOLDER_CHILD_FROM_NAME, JAVA_INT, ADDRESS, ADDRESS);
		private static final String      S_FILE_CHILD_FROM_NAME   = "pfs_folder_file_child_from_name";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_folder_file_child_from_name(pfs_eh f, char *name);
		 * </code>
		 */
		public static final MethodHandle FILE_CHILD_FROM_NAME     = handle(S_FILE_CHILD_FROM_NAME, JAVA_INT, ADDRESS, ADDRESS);
		private static final String      S_PIPE_CHILD_FROM_NAME   = "pfs_folder_pipe_child_from_name";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_folder_pipe_child_from_name(pfs_eh f, char *name);
		 * </code>
		 */
		public static final MethodHandle PIPE_CHILD_FROM_NAME     = handle(S_PIPE_CHILD_FROM_NAME, JAVA_INT, ADDRESS, ADDRESS);
		private static final String      S_CREATE_FOLDER          = "pfs_folder_create_folder";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_folder_create_folder(pfs_eh f, pfs_eh parent, const char *name);
		 * </code>
		 */
		public static final MethodHandle CREATE_FOLDER            = handle(S_CREATE_FOLDER, JAVA_INT, ADDRESS, ADDRESS);
		private static final String      S_CREATE_FILE            = "pfs_folder_create_file";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_folder_create_file(pfs_eh f, pfs_eh parent, const char *name);
		 * </code>
		 */
		public static final MethodHandle CREATE_FILE              = handle(S_CREATE_FILE, JAVA_INT, ADDRESS, ADDRESS);
		private static final String      S_CREATE_PIPE            = "pfs_folder_create_pipe";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_folder_create_pipe(pfs_eh f, pfs_eh parent, const char *name);
		 * </code>
		 */
		public static final MethodHandle CREATE_PIPE              = handle(S_CREATE_PIPE, JAVA_INT, ADDRESS, ADDRESS);
		
	}
	
	public static class File {
		
		private static final String      S_READ     = "pfs_file_read";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_file_read(pfs_eh f, i64 position, void *buffer, i64 length);
		 * </code>
		 */
		public static final MethodHandle READ       = handle(S_READ, JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, JAVA_LONG);
		private static final String      S_WRITE    = "pfs_file_write";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_file_write(pfs_eh f, i64 position, void *data, i64 length);
		 * </code>
		 */
		public static final MethodHandle WRITE      = handle(S_WRITE, JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, JAVA_LONG);
		private static final String      S_APPEND   = "pfs_file_append";
		/**
		 * declaration:<br>
		 * <code>
		 * extern i64 pfs_file_append(pfs_eh f, void *data, i64 length);
		 * </code>
		 */
		public static final MethodHandle APPEND     = handle(S_APPEND, JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG);
		private static final String      S_TRUNCATE = "pfs_file_truncate";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_file_truncate(pfs_eh f, i64 new_length);
		 * </code>
		 */
		public static final MethodHandle TRUNCATE   = handle(S_TRUNCATE, JAVA_INT, ADDRESS, JAVA_LONG);
		private static final String      S_LENGTH   = "pfs_file_length";
		/**
		 * declaration:<br>
		 * <code>
		 * extern i64 pfs_file_length(pfs_eh f);
		 * </code>
		 */
		public static final MethodHandle LENGTH     = handle(S_LENGTH, JAVA_LONG, ADDRESS);
		
	}
	
	public static class Pipe {
		
		private static final String      S_READ   = "pfs_pipe_read";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int pfs_pipe_read(pfs_eh p, void *buffer, i64 length);
		 * </code>
		 */
		public static final MethodHandle READ     = handle(S_READ, JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG);
		private static final String      S_APPEND = "pfs_file_append";
		/**
		 * declaration:<br>
		 * <code>
		 * extern int __REDIRECT(pfs_pipe_append, (pfs_eh p, void *data, i64 length), pfs_file_append);
		 * </code>
		 */
		public static final MethodHandle APPEND   = handle(S_APPEND, JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG);
		private static final String      S_LENGTH = "pfs_pipe_length";
		/**
		 * declaration:<br>
		 * <code>
		 * extern i64 pfs_pipe_length(pfs_eh p);
		 * </code>
		 */
		public static final MethodHandle LEGNTH   = handle(S_LENGTH, JAVA_LONG, ADDRESS);
		
	}
	
}
