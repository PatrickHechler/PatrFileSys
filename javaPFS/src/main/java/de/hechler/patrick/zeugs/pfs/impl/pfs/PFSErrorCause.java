//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.zeugs.pfs.impl.pfs;

import java.io.IOError;
import java.io.IOException;
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
	
	SET_POS("set position of a stream"), GET_POS("get position of a stream"),
	ADD_POS("add a value to the position of a stream"), SEEK_EOF("set the position of a stream to EOF"),
	
	CLOSE_STREAM("close stream handle"),
	
	LOAD_PFS_AND_FORMAT(info -> "load and format ram PFS (" + info + ")"), LOAD_PFS(info -> "load PFS (" + info + ")"),
	
	GET_PARENT("get parent", (msg, errno) -> {
		if (errno == ErrConsts.ROOT_FOLDER) {
			throw new IllegalStateException(msg);
		}
		return ErrConsts.FUNC.apply(msg, errno);
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
	
	GET_CHILD(name -> "get the child with name '" + name + "'"),
	CREATE_CHILD(name -> "create a child with name '" + name + "'"),
	
	;
	
	public final Function<Object, String>               str;
	public final IntObjectFunction<String, IOException> func;
	
	private PFSErrorCause(String str) {
		this.str = info -> str;
		this.func = ErrConsts.FUNC;
	}
	
	private PFSErrorCause(Function<Object, String> str) {
		this.str = str;
		this.func = ErrConsts.FUNC;
	}
	
	private PFSErrorCause(String str, IntObjectFunction<String, IOException> func) {
		this.str = info -> str;
		this.func = func;
	}
	
}

class ErrConsts {
	
	private ErrConsts() {}
	
	/**
	 * if pfs_errno is not set/no error occurred
	 */
	static final int NONE                  = 0;
	/**
	 * if an operation failed because of an unknown/unspecified error
	 */
	static final int UNKNOWN               = 1;
	/**
	 * if the iterator has no next element
	 */
	static final int NO_MORE_ELEMENTS      = 2;
	/**
	 * if an IO operation failed because the element is not of the correct type (file expected, but folder or reverse)
	 */
	static final int ELEMENT_WRONG_TYPE    = 3;
	/**
	 * if an IO operation failed because the element does not exist
	 */
	static final int ELEMENT_NOT_EXIST     = 4;
	/**
	 * if an IO operation failed because the element already existed
	 */
	static final int ELEMENT_ALREADY_EXIST = 5;
	/**
	 * if an IO operation failed because there was not enough space in the file system
	 */
	static final int OUT_OF_SPACE          = 6;
	/**
	 * if an unspecified IO error occurred
	 */
	static final int IO_ERR                = 7;
	/**
	 * if there was at least one invalid argument
	 */
	static final int ILLEGAL_ARG           = 8;
	/**
	 * if there was an invalid magic value
	 */
	static final int ILLEGAL_MAGIC         = 9;
	/**
	 * if an IO operation failed because there was not enough memory available
	 */
	static final int OUT_OF_MEMORY         = 10;
	/**
	 * if an IO operation failed because the root folder has some restrictions
	 */
	static final int ROOT_FOLDER           = 11;
	/**
	 * if an folder can not be moved because the new child (maybe a deep/indirect child) is a child of the folder
	 */
	static final int PARENT_IS_CHILD       = 12;
	/**
	 * if an element which is opened elsewhere is tried to be deleted
	 */
	static final int ELEMENT_USED          = 13;
	
	static final IntObjectFunction<String, IOException> FUNC = (msg, errno) -> {
		switch (errno) {
		case NONE:
			throw new AssertionError("no error: " + msg);
		case UNKNOWN:
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
		case ILLEGAL_MAGIC:
			throw new IOError(new IOException("invalid magic: " + msg));
		case OUT_OF_MEMORY:
			throw new OutOfMemoryError("out of memory: " + msg);
		case ROOT_FOLDER:
			return new IOException("root folder restrictions: " + msg);
		case PARENT_IS_CHILD:
			return new IOException("I won't move a folder to a child of it self: " + msg);
		case ELEMENT_USED:
			return new IOException("The element is curently used somewhere different: " + msg);
		default:
			throw new InternalError("unknown PFS errno (" + errno + "): " + msg);
		}
	};
	
}
