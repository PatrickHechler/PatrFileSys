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
package de.hechler.patrick.zeugs.pfs.interfaces;

import de.hechler.patrick.zeugs.pfs.FSProvider;

/**
 * this interface is used to mark File System Open Options.
 * <p>
 * since it is very implementation specific, how to open/create a file system
 * this interface provides no common functions.<br>
 * each {@link FSProvider} should document, which implementations of this
 * interface are supported
 * 
 * @author pat
 * 
 * @see FSProvider#loadFS(FSOptions)
 */
public interface FSOptions {
	
}
