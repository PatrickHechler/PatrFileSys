package de.hechler.patrick.pfs.objects.ba;

import static de.hechler.patrick.zeugs.check.Assert.assertArrayEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertNotArrayEquals;

import java.io.IOException;
import java.util.Random;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.objects.Checker;

public class BlockAccessorChecker extends Checker {
	
	protected BlockAccessor ba;
	
	@End
	private void end() {
		if (ba != null) {
			ba.close();
		}
		ba = null;
	}
	
	@Check
	private void checkReaLoadSave() throws IOException {
		byte[] bytes = ba.loadBlock(0L);
		new Random().nextBytes(bytes);
		ba.saveBlock(bytes, 0L);
		byte[] secondBytes = ba.loadBlock(0L);
		ba.saveBlock(secondBytes, 0L);
		assertArrayEquals(bytes, secondBytes);
		secondBytes = ba.loadBlock(1L);
		assertNotArrayEquals(bytes, secondBytes);
		ba.saveBlock(secondBytes, 1L);
	}
	
	@Check
	private void checkReaLoadDiscard() throws IOException {
		byte[] bytes = ba.loadBlock(0L);
		new Random().nextBytes(bytes);
		ba.discardBlock(0L);
		byte[] secondBytes = ba.loadBlock(0L);
		ba.discardBlock(0L);
		assertNotArrayEquals(bytes, secondBytes);
		secondBytes = ba.loadBlock(1L);
		assertNotArrayEquals(bytes, secondBytes);
		ba.discardBlock(1L);
	}
	
}
