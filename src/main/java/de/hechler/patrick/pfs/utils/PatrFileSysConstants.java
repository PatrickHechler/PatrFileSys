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
	public static final int FILE_OFFSET_FILE_DATA_TABLE = FILE_OFFSET_FILE_LENGTH + 8;
	
	public static final int FOLDER_ELEMENT_OFFSET_BLOCK = 0;
	public static final int FOLDER_ELEMENT_OFFSET_POS   = FOLDER_ELEMENT_OFFSET_BLOCK + 8;
	public static final int FOLDER_ELEMENT_LENGTH       = FOLDER_ELEMENT_OFFSET_POS + 4;
	
	public static final int  OWNER_NO_OWNER = 0;
	public static final long LOCK_NO_LOCK   = 0L;
	
}
