package de.hechler.patrick.zeugs.pfs.impl.pfs;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.NoSuchElementException;
import java.util.function.Function;

import de.hechler.patrick.zeugs.pfs.interfaces.IntObjectFunction;

public enum PFSErrorCause {
	
	GET_FILE_LEN("get file length"), GET_PIPE_LEN("get pipe length"),
	
	OPEN_STREAM(path -> path != null ? "open stream for element '" + path + "'" : "open stream"),
	
	WRITE(info -> "write " + info + " bytes"), READ(info -> "read " + info + " bytes"),
	
	SET_POS("set position of a stream"), GET_POS("get position of a stream"), ADD_POS("add a value to the position of a stream"),
	SEEK_EOF("set the position of a stream to EOF"),
	
	CLOSE_STREAM("close stream handle"),
	
	LOAD_PFS_AND_FORMAT(info -> "load and format ram PFS (" + info + ")"), LOAD_PFS(info -> "load PFS (" + info + ")"),
	
	GET_PARENT("get parent", (msg, errno) -> {
		if (errno == ErrConsts.ROOT_FOLDER) {
			throw new IllegalStateException(msg);
		} else {
			return ErrConsts.FUNC.apply(msg, errno);
		}
	}),
	
	GET_FLAGS(path -> path != null ? " get flags for '" + path + "'" : "get flags"), MODIFY_FLAGS("modify flags"),
	
	GET_LAST_MODIFY_TIME("get the last modification time"), SET_LAST_MODIFY_TIME("set the last modification time"),
	GET_CREATE_TIME("get createion time"), SET_CREATE_TIME("set createion time"),
	
	GET_NAME("get name"), SET_NAME("set name"), SET_PARENT("set parent folder"), MOVE_ELEMENT("move element"),
	
	DELETE_ELEMENT("delete element"),
	
	CLOSE_ELEMENT("close element handle"),
	
	SAME(info -> info == null ? "check same elements" : ("check same elements : unknown result <" + info + ">")),
	
	GET_BLOCK_COUNT("get block count"), GET_BLOCK_SIZE("get block size"),
	
	GET_ELEMENT(path -> "get the element '" + path + "'"),
	
	SET_CWD("set the working directory"),
	
	CLOSE_PFS("close the file system"),
	
	ITER_NEXT("get the next element of the folder iterator"),
	
	CLOSE_ITER("close the folder iterator"),
	
	GET_CHILD_COUNT("get the child count of a folder"),
	
	GET_CHILD(name -> "get the child with name '" + name + "'"), CREATE_CHILD(name -> "create a child with name '" + name + "'"),
	
	;
	
	public final Function<Object, String>               str;
	public final IntObjectFunction<String, IOException> func;
	
	private PFSErrorCause(String str) {
		this.str  = info -> str;
		this.func = ErrConsts.FUNC;
	}
	
	private PFSErrorCause(Function<Object, String> str) {
		this.str  = str;
		this.func = ErrConsts.FUNC;
	}
	
	private PFSErrorCause(Function<Object, String> str, IntObjectFunction<String, IOException> func) {
		this.str  = str;
		this.func = func;
	}
	
	private PFSErrorCause(String str, IntObjectFunction<String, IOException> func) {
		this.str  = info -> str;
		this.func = func;
	}
	
}

class ErrConsts {
	
	private ErrConsts() {}
	
	static final int MOV_TO_CHILD           = 12;
	static final int ROOT_FOLDER            = 11;
	static final int OUT_OF_MEM             = 10;
	static final int INVALID_MAGIC          = 9;
	static final int ILLEGA_ARG             = 8;
	static final int UNKNOWN_IO_ERROR       = 7;
	static final int OUT_OF_SPACE           = 6;
	static final int ELEMENT_ALREADY_EXISTS = 5;
	static final int NO_SUCH_ELEMENT        = 4;
	static final int WRONG_TYPE             = 3;
	static final int NO_MORE_ELEMENTS       = 2;
	static final int UNKNOWN                = 1;
	static final int NONE                   = 0;
	
	static final IntObjectFunction<String, IOException> FUNC = (msg, errno) -> {
		switch (errno) {
		case NONE: /* if pfs_errno is not set/no error occurred */
			throw new AssertionError("no error: " + msg);
		case UNKNOWN: /* if an operation failed because of an unknown/unspecified error */
			return new IOException("unknown error: " + msg);
		case NO_MORE_ELEMENTS: /* if the iterator has no next element */
			throw new NoSuchElementException("no more elements: " + msg);
		case WRONG_TYPE: /*
							 * if an IO operation failed because the element is not of the correct type
							 * (file expected, but folder or reverse)
							 */
			return new IOException("the element has not the expected type: " + msg);
		case NO_SUCH_ELEMENT: /* if an IO operation failed because the element does not exist */
			return new NoSuchFileException("there is no such file: " + msg);
		case ELEMENT_ALREADY_EXISTS: /* if an IO operation failed because the element already existed */
			return new FileAlreadyExistsException("the file exists already: " + msg);
		case OUT_OF_SPACE: /*
							 * if an IO operation failed because there was not enough space in the file
							 * system
							 */
			return new IOException("out of space: " + msg);
		case UNKNOWN_IO_ERROR: /* if an unspecified IO error occurred */
			return new IOException("IO error: " + msg);
		case ILLEGA_ARG: /* if there was at least one invalid argument */
			throw new IllegalArgumentException("illegal argument: " + msg);
		case INVALID_MAGIC: /* if there was an invalid magic value */
			throw new IOError(new IOException("invalid magic: " + msg));
		case OUT_OF_MEM: /* if an IO operation failed because there was not enough memory available */
			throw new OutOfMemoryError("out of memory: " + msg);
		case ROOT_FOLDER: /* if an IO operation failed because the root folder has some restrictions */
			return new IOException("root folder restrictions: " + msg);
		case MOV_TO_CHILD: /*
							 * if an folder can not be moved because the new child (maybe a deep/indirect
							 * child) is a child of the folder
							 */
			return new IOException("I won't move a folder to a child of it self: " + msg);
		default:
			throw new InternalError("unknown PFS errno (" + errno + "): " + msg);
		}
	};
	
}
