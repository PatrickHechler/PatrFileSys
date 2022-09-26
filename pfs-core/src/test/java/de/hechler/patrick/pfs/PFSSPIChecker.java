package de.hechler.patrick.pfs;

import static de.hechler.patrick.zeugs.check.Assert.assertNotNull;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
public class PFSSPIChecker {
	
	@Check
	private void check() {
		assertNotNull(PFSProvider.defaultProvider());
	}
	
}
