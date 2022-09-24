package de.hechler.patrick.interfaces;

import java.util.Iterator;

public interface ThrowableIter <T extends Throwable, E> extends Iterator <E> {
	
	@Override
	default E next() {
		try {
			return nextElement();
		} catch (Error e) {
			throw e;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} catch (Throwable t) {
			throw new Error(t);
		}
	}
	
	E nextElement() throws T;
	
	@Override
	default boolean hasNext() {
		try {
			return hasNextElement();
		} catch (Error e) {
			throw e;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} catch (Throwable t) {
			throw new Error(t);
		}
	}
	
	boolean hasNextElement() throws T;
	
}
