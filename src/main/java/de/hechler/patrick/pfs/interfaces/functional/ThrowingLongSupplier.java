package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingLongSupplier <T extends Throwable> {
	
	long supply() throws T;
	
}

