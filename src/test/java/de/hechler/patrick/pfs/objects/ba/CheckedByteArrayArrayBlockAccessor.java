package de.hechler.patrick.pfs.objects.ba;

import java.io.IOException;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
public class CheckedByteArrayArrayBlockAccessor extends ByteArrayArrayBlockAccessorChecker {
	
	@Override
	protected void newByteArrayArrayBlockAccessor(int blockCount, int blockSize) {
		ByteArrayArrayBlockAccessor baba = new ByteArrayArrayBlockAccessor(blockCount, blockSize);
		ba = new CheckedBlockAccessor(baba);
	}
	
	@Check
	@Override
	protected void checkSaveAll() throws IOException {
		super.checkSaveAll();
	}
	
}
