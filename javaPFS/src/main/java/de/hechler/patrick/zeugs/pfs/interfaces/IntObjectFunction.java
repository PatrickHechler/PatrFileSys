package de.hechler.patrick.zeugs.pfs.interfaces;


@FunctionalInterface
public interface IntObjectFunction<P, R> {
	
	R apply(P objVal, int intVal);
	
}
