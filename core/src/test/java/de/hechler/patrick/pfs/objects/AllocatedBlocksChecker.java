package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertFalse;
import static de.hechler.patrick.zeugs.check.Assert.assertNotEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertTrue;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
public class AllocatedBlocksChecker {
	
	@Check
	private void checkContains() {
		AllocatedBlocks ab = new AllocatedBlocks(0L, 0L);
		assertFalse(ab.contains(0L));
		ab = new AllocatedBlocks(0L, 1L);
		assertTrue(ab.contains(0L));
		assertFalse(ab.contains(1L));
		ab = new AllocatedBlocks(10L, 1L);
		assertFalse(ab.contains(0L));
		assertFalse(ab.contains(1L));
		assertTrue(ab.contains(10L));
		assertFalse(ab.contains(11L));
		ab = new AllocatedBlocks(10L, 4L);
		assertFalse(ab.contains(0L));
		assertFalse(ab.contains(1L));
		assertTrue(ab.contains(10L));
		assertTrue(ab.contains(11L));
		assertTrue(ab.contains(12L));
		assertTrue(ab.contains(13L));
		assertFalse(ab.contains(14L));
	}
	
	@Check
	private void checkRemove() {
		AllocatedBlocks ab = new AllocatedBlocks(0L, 0L);
		assertEquals(0, ab.remove(0L, 1L).length);
		assertEquals(0, ab.remove(0L, 0L).length);
		assertEquals(0, ab.remove(10L, 0L).length);
		ab = new AllocatedBlocks(0L, 1L);
		assertEquals(0, ab.remove(0L, 1L).length);
		assertEquals(1, ab.remove(0L, 0L).length);
		assertEquals(ab, ab.remove(0L, 0L)[0]);
		assertEquals(1, ab.remove(1L, 0L).length);
		assertEquals(ab, ab.remove(1L, 0L)[0]);
		assertEquals(1, ab.remove(1L, 1L).length);
		assertEquals(ab, ab.remove(1L, 1L)[0]);
		ab = new AllocatedBlocks(10L, 5L);
		assertEquals(1, ab.remove(0L, 10L).length);
		assertEquals(ab, ab.remove(0L, 10L)[0]);
		assertEquals(1, ab.remove(15L, 10L).length);
		assertEquals(ab, ab.remove(15L, 10L)[0]);
		assertEquals(0, ab.remove(ab).length);
		assertEquals(1, ab.remove(10L, 2L).length);
		assertEquals(new AllocatedBlocks(12L, 3L), ab.remove(10L, 2L)[0]);
		assertEquals(2, ab.remove(11L, 2L).length);
		assertEquals(new AllocatedBlocks(10L, 1L), ab.remove(11L, 2L)[0]);
		assertEquals(new AllocatedBlocks(13L, 2L), ab.remove(11L, 2L)[1]);
	}
	
	@Check
	private void checkHasOverlap() {
		AllocatedBlocks ab = new AllocatedBlocks(0L, 0L);
		assertFalse(ab.hasOverlapp(ab));
		ab = new AllocatedBlocks(100L, 5L);
		assertTrue(ab.hasOverlapp(ab));
		assertTrue(ab.hasOverlapp(100L, 10L));
		assertFalse(ab.hasOverlapp(90L, 10L));
		assertFalse(ab.hasOverlapp(100L, 0L));
		assertTrue(ab.hasOverlapp(90L, 20L));
		assertTrue(ab.hasOverlapp(102L, 2L));
		assertTrue(ab.hasOverlapp(102L, 20L));
	}
	
	@Check
	private void checkOverlap() {
		AllocatedBlocks ab = new AllocatedBlocks(0L, 0L);
		assertEquals(null, ab.overlapp(ab));
		ab = new AllocatedBlocks(100L, 5L);
		assertEquals(ab, ab.overlapp(ab));
		assertEquals(ab, ab.overlapp(100L, 10L));
		assertEquals(null, ab.overlapp(90L, 10L));
		assertEquals(null, ab.overlapp(100L, 0L));
		assertEquals(ab, ab.overlapp(90L, 20L));
		assertEquals(new AllocatedBlocks(100L, 2L), ab.overlapp(90L, 12L));
		assertEquals(new AllocatedBlocks(102L, 2L), ab.overlapp(102L, 2L));
		assertEquals(new AllocatedBlocks(102L, 3L), ab.overlapp(102L, 20L));
	}
	
	@Check
	private void checkHashCodeAndEquals() {
		AllocatedBlocks a = new AllocatedBlocks(0L, 0L);
		AllocatedBlocks b = new AllocatedBlocks(0L, 10L);
		AllocatedBlocks c = new AllocatedBlocks(10L, 0L);
		AllocatedBlocks d = new AllocatedBlocks(10L, 10L);
		assertEquals(a, a);
		assertEquals(a, new AllocatedBlocks(a.startBlock, a.count));
		assertEquals(a.hashCode(), new AllocatedBlocks(a.startBlock, a.count).hashCode());
		assertNotEquals(a, b);
		assertNotEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(a.hashCode(), c.hashCode());
		assertNotEquals(a, d);
		assertNotEquals(a.hashCode(), d.hashCode());
		assertNotEquals(b, a);
		assertNotEquals(b.hashCode(), a.hashCode());
		assertEquals(b, b);
		assertEquals(b, new AllocatedBlocks(b.startBlock, b.count));
		assertEquals(b.hashCode(), new AllocatedBlocks(b.startBlock, b.count).hashCode());
		assertNotEquals(b, c);
		assertNotEquals(b.hashCode(), c.hashCode());
		assertNotEquals(b, d);
		assertNotEquals(b.hashCode(), d.hashCode());
		assertNotEquals(c, a);
		assertNotEquals(c.hashCode(), a.hashCode());
		assertNotEquals(c, b);
		assertNotEquals(c.hashCode(), b.hashCode());
		assertEquals(c, c);
		assertEquals(c, new AllocatedBlocks(c.startBlock, c.count));
		assertEquals(c.hashCode(), new AllocatedBlocks(c.startBlock, c.count).hashCode());
		assertNotEquals(c, d);
		assertNotEquals(c.hashCode(), d.hashCode());
		assertNotEquals(d, a);
		assertNotEquals(d.hashCode(), a.hashCode());
		assertNotEquals(d, b);
		assertNotEquals(d.hashCode(), b.hashCode());
		assertNotEquals(d, c);
		assertNotEquals(d.hashCode(), c.hashCode());
		assertEquals(d, d);
		assertEquals(d, new AllocatedBlocks(d.startBlock, d.count));
		assertEquals(d.hashCode(), new AllocatedBlocks(d.startBlock, d.count).hashCode());
	}
	
}
