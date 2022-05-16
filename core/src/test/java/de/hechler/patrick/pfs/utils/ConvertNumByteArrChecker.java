package de.hechler.patrick.pfs.utils;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.util.Random;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
public class ConvertNumByteArrChecker {
	
	@Check
	private void longNumCheck() {
		Random rnd = new Random();
		byte[] bytes = new byte[8];
		for (int i = 0; i < 1 << 10; i ++ ) {
			long orig = rnd.nextLong();
			ConvertNumByteArr.longToByteArr(bytes, 0, orig);
			long fromBytes = ConvertNumByteArr.byteArrToLong(bytes, 0);
			assertEquals(orig, fromBytes);
		}
	}
	
	@Check
	private void intNumCheck() {
		Random rnd = new Random();
		byte[] bytes = new byte[4];
		for (int i = 0; i < 1 << 10; i ++ ) {
			int orig = rnd.nextInt();
			ConvertNumByteArr.intToByteArr(bytes, 0, orig);
			int fromBytes = ConvertNumByteArr.byteArrToInt(bytes, 0);
			assertEquals(orig, fromBytes);
		}
	}
	
	@Check
	private void longNumWithOffCheck() {
		Random rnd = new Random();
		byte[] bytes = new byte[100];
		for (int i = 0; i < 1 << 10; i ++ ) {
			long orig = rnd.nextLong();
			int off = rnd.nextInt(100 - 8);
			ConvertNumByteArr.longToByteArr(bytes, off, orig);
			long fromBytes = ConvertNumByteArr.byteArrToLong(bytes, off);
			assertEquals(orig, fromBytes);
		}
	}
	
	@Check
	private void intNumWithOffCheck() {
		Random rnd = new Random();
		byte[] bytes = new byte[100];
		for (int i = 0; i < 1 << 10; i ++ ) {
			int orig = rnd.nextInt();
			int off = rnd.nextInt(100 - 4);
			ConvertNumByteArr.intToByteArr(bytes, off, orig);
			int fromBytes = ConvertNumByteArr.byteArrToInt(bytes, off);
			assertEquals(orig, fromBytes);
		}
	}
	
}
