package de.hechler.patrick.pfs.shell.objects;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingBooleanSupplier;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImpl;
import de.hechler.patrick.pfs.objects.jfs.PFSFileSystemImpl;
import de.hechler.patrick.pfs.objects.jfs.PFSFileSystemProviderImpl;
import de.hechler.patrick.pfs.shell.ShellMain;
import de.hechler.patrick.pfs.shell.utils.ConsoleColors;
import de.hechler.patrick.pfs.utils.ConvertNumByteArr;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public class PFSShell implements Runnable {
	
	private static final PFSFileSystemProviderImpl PROVIDER = new PFSFileSystemProviderImpl();
	
	private static final String CMD_CD     = "cd";
	private static final String CMD_CP     = "cp";
	private static final String CMD_CHANGE = "change";
	private static final String CMD_ECHO   = "echo";
	private static final String CMD_EXIT   = "exit";
	private static final String CMD_PFS    = "pfs";
	private static final String CMD_HELP   = "help";
	
	private static final String GENERAL_HELP_MSG = "patr shell, version " + ShellMain.VERSION + " (" + ShellMain.ARCH + ")\n"
		+ "These shell commands are defined internally.  Type `help' to see this list.\n"
		+ "Type `help name' to find out more about the function `name'.\n"
		+ "\n"
		+ CMD_CD + " [-L|-P [-e]]\n"
		+ CMD_CP + " [OPT... [-T] SRC DEST | OPT... SRC... DIR | OPT... -t DIR SRC...]\n"
		+ CMD_CHANGE + " [PATR_FILE_SYSTEM_PATH]\n"
		+ CMD_ECHO + " ECHO_MSG..."
		+ CMD_EXIT + " [n]\n"
		+ CMD_PFS + " OPTION... [PATR_FILE_SYSTEM_PATH]\n"
		+ CMD_HELP + " [" + CMD_CD + "|" + CMD_CHANGE + "|" + CMD_ECHO + "|" + CMD_EXIT + "|" + CMD_HELP + "]\n"
		+ "";
	
	private static final String CD_HELP_MSG     = "cd: cd [-L|[-P [-e]]] [dir]\n"
		+ "    Change the shell working directory.\n"
		+ "    \n"
		+ "    Options:\n"
		+ "      -L        force symbolic links to be followed: resolve symbolic\n"
		+ "                links in DIR after processing instances of `..'\n"
		+ "      -P        use the physical directory structure without following\n"
		+ "                symbolic links: resolve symbolic links in DIR before\n"
		+ "                processing instances of `..'\n"
		+ "      -e        if the -P option is supplied, and the current working\n"
		+ "                directory cannot be determined successfully, exit with\n"
		+ "                a non-zero status\n"
		+ "    \n"
		+ "    The default is to follow links, as if `-L' were specified.\n"
		+ "    `..' is processed by removing the immediately previous pathname component\n"
		+ "    back to a slash or the beginning of DIR.\n"
		+ "    \n"
		+ "    Exit Status:\n"
		+ "    Returns 0 if the directory is changed, and if the current directory is set\n"
		+ "    successfully when -P is used; non-zero otherwise.\n"
		+ "";
	private static final String SP_HELP_MSG     = "Usage: cp [OPTION]... [-T] SOURCE DEST\n"
		+ "  or:  cp [OPTION]... SOURCE... DIRECTORY\n"
		+ "  or:  cp [OPTION]... -t DIRECTORY SOURCE...\n"
		+ "Copy SOURCE to DEST, or multiple SOURCE(s) to DIRECTORY.\n"
		+ "\n"
		+ "Mandatory arguments to long options are mandatory for short options too.\n"
		// + " -a, --archive same as -dR --preserve=all\n"
		// + " --attributes-only don't copy the file data, just the attributes\n"
		// + " --backup[=CONTROL] make a backup of each existing destination file\n"
		// + " -b like --backup but does not accept an argument\n"
		// + " --copy-contents copy contents of special files when recursive\n"
		// + " -d same as --no-dereference --preserve=links\n"
		+ "  -f, --force                  if an existing destination file cannot be\n"
		+ "                                 opened, remove it and try again (this option\n"
		+ "                                 is ignored when the -n option is also used)\n"
		+ "  -i, --interactive            prompt before overwrite (overrides a previous -n\n"
		+ "                                  option)\n"
		// + " -H follow command-line symbolic links in SOURCE\n"
		+ "  -l, --link                   hard link files instead of copying (only possible\n"
		+ "                                  when using only patr-file-system)\n"
		// + " -L, --dereference always follow symbolic links in SOURCE\n"
		+ "  -n, --no-clobber             do not overwrite an existing file (overrides\n"
		+ "                                 a previous -i option)\n"
		// + " -P, --no-dereference never follow symbolic links in SOURCE\n"
		+ "  -p                           same as --preserve=mode,ownership,timestamps\n"
		// + " --preserve[=ATTR_LIST] preserve the specified attributes (default:\n"
		// + " mode,ownership,timestamps), if possible\n"
		// + " additional attributes: context, links, xattr,\n"
		// + " all\n"
		// + " --no-preserve=ATTR_LIST don't preserve the specified attributes\n"
		// + " --parents use full source file name under DIRECTORY\n"
		+ "  -R, -r, --recursive          copy directories recursively\n"
		// + " --reflink[=WHEN] control clone/CoW copies. See below\n"
		+ "      --remove-destination     remove each existing destination file before\n"
		+ "                                 attempting to open it (contrast with --force)\n"
		// + " --strip-trailing-slashes remove any trailing slashes from each SOURCE\n"
		// + " argument\n"
		// + " -s, --symbolic-link make symbolic links instead of copying\n"
		// + " -S, --suffix=SUFFIX override the usual backup suffix\n"
		+ "  -t, --target-directory=DIRECTORY  copy all SOURCE arguments into DIRECTORY\n"
		+ "  -T, --no-target-directory    treat DEST as a normal file\n"
		+ "  -u, --update                 copy only when the SOURCE file is newer\n"
		+ "                                 than the destination file or when the\n"
		+ "                                 destination file is missing\n"
		+ "  -v, --verbose                explain what is being done\n"
		+ "  -e, --executable             mark target(s) as executable"
		+ "  -h, --hidden                 mark target(s) as hidden"
		+ "";
	private static final String CHANGE_HELP_MSG = "change: change [PATR_FILE_SYSTEM_PATH]\n"
		+ "    changes the shell mode.\n"
		+ "    \n"
		+ "    the shell can be in two modes:\n"
		+ "      'normal'-file-system mode (nfs)\n"
		+ "      patr-file-system mode (pfs)\n"
		+ "    \n"
		+ "    if no argument is set the shell toggles the mode\n"
		+ "      if no pfs-path is set this operation will fail\n"
		+ "    if a argument is set the mode will be set to the pfs-mode, the pfs will be\n"
		+ "    set to the given argument and the pfs-path will be set to the root folder\n"
		+ "    \n"
		+ "    Arguments:\n"
		+ "      PATR_FILE_SYSTEM_PATH  the path to the patr-file-system, which should be\n"
		+ "                             used in the future\n"
		+ "";
	private static final String ECHO_HELP_MSG   = "echo: echo [arg ...]\n"
		+ "    Write arguments to the standard output.\n"
		+ "    \n"
		+ "    Display the ARGs, separated by a single space character and followed by a\n"
		+ "    newline, on the standard output.\n"
		+ "";
	private static final String EXIT_HELP_MSG   = "exit: exit [n]\n"
		+ "    Exit the shell.\n"
		+ "    \n"
		+ "    Exits the shell with a status of N.  If N is omitted, the exit status\n"
		+ "    is that of the last command executed.\n"
		+ "";
	private static final String PFS_HELP_MSG    = "pfs: pfs OPTION... [PATR_FILE_SYSTEM_PATH]\n"
		+ "    does something with the file system (normally used to format the file system)"
		+ "    options:\n"
		+ "      --force                      do even if there may get some data lost\n"
		+ "      --block-size [BLOCK_SIZE]    set the block size of the file system\n"
		+ "                                   requires --force if file system is not empty\n"
		+ "      --block-count [BLOCK_COUNT]  set the number of blocks available for the\n"
		+ "                                   patr-file-system\n"
		+ "                                   requires --force if file system is not empty\n"
		+ "      --formatt                    formatts the file system"
		+ "                                   requires --force if file system is not empty\n"
		+ "";
	private static final String HELP_HELP_MSG   = "help: help [TOPIC ...]\n"
		+ "    Display information about builtin commands.\n"
		+ "    \n"
		+ "    If TOPIC is specified, gives detailed help on all commands from\n"
		+ "    the given TOPIC, otherwise the list of intern commands is printed.\n"
		+ "    \n"
		+ "    Arguments:\n"
		+ "      TOPIC     specifying a help topic\n"
		+ "    \n"
		+ "    Exit Status:\n"
		+ "    Returns success unless PATTERN is not found or an invalid option is given.\n"
		+ "";
	
	private final String pvm;
	private Path         outPath;
	private Path         pfsPath;
	private FileSystem   patrFileSys;
	private Path         pathToPfs;
	private boolean      inPfs;
	private int          lastExitNum;
	
	private final Scanner     in;
	private final PrintStream out;
	private final PrintStream err;
	
	public PFSShell(Scanner in, PrintStream out, PrintStream err) {
		this(in, out, err, Paths.get("."));
	}
	
	public PFSShell(Scanner in, PrintStream out, PrintStream err, Path outPath) {
		this(in, out, err, outPath, "pvm");
	}
	
	public PFSShell(Scanner in, PrintStream out, PrintStream err, Path outPath, String pvm) {
		this.pvm = pvm;
		this.outPath = outPath.toAbsolutePath();
		this.pfsPath = null;
		this.inPfs = false;
		this.patrFileSys = null;
		this.pathToPfs = null;
		this.in = in;
		this.out = out;
		this.err = err;
	}
	
	
	@Override
	public void run() {
		while (true) {
			String[] cmd = nextCmd();
			try {
				switch (cmd[0]) {
				case CMD_CD:
					cd(cmd);
					break;
				case CMD_CP:
					cp(cmd);
					break;
				case CMD_CHANGE:
					change(cmd);
					break;
				case CMD_ECHO:
					echo(cmd);
					break;
				case CMD_EXIT:
					exit(cmd);
					break;
				case CMD_PFS:
					pfs(cmd);
					break;
				case CMD_HELP:
					help(cmd);
					break;
				default:
					execute(cmd);
					break;
				}
			} catch (Throwable t) {
				if (t instanceof ThreadDeath) {
					throw t;
				}
				out.print(ConsoleColors.RED + "error: " + t.getClass().getSimpleName() + ": " + t.getMessage() + "\n" + ConsoleColors.RESET);
			}
		}
	}
	
	public void cd(String[] cmd) {
		if (cmd.length <= 1) {
			out.print(ConsoleColors.RED + CD_HELP_MSG + ConsoleColors.RESET);
			lastExitNum = 1;
			return;
		}
		boolean l = false, p = false, e = false;
		for (int i = 1; i + 1 < cmd.length; i ++ ) {
			switch (cmd[i]) {
			case "--help":
			case "--?":
				out.print(CD_HELP_MSG);
				return;
			case "-L":
				if (l || p) {
					if (p) {
						out.print(ConsoleColors.RED + "illegal arguments: -P combined with -L" + ConsoleColors.RESET);
					} else {
						out.print(ConsoleColors.RED + "illegal arguments: -L doubled" + ConsoleColors.RESET);
					}
					lastExitNum = 1;
					return;
				}
				l = true;
				break;
			case "-P":
				if (l || p) {
					if (l) {
						out.print(ConsoleColors.RED + "illegal arguments: -L combined with -P" + ConsoleColors.RESET);
					} else {
						out.print(ConsoleColors.RED + "illegal arguments: -P doubled" + ConsoleColors.RESET);
					}
					lastExitNum = 1;
					return;
				}
				p = true;
				break;
			case "-e":
				if (l || e || !p) {
					if (l) {
						out.print(ConsoleColors.RED + "illegal arguments: -L combined with -e" + ConsoleColors.RESET);
					} else if (e) {
						out.print(ConsoleColors.RED + "illegal arguments: -e doubled" + ConsoleColors.RESET);
					} else {
						out.print(ConsoleColors.RED + "illegal arguments: -e without -P" + ConsoleColors.RESET);
					}
					lastExitNum = 1;
					return;
				}
				e = true;
				break;
			}
		}
		Path path = inPfs ? pfsPath : outPath;
		try {
			if (p) {
				try {
					path = path.toRealPath();
				} catch (Exception e1) {
					if ( !e) {
						throw e1;
					}
					path = path.normalize();
				}
			} else {
				path = path.normalize();
			}
			path = path.resolve(cmd[cmd.length - 1]);
			if (inPfs) {
				pfsPath = path;
			} else {
				outPath = path;
			}
			lastExitNum = 0;
		} catch (IOException ioe) {
			out.print(ConsoleColors.RED + "error: " + ioe.getClass().getSimpleName() + ": " + ioe.getMessage() + "\n" + ConsoleColors.RESET);
			lastExitNum = 1;
		}
	}
	
	public void cp(String[] cmd) {
		// TODO Auto-generated method stub
		
	}
	
	public void change(String[] cmd) {
		if (cmd.length > 1) {
			int i = 1;
			boolean readOnly = false;
			if (cmd.length > 2) {
				if ("--read-only".equalsIgnoreCase(cmd[1]) && cmd.length == 2) {
					readOnly = true;
				} else {
					out.print(ConsoleColors.RED + "change: too many arguments\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
			}
			Path newpfs = Paths.get(cmd[i]);
			if ( !Files.exists(newpfs)) {
				out.print(ConsoleColors.RED + "error: the new-pfs-path does not exist!\n" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			}
			SeekableByteChannel channel;
			try {
				if (readOnly) {
					channel = Files.newByteChannel(newpfs, StandardOpenOption.READ);
				} else {
					channel = Files.newByteChannel(newpfs, StandardOpenOption.READ, StandardOpenOption.WRITE);
				}
				byte[] bytes = new byte[4];
				channel.position(PatrFileSysConstants.FB_BLOCK_LENGTH_OFFSET);
				channel.read(ByteBuffer.wrap(bytes));
				int blockSize = ConvertNumByteArr.byteArrToInt(bytes, 0);
				BlockAccessor ba = new SeekablePathBlockAccessor(channel, blockSize);
				PatrFileSystem pfs = new PatrFileSysImpl(ba);
				FileSystem jfs = new PFSFileSystemImpl(PROVIDER, pfs);
				pfsPath = jfs.getPath("/");
				if (patrFileSys != null) {
					try {
						patrFileSys.close();
					} catch (IOException e) {
						out.print(ConsoleColors.RED + "error: on closing old pfs: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n" + ConsoleColors.RESET);
					}
				}
				pathToPfs = newpfs;
				patrFileSys = jfs;
			} catch (IOException e) {
				out.print(ConsoleColors.RED + "error: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			}
			inPfs = true;
		} else {
			if (pfsPath == null) {
				out.print(ConsoleColors.RED + "error: there is no pfs-path!\n" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			}
			inPfs = !inPfs;
		}
		lastExitNum = 0;
	}
	
	public void echo(String[] cmd) {
		if (cmd.length > 1) {
			for (int i = 1; i + 1 < cmd.length; i ++ ) {
				out.print(cmd[i] + ' ');
			}
			out.print(cmd[cmd.length - 1] + '\n');
		}
		lastExitNum = 0;
	}
	
	public void exit(String[] cmd) {
		int exitCode = lastExitNum;
		if (cmd.length > 1) {
			if (cmd.length > 2) {
				out.print(ConsoleColors.RED + "exit: too many arguments\n" + ConsoleColors.RESET);
				if (exitCode == 0) {
					exitCode = 1;
				}
			}
			switch (cmd[1].toLowerCase()) {
			default:
				try {
					exitCode = Integer.parseInt(cmd[1]);
				} catch (NumberFormatException e) {
					if (exitCode == 0) {
						exitCode = 1;
					}
					out.print(ConsoleColors.RED + "exit: illegal: numeric argument required\n" + ConsoleColors.RESET);
				}
				break;
			case "--help":
			case "--?":
				out.print(EXIT_HELP_MSG);
				return;
			}
		}
		System.exit(exitCode);
	}
	
	public void pfs(String[] cmd) {
		// TODO Auto-generated method stub
		
	}
	
	public void help(String[] cmd) {
		if (cmd.length == 1) {
			out.print(GENERAL_HELP_MSG);
		}
		for (int i = 1; i < cmd.length; i ++ ) {
			boolean all = false;
			switch (cmd[1].toLowerCase()) {
			case "all":
			case CMD_CD:
				out.print(CMD_CD + ":\n" + CD_HELP_MSG);
				if ( !all) break;
			case CMD_CHANGE:
				out.print(CMD_CHANGE + ":\n" + CHANGE_HELP_MSG);
				if ( !all) break;
			case CMD_ECHO:
				out.print(CMD_ECHO + ":\n" + ECHO_HELP_MSG);
				if ( !all) break;
			case CMD_EXIT:
				out.print(CMD_EXIT + ":\n" + EXIT_HELP_MSG);
				if ( !all) break;
			case CMD_HELP:
			case "--help":
			case "--?":
				out.print(CMD_HELP + ":\n" + HELP_HELP_MSG);
				break;
			default:
				out.print(ConsoleColors.RED + "unknown help topic: '" + cmd[1] + "'\n" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			}
		}
		lastExitNum = 0;
	}
	
	public void execute(String[] cmd) {
		if (inPfs) {
			String[] pvmcmd = new String[cmd.length + 2];
			pvmcmd[0] = pvm;
			pvmcmd[1] = "-pfs=" + pathToPfs.toString();
			String command = cmd[0];
			if (command.startsWith("/") || command.startsWith("./") || command.startsWith("../")) {
				command = "/bin/" + command;
			} else {
				Path path = patrFileSys.getPath(command);
				if ( !path.isAbsolute()) {
					path = pfsPath.resolve(path);
				}
				command = path.normalize().toString();
			}
			pvmcmd[2] = "-pmc=" + command;
			System.arraycopy(cmd, 1, pvmcmd, 3, cmd.length - 1);
			cmd = pvmcmd;
		}
		try {
			final Process p = Runtime.getRuntime().exec(cmd);
			Scanner stderr = new Scanner(p.getErrorStream(), StandardCharsets.UTF_8);
			delegate(() -> stderr.hasNextLine(), "delegate stderr for " + cmd[0], stderr, err);
			final Scanner stdout = new Scanner(p.getInputStream(), StandardCharsets.UTF_8);
			delegate(() -> stdout.hasNextLine(), "delegate stdout for " + cmd[0], stdout, out);
			PrintStream stdin = new PrintStream(p.getOutputStream(), true, StandardCharsets.UTF_8);
			delegate(() -> in.hasNextLine() && p.isAlive(), "delegate stdin for " + cmd[0], in, stdin);
			while (true) {
				try {
					lastExitNum = p.waitFor();
					stderr.close();
					stdout.close();
					stdin.close();
					return;
				} catch (InterruptedException e) {
					out.print(ConsoleColors.PURPLE + "I have been interrupted, while I was wating for the child process to finish\n" + ConsoleColors.RESET);
				}
			}
		} catch (IOException e) {
			out.print(ConsoleColors.RED + "error on starting the commmand: '" + e.getClass().getSimpleName() + ": " + e.getMessage() + "'\n" + ConsoleColors.RESET);
			lastExitNum = 1;
			return;
		}
	}
	
	private void delegate(ThrowingBooleanSupplier <RuntimeException> condition, String name, Scanner sc, PrintStream out) {
		Thread delegate = new Thread(() -> {
			while (condition.supply()) {
				String line = sc.nextLine();
				out.print(line + '\n');
			}
		}, name);
		delegate.setDaemon(true);
		delegate.start();
	}
	
	private String[] nextCmd() {
		List <String> cmd = new ArrayList <>();
		StringBuilder build = new StringBuilder();
		String line = in.nextLine();
		char[] newargs = line.toCharArray();
		for (int i = 0;; i ++ ) {
			if (i >= newargs.length) {
				if ( !cmd.isEmpty()) {
					break;
				}
				line = in.nextLine();
				newargs = line.toCharArray();
				i = 0;
			}
			switch (newargs[i]) {
			case '\'': {
				char end = '\'';
				for (; end != newargs[i]; i ++ ) {
					if (i >= newargs.length) {
						build.append('\n');
						line = in.nextLine();
						newargs = line.toCharArray();
						i = -1; // (i ++)
					} else {
						build.append(newargs[i]);
					}
				}
				cmd.add(build.toString());
				build = new StringBuilder();
				break;
			}
			case '"': {
				char end = '"';
				for (; end != newargs[i]; i ++ ) {
					if (i >= newargs.length) {
						build.append('\n');
						line = in.nextLine();
						newargs = line.toCharArray();
						i = -1; // (i ++)
					} else {
						build.append(newargs[i]);
					}
				}
				cmd.add(build.toString());
				build = new StringBuilder();
				break;
			}
			case '\\':
				if ( ++ i >= newargs.length) {
					build.append('\n');
					line = in.nextLine();
					newargs = line.toCharArray();
					i = -1; // (i ++)
				} else {
					build.append(newargs[i]);
				}
				break;
			case '$':
				if (i + 1 <= newargs.length
					&& newargs[i + 1] == '?') {
					build.append(lastExitNum);
				} else {
					build.append('$');
				}
			case '#':
				if (build.isEmpty()) {
					i = newargs.length;
					break;
				}
			default:
				if (newargs[i] > ' ') {
					build.append(newargs[i]);
					break;
				}
			case '\t':
			case ' ':
				if ( !build.isEmpty()) {
					cmd.add(build.toString());
					build = new StringBuilder();
				}
			}
		}
		cmd.add(build.toString());
		return cmd.toArray(new String[cmd.size()]);
	}
	
}
