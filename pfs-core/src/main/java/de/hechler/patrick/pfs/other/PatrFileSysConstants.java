package de.hechler.patrick.pfs.other;


public class PatrFileSysConstants {
	
	public static class B0 {
		
		public static final long MAGIC                       = 0xF17565393C422698L;
		public static final int  OFF_ROOT_BLOCK              = 8;
		public static final int  OFF_ROOT_POS                = 16;
		public static final int  OFF_BLOCK_SIZE              = 20;
		public static final int  OFF_BLOCK_COUNT             = 24;
		public static final int  OFF_BLOCK_TABLE_FIRST_BLOCK = 32;
		public static final int  SIZE                        = 40;
		
	}
	
	public static class BlockFlags {
		
		public static final long USED_BIT      = 0;
		public static final long ENTRIES_BIT   = 1;
		public static final long FILE_DATA_BIT = 2;
		
		public static final long USED      = 1;
		public static final long ENTRIES   = 2;
		public static final long FILE_DATA = 4;
		
	}
	
	public static class Element {
		
		public static final int OFF_LAST_MODIFY_TIME = 0;
		
		public static class Folder {
			
			public static final int OFF_PARENT_BLOCK       = 8;
			public static final int OFF_PARENT_POS         = 16;
			public static final int OFF_DIRECT_CHILD_COUNT = 20;
			public static final int OFF_ENTRY_BLOCK        = 24;
			public static final int OFF_ENTRY_POS          = 32;
			public static final int OFF_HELPER_INDEX       = 36;
			public static final int OFF_ENTRIES            = 40;
			public static final int EMPTY_SIZE             = OFF_ENTRIES;
			
			public static class Entry {
				
				public static final int OFF_CHILD_BLOCK = 0;
				public static final int OFF_CHILD_POS   = 8;
				public static final int OFF_NAME_POS    = 12;
				public static final int OFF_CREATE_TIME = 16;
				public static final int OFF_FLAGS       = 24;
				public static final int SIZE            = 28;
				
				public static class Flags {
					
					public static final int UNMODIFIABLE    = 0x000000FF;
					public static final int FOLDER          = 0x00000001;
					public static final int FILE            = 0x00000002;
					public static final int PIPE            = 0x00000004;
					public static final int FILE_EXECUTABLE = 0x00000100;
					public static final int FILE_ENCRYPTED  = 0x00000200;
					public static final int HIDDEN          = 0x01000000;
					
				}
				
			}
			
		}
		
		public static class File {
			
			public static final int OFF_FILE_LENGTH = 8;
			public static final int OFF_FIRST_BLOCK = 16;
			public static final int SIZE            = 24;
			
			public static class Pipe {
				
				public static final int OFF_START_OFFSET = 24;
				public static final int SIZE             = 28;
				
			}
			
		}
		
	}
	
}
