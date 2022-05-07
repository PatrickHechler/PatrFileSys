package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingConsumer <T extends Throwable, C> {
	
	void consumer(C c) throws T;
	
}

