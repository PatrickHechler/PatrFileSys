package de.hechler.patrick.pfs.interfaces.functional;

@FunctionalInterface
public interface ThrowingBooleanFunction <P, T extends Throwable> {
	
	boolean calc(P p) throws T;
	
}

