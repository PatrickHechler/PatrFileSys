package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingLongConsumer <T extends Throwable> {
	
	void accept(long c) throws T;
	
}

