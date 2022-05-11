package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingLongBiConsumer <T extends Throwable> {
	
	void accept(long c, long b) throws T;
	
}

