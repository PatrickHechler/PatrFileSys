package de.hechler.patrick.pfs.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class PatrFileSysConstants {
	
	/**
	 * the charset used by this file system
	 */
	public static final Charset CHARSET = StandardCharsets.UTF_16BE;
	
	public static final int FB_BLOCK_COUNT_OFFSET      = 0;
	public static final int FB_BLOCK_LENGTH_OFFSET     = FB_BLOCK_COUNT_OFFSET + 8;
	public static final int FB_ROOT_BLOCK_OFFSET       = FB_BLOCK_LENGTH_OFFSET + 4;
	public static final int FB_ROOT_POS_OFFSET         = FB_ROOT_BLOCK_OFFSET + 8;
	public static final int FB_TABLE_FILE_BLOCK_OFFSET = FB_ROOT_POS_OFFSET + 4;
	public static final int FB_TABLE_FILE_POS_OFFSET   = FB_TABLE_FILE_BLOCK_OFFSET + 8;
	public static final int FB_FILE_SYS_LOCK_VALUE     = FB_TABLE_FILE_POS_OFFSET + 4;
	public static final int FB_FILE_SYS_LOCK_TIME      = FB_FILE_SYS_LOCK_VALUE + 8;
	public static final int FB_FILE_SYS_STATE_VALUE    = FB_FILE_SYS_LOCK_TIME + 8;
	public static final int FB_FILE_SYS_STATE_TIME     = FB_FILE_SYS_STATE_VALUE + 4;
	public static final int FB_START_ROOT_POS          = FB_FILE_SYS_STATE_TIME + 8;
	
	/**
	 * flag used to indicate that the element is a folder
	 */
	public static final int ELEMENT_FLAG_FOLDER         = 0x00000001;
	/**
	 * flag used to indicate that the element is a file
	 */
	public static final int ELEMENT_FLAG_FILE           = 0x00000002;
	/**
	 * flag used to indicate that the element is a link
	 */
	public static final int ELEMENT_FLAG_LINK           = 0x00000004;
	/**
	 * flag used to indicate that the element is read only
	 */
	public static final int ELEMENT_FLAG_READ_ONLY      = 0x00000008;
	/**
	 * flag used to indicate that the element is executable
	 */
	public static final int ELEMENT_FLAG_EXECUTABLE     = 0x00000010;
	/**
	 * flag used to indicate that the element is hidden
	 */
	public static final int ELEMENT_FLAG_HIDDEN         = 0x00000020;
	/**
	 * flag used to indicate that the element is sorted<br>
	 * this flag is used only on folders.
	 * <p>
	 * a folder assigned with the given flag will have its children sorted.<br>
	 * this flag can be used to optimize search operations.
	 * <p>
	 * the current implementation ignores this flag, but removes it every time an element gets added.
	 */
	public static final int ELEMENT_FLAG_FOLDER_SORTED  = 0x00000040;
	/**
	 * flag used to indicate that the element is encrypted.<br>
	 * this flag is used only on files.
	 * <p>
	 * this flag does not specify in which way or with wich algorithm the given file is encrypted
	 * <p>
	 * the current implementation ignores this flag
	 */
	public static final int ELEMENT_FLAG_FILE_ENCRYPTED = 0x00000080;
	
	/**
	 * the offset of the flags relative from the position of a file system element
	 */
	public static final int ELEMENT_OFFSET_FLAGS              = 0;
	/**
	 * the offset of the parent ID relative from the position of a file system element
	 */
	public static final int ELEMENT_OFFSET_PARENT_ID          = ELEMENT_OFFSET_FLAGS + 4;
	/**
	 * the offset of the lock value relative from the position of a file system element
	 */
	public static final int ELEMENT_OFFSET_LOCK_VALUE         = ELEMENT_OFFSET_PARENT_ID + 8;
	/**
	 * the offset of the lock time relative from the position of a file system element
	 */
	public static final int ELEMENT_OFFSET_LOCK_TIME          = ELEMENT_OFFSET_LOCK_VALUE + 8;
	/**
	 * the offset of the create time relative from the position of a file system element
	 */
	public static final int ELEMENT_OFFSET_CREATE_TIME        = ELEMENT_OFFSET_LOCK_TIME + 8;
	/**
	 * the offset of the last modification time relative from the position of a file system element
	 */
	public static final int ELEMENT_OFFSET_LAST_MOD_TIME      = ELEMENT_OFFSET_CREATE_TIME + 8;
	/**
	 * the offset of the last modification inclusive metadata modification time relative from the position of a file system element
	 */
	public static final int ELEMENT_OFFSET_LAST_META_MOD_TIME = ELEMENT_OFFSET_LAST_MOD_TIME + 8;
	/**
	 * the offset of the name position relative from the position of a file system element
	 * <p>
	 * the name is saved in UTF-16-LE ({@link StandardCharsets#UTF_16LE}) and ends with a zero terminating character ({@code '\0'})<br>
	 * the zero terminating character will be converted in two bytes with the value {@code 0}.
	 * <p>
	 * if this element has an empty name, the pointer may be {@code -1} (the root element often has an empty name)
	 */
	public static final int ELEMENT_OFFSET_NAME               = ELEMENT_OFFSET_LAST_META_MOD_TIME + 8;
	
	public static final int FOLDER_OFFSET_ELEMENT_COUNT   = ELEMENT_OFFSET_NAME + 4;
	public static final int FOLDER_OFFSET_FOLDER_ELEMENTS = FOLDER_OFFSET_ELEMENT_COUNT + 4;
	
	public static final int FILE_OFFSET_FILE_LENGTH     = ELEMENT_OFFSET_NAME + 4;
	public static final int FILE_OFFSET_FILE_HASH_TIME  = FILE_OFFSET_FILE_LENGTH + 8;
	public static final int FILE_OFFSET_FILE_HASH_CODE  = FILE_OFFSET_FILE_HASH_TIME + 8;
	public static final int FILE_OFFSET_FILE_DATA_TABLE = FILE_OFFSET_FILE_HASH_CODE + 32;
	
	public static final int LINK_OFFSET_TARGET_ID = ELEMENT_OFFSET_NAME + 4;
	public static final int LINK_LENGTH           = LINK_OFFSET_TARGET_ID + 8;
	
	public static final int FOLDER_ELEMENT_LENGTH = 8;
	
	public static final int ELEMENT_TABLE_OFFSET_BLOCK   = 0;
	public static final int ELEMENT_TABLE_OFFSET_POS     = ELEMENT_TABLE_OFFSET_BLOCK + 8;
	public static final int ELEMENT_TABLE_ELEMENT_LENGTH = ELEMENT_TABLE_OFFSET_POS + 4;
	
	public static final long NO_LOCK = 0L;
	
	public static final long LOCK_START_MAX_VALUE             = 0x00000000FFFFFFFFL;
	public static final long LOCK_USER_RANDOM_BYTE_AND        = 0x0FF0000000000000L;
	public static final int  LOCK_USER_RANDOM_BYTE_SHIFT      = 52;
	public static final long LOCK_NO_READ_ALLOWED_LOCK        = 0x0000000100000000L;
	public static final long LOCK_NO_WRITE_ALLOWED_LOCK       = 0x0000000200000000L;
	public static final long LOCK_NO_DELETE_ALLOWED_LOCK      = 0x0000000400000000L;
	public static final long LOCK_NO_META_CHANGE_ALLOWED_LOCK = 0x0000000800000000L;
	public static final int  LOCK_SHARED_COUNTER_SHIFT        = 36;
	public static final long LOCK_SHARED_COUNTER_AND          = 0x000FFFF000000000L;
	public static final long LOCK_SHARED_RANDOM               = LOCK_USER_RANDOM_BYTE_AND | LOCK_START_MAX_VALUE;
	public static final long LOCK_SHARED_COUNTER_MAX_VALUE    = LOCK_SHARED_COUNTER_AND >>> LOCK_SHARED_COUNTER_SHIFT;
	public static final long LOCK_SHARED_LOCK                 = 0x4000000000000000L;
	public static final long LOCK_LOCKED_LOCK                 = 0x8000000000000000L;
	public static final long LOCK_NO_DATA                     = LOCK_SHARED_RANDOM | LOCK_SHARED_COUNTER_AND;
	public static final long LOCK_DATA                        = ~LOCK_NO_DATA;
	
	public static final long NO_TIME = -1L;
	
	public static final long MIN_NORMAL_ID         = 0L;
	public static final long NO_ID                 = -1L;
	public static final long ROOT_FOLDER_ID        = -2L;
	public static final long ELEMENT_TABLE_FILE_ID = -3L;
	
}
