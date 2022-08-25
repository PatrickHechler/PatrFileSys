package de.hechler.patrick.pfs.shell.objects;

import static de.hechler.patrick.pfs.utils.JavaPFSConsants.ATTR_VIEW_PATR;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.PATR_VIEW_ATTR_EXECUTABLE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.PATR_VIEW_ATTR_HIDDEN;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.PATR_VIEW_ATTR_READ_ONLY;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.objects.ba.BlockManagerImpl;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImpl;
import de.hechler.patrick.pfs.objects.jfs.PFSFileSystemImpl;
import de.hechler.patrick.pfs.objects.jfs.PFSFileSystemProviderImpl;
import de.hechler.patrick.pfs.shell.ShellMain;
import de.hechler.patrick.pfs.shell.utils.ConsoleColors;
import de.hechler.patrick.pfs.utils.ConvertNumByteArr;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

@SuppressWarnings("resource")
public class PFSShell implements Runnable {
	
	private static final String RFS_PATH_PREFIX = "//rfs/";
	private static final String PFS_PATH_PREFIX = "//pfs/";
	
	private static final PFSFileSystemProviderImpl PROVIDER = new PFSFileSystemProviderImpl();
	
	private static final long DEFAULT_CREATE_BLOCK_COUNT = Long.MAX_VALUE;
	
	private static final int DEFAULT_CREATE_BLOCK_SIZE = 1 << 14;
	
	private static final String CMD_CD     = "cd";
	private static final String CMD_CP     = "cp";
	private static final String CMD_CHANGE = "change";
	private static final String CMD_ECHO   = "echo";
	private static final String CMD_EXIT   = "exit";
	private static final String CMD_HELP   = "help";
	private static final String CMD_LOG    = "log";
	private static final String CMD_PFS    = "pfs";
	
	private static final String GENERAL_HELP_MSG = "patr shell, version " + ShellMain.VERSION + " (" + ShellMain.ARCH + ")\n"
		+ "These shell commands are defined internally.  Type `help' to see this message.\n"
		+ "Type `help name' to find out more about the function `name'.\n"
		+ "\n"
		+ CMD_CD + " [-L|-P [-e]]\n"
		+ CMD_CHANGE + " [PATR_FILE_SYSTEM_PATH]\n"
		+ CMD_CP + " [OPT... [-T] SRC DEST | OPT... SRC... DIR | OPT... -t DIR SRC...]\n"
		+ CMD_ECHO + " ECHO_MSG..."
		+ CMD_EXIT + " [n]\n"
		+ CMD_HELP + " [" + CMD_CD + "|" + CMD_CHANGE + "|" + CMD_CP + "|" + CMD_ECHO + "|" + CMD_EXIT + "|" + CMD_HELP + "|" + CMD_PFS + "]\n"
		+ CMD_LOG + " [MODE]\n"
		+ CMD_PFS + " [--create|--format|--block-count [BLOCK_COUNT]] OPTION... [PATR_FILE_SYSTEM_PATH]\n"
		+ "\n"
		+ "to mix paths from the 'real' file system and the patr-file-system paths on intern commands a path can be used with a prefix.\n"
		+ "to explicitly use the patr-file-system use '//pfs/' as prefix before the path\n"
		+ "to explicitly use the 'real'-file-system use '//rfs/' as prefix before the path\n"
		+ "these paths/the file-systems can also be mixed in intern commands\n"
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
	private static final String CP_HELP_MSG     = "Usage: cp [OPTION]... [-T] SOURCE DEST\n"
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
		// + " -p same as --preserve=mode,ownership,timestamps\n"
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
		+ "  -e, --executable             mark target(s) as executable (fails on nfs target(s))\n"
		+ "  -h, --hidden                 mark target(s) as hidden (fails on nfs target(s))\n"
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
	private static final String LOG_HELP_MSG    = "debug: debug [MODE]\n"
		+ "or:    debug\n"
		+ "    sets or prints the debug mode\n"
		+ "    modes:\n"
		+ "      " + LogModes.full + "              print full error message\n"
		+ "      " + LogModes.simple + " (default)  print simple error messages\n"
		+ "      " + LogModes.none + "              supress error messages\n"
		+ "";
	private static final String PFS_HELP_MSG    = "Usage: pfs [--formatt|--create] [OPTIONS...] [PATR_FILE_SYSTEM_PATH]\n"
		+ "or:    pfs --block-count [BLOCK_COUNT] [PATR_FILE_SYSTEM_PATH]\n"
		+ "or:    pfs [OPTIONS...] [PATR_FILE_SYSTEM_PATH]\n"
		+ "    --format                           formats the file system\n"
		+ "                                       requires --force if file system is not empty\n"
		+ "                                       when --format is set, a PATR_FILE_SYSTEM_PATH must be spezified\n"
		+ "    --create                           creates a new file system\n"
		+ "                                       requires --force if the file already exists\n"
		+ "                                       when --create is set, a PATR_FILE_SYSTEM_PATH must be spezified\n"
		+ "    --block-count [BLOCK_COUNT]        sets the number of blocks available for the file systen.\n"
		+ "                                       this operation will fail, if the number is smaller or equal\n"
		+ "                                       to the greatest allocated block.\n"
		+ "    when --formatt or --create is set:\n"
		+ "      --force                          do even if the file-system is not empty or if the file already exists\n"
		+ "                                       on --create this option is needed if the file already exists\n"
		+ "                                       on --format this option is needed if the file system is not empty\n"
		+ "                                       if the file system does not contain a valid patr-file-system on --format\n"
		+ "                                       this option should be used.\n"
		+ "                                       this option is ignored when --create and --format are not set\n"
		+ "      --block-size, -s [BLOCK_SIZE]    set the block size of the patr-file-system\n"
		+ "                                       requires --force if file system is not empty\n"
		+ "                                       default value is the current block size for --format\n"
		+ "                                       default value is " + DEFAULT_CREATE_BLOCK_SIZE + " (0x" + Long.toHexString(DEFAULT_CREATE_BLOCK_SIZE) + ") for --create\n"
		+ "      --block-count, -c [BLOCK_COUNT]  set the number of blocks available for the patr-file-system\n"
		+ "                                       default value is the current block count for --format\n"
		+ "                                       default value is " + DEFAULT_CREATE_BLOCK_COUNT + " (0x" + Long.toHexString(DEFAULT_CREATE_BLOCK_COUNT) + ") for --create\n"
		+ "    when --formatt and --create is not set:\n"
		+ "      --force                          ignored (only used for --format and --create)\n"
		+ "      --block-size, -s                 print the size of each block in bytes of the file system\n"
		+ "      --block-count, -c                print the block-count available to the file system\n"
		+ "      --free-blocks, -f                print the number of unallocated blocks\n"
		+ "      --used-blocks, -u                print the number of used/allocated blocks\n"
		+ "      --free-space, -F                 print the number of free blocks in a human readable format\n"
		+ "                                       the number of blocks will first be converted in bytes (see --block-size)\n"
		+ "      --used-space, -U                 print the number of used blocks in a human readable format\n"
		+ "                                       the number of blocks will first be converted in bytes (see --block-size)\n"
		+ "      --total-space, -T                print the total number of availabel blocks in a human readable format\n"
		+ "                                       the number of blocks will first be converted in bytes (see --block-size)\n"
		+ "      --greatest, -g                   prints the number of the greatest block used.\n"
		+ "";
	
