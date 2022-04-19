package de.hechler.patrick.pfs.objects.ba;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.io.IOException;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass
public class ByteArrayArrayBlockAccessorChecker extends BlockAccessorChecker {
	
	static final int START_SIZE = 128;
	
	@Start
	private void setup() {
		ba = new ByteArrayArrayBlockAccessor(START_SIZE, START_SIZE);
	}
	
	@Check
	private void checBlockkSize() throws IOException {
		assertEquals(ba.blockSize(), START_SIZE);
		assertEquals(ba.blockSize(), START_SIZE);
		assertEquals(ba.blockSize(), START_SIZE);
		ba = new ByteArrayArrayBlockAccessor(1024, 16);
		assertEquals(ba.blockSize(), 16);
		assertEquals(ba.blockSize(), 16);
		assertEquals(ba.blockSize(), 16);
		ba = new ByteArrayArrayBlockAccessor(16, 1024);
		assertEquals(ba.blockSize(), 1024);
		ba = new ByteArrayArrayBlockAccessor(1, 1024);
		assertEquals(ba.blockSize(), 1024);
		ba = new ByteArrayArrayBlockAccessor(1024, 1);
		assertEquals(ba.blockSize(), 1);
	}
	
}
