package de.hechler.patrick.pfs.objects.ba;

import de.hechler.patrick.zeugs.check.Checker;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.Start;

public class ByteArrayArrayBlockAccessorChecker extends Checker {
	
	private static final int    START_SIZE = 128;
	ByteArrayArrayBlockAccessor ba;
	
	@Start
	private void setup() {
		ba = new ByteArrayArrayBlockAccessor(START_SIZE, START_SIZE);
	}
	
	@End
	private void end() {
		ba = null;
	}
	
	@Check
	private void checkSize() {
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