	private final String         pvm;
	private Path                 outPath;
	private Path                 pfsPath;
	private Path                 pathToPfs;
	private boolean              inPfs;
	private int                  lastExitNum;
	private List <FileSystem>    patrFileSyss;
	private Map <String, String> myenv;
	private LogModes             logMode;
	private volatile int         runningJobs = 0;
	
	private static final Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
	
	public PFSShell() {
		this(null, null);
	}
	
	public PFSShell(Path outPath) {
		this(outPath, null);
	}
	
	public PFSShell(String pvm) {
		this(null, pvm);
	}
	
	public PFSShell(Path outPath, String pvm) {
		this.pvm = pvm == null ? "pvm" : pvm;
		this.outPath = (outPath == null ? Paths.get(".") : outPath).toAbsolutePath().normalize();
		this.pfsPath = null;
		this.inPfs = false;
		this.patrFileSyss = new ArrayList <>();
		this.pathToPfs = null;
		this.myenv = new HashMap <>(System.getenv());
		this.logMode = LogModes.simple;
	}
	
	@Override
	public void run() {
		System.out.print("pfs-shell " + ShellMain.VERSION + " (" + ShellMain.ARCH + ")\n"
			+ "");
		for (prompt(); in.hasNextLine(); prompt()) {
			try {
				PatrCommand[] cmd = nextCmd();
				if (cmd.length == 1 && cmd[0].cmd.length == 0) {
					continue;
				}
				assert runningJobs == 0;
				for (int i = 0; i < cmd.length; i ++ ) {
					final PatrCommand command = cmd[i];
					runningJobs ++ ;
					new Thread(() -> {
						try {
							executeAny(command);
						} finally {
							synchronized (PFSShell.this) {
								runningJobs -- ;
								PFSShell.this.notify();
							}
						}
					});
				}
				while (runningJobs > 0) {
					synchronized (this) {
						try {
							wait(1000L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Throwable t) {
				if (t instanceof ThreadDeath) {
					throw t;
				}
				logErr(t, "unknown ");
			}
		}
	}
	
	private void logErr(Throwable t, String simplePrefix) throws InternalError {
		logErr(logMode, t, simplePrefix);
	}
	
	private static void logErr(LogModes logMode, Throwable t, String simplePrefix) throws InternalError {
		switch (logMode) {
		case full:
			System.out.print(ConsoleColors.RED);
			t.printStackTrace(System.out);
			System.out.print(ConsoleColors.RESET);
			break;
		case simple:
			System.out.print(ConsoleColors.RED + simplePrefix + "error: " + t.getClass().getSimpleName() + ": " + t.getMessage() + "\n" + ConsoleColors.RESET);
			break;
		default:
			throw new InternalError("unknown log mode: " + logMode.name());
		case none:
		}
	}
	
	private void prompt() {
		if (inPfs) {
			System.out.print("[ pfs " + pfsPath.getFileName() + "] ");
		} else {
			System.out.print("[ rfs " + outPath.getFileName() + "] ");
		}
	}
	
	private void executeAny(PatrCommand cmd) {
		switch (cmd.cmd[0]) {
		case CMD_CD:
			cd(cmd);
			break;
		case CMD_CP:
			cp(cmd);
			break;
		case CMD_CHANGE:
			change(cmd);
			break;
		case CMD_LOG:
			log(cmd);
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
			executeExtern(cmd);
			break;
		}
		close(cmd.in, cmd, "stdin");
		close(cmd.out, cmd, "stdout");
		close(cmd.err, cmd, "stderr");
	}
	
	private void close(Closeable close, PatrCommand cmd, String name) throws InternalError {
		if (close == null) {
			return;
		}
		try {
			close.close();
		} catch (IOException e) {
			logErr(e, "error on closing " + name + " of " + cmd + ": ");
		}
	}
	
	public void cd(PatrCommand cmd) {
		PrintStream out = cmd.out == null ? System.out : new PrintStream(cmd.out, true, StandardCharsets.UTF_8);
		if (cmd.cmd.length <= 1) {
			out.print(ConsoleColors.RED + CD_HELP_MSG + ConsoleColors.RESET);
			lastExitNum = 1;
			return;
		}
		boolean l = false, p = false, e = false;
		for (int i = 1; i + 1 < cmd.cmd.length; i ++ ) {
			switch (cmd.cmd[i]) {
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
			path = path.resolve(cmd.cmd[cmd.cmd.length - 1]);
			path = path.toRealPath().normalize();
			if ( !patrFileSyss.isEmpty() && path.getFileSystem() == patrFileSyss.get(patrFileSyss.size() - 1)) {
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
	
	public void cp(PatrCommand cmd) {
		PrintStream out = cmd.out == null ? System.out : new PrintStream(cmd.out, true, StandardCharsets.UTF_8);
		int i;
		boolean cont = true;
		boolean force = false, interactive = false, link = false, no_clobber = false, recursive = false, remove_destination = false, no_target_directory = false, update = false, verbose = false, executable = false,
			hidden = false, read_only = false;
		String target_directory = null;
		for (i = 1; i < cmd.cmd.length && cont; i ++ ) {
			switch (cmd.cmd[i]) {
			case "--help":
			case "--?":
				out.print(CP_HELP_MSG);
				lastExitNum = 0;
				return;
			case "-f":
			case "--force":
				if (force) {
					out.print(ConsoleColors.RED + "error: force doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				if (remove_destination) {
					out.print(ConsoleColors.RED + "error: remove-destination mixed with force set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				force = true;
				break;
			case "--remove-destination":
				if (remove_destination) {
					out.print(ConsoleColors.RED + "error: remove-destination doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				if (remove_destination) {
					out.print(ConsoleColors.RED + "error: force mixed with remove-destination set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				remove_destination = true;
				break;
			case "-i":
			case "--interactive":
				if (interactive) {
					out.print(ConsoleColors.RED + "error: interactive doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				interactive = true;
				no_clobber = false;
				break;
			case "-l":
			case "--link":
				if (link) {
					out.print(ConsoleColors.RED + "error: link doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				link = true;
				break;
			case "-n":
			case "--no-clobber":
				if (no_clobber) {
					out.print(ConsoleColors.RED + "error: no-clobber doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				no_clobber = true;
				interactive = false;
				break;
			case "-R":
			case "-r":
			case "--recursive":
				if (recursive) {
					out.print(ConsoleColors.RED + "error: recursive doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				recursive = true;
				break;
			default:
				if ( !cmd.cmd[i].startsWith("--target-directory=")) {
					cont = false;
					i -- ;
					break;
				}
				target_directory = cmd.cmd[i].substring("--target-directory=".length());
				cont = false;
				break;
			case "-t":
				if ( ++ i >= cmd.cmd.length) {
					out.print(ConsoleColors.RED + "error: not enugh args for -t option!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				target_directory = cmd.cmd[i];
				cont = false;
				break;
			case "-T":
			case "--no-target-directory":
				if ( ++ i >= cmd.cmd.length) {
					out.print(ConsoleColors.RED + "error: not enugh args for -T, --no-target-directory option!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				no_target_directory = true;
				cont = false;
				break;
			case "-u":
			case "--update":
				if (update) {
					out.print(ConsoleColors.RED + "error: update doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				update = true;
				break;
			case "-v":
			case "--verbose":
				if (verbose) {
					out.print(ConsoleColors.RED + "error: verbose doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				verbose = true;
				break;
			case "-e":
			case "--executable":
				if (executable) {
					out.print(ConsoleColors.RED + "error: executable doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				executable = true;
				break;
			case "-h":
			case "--hidden":
				if (hidden) {
					out.print(ConsoleColors.RED + "error: hidden doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				hidden = true;
				break;
			case "--read-only":
				if (read_only) {
					out.print(ConsoleColors.RED + "error: hidden doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				read_only = true;
				break;
			}
		}
		if (target_directory != null) {
			Path target = getPath(inPfs, false, target_directory);
			for (; i < cmd.cmd.length; i ++ ) {
				Path source = getPath(inPfs, false, cmd.cmd[i]);
				Path targetFile = target.resolve(source.getFileName());
				boolean ret = copy(force, remove_destination, link, recursive, interactive, no_clobber, update, verbose, hidden, executable, read_only, source, targetFile, out);
				if (ret) return;
			}
		} else if (no_target_directory) {
			if (cmd.cmd.length != i + 2) {
				if (cmd.cmd.length <= i + 1) {
					out.print(ConsoleColors.RED + "not enugh arguments source & dest file!\n" + ConsoleColors.RESET);
				} else if (cmd.cmd.length <= i) {
					out.print(ConsoleColors.RED + "not enugh arguments dest file!\n" + ConsoleColors.RESET);
				} else {
					out.print(ConsoleColors.RED + "too many args (after -T only SOURCE and than DEST)!\n" + ConsoleColors.RESET);
				}
				lastExitNum = 1;
				return;
			}
			Path source = getPath(inPfs, false, cmd.cmd[i]);
			Path target = getPath(inPfs, false, cmd.cmd[i + 1]);
			copy(force, remove_destination, link, recursive, interactive, no_clobber, update, verbose, hidden, executable, read_only, source, target, out);
		} else if (cmd.cmd.length < i + 2) {
			out.print(ConsoleColors.RED + "not enugh arguments (need at least one source and the target)!\n" + ConsoleColors.RESET);
			lastExitNum = 1;
			return;
		} else {
			Path target = getPath(inPfs, false, cmd.cmd[cmd.cmd.length - 1]);
			for (; i < cmd.cmd.length - 1; i ++ ) {
				Path source = getPath(inPfs, false, cmd.cmd[i]);
				Path targetFile;
				if (cmd.cmd.length == i + 2 && Files.isRegularFile(source) && !cmd.cmd[cmd.cmd.length - 1].endsWith("/")) {
					targetFile = target;
				} else {
					targetFile = target.resolve(source.getFileName());
				}
				boolean ret = copy(force, remove_destination, link, recursive, interactive, no_clobber, update, verbose, hidden, executable, read_only, source, targetFile, out);
				if (ret) return;
			}
		}
		lastExitNum = 0;
	}
	
	private boolean copy(boolean force, boolean remove_destination, boolean link, boolean recursive, boolean interactive, boolean no_clobber, boolean update, boolean verbose, boolean hidden, boolean executable,
		boolean read_only, Path source, Path target, PrintStream out) {
		if (recursive && Files.isDirectory(source)) {
			if ( !Files.exists(target)) {
				try {
					Files.createDirectories(target);
				} catch (IOException e) {
					out.print(ConsoleColors.RED + "could not create target directory " + target + " : " + e.getClass().getSimpleName() + ": " + e.getMessage() + ConsoleColors.RESET);
					lastExitNum = 1;
					return true;
				}
				if (verbose) {
					out.print("created directory: " + target + '\n');
				}
			}
			try (DirectoryStream <Path> dir = Files.newDirectoryStream(source)) {
				for (Path sourceChild : dir) {
					String name = sourceChild.getFileName().toString();
					Path newTarget = target.resolve(name);
					boolean ret = copy(force, remove_destination, false, true, interactive, no_clobber, update, verbose, hidden, executable, read_only, sourceChild, newTarget, out);
					if (ret) return true;
				}
			} catch (IOException e) {
				out.print(ConsoleColors.RED + "could not open directory stream for the source file " + source + " : " + e.getClass().getSimpleName() + ": " + e.getMessage() + '\n' + ConsoleColors.RESET);
				lastExitNum = 1;
				return true;
			}
		} else {
			if (Files.exists(target)) {
				if (update) {
					try {
						FileTime lastTargetMod = Files.getLastModifiedTime(target);
						FileTime lastSourceMod = Files.getLastModifiedTime(source);
						if (lastTargetMod.compareTo(lastSourceMod) < 0) {
							if (verbose) {
								out.print("skiped target " + target + " (target is not older than source)\n");
							}
							return false;
						}
					} catch (IOException e) {
						out.print(ConsoleColors.RED + "target file " + target + " already exists!\n" + ConsoleColors.RESET);
						lastExitNum = 1;
						return true;
					}
				} else if (interactive) {
					out.print("overwrite file (" + target + ")? (yes|No): ");
					String str = in.nextLine().trim();
					if (str.isEmpty() && str.charAt(0) != 'y' && str.charAt(0) != 'Y') {
						lastExitNum = 0;
						return true;
					}
				} else if (no_clobber) {
					out.print(ConsoleColors.RED + "target file " + target + " already exists!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return true;
				} else if (remove_destination) {
					try {
						Files.delete(target);
						if (verbose) {
							out.print(ConsoleColors.RED + "deleted target file " + target + '\n');
						}
					} catch (IOException e) {
						out.print(ConsoleColors.RED + "target file " + target + " could not be deleted!\n" + ConsoleColors.RESET);
						lastExitNum = 1;
						return true;
					}
				}
			}
			try {
				if (link) {
					Files.createLink(target, source);
					if (verbose) {
						out.print("created link from " + target + " to " + source + '\n');
					}
				} else {
					Files.copy(source, target);
					if (verbose) {
						out.print("copied file from " + target + " to " + source + '\n');
					}
				}
				if (executable) {
					Files.setAttribute(target, ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_EXECUTABLE, true);
					if (verbose) {
						out.print("marked " + target + " as executable\n");
					}
				}
				if (hidden) {
					Files.setAttribute(target, ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_HIDDEN, true);
					if (verbose) {
						out.print("marked " + target + " as hidden\n");
					}
				}
				if (read_only) {
					Files.setAttribute(target, ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_READ_ONLY, true);
					if (verbose) {
						out.print("marked " + target + " as read-only\n");
					}
				}
			} catch (IOException e) {
				if ( !force) {
					out.print(ConsoleColors.RED + "target file: " + target + " error: " + e.getClass().getSimpleName() + ": " + e.getMessage() + '\n' + ConsoleColors.RESET);
					lastExitNum = 1;
					return true;
				}
				try {
					Files.delete(target);
					if (verbose) {
						out.print(ConsoleColors.RED + "deleted target file " + target + '\n');
					}
				} catch (IOException e1) {
					out.print(ConsoleColors.RED + "could not delete/overwrite target file " + target + '\n');
				}
				boolean ret = copy(false, false, link, recursive, interactive, no_clobber, update, verbose, hidden, executable, read_only, source, target, out);
				if (ret) return true;
			}
		}
		return false;
	}
	
	public void change(PatrCommand cmd) {
		PrintStream out = cmd.out == null ? System.out : new PrintStream(cmd.out, true, StandardCharsets.UTF_8);
		if (cmd.cmd.length > 1) {
			int i = 1;
			boolean readOnly = false;
			if (cmd.cmd.length > 2) {
				if ("--read-only".equalsIgnoreCase(cmd.cmd[1]) && cmd.cmd.length == 2) {
					readOnly = true;
				} else {
					out.print(ConsoleColors.RED + "change: too many arguments\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
			}
			Path newpfs = getPath(false, false, cmd.cmd[i]);
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
				PatrFileSystem pfs = new PatrFileSysImpl(new Random(), new BlockManagerImpl(ba), readOnly);
				FileSystem jfs = new PFSFileSystemImpl(PROVIDER, pfs);
				pfsPath = jfs.getPath("/");
				if ( !patrFileSyss.isEmpty()) {
					if (newpfs.getFileSystem() != patrFileSyss.get(patrFileSyss.size() - 1)) {
						for (int index = patrFileSyss.size() - 1; index >= 0; index -- ) {
							FileSystem patrFileSys = patrFileSyss.get(index);
							try {
								patrFileSys.close();
							} catch (IOException e) {
								out.print(ConsoleColors.RED + "error: on closing old pfs: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n" + ConsoleColors.RESET);
							}
						}
						patrFileSyss.clear();
					}
				}
				pathToPfs = newpfs;
				patrFileSyss.add(jfs);
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
	
	public void echo(PatrCommand cmd) {
		PrintStream out = cmd.out == null ? System.out : new PrintStream(cmd.out, true, StandardCharsets.UTF_8);
		if (cmd.cmd.length > 1) {
			for (int i = 1; i + 1 < cmd.cmd.length; i ++ ) {
				out.print(cmd.cmd[i] + ' ');
			}
			out.print(cmd.cmd[cmd.cmd.length - 1] + '\n');
		}
		lastExitNum = 0;
	}
	
	public void exit(PatrCommand cmd) {
		PrintStream out = cmd.out == null ? System.out : new PrintStream(cmd.out, true, StandardCharsets.UTF_8);
		int exitCode = lastExitNum;
		if (cmd.cmd.length > 1) {
			if (cmd.cmd.length > 2) {
				out.print(ConsoleColors.RED + "exit: too many arguments\n" + ConsoleColors.RESET);
				if (exitCode == 0) {
					exitCode = 1;
				}
			}
			switch (cmd.cmd[1].toLowerCase()) {
			default:
				try {
					exitCode = Integer.parseInt(cmd.cmd[1]);
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
	
	public void help(PatrCommand cmd) {
		PrintStream out = cmd.out == null ? System.out : new PrintStream(cmd.out, true, StandardCharsets.UTF_8);
		if (cmd.cmd.length == 1) {
			out.print(GENERAL_HELP_MSG);
		}
		for (int i = 1; i < cmd.cmd.length; i ++ ) {
			boolean all = false;
			switch (cmd.cmd[1].toLowerCase()) {
			case "all":
			case CMD_CD:
				out.print(CD_HELP_MSG);
				if ( !all) break;
			case CMD_CHANGE:
				out.print(CHANGE_HELP_MSG);
				if ( !all) break;
			case CMD_CP:
				out.print(CP_HELP_MSG);
				if ( !all) break;
			case CMD_ECHO:
				out.print(ECHO_HELP_MSG);
				if ( !all) break;
			case CMD_EXIT:
				out.print(EXIT_HELP_MSG);
				if ( !all) break;
			case CMD_HELP:
			case "--help":
			case "--?":
				out.print(HELP_HELP_MSG);
				if ( !all) break;
			case CMD_PFS:
				out.print(PFS_HELP_MSG);
				break;
			default:
				out.print(ConsoleColors.RED + "unknown help topic: '" + cmd.cmd[1] + "'\n" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			}
		}
		lastExitNum = 0;
	}
	
	public void log(PatrCommand cmd) {
		PrintStream out = cmd.out == null ? System.out : new PrintStream(cmd.out, true, StandardCharsets.UTF_8);
		if (cmd.cmd.length == 1) {
			out.print("current debug mode: " + logMode + '\n');
		}
		for (int i = 1; i < cmd.cmd.length; i ++ ) {
			switch (cmd.cmd[i]) {
			case "--help":
			case "--?":
				out.print(LOG_HELP_MSG);
				break;
			default:
				try {
					logMode = LogModes.valueOf(cmd.cmd[i]);
				} catch (Exception e) {
					logErr(e, "something went wrong by recieving the log mode ");
					return;
				}
			}
		}
		lastExitNum = 0;
	}
	
	public void pfs(PatrCommand cmd) {
		PrintStream out = cmd.out == null ? System.out : new PrintStream(cmd.out, true, StandardCharsets.UTF_8);
		boolean format = false, create = false, setBlockCount = false, force = false, block_size = false, block_count = false, free_blocks = false, used_blocks = false, free_space = false, used_space = false,
			total_space = false, greatest = false;
		int block_size_num = -1;
		long block_count_num = -1L;
		Path pfs = null;
		for (int i = 1; i < cmd.cmd.length; i ++ ) {
			switch (cmd.cmd[i]) {
			case "--help":
			case "--?":
				out.print(PFS_HELP_MSG);
				lastExitNum = 0;
				return;
			case "--format":
				if (i != 1) {
					out.print(ConsoleColors.RED + "--format must be the first argument\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				format = true;
				break;
			case "--create":
				if (i != 1) {
					out.print(ConsoleColors.RED + "--format must be the first argument\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				create = true;
				break;
			case "--block-count":
			case "-c":
				if (i == 1) {
					if (cmd.cmd.length == 4 || cmd.cmd.length == 3) {
						if ( !cmd.cmd[2].startsWith("-")) {
							try {
								block_count_num = Long.parseLong(cmd.cmd[2]);
							} catch (NumberFormatException e) {
								out.print(ConsoleColors.RED + "error on parsing the number '" + cmd.cmd[2] + "' (" + e.getMessage() + ")");
								lastExitNum = 1;
								return;
							}
							if (cmd.cmd.length > 3) {
								pfs = getPath(false, false, cmd.cmd[3]);
							}
							setBlockCount = true;
							i = 4;
							break;
						}
					}
				}
				if (block_count) {
					out.print(ConsoleColors.RED + "block-count already set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				block_count = true;
				if (create || format) {
					if ( ++ i >= cmd.cmd.length) {
						out.print(ConsoleColors.RED + "not enugh arguments for block-count!\n" + ConsoleColors.RESET);
						lastExitNum = 1;
						return;
					}
					try {
						block_count_num = Long.parseLong(cmd.cmd[2]);
					} catch (NumberFormatException e) {
						out.print(ConsoleColors.RED + "error on parsing the number '" + cmd.cmd[2] + "' (" + e.getMessage() + ")");
						lastExitNum = 1;
						return;
					}
				}
				break;
			case "--force":
				if (force) {
					out.print(ConsoleColors.RED + "force doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				force = true;
				break;
			case "--block-size":
			case "-s":
				if (block_size) {
					out.print(ConsoleColors.RED + "block-size already set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				block_size = true;
				if (create || format) {
					if ( ++ i >= cmd.cmd.length) {
						out.print(ConsoleColors.RED + "not enugh arguments for block-size!\n" + ConsoleColors.RESET);
						lastExitNum = 1;
						return;
					}
					try {
						block_size_num = Integer.parseInt(cmd.cmd[2]);
					} catch (NumberFormatException e) {
						out.print(ConsoleColors.RED + "error on parsing the number '" + cmd.cmd[2] + "' (" + e.getMessage() + ")");
						lastExitNum = 1;
						return;
					}
				}
				break;
			case "--free-blocks":
			case "-f":
				if (free_blocks) {
					out.print(ConsoleColors.RED + "free-blocks already set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				free_blocks = true;
				break;
			case "--used-blocks":
			case "-u":
				if (used_blocks) {
					out.print(ConsoleColors.RED + "used-blocks already set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				used_blocks = true;
				break;
			case "--free-space":
			case "-F":
				if (free_space) {
					out.print(ConsoleColors.RED + "free-space already set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				free_space = true;
				break;
			case "--used-space":
			case "-U":
				if (used_space) {
					out.print(ConsoleColors.RED + "used-space already set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				used_space = true;
				break;
			case "--total-space":
			case "-T":
				if (total_space) {
					out.print(ConsoleColors.RED + "total-space already set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				total_space = true;
				break;
			case "--greatest":
			case "-g":
				if (greatest) {
					out.print(ConsoleColors.RED + "greatest already set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				greatest = true;
				break;
			default:
				if (create || format) {
					if (i == cmd.cmd.length - 1) {
						pfs = getPath(false, false, cmd.cmd[i]);
					} else {
						out.print(ConsoleColors.RED + "unknown argument: '" + cmd.cmd[i] + "'\n" + ConsoleColors.RESET);
						lastExitNum = 1;
						return;
					}
				} else if (cmd.cmd[i].matches("\\-[scTfFuUg]+")) {
					if (cmd.cmd[i].indexOf('s') != -1) {
						if (block_size) {
							out.print(ConsoleColors.RED + "block-size already set!\n" + ConsoleColors.RESET);
							lastExitNum = 1;
							return;
						}
						block_size = true;
					}
					if (cmd.cmd[i].indexOf('c') != -1) {
						if (block_count) {
							out.print(ConsoleColors.RED + "block-count already set!\n" + ConsoleColors.RESET);
							lastExitNum = 1;
							return;
						}
						block_count = true;
					}
					if (cmd.cmd[i].indexOf('T') != -1) {
						if (total_space) {
							out.print(ConsoleColors.RED + "total-space already set!\n" + ConsoleColors.RESET);
							lastExitNum = 1;
							return;
						}
						total_space = true;
					}
					if (cmd.cmd[i].indexOf('f') != -1) {
						if (free_blocks) {
							out.print(ConsoleColors.RED + "free-blocks already set!\n" + ConsoleColors.RESET);
							lastExitNum = 1;
							return;
						}
						free_blocks = true;
					}
					if (cmd.cmd[i].indexOf('F') != -1) {
						if (free_space) {
							out.print(ConsoleColors.RED + "free-space already set!\n" + ConsoleColors.RESET);
							lastExitNum = 1;
							return;
						}
						free_space = true;
					}
					if (cmd.cmd[i].indexOf('u') != -1) {
						if (used_blocks) {
							out.print(ConsoleColors.RED + "used-blocks already set!\n" + ConsoleColors.RESET);
							lastExitNum = 1;
							return;
						}
						used_blocks = true;
					}
					if (cmd.cmd[i].indexOf('U') != -1) {
						if (used_space) {
							out.print(ConsoleColors.RED + "used-space already set!\n" + ConsoleColors.RESET);
							lastExitNum = 1;
							return;
						}
						used_space = true;
					}
					if (cmd.cmd[i].indexOf('g') != -1) {
						if (greatest) {
							out.print(ConsoleColors.RED + "greatest already set!\n" + ConsoleColors.RESET);
							lastExitNum = 1;
							return;
						}
						greatest = true;
					}
				} else if (i == cmd.cmd.length - 1) {
					pfs = getPath(false, false, cmd.cmd[i]);
				} else {
					out.print(ConsoleColors.RED + "unknown argument: '" + cmd.cmd[i] + "'\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
			}
		}
		if (pfs == null) {
			if (create || format) {
				out.print(ConsoleColors.RED + "PATR_FILE_SYSTEM_PATH must be set when --create or --formatt is used!\n" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			} else if (patrFileSyss.isEmpty()) {
				out.print(ConsoleColors.RED + "PATR_FILE_SYSTEM_PATH must be set when there is no other pfs!\n" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			} else {
				pfs = pathToPfs;
			}
		}
		try {
			SeekableByteChannel chnanel;
			if (create) {
				chnanel = Files.newByteChannel(pfs, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, force ? StandardOpenOption.CREATE : StandardOpenOption.CREATE_NEW);
			} else if (format) {
				chnanel = Files.newByteChannel(pfs, StandardOpenOption.READ, StandardOpenOption.WRITE);
			} else {
				chnanel = Files.newByteChannel(pfs, StandardOpenOption.READ);
			}
			if (block_size_num == -1) {
				if (create) {
					block_size_num = DEFAULT_CREATE_BLOCK_SIZE;
				} else {
					byte[] bytes = new byte[4];
					chnanel.position(PatrFileSysConstants.FB_BLOCK_LENGTH_OFFSET);
					chnanel.read(ByteBuffer.wrap(bytes));
					block_size_num = ConvertNumByteArr.byteArrToInt(bytes, 0);
				}
			}
			BlockAccessor ba = new SeekablePathBlockAccessor(chnanel, block_size_num);
			try (PatrFileSysImpl fs = new PatrFileSysImpl(ba)) {
				if (format) {
					if ( !force) {
						int ec = fs.getRoot().elementCount(PatrFileSysConstants.NO_LOCK);
						if (ec != 0) {
							out.print(ConsoleColors.RED + "the file system is not empty, use --force to format anyway\n" + ConsoleColors.RESET);
						}
					}
					if (block_count_num == -1L) {
						byte[] bytes = new byte[8];
						chnanel.position(PatrFileSysConstants.FB_BLOCK_COUNT_OFFSET);
						chnanel.read(ByteBuffer.wrap(bytes));
						block_count_num = ConvertNumByteArr.byteArrToLong(bytes, 0);
					}
					fs.format(block_count_num, block_size_num);
				} else if (create) {
					if (block_count_num == -1L) {
						block_count_num = DEFAULT_CREATE_BLOCK_COUNT;
					}
					fs.format(block_count_num, block_size_num);
				} else if (setBlockCount) {
					fs.lock(PatrFileSysConstants.LOCK_LOCKED_LOCK | PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK | PatrFileSysConstants.LOCK_NO_DELETE_ALLOWED_LOCK);
					try {
						int key = fs.lockFS();
						try {
							synchronized (fs) {
								byte[] bytes = ba.loadBlock(1L);
								try {
									int end = ConvertNumByteArr.byteArrToInt(bytes, bytes.length - 4);
									long minBlockCount = ConvertNumByteArr.byteArrToLong(bytes, end);
									if (block_count_num < minBlockCount) {
										out.print(ConsoleColors.RED + "min-block-count is larget than the new block count (min: " + minBlockCount + " : 0x" + Long.toHexString(minBlockCount) + ")\n"
											+ ConsoleColors.RESET);
										lastExitNum = 1;
										return;
									}
									bytes = ba.loadBlock(0L);
									try {
										ConvertNumByteArr.longToByteArr(bytes, PatrFileSysConstants.FB_BLOCK_COUNT_OFFSET, block_count_num);
									} finally {
										ba.saveBlock(bytes, 0L);
									}
								} finally {
									ba.discardBlock(1L);
								}
							}
						} finally {
							fs.unlockFS(key);
						}
					} finally {
						fs.removeLock();
					}
				} else {
					int blockSize = fs.blockSize();
					if (block_size) {
						out.print("block-count: " + blockSize + " (0x" + Long.toHexString(blockSize) + ")\n");
					}
					write("block-count", "total-space", block_count, total_space, fs, blockSize, out);
					write("used-blocks", "used-space", used_blocks, used_space, fs, blockSize, out);
					write("free-blocks", "free-space", free_blocks, free_space, fs, blockSize, out);
				}
			}
		} catch (IOException e) {
			logErr(e, "");
			lastExitNum = 1;
			return;
		}
	}
	
	private void write(String rawName, String humanReadableName, boolean raw, boolean humanReadable, PatrFileSysImpl fs, int blockSize, PrintStream out) throws IOException {
		if (raw) {
			long blockCount = fs.blockCount();
			out.print(rawName + ": " + blockCount + " (0x" + Long.toHexString(blockCount) + ")");
			if (humanReadable) {
				out.print(" " + humanReadableName + ": " + humanReadable(blockCount, blockSize));
			}
			out.print('\n');
		} else if (humanReadable) {
			long blockCount = fs.blockCount();
			out.print(humanReadableName + ": " + humanReadable(blockCount, blockSize) + "\n");
		}
	}
	
	private String humanReadable(long blocks, int blockSize) {
		try {
			long count = Math.multiplyExact(blocks, blockSize);
			String postfix = "bytes";
			if (count > 1024 * 10) {
				count /= 1024;
				postfix = "KB";
				if (count > 1024 * 10) {
					count /= 1024;
					postfix = "MB";
					if (count > 1024 * 10) {
						count /= 1024;
						postfix = "GB";
						if (count > 1024 * 10) {
							count /= 1024;
							postfix = "TB";
						}
					}
				}
			}
			return count + postfix;
		} catch (ArithmeticException e) {
			String max = humanReadable(Long.MAX_VALUE, 1);
			return "very large! (>" + max + ")";
		}
	}
	
	public void executeExtern(PatrCommand cmd) {
		PrintStream out = cmd.out == null ? System.out : new PrintStream(cmd.out, true, StandardCharsets.UTF_8);
		Path commandPath = getPath(true, true, cmd.cmd[0]);
		cmd.cmd[0] = commandPath.normalize().toString();
		if (patrFileSyss.size() > 0 && commandPath.getFileSystem() == patrFileSyss.get(patrFileSyss.size() - 1)) {
			if (patrFileSyss.size() != 1) {
				out.print(ConsoleColors.RED + "error: can not execute from nested patr file system!" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			}
			String[] pvmcmd = new String[cmd.cmd.length + 2];
			pvmcmd[0] = pvm;
			pvmcmd[1] = "--pfs=" + pathToPfs.toString();
			pvmcmd[2] = "--pmc=" + cmd.cmd[0];
			System.arraycopy(cmd.cmd, 1, pvmcmd, 3, cmd.cmd.length - 1);
			cmd.cmd = pvmcmd;
		}
		try {
			ProcessBuilder builder = new ProcessBuilder(cmd.cmd);
			builder.directory(outPath.toFile());
			if (cmd.out == null) {
				builder.redirectInput(Redirect.INHERIT);
			}
			if (cmd.err == null) {
				builder.redirectError(Redirect.INHERIT);
			}
			if (cmd.in == null) {
				builder.redirectOutput(Redirect.INHERIT);
			}
			Map <String, String> env = builder.environment();
			env.clear();
			env.putAll(myenv);
			Process p = builder.start();
			if (cmd.err != null) {
				delegate("delegate stderr for " + cmd.cmd[0], p.getErrorStream(), cmd.err, p, true, logMode);
			}
			if (cmd.out != null) {
				delegate("delegate stdout for " + cmd.cmd[0], p.getInputStream(), cmd.out, p, true, logMode);
			}
			if (cmd.in != null) {
				delegate("delegate stdin for " + cmd.cmd[0], cmd.in, p.getOutputStream(), p, false, logMode);
			}
			while (true) {
				try {
					lastExitNum = p.waitFor();
					return;
				} catch (InterruptedException e) {
					out.print(ConsoleColors.PURPLE + "I have been interrupted, while I was wating for the child process to finish\n" + ConsoleColors.RESET);
				}
			}
		} catch (IOException e) {
			logErr(e, "by starting of the command: ");
			lastExitNum = 1;
			return;
		}
	}
	
	private static void delegate(String name, InputStream in, OutputStream out, Process p, boolean always, LogModes logMode) {
		Thread delegate = new Thread(() -> {
			byte[] buffer = new byte[1 << 10];
			while (always || p.isAlive()) {
				try {
					int len = in.available();
					if (len == 0) {
						int b = in.read();
						if (b == -1) {
							return;
						}
						out.write(b);
					}
					len = Math.min(len, buffer.length);
					len = in.read(buffer, 0, len);
					out.write(buffer, 0, len);
				} catch (IOException e) {
					if (p.isAlive() || !always) {
						logErr(logMode, e, "[" + name + "]: ");
					}
				}
			}
		}, name);
		delegate.setDaemon(true);
		delegate.start();
	}
	
	private Path getPath(boolean pfsIsDefault, boolean executable, String path) {
		boolean isPfsPath = pfsIsDefault;
		if (path.startsWith(PFS_PATH_PREFIX)) {
			isPfsPath = true;
			path = path.substring(PFS_PATH_PREFIX.length());
		} else if (path.startsWith(RFS_PATH_PREFIX)) {
			isPfsPath = false;
			path = path.substring(RFS_PATH_PREFIX.length());
		}
		Path p;
		Path rel = isPfsPath ? pfsPath : outPath;
		switch (path.charAt(0)) {
		default:
			if (executable) {
				p = rel.getFileSystem().getPath("/bin/", path);
				break;
			}
		case '.':
			p = rel.resolve(path);
			break;
		case '/':
			p = rel.getFileSystem().getPath(path);
			break;
		}
		return p;
	}
	
	private PatrCommand[] nextCmd() {
		List <PatrCommand> result = new ArrayList <>();
		nextCmd(result, in.nextLine());
		return result.toArray(new PatrCommand[result.size()]);
	}
	
	private PatrCommand nextCmd(List <PatrCommand> result, String line) {
		PatrCommand command = new PatrCommand();
		List <String> cmd = new ArrayList <>();
		StringBuilder build = new StringBuilder();
		char[] newargs = line.toCharArray();
		boolean emptyBuild = true;
		int delegateNextStreamID = -1;
		for (int i = 0; i < newargs.length; i ++ ) {
			switch (newargs[i]) {
			case '\'': {
				char end = '\'';
				for (;;) {
					if ( ++ i >= newargs.length) {
						build.append('\n');
						emptyBuild = false;
						System.out.print("\\> ");
						line = in.nextLine();
						newargs = line.toCharArray();
						i = -1; // (i ++)
					} else if (end != newargs[i]) {
						if (newargs[i] == '$') {
							i = readCommandVariable(build, newargs, i);
						} else {
							build.append(newargs[i]);
							emptyBuild = false;
						}
					} else break;
				}
				add(command, cmd, build, delegateNextStreamID);
				delegateNextStreamID = -1;
				build = new StringBuilder();
				emptyBuild = true;
				break;
			}
			case '"': {
				char end = '"';
				for (;;) {
					if ( ++ i >= newargs.length) {
						build.append('\n');
						emptyBuild = false;
						System.out.print("\\> ");
						line = in.nextLine();
						newargs = line.toCharArray();
						i = -1; // (i ++)
					} else if (end != newargs[i]) {
						if (newargs[i] == '$') {
							i = readCommandVariable(build, newargs, i);
						} else {
							build.append(newargs[i]);
							emptyBuild = false;
						}
					} else break;
				}
				add(command, cmd, build, delegateNextStreamID);
				delegateNextStreamID = -1;
				build = new StringBuilder();
				emptyBuild = true;
				break;
			}
			case '\\':
				if ( ++ i >= newargs.length) {
					build.append('\n');
					emptyBuild = false;
					System.out.print("\\> ");
					line = in.nextLine();
					newargs = line.toCharArray();
					i = -1; // (i ++)
				} else {
					build.append(newargs[i]);
					emptyBuild = false;
				}
				break;
			case '$':
				i = readCommandVariable(build, newargs, i);
				emptyBuild = false;
				break;
			case '#':
				if (emptyBuild) {
					i = newargs.length;
					break;
				}
			case '|':
				i = newargs.length;
				PatrCommand sub = nextCmd(result, line.substring(i + 1));
				try {
					PipedOutputStream pout = new PipedOutputStream();
					PipedInputStream pin = new PipedInputStream();
					pout.connect(pin);
					sub.out = pout;
					command.in = pin;
				} catch (IOException e) {
					throw new RuntimeException(e.getClass() + ": " + e.getMessage());
				}
				break;
			case '1':
			case '2':
				if (i + 1 >= newargs.length) {
					build.append(newargs[i]);
					break;
				}
				if (newargs[i + 1] != '>') {
					build.append(newargs[i]);
					break;
				}
				if (newargs[i ++ ] == '2') {
					delegateNextStreamID = 2;
					if (i + 1 >= newargs.length) {
						break;
					}
					if (newargs[i + 1] != '>') {
						break;
					}
					delegateNextStreamID = 4;
					break;
				}
			case '>':
				delegateNextStreamID = 1;
				if (i + 1 >= newargs.length) {
					break;
				}
				if (newargs[i + 1] != '>') {
					break;
				}
				delegateNextStreamID = 3;
				break;
			case '0':
				if (i + 1 >= newargs.length) {
					build.append(newargs[i]);
					break;
				}
				if (newargs[i + 1] != '<') {
					build.append(newargs[i]);
					break;
				}
				i ++ ;
			case '<':
				delegateNextStreamID = 0;
				break;
			default:
				if (newargs[i] > ' ') {
					build.append(newargs[i]);
					emptyBuild = false;
					break;
				}
			case '\t':
			case ' ':
				if ( !emptyBuild) {
					add(command, cmd, build, delegateNextStreamID);
					delegateNextStreamID = -1;
					build = new StringBuilder();
					emptyBuild = true;
				}
			}
		}
		if ( !emptyBuild) {
			add(command, cmd, build, delegateNextStreamID);
		}
		command.cmd = cmd.toArray(new String[cmd.size()]);
		result.add(command);
		return command;
	}
	
	private void add(PatrCommand command, List <String> cmd, StringBuilder build, int delegateNextStreamID) {
		String string = build.toString();
		if (delegateNextStreamID == -1) {
			cmd.add(string);
			return;
		}
		Path path = getPath(inPfs, false, string);
		try {
			switch (delegateNextStreamID) {
			case 0:
				command.in = Files.newInputStream(path, StandardOpenOption.READ);
				break;
			case 1:
				command.out = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
				break;
			case 2:
				command.err = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
				break;
			case 3:
				command.out = Files.newOutputStream(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
				break;
			case 4:
				command.err = Files.newOutputStream(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
				break;
			default:
				throw new InternalError("illegal value of delegateNextStreamFD: " + delegateNextStreamID);
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getClass() + ": " + e.getMessage());
		}
	}
	
	private int readCommandVariable(StringBuilder build, char[] newargs, int i) {
		int dollarI = i;
		for (i ++ ; i < newargs.length; i ++ ) {
			if (newargs[i] < 'A') break;
			if (newargs[i] <= 'Z') continue;
			if (newargs[i] < 'a') break;
			if (newargs[i] <= 'z') continue;
			break;
		}
		String str = new String(newargs, dollarI + 1, i - dollarI - 1);
		switch (str) {
		case "?":
			str = Integer.toString(lastExitNum);
			break;
		case "PWD":
			str = inPfs ? pfsPath.toString() : outPath.toString();
			break;
		case "PFSWD":
			str = pfsPath.toString();
			break;
		case "RFSWD":
			str = outPath.toString();
			break;
		default:
			str = myenv.getOrDefault(str, "");
		}
		build.append(str);
		return i;
	}
	
	public static enum LogModes {
		full, simple, none;
	}
	
}
