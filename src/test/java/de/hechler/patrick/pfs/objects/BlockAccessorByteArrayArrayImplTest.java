package de.hechler.patrick.pfs.objects;

import de.hechler.patrick.zeugs.check.Checker;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass
public class BlockAccessorByteArrayArrayImplTest extends Checker {

	BlockAccessorByteArrayArrayImpl ba;
	
	@Start
	void setUp() throws Exception {
		ba = new BlockAccessorByteArrayArrayImpl(100, 256);  
	}

	@End
	void tearDown() throws Exception {
		ba = null;
	}

	@Check
	void testBlockSize() {

		assertEquals(256, ba.blockSize());

		@SuppressWarnings("resource")
		BlockAccessorByteArrayArrayImpl ba512 = new BlockAccessorByteArrayArrayImpl(1, 512);
		assertEquals(512, ba512.blockSize());
		
		@SuppressWarnings("resource")
		BlockAccessorByteArrayArrayImpl ba32768 = new BlockAccessorByteArrayArrayImpl(1, 32768);
		assertEquals(32768, ba32768.blockSize());

	}

	@Check(disabled = true)
	void testLoadBlock() {
		fail("Not yet implemented");
	}

	@Check(disabled = true)
	void testSaveBlock() {
		fail("Not yet implemented");
	}

	@Check(disabled = true)
	void testUnloadBlock() {
		fail("Not yet implemented");
	}

	@Check(disabled = true)
	void testClose() {
		fail("Not yet implemented");
	}

	@Check(disabled = true)
	void testSaveAll() {
		fail("Not yet implemented");
	}

	@Check(disabled = true)
	void testUnloadAll() {
		fail("Not yet implemented");
	}

}
