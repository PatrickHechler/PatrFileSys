package de.hechler.patrick.pfs.utils;


public class PatrFileSysConstants {
	
	public static final int FB_BLOCK_COUNT_OFFSET  = 0;
	public static final int FB_BLOCK_LENGTH_OFFSET = FB_BLOCK_COUNT_OFFSET + 8;
	public static final int FB_ROOT_BLOCK_OFFSET   = FB_BLOCK_LENGTH_OFFSET + 4;
	public static final int FB_ROOT_POS_OFFSET     = FB_ROOT_BLOCK_OFFSET + 8;
	public static final int FB_START_ROOT_POS      = FB_ROOT_POS_OFFSET + 4;
	
	public static final int ELEMENT_FLAG_FOLDER     = 0x00000001;
	public static final int ELEMENT_FLAG_READ_ONLY  = 0x00000002;
	public static final int ELEMENT_FLAG_EXECUTABLE = 0x00000004;
	public static final int ELEMENT_FLAG_HIDDEN     = 0x00000008;
	
	public static final int ELEMENT_OFFSET_FLAGS              = 0;
	public static final int ELEMENT_OFFSET_PARENT_POS         = ELEMENT_OFFSET_FLAGS + 4;
	public static final int ELEMENT_OFFSET_PARENT_BLOCK       = ELEMENT_OFFSET_PARENT_POS + 4;
	public static final int ELEMENT_OFFSET_LOCK_VALUE         = ELEMENT_OFFSET_PARENT_BLOCK + 8;
	public static final int ELEMENT_OFFSET_LOCK_TIME          = ELEMENT_OFFSET_LOCK_VALUE + 8;
	public static final int ELEMENT_OFFSET_CREATE_TIME        = ELEMENT_OFFSET_LOCK_TIME + 8;
	public static final int ELEMENT_OFFSET_LAST_MOD_TIME      = ELEMENT_OFFSET_CREATE_TIME + 8;
	public static final int ELEMENT_OFFSET_LAST_META_MOD_TIME = ELEMENT_OFFSET_LAST_MOD_TIME + 8;
	public static final int ELEMENT_OFFSET_NAME               = ELEMENT_OFFSET_LAST_META_MOD_TIME + 8;
	public static final int ELEMENT_OFFSET_OWNER              = ELEMENT_OFFSET_NAME + 4;
	
	public static final int FOLDER_OFFSET_ELEMENT_COUNT   = ELEMENT_OFFSET_OWNER + 4;
	public static final int FOLDER_OFFSET_FOLDER_ELEMENTS = FOLDER_OFFSET_ELEMENT_COUNT + 4;
	
	public static final int FILE_OFFSET_FILE_LENGTH     = ELEMENT_OFFSET_OWNER + 4;
	public static final int FILE_OFFSET_FILE_HASH_TIME  = FILE_OFFSET_FILE_LENGTH + 8;
	public static final int FILE_OFFSET_FILE_HASH_CODE  = FILE_OFFSET_FILE_HASH_TIME + 8;
	public static final int FILE_OFFSET_FILE_DATA_TABLE = FILE_OFFSET_FILE_HASH_CODE + 16;
	
	public static final int FOLDER_ELEMENT_OFFSET_BLOCK = 0;
	public static final int FOLDER_ELEMENT_OFFSET_POS   = FOLDER_ELEMENT_OFFSET_BLOCK + 8;
	public static final int FOLDER_ELEMENT_LENGTH       = FOLDER_ELEMENT_OFFSET_POS + 4;
	
	public static final int  OWNER_NO_OWNER = 0;
	public static final long LOCK_NO_LOCK   = 0L;
	
	public static final int  LOCK_USER_SHIFT                  = 0;
	public static final long LOCK_USER_MAX_VALUE              = 0x00000000FFFFFFFFL;
	public static final long LOCK_USER_RANDOM_BYTE_AND        = 0x0FF0000000000000L;
	public static final int  LOCK_USER_RANDOM_BYTE_SHIFT      = 52;
	public static final long LOCK_NO_READ_ALLOWED_LOCK        = 0x0000000100000000L;
	public static final long LOCK_NO_WRITE_ALLOWED_LOCK       = 0x0000000200000000L;
	public static final long LOCK_NO_DELETE_ALLOWED_LOCK      = 0x0000000400000000L;
	public static final long LOCK_NO_META_CHANGE_ALLOWED_LOCK = 0x0000000800000000L;
	public static final int  LOCK_SHARED_COUNTER_SHIFT        = 36;
	public static final long LOCK_SHARED_COUNTER_AND          = 0x000FFFF000000000L;
	public static final long LOCK_SHARED_RANDOM               = LOCK_USER_RANDOM_BYTE_AND | (LOCK_USER_MAX_VALUE << LOCK_USER_SHIFT);
	public static final long LOCK_SHARED_COUNTER_MAX_VALUE    = LOCK_SHARED_COUNTER_AND >>> LOCK_SHARED_COUNTER_SHIFT;
	public static final long LOCK_SHARED_LOCK                 = 0x4000000000000000L;
	public static final long LOCK_LOCKED_LOCK                 = 0x8000000000000000L;
	public static final long LOCK_NO_DATA                     = LOCK_SHARED_RANDOM | LOCK_SHARED_COUNTER_AND;
	public static final long LOCK_DATA                        = ~LOCK_NO_DATA;
	
	public static final long NO_TIME = -1L;
	
}
