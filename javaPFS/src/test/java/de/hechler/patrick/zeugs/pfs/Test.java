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
package de.hechler.patrick.zeugs.pfs;

import de.hechler.patrick.zeugs.check.objects.BigCheckResult;
import de.hechler.patrick.zeugs.check.objects.BigChecker;
import de.hechler.patrick.zeugs.check.objects.Checker;
import de.hechler.patrick.zeugs.check.objects.LogHandler;

public class Test {

	static {
		System.setProperty(Checker.LOG_LEVEL_PROP, "ALL");
		LogHandler.LOG.info(() -> "checker module: " + Checker.class.getModule() + "\npfs module:     " + FSChecker.class.getModule()+ "\npfs module2:    " + FSProvider.class.getModule());
		Checker.registerExporter(pkg -> Test.class.getModule().addOpens(pkg, Checker.class.getModule()));
	}
	
	public void testname() throws Exception {
		main(new String[0]);
	}
	
	public static void main(String[] args) {
		BigCheckResult result = BigChecker.tryCheckAll(true, Test.class.getPackage(), Test.class.getClassLoader());
		result.detailedPrint(System.err);
		if (result.wentUnexpected()) throw new AssertionError(result);
		System.err.println("finish: successful");
	}
	
}
