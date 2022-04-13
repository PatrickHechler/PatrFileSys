package de.hechler.patrick.pfs.interfaces.functional;
@FunctionalInterface
	public interface ThrowingRunnable <T extends Throwable> {
		
		void execute() throws T;
		
	}
	
	