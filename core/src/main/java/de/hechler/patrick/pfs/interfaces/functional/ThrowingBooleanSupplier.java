package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingBooleanSupplier <T extends Throwable> {
	
	boolean supply() throws T;
	
}

