package de.hechler.patrick.pfs.objects.ba;

import java.io.IOException;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
public class CheckedFileBlockAccessorChecker extends FileBlockAccessorChecker {
	
	@Override
	protected void newFileBlockAccessor(int blockSize) {
		FileBlockAccessor fba = new FileBlockAccessor(blockSize, raf);
		ba = new CheckedBlockAccessor(fba);
	}
	
	@Check
	@Override
	protected void checkSaveAll() throws IOException {
		super.checkSaveAll();
	}
	
}
