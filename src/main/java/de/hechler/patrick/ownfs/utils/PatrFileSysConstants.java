package de.hechler.patrick.ownfs.utils;


public class PatrFileSysConstants {
	
	public static final int FB_BLOCK_COUNT_OFFSET      = 0;
	public static final int FB_BLOCK_LENGTH_OFFSET     = FB_BLOCK_COUNT_OFFSET + 8;
	public static final int FB_BLOCK_ROOT_BLOCK_OFFSET = FB_BLOCK_LENGTH_OFFSET + 4;
	public static final int FB_BLOCK_ROOT_POS_OFFSET   = FB_BLOCK_ROOT_BLOCK_OFFSET + 8;
	
}
