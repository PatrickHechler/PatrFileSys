package de.hechler.patrick.pfs.interfaces;


public interface ThrowingIterator <S, T extends Throwable> {
	
	boolean hasNext() throws T;
	
	S next() throws T;
	
}
