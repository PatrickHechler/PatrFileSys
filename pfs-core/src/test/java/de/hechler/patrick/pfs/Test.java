package de.hechler.patrick.pfs;

import de.hechler.patrick.zeugs.check.objects.BigCheckResult;
import de.hechler.patrick.zeugs.check.objects.BigChecker;
import de.hechler.patrick.zeugs.check.objects.Checker;

public class Test {
	
	public void testname() throws Exception {
		main(new String[0]);
	}
	
	public static void main(String[] args) throws InterruptedException {
		Checker.registerExporter((mymod, pkg, cmod) -> {
			mymod.addExports(pkg, cmod);
			mymod.addOpens(pkg, cmod);
		});
		System.out.println(System.getProperty("java.version"));
		BigCheckResult cr = BigChecker.tryCheckAll(true, Test.class.getPackage(), Test.class.getClassLoader());
		cr.detailedPrint();
		if (cr.wentUnexpected()) {
			throw new Error("unexpected BigCheckResult:\n" + cr);
		}
	}
	
}
