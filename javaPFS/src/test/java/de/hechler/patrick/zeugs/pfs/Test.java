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
		result.detailedPrint();
		if (result.wentUnexpected()) { throw new AssertionError(result); }
	}
	
}
