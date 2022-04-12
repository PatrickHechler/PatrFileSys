package de.hechler.patrick.pfs.utils;


public class ConvertNumByteArr {
	
	public static long byteArrToLong(byte[] bytes, int off) {
		long result = bytes[off] & 0xFF;
		result |= (bytes[off + 1] & 0xFF) << 8;
		result |= (bytes[off + 2] & 0xFF) << 16;
		result |= (bytes[off + 3] & 0xFF) << 24;
		result |= (bytes[off + 4] & 0xFF) << 32;
		result |= (bytes[off + 5] & 0xFF) << 40;
		result |= (bytes[off + 6] & 0xFF) << 48;
		result |= (bytes[off + 7] & 0xFF) << 56;
		return result;
	}
	
	public static void longToByteArr(byte[] bytes, int off, long value) {
		bytes[off] = (byte) value;
		bytes[off + 1] = (byte) (value >> 8);
		bytes[off + 2] = (byte) (value >> 16);
		bytes[off + 3] = (byte) (value >> 24);
		bytes[off + 4] = (byte) (value >> 32);
		bytes[off + 5] = (byte) (value >> 40);
		bytes[off + 6] = (byte) (value >> 48);
		bytes[off + 7] = (byte) (value >> 56);
	}
	
	public static int byteArrToInt(byte[] bytes, int off) {
		int result = bytes[off] & 0xFF;
		result |= (bytes[off + 1] & 0xFF) << 8;
		result |= (bytes[off + 2] & 0xFF) << 16;
		result |= (bytes[off + 3] & 0xFF) << 24;
		return result;
	}
	
	public static void intToByteArr(byte[] bytes, int off, int value) {
		bytes[off] = (byte) value;
		bytes[off + 1] = (byte) (value >> 8);
		bytes[off + 2] = (byte) (value >> 16);
		bytes[off + 3] = (byte) (value >> 24);
	}
	
}
