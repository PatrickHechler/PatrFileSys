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
package de.hechler.patrick.zeugs.pfs.opts;

import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;

/**
 * the {@link PatrRamFSOpts} provides options to create a new patr file system
 * which will be stored in the computers ram. after {@link FS#close()} is called
 * of the ram file system all stored data will be lost
 * 
 * @param blockCount the number of blocks, which can be used by the file system
 * @param blockSize  the size of the blocks, which can be used by the file
 *                   system
 * 
 * @author pat
 */
public record PatrRamFSOpts(long blockCount, int blockSize) implements FSOptions {
	
}
