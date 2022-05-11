package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingConsumer <T extends Throwable, C> {
	
	void accept(C c) throws T;
	
}

