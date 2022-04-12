package de.hechler.patrick.pfs;

import de.hechler.patrick.pfs.objects.ba.ByteArrayArrayBlockAccessorChecker;
import de.hechler.patrick.zeugs.check.BigCheckResult;
import de.hechler.patrick.zeugs.check.Checker;

public class Test {
	
	public void testname() throws Exception {
		main(new String[0]);
	}
	
	public static void main(String[] args) {
		BigCheckResult cr = Checker.checkAll(true, ByteArrayArrayBlockAccessorChecker.class);
		cr.print();
		if (cr.wentUnexpected()) {
			cr.detailedPrintUnexpected(System.err);
			throw new Error("unexpected BigCheckResult: " + cr);
		}
	}
	
}
