package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingIntSupplier <T extends Throwable> {
	
	int supply() throws T;
	
}

