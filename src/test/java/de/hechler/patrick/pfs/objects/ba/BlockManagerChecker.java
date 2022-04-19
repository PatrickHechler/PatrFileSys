package de.hechler.patrick.pfs.objects.ba;

import static de.hechler.patrick.zeugs.check.Assert.assertArrayEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertNotArrayEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertSame;

import java.io.IOException;
import java.util.Random;

import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass
public class BlockManagerChecker {
	
	static final int START_SIZE = 1024;
	BlockManager     bm;
	
	@Start
	private void start() {
		bm = new BlockManagerImpl(new ByteArrayArrayBlockAccessor(START_SIZE, START_SIZE));
	}
	
	@Check
	private void checkGetSetUnget() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		assertSame(bytes, bm.getBlock(0L));
		Random rnd = new Random();
		rnd.nextBytes(bytes);
		bm.setBlock(0L);
		bm.ungetBlock(0L);
		assertArrayEquals(bytes, bm.getBlock(0L));
		assertNotArrayEquals(bytes, bm.getBlock(1L));
		bm.ungetBlock(0L);
		bytes = bm.getBlock(1L);
		rnd.nextBytes(bytes);
		bm.ungetBlock(1L);
		bm.setBlock(1L);
		assertArrayEquals(bytes, bm.getBlock(1L));
		assertNotArrayEquals(bytes, bm.getBlock(0L));
	}
	
	@Check
	private void checkGetSet() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		new Random().nextBytes(bytes);
		bm.setBlock(0L);
		byte[] secondBytes = bm.getBlock(0L);
		bm.setBlock(0L);
		assertArrayEquals(bytes, secondBytes);
		secondBytes = bm.getBlock(1L);
		assertNotArrayEquals(bytes, secondBytes);
		bm.setBlock(1L);
	}
	
	@Check
	private void checkGetUnget() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		new Random().nextBytes(bytes);
		bm.ungetBlock(0L);
		byte[] secondBytes = bm.getBlock(0L);
		bm.ungetBlock(0L);
		assertNotArrayEquals(bytes, secondBytes);
		secondBytes = bm.getBlock(1L);
		bm.getBlock(1L);
		bm.ungetBlock(1L);
		assertNotArrayEquals(bytes, secondBytes);
		bytes = bm.getBlock(1L);
		assertSame(bytes, secondBytes);
		bm.ungetBlock(1L);
		bm.ungetBlock(1L);
	}
	
	@Check
	private void checkDiscardAll2() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		assertSame(bytes, bm.getBlock(0L));
		Random rnd = new Random();
		assertSame(bytes, bm.getBlock(0L));
		rnd.nextBytes(bytes);
		byte[] secondBytes = bm.getBlock(1L);
		assertSame(secondBytes, bm.getBlock(1L));
		assertSame(secondBytes, bm.getBlock(1L));
		assertSame(bytes, bm.getBlock(0L));
		assertSame(bytes, bm.getBlock(0L));
		rnd.nextBytes(secondBytes);
		bm.discardAll();
		byte[] bytes2 = bm.getBlock(0L);
		byte[] secondBytes2 = bm.getBlock(1L);
		assertNotArrayEquals(bytes, bytes2);
		assertNotArrayEquals(secondBytes, secondBytes2);
	}
	
	@Check
	private void checkSaveAll2() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		assertSame(bytes, bm.getBlock(0L));
		Random rnd = new Random();
		assertSame(bytes, bm.getBlock(0L));
		rnd.nextBytes(bytes);
		byte[] secondBytes = bm.getBlock(1L);
		assertSame(secondBytes, bm.getBlock(1L));
		assertSame(secondBytes, bm.getBlock(1L));
		assertSame(bytes, bm.getBlock(0L));
		assertSame(bytes, bm.getBlock(0L));
		rnd.nextBytes(secondBytes);
		bm.saveAll();
		byte[] bytes2 = bm.getBlock(0L);
		byte[] secondBytes2 = bm.getBlock(1L);
		assertArrayEquals(bytes, bytes2);
		assertArrayEquals(secondBytes, secondBytes2);
	}
	
	@End
	private void end() throws IOException {
		if (bm != null) {
			bm.close();
		}
		bm = null;
	}
	
	@Check
	private void checkReaLoadSave() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		new Random().nextBytes(bytes);
		bm.setBlock(0L);
		byte[] secondBytes = bm.getBlock(0L);
		bm.setBlock(0L);
		assertArrayEquals(bytes, secondBytes);
		secondBytes = bm.getBlock(1L);
		assertNotArrayEquals(bytes, secondBytes);
		bm.setBlock(1L);
	}
	
	@Check
	private void checkReadLoadDiscard() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		new Random().nextBytes(bytes);
		bm.ungetBlock(0L);
		byte[] secondBytes = bm.getBlock(0L);
		bm.ungetBlock(0L);
		assertNotArrayEquals(bytes, secondBytes);
		secondBytes = bm.getBlock(1L);
		assertNotArrayEquals(bytes, secondBytes);
		bm.ungetBlock(1L);
	}
	
	@Check
	private void checkDiscardAll() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		Random rnd = new Random();
		rnd.nextBytes(bytes);
		byte[] secondBytes = bm.getBlock(1L);
		rnd.nextBytes(secondBytes);
		bm.discardAll();
		byte[] bytes2 = bm.getBlock(0L);
		byte[] secondBytes2 = bm.getBlock(1L);
		assertNotArrayEquals(bytes, bytes2);
		assertNotArrayEquals(secondBytes, secondBytes2);
	}
	
	@Check
	private void checkSaveAll() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		Random rnd = new Random();
		rnd.nextBytes(bytes);
		byte[] secondBytes = bm.getBlock(1L);
		rnd.nextBytes(secondBytes);
		bm.saveAll();
		byte[] bytes2 = bm.getBlock(0L);
		byte[] secondBytes2 = bm.getBlock(1L);
		assertArrayEquals(bytes, bytes2);
		assertArrayEquals(secondBytes, secondBytes2);
	}
	
}
