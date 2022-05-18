package de.hechler.patrick.pfs.shell;

import de.hechler.patrick.pfs.shell.objects.PFSShell;
import de.hechler.patrick.pfs.shell.utils.ConsoleColors;

public class ShellMain {
	
	public static final String VERSION = "2.4.6";
	public static final String ARCH    = "jvm-11";
	
	private static PFSShell shell;
	
	public static void main(String[] args) {
		boolean execute = setup(args);
		if (execute) {
			shell.run();
		}
	}
	
	private static boolean setup(String[] args) {
		String pvm = null;
		for (int i = 0; i < args.length; i ++ ) {
			switch (args[i]) {
			case "--help":
			case "--?":
				help();
				return false;
			case "--version":
				version();
				return false;
			case "--pvm":
				if ( ++ i >= args.length) {
					exit("not enough arguments!");
				}
				pvm = args[i];
				break;
			default:
				exit("unknown argument: '" + args[i] + "'");
			}
		}
		if (pvm == null) {
			shell = new PFSShell();
		} else {
			shell = new PFSShell(pvm);
		}
		return true;
	}
	
	private static void version() {
		System.out.print("pfs-shell " + VERSION + " (" + ARCH + ")\n");
	}
	
	private static void help() {
		version();
		System.out.print("options:\n"
			+ "    --help                 to print this message and exit\n"
			+ "    --version              to print the version and exit\n"
			+ "    --pvm [PVM_CONNAND]    to set the comand to execute the primitive-virtual-mashine\n"
			+ "");
	}
	
	private static void exit(String msg) {
		System.err.print(ConsoleColors.RED + msg + ConsoleColors.RESET);
		System.exit(1);
	}
	
}
