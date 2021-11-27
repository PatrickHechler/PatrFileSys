package de.hechler.patrick.ownfs;

import de.hechler.patrick.ownfs.objects.BlockAccessorByteArrayArrayImplTest;
import de.hechler.patrick.ownfs.objects.PatrFileSysChecker;
import de.hechler.patrick.zeugs.check.BigCheckResult;
import de.hechler.patrick.zeugs.check.Checker;

public class Test {
	
	public static void main(String[] args) {
		BigCheckResult cr = Checker.checkAll(true, PatrFileSysChecker.class, BlockAccessorByteArrayArrayImplTest.class);
		cr.detailedPrint();
		if (cr.wentUnexpected()) {
			throw new Error("unexpected BigCheckResult: " + cr);
		}
	}
	
}
