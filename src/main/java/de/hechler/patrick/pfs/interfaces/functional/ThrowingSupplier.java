package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingSupplier <T extends Throwable, R> {
	
	R supply() throws T;
	
}

