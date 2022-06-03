package de.hechler.patrick.pfs.utils;

import java.net.URI;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Map;

import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImpl;

public class JavaPFSConsants {
	
	/**
	 * the constant used when the basic attribute view should be used
	 */
	public static final String ATTR_VIEW_BASIC = "basic";
	/**
	 * the constant used when the patr attribute view should be used
	 */
	public static final String ATTR_VIEW_PATR  = "patr";
	
	/**
	 * name of the basic attribute view for the file key
	 * <p>
	 * the corresponding value must be of type {@link Object}
	 */
	public static final String BASIC_ATTRIBUTE_FILE_KEY           = "fileKey";
	/**
	 * name of the basic attribute view for is other (not file,dir,symbol-link)
	 * <p>
	 * the corresponding value must be of type {@link Boolean}
	 */
	public static final String BASIC_ATTRIBUTE_IS_OTHER           = "isOther";
	/**
	 * name of the basic attribute view for is symbol-link
	 * <p>
	 * the corresponding value must be of type {@link Boolean}
	 */
	public static final String BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK   = "isSymbolicLink";
	/**
	 * name of the basic attribute view for is directory
	 * <p>
	 * the corresponding value must be of type {@link Boolean}
	 */
	public static final String BASIC_ATTRIBUTE_IS_DIRECTORY       = "isDirectory";
	/**
	 * name of the basic attribute view for is file
	 * <p>
	 * the corresponding value must be of type {@link Boolean}
	 */
	public static final String BASIC_ATTRIBUTE_IS_REGULAR_FILE    = "isRegularFile";
	/**
	 * name of the basic attribute view for the size
	 * <p>
	 * the corresponding value must be of type {@link Long}
	 */
	public static final String BASIC_ATTRIBUTE_SIZE               = "size";
	/**
	 * name of the basic attribute view for the creation time
	 * <p>
	 * the corresponding value must be of type {@link FileTime}
	 */
	public static final String BASIC_ATTRIBUTE_CREATION_TIME      = "creationTime";
	/**
	 * name of the basic attribute view for the last access time
	 * <p>
	 * the corresponding PFSFileSystemProviderImplvalue must be of type {@link FileTime}
	 */
	public static final String BASIC_ATTRIBUTE_LAST_ACCESS_TIME   = "lastAccessTime";
	/**
	 * name of the basic attribute view for the last modified time
	 * <p>
	 * the corresponding value must be of type {@link FileTime}
	 */
	public static final String BASIC_ATTRIBUTE_LAST_MODIFIED_TIME = "lastModifiedTime";
	/**
	 * {@link FileAttribute} name for the hidden flag.<br>
	 * {@link FileAttribute#value()} must be of the {@link Boolean} type<br>
	 * <code>true</code> to mark the element as executable
	 */
	public static final String PATR_VIEW_ATTR_HIDDEN              = "hidden";
	/**
	 * {@link FileAttribute} name for the executable flag.<br>
	 * {@link FileAttribute#value()} must be of the {@link Boolean} type<br>
	 * <code>true</code> to mark the element as executable
	 */
	public static final String PATR_VIEW_ATTR_EXECUTABLE          = "executable";
	/**
	 * {@link FileAttribute} name for the read only flag.<br>
	 * {@link FileAttribute#value()} must be of the {@link Boolean} type<br>
	 * <code>true</code> to mark the element as executable
	 */
	public static final String PATR_VIEW_ATTR_READ_ONLY           = "read_only";
	
	/**
	 * the attribute key for the block size of each block.<br>
	 * 
	 * the value must to be of the type {@link Integer}
	 * <p>
	 * if a file system is specified and format is set to <code>false</code> (default value), this value must be the block size value saved by the file system.
	 * 
	 * @see #newFileSystem(URI, Map)
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE  = "block-size";
	/**
	 * the attribute key for the block count.<br>
	 * 
	 * the value must be of the tyPFSFileSystemProviderImplpe {@link Long}
	 * <p>
	 * if a block manager is specified and format is set to <code>false</code> (default value), this value must be the block count value saved from the block manager.
	 * 
	 * @see #newFileSystem(URI, Map)
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT = "block-count";
	/**
	 * the attribute key to set the underling file system.
	 * <p>
	 * the value must implement the interface {@link PatrFileSystem}
	 * <p>
	 * if no value is set a {@link PatrFileSysImpl} will be created.<br>
	 * if no value the generated file system will use virtual block manager.<br>
	 * This automatic generated block manager and thus the file system will not be saved.
	 * 
	 * @see #newFileSystem(URI, Map)
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_FILE_SYS    = "file-sys";
	/**
	 * the attribute key to specify if the file system should be formatted.
	 * <p>
	 * if no value is set, but {@link #NEW_FILE_SYS_ENV_ATTR_FILE_SYS} is set, the file system will be interpreted as already formatted.<br>
	 * if a value is set, but {@link #NEW_FILE_SYS_ENV_ATTR_FILE_SYS} is not set, the value will be ignored.
	 * <p>
	 * the value must be of type {@link Boolean}.<br>
	 * if the {@link Boolean#booleanValue()} is <code>true</code>, the block manager will be formatted. If the {@link Boolean#booleanValue()} is <code>false</code> the block manager will not be.
	 * formatted
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_DO_FORMATT  = "do-formatt";
	
	/**
	 * the URI scheme of the patr-file-system
	 */
	public static final String URI_SHEME = "patrFileSys";
	
}
