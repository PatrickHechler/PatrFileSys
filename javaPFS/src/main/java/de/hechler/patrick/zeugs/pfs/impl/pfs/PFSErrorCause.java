// This file is part of the Patr File System Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.zeugs.pfs.impl.pfs;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.NoSuchElementException;
import java.util.function.Function;

import de.hechler.patrick.zeugs.pfs.interfaces.IntObjectFunction;

@SuppressWarnings("javadoc")
public enum PFSErrorCause {
	
	GET_FILE_LEN("get file length"), GET_PIPE_LEN("get pipe length"),
	
	OPEN_STREAM(path -> path != null ? "open stream for element '" + path + "'" : "open stream"),
	
	WRITE(info -> "write " + info + " bytes"), READ(info -> "read " + info + " bytes"),
	
	SET_POS("set position of a stream"), GET_POS("get position of a stream"), ADD_POS("add a value to the position of a stream"),
	SEEK_EOF("set the position of a stream to EOF"),
	
	CLOSE_STREAM("close stream handle"),
	
	LOAD_PFS_AND_FORMAT(info -> "load and format PFS (" + info + ")"), LOAD_PFS(info -> "load PFS (" + info + ")"),
	
	GET_PARENT("get parent", (msg, errno) -> {
		if (errno == ErrConsts.ROOT_FOLDER) {
			throw new IllegalStateException(msg);
		}
		return ErrConsts.FUNC.apply(msg, errno);
	}),
	
	GET_FLAGS(path -> path != null ? " get flags for '" + path + "'" : "get flags"), MODIFY_FLAGS("modify flags"),
	
	GET_LAST_MODIFY_TIME("get the last modification time"), SET_LAST_MODIFY_TIME("set the last modification time"), GET_CREATE_TIME("get createion time"),
	SET_CREATE_TIME("set createion time"),
	
	GET_NAME("get name"), SET_NAME("set name"), SET_PARENT("set parent folder"), MOVE_ELEMENT("move element"),
	
	DELETE_ELEMENT("delete element"),
	
	CLOSE_ELEMENT("close element handle"),
	
	SAME(info -> info == null ? "check same elements" : ("check same elements : unknown result <" + info + ">")),
	
	GET_BLOCK_COUNT(info -> info == null ? "get block count" : "get block count of '" + info + "'"),
	GET_BLOCK_SIZE(info -> info == null ? "get block size" : "get block size of '" + info + "'"),
	GET_READ_ONLY(info -> info == null ? "get read-only mode" : "get read-only mode of '" + info + "'"),
	GET_MOUNT_TYPPE(info -> info == null ? "get mount point type" : "get mount point type of '" + info + "'"),
	
	GET_UUID(info -> info == null ? "get UUID" : "get UUID of '" + info + "'"),
	
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
	
	private PFSErrorCause(String str, IntObjectFunction<String, IOException> func) {
		this.str  = info -> str;
		this.func = func;
	}
	
}

class ErrConsts {
	
	private ErrConsts() {}
	
	// GENERATED-CODE-START
	// this code-block is automatic generated, do not modify
	/** indicates no error */
	static final int NONE                          = 0;
	/** indicates an unknown error */
	static final int UNKNOWN_ERROR                 = 1;
	/** indicates that there are no more params */
	static final int NO_MORE_ELEMENTS              = 2;
	/** indicates that the element has not the wanted/allowed type */
	static final int ELEMENT_WRONG_TYPE            = 3;
	/** indicates that the element does not exist */
	static final int ELEMENT_NOT_EXIST             = 4;
	/** indicates that the element already exists */
	static final int ELEMENT_ALREADY_EXIST         = 5;
	/** indicates that there is not enough space on the device */
	static final int OUT_OF_SPACE                  = 6;
	/** indicates an IO error */
	static final int IO_ERR                        = 7;
	/** indicates an illegal argument */
	static final int ILLEGAL_ARG                   = 8;
	/** indicates that some data is invalid */
	static final int ILLEGAL_DATA                  = 9;
	/** indicates that the system is out of memory */
	static final int OUT_OF_MEMORY                 = 10;
	/** indicates that the root folder does not support this operation */
	static final int ROOT_FOLDER                   = 11;
	/** indicates that the parent can't be made to it's own child */
	static final int PARENT_IS_CHILD               = 12;
	/** indicates the element is still used somewhere else */
	static final int ELEMENT_USED                  = 13;
	/** indicates that some value was outside of the allowed range */
	static final int OUT_OF_RANGE                  = 14;
	/** indicates that the operation failed, because only empty folders can be deleted */
	static final int FOLDER_NOT_EMPTY              = 15;
	/** indicates that the operation failed, because the element was deleted */
	static final int ELEMENT_DELETED               = 16;
	/** indicates that the operation failed, because the file system or the element is read only */
	static final int READ_ONLY                     = 17;
	/** indicates that the operation failed, because the different file systems should be used (for example move entry) */
	static final int DIFFERENT_FILE_SYSTEMS        = 18;
	
	// here is the end of the automatic generated code-block
	// GENERATED-CODE-END
	
	static final IntObjectFunction<String, IOException> FUNC = (msg, errno) -> {
		switch (errno) {
		case NONE:
			throw new AssertionError("no error: " + msg);
		case UNKNOWN_ERROR:
			return new IOException("unknown error: " + msg);
		case NO_MORE_ELEMENTS:
			throw new NoSuchElementException("no more elements: " + msg);
		case ELEMENT_WRONG_TYPE:
			return new IOException("the element has not the expected type: " + msg);
		case ELEMENT_NOT_EXIST:
			return new NoSuchFileException("there is no such file: " + msg);
		case ELEMENT_ALREADY_EXIST:
			return new FileAlreadyExistsException("the file exists already: " + msg);
		case OUT_OF_SPACE:
			return new IOException("out of space: " + msg);
		case IO_ERR:
			return new IOException("IO error: " + msg);
		case ILLEGAL_ARG:
			throw new IllegalArgumentException("illegal argument: " + msg);
		case ILLEGAL_DATA:
			return new StreamCorruptedException("invalid magic: " + msg);
		case OUT_OF_MEMORY:
			throw new OutOfMemoryError("out of memory: " + msg);
		case ROOT_FOLDER:
			return new IOException("root folder restrictions: " + msg);
		case PARENT_IS_CHILD:
			return new IOException("I won't move a folder to a child of it self: " + msg);
		case ELEMENT_USED:
			return new IOException("The element is curently used somewhere different: " + msg);
		case OUT_OF_RANGE:
			throw new IndexOutOfBoundsException("a value was outside of the allowed range: " + msg);
		case FOLDER_NOT_EMPTY:
			return new DirectoryNotEmptyException("the folder is not empty: " + msg);
		case ELEMENT_DELETED:
			return new NoSuchFileException("the element was deleted: " + msg);
		default:
			throw new AssertionError("unknown PFS errno (" + errno + "): " + msg);
		}
	};
	
}
