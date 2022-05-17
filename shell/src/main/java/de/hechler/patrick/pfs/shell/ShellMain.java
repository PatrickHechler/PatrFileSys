package de.hechler.patrick.pfs.shell;

import de.hechler.patrick.pfs.shell.objects.PFSShell;

public class ShellMain {
	
	public static final String VERSION = "1.0.0-SNAPSHOT";
	public static final String ARCH    = "java-virtual-mashine";
	
	private static PFSShell shell;
	
	public static void main(String[] args) {
		setup(args);
		shell.run();
	}
	
	private static void setup(String[] args) {
		String pvm = null;
		for (int i = 0; i < args.length; i ++ ) {
			switch (args[i]) {
			case "--help":
			case "--?":
				help();
				break;
			case "--pvm":
				if ( ++ i >= args.length) {
					exit(args, i, "not enough arguments!");
				}
				pvm = args[i];
				break;
			default:
				exit(args, i, "unknown argument!");
			}
		}
	}
	
	private static void help() {
		// TODO Auto-generated method stub
		
	}
	
	private static void exit(String[] args, int index, String string) {
		// TODO Auto-generated method stub
		
	}
	
}
