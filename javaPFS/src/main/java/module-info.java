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
import de.hechler.patrick.zeugs.pfs.FSProvider;

/**
 * this module provides the {@link FSProvider} class and two implementations for
 * it.
 * <ul>
 * <li>the {@link FSProvider}, which wraps a nio File System</li>
 * <li>the {@link FSProvider}, which uses the linux only native api for the
 * PatrFileSystem</li>
 * </ul>
 * 
 * @author pat
 *
 * @provides FSProvider with a wrapper impl around the linux only native PatrFS
 *           impl and a wrapper around a nio File System
 * 			
 * @uses FSProvider as part of the project
 */
module de.hechler.patrick.zeugs.pfs {
	
	requires java.logging;
	
	exports de.hechler.patrick.zeugs.pfs;
	exports de.hechler.patrick.zeugs.pfs.interfaces;
	exports de.hechler.patrick.zeugs.pfs.misc;
	exports de.hechler.patrick.zeugs.pfs.opts;
	
	exports de.hechler.patrick.zeugs.pfs.impl.java to de.hechler.patrick.zeugs.check;
	exports de.hechler.patrick.zeugs.pfs.impl.pfs to de.hechler.patrick.zeugs.check;
	
	opens de.hechler.patrick.zeugs.pfs to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.interfaces to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.misc to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.opts to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.impl.pfs to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.impl.java to de.hechler.patrick.zeugs.check;
	
	uses FSProvider;
	// my implementations of the FSProvider are done internally
	
}
