package de.hechler.patrick.pfs.objects.ba;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.io.IOException;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass
public class ByteArrayArrayBlockAccessorChecker extends BlockAccessorChecker {
	
	private static final int START_SIZE = 128;
	
	@Start
	protected void setup() {
		newByteArrayArrayBlockAccessor(START_SIZE, START_SIZE);
	}
	
	@Check
	protected void checBlockkSize() throws IOException {
		assertEquals(ba.blockSize(), START_SIZE);
		assertEquals(ba.blockSize(), START_SIZE);
		assertEquals(ba.blockSize(), START_SIZE);
		newByteArrayArrayBlockAccessor(1024, 16);
		assertEquals(ba.blockSize(), 16);
		assertEquals(ba.blockSize(), 16);
		assertEquals(ba.blockSize(), 16);
		newByteArrayArrayBlockAccessor(16, 1024);
		assertEquals(ba.blockSize(), 1024);
		newByteArrayArrayBlockAccessor(1, 1024);
		assertEquals(ba.blockSize(), 1024);
		newByteArrayArrayBlockAccessor(1024, 1);
		assertEquals(ba.blockSize(), 1);
	}
	
	protected void newByteArrayArrayBlockAccessor(int blockCount, int blockSize) {
		ba = new ByteArrayArrayBlockAccessor(blockCount, blockSize);
	}
	
}
