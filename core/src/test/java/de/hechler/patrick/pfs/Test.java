package de.hechler.patrick.pfs;

import de.hechler.patrick.zeugs.check.objects.BigCheckResult;
import de.hechler.patrick.zeugs.check.objects.BigChecker;

public class Test {
	
	public void testname() throws Exception {
		main(new String[0]);
	}
	
	public static void main(String[] args) throws InterruptedException {
		Thread.sleep(10000L);
		BigCheckResult cr = BigChecker.tryCheckAll(true, Test.class.getPackage(), Test.class.getClassLoader());
		cr.detailedPrint();
		if (cr.wentUnexpected()) {
			throw new Error("unexpected BigCheckResult: " + cr);
		}
	}
	
}
