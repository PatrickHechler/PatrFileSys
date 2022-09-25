package de.hechler.patrick.pfs.fs.impl;

import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.fs.PFS;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
public class NativePatrFileSysChecker {
	
	@Check
	public void check() throws PatrFileSysException {
		System.out.println("version: " + Runtime.version());
		PFS pfs = NativePatrFileSys.create("testout/name.pfs", 1024, 4096);
		System.out.println("created the pfs");
		pfs.close();
		System.out.println("closed the pfs");
		pfs = NativePatrFileSys.load("testout/name.pfs");
		System.out.println("loaded the pfs");
		pfs.close();
		System.out.println("closed the pfs (again)");
	}
	
}
