package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingBiConsumer <T extends Throwable, C, B> {
	
	void accept(C c, B b) throws T;
	
}

