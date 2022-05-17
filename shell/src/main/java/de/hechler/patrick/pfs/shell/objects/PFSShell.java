package de.hechler.patrick.pfs.shell.objects;

import java.io.IOException;
import java.io.PrintStream;
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
	private static final String CMD_HELP   = "help";
	private static final String CMD_PFS    = "pfs";
	
	private static final String GENERAL_HELP_MSG = "patr shell, version " + ShellMain.VERSION + " (" + ShellMain.ARCH + ")\n"
		+ "These shell commands are defined internally.  Type `help' to see this list.\n"
		+ "Type `help name' to find out more about the function `name'.\n"
		+ "\n"
		+ CMD_CD + " [-L|-P [-e]]\n"
		+ CMD_CHANGE + " [PATR_FILE_SYSTEM_PATH]\n"
		+ CMD_CP + " [OPT... [-T] SRC DEST | OPT... SRC... DIR | OPT... -t DIR SRC...]\n"
		+ CMD_ECHO + " ECHO_MSG..."
		+ CMD_EXIT + " [n]\n"
		+ CMD_HELP + " [" + CMD_CD + "|" + CMD_CHANGE + "|" + CMD_CP + "|" + CMD_ECHO + "|" + CMD_EXIT + "|" + CMD_HELP + "|" + CMD_PFS + "]\n"
		+ CMD_PFS + " OPTION... [PATR_FILE_SYSTEM_PATH]\n"
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
	private static final String PFS_HELP_MSG    =   "Usage: pfs [--formatt] [OPTIONS...] [PATR_FILE_SYSTEM_PATH]\n"
		+ "or:    pfs [OPTIONS...] [PATR_FILE_SYSTEM_PATH]\n"
		+ "    --formatt                            formatts the file system\n"
		+ "                                         requires --force if file system is not empty\n"
		+ "    options:\n"
		+ "      when --formatt is set:\n"
		+ "        --force                          do even if there may get some data lost\n"
		+ "        --block-size, -s [BLOCK_SIZE]    set the block size of the file system\n"
		+ "                                         requires --force if file system is not empty\n"
		+ "        --block-count, -c [BLOCK_COUNT]  set the number of blocks available for the\n"
		+ "                                         patr-file-system\n"
		+ "                                         maby requires --force\n"
		+ "        --just-set, -j                   just set the block size, do not format the file system.\n"
		+ "                                         fails if --block-size is set.\n"
		+ "      when --formatt is not set:\n"
		+ "        --block-size, -s                 print the size of each block in bytes of the file system\n"
		+ "        --block-count, -c                print the block-count available to the file system\n"
		+ "        --free-blocks, -f                print the number of unallocated blocks\n"
		+ "        --used-blocks, -u                print the number of used/allocated blocks\n"
		+ "        --free-space, -F                 print the number of free blocks in a human readable format\n"
		+ "                                         the number of blocks will first be converted in bytes (see --block-size)\n"
		+ "        --used-space, -F                 print the number of used blocks in a human readable format\n"
		+ "                                         the number of blocks will first be converted in bytes (see --block-size)\n"
		+ "        --total-space, -t                print the total number of availabel blocks in a human readable format\n"
		+ "                                         the number of blocks will first be converted in bytes (see --block-size)\n"
		+ "";
	
	private final String      pvm;
	private Path              outPath;
	private Path              pfsPath;
	private Path              pathToPfs;
	private boolean           inPfs;
	private int               lastExitNum;
	private List <FileSystem> patrFileSyss;
	
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
		this.patrFileSyss = new ArrayList <>();
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
		int i;
		boolean cont = true;
		boolean force = false, interactive = false, link = false, no_clobber = false, recursive = false, remove_destination = false, no_target_directory = false, update = false, verbose = false, executable = false,
			hidden = false, read_only = false;
		String target_directory = null;
		for (i = 1; i < cmd.length && cont; i ++ ) {
			switch (cmd[i]) {
			case "--help":
			case "--?":
				out.print(CP_HELP_MSG);
				lastExitNum = 0;
				return;
			//@formatter:off
//				+ "  -f, --force                  if an existing destination file cannot be\n"
//				+ "                                 opened, remove it and try again (this option\n"
//				+ "                                 is ignored when the -n option is also used)\n"
			//@formatter:on
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
			//@formatter:off
//				+ "  -i, --interactive            prompt before overwrite (overrides a previous -n\n"
//				+ "                                  option)\n"
			//@formatter:on
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
			//@formatter:off
//				+ "  -l, --link                   hard link files instead of copying (only possible\n"
//				+ "                                  when using only patr-file-system)\n"
			//@formatter:on
			case "-l":
			case "--link":
				if (link) {
					out.print(ConsoleColors.RED + "error: link doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				link = true;
				break;
			//@formatter:off
//				+ "  -n, --no-clobber             do not overwrite an existing file (overrides\n"
//				+ "                                 a previous -i option)\n"
			//@formatter:on
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
			//@formatter:off
//				+ "  -R, -r, --recursive          copy directories recursively\n"
//				+ "      --remove-destination     remove each existing destination file before\n"
//				+ "                                 attempting to open it (contrast with --force)\n"
				//@formatter:on
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
			//@formatter:off
//				+ "  -t, --target-directory=DIRECTORY  copy all SOURCE arguments into DIRECTORY\n"
					//@formatter:on
			default:
				if ( !cmd[i].startsWith("--target-directory=")) {
					out.print(ConsoleColors.RED + "error: unknown argument: '" + cmd[i] + "'\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				target_directory = cmd[i].substring("--target-directory=".length());
				cont = false;
				break;
			case "-t":
				if ( ++ i >= cmd.length) {
					out.print(ConsoleColors.RED + "error: not enugh args for -t option!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				target_directory = cmd[i];
				cont = false;
				break;
			//@formatter:off
//				+ "  -T, --no-target-directory    treat DEST as a normal file\n"
			//@formatter:on
			case "-T":
			case "--no-target-directory":
				if ( ++ i >= cmd.length) {
					out.print(ConsoleColors.RED + "error: not enugh args for -T, --no-target-directory option!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				no_target_directory = true;
				cont = false;
				break;
			//@formatter:off
//				+ "  -u, --update                 copy only when the SOURCE file is newer\n"
//				+ "                                 than the destination file or when the\n"
//				+ "                                 destination file is missing\n"
			//@formatter:on
			case "-u":
			case "--update":
				if (update) {
					out.print(ConsoleColors.RED + "error: update doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				update = true;
				break;
			//@formatter:off
//				+ "  -v, --verbose                explain what is being done\n"
			//@formatter:on
			case "-v":
			case "--verbose":
				if (verbose) {
					out.print(ConsoleColors.RED + "error: verbose doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				verbose = true;
				break;
			//@formatter:off
//				+ "  -e, --executable             mark target(s) as executable (fails on nfs target(s))"
				//@formatter:on
			case "-e":
			case "--executable":
				if (executable) {
					out.print(ConsoleColors.RED + "error: executable doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				executable = true;
				break;
			//@formatter:off
//				+ "  -h, --hidden                 mark target(s) as hidden (fails on nfs target(s))"
			//@formatter:on
			case "-h":
			case "--hidden":
				if (hidden) {
					out.print(ConsoleColors.RED + "error: hidden doubled set!\n" + ConsoleColors.RESET);
					lastExitNum = 1;
					return;
				}
				hidden = true;
				break;
			//@formatter:off
//				+ "  --read-only                  mark target(s) as read only (fails on nfs target(s))"
			//@formatter:on
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
			for (; i < cmd.length; i ++ ) {
				Path source = getPath(inPfs, false, cmd[i]);
				Path targetFile = target.resolve(source.getFileName());
				boolean ret = copy(force, remove_destination, link, recursive, interactive, no_clobber, update, verbose, hidden, executable, read_only, source, targetFile);
				if (ret) return;
			}
		} else if (no_target_directory) {
			if (cmd.length != i + 2) {
				if (cmd.length <= i + 1) {
					out.print(ConsoleColors.RED + "not enugh arguments source & dest file!\n" + ConsoleColors.RESET);
				} else if (cmd.length <= i) {
					out.print(ConsoleColors.RED + "not enugh arguments dest file!\n" + ConsoleColors.RESET);
				} else {
					out.print(ConsoleColors.RED + "too many args (after -T only SOURCE and than DEST)!\n" + ConsoleColors.RESET);
				}
				lastExitNum = 1;
				return;
			}
			Path source = getPath(inPfs, false, cmd[i]);
			Path target = getPath(inPfs, false, cmd[i + 1]);
			copy(force, remove_destination, link, recursive, interactive, no_clobber, update, verbose, hidden, executable, read_only, source, target);
		} else if (cmd.length <= i + 2) {
			out.print(ConsoleColors.RED + "not enugh arguments (need at least one source and the target)!\n" + ConsoleColors.RESET);
			lastExitNum = 1;
			return;
		} else {
			Path target = getPath(inPfs, false, cmd[cmd.length - 1]);
			for (; i < cmd.length - 1; i ++ ) {
				Path source = getPath(inPfs, false, cmd[i]);
				Path targetFile;
				if (cmd.length == i + 2 && Files.isRegularFile(source) && !cmd[cmd.length - 1].endsWith("/")) {
					targetFile = target;
				} else {
					targetFile = target.resolve(source.getFileName());
				}
				boolean ret = copy(force, remove_destination, link, recursive, interactive, no_clobber, update, verbose, hidden, executable, read_only, source, targetFile);
				if (ret) return;
			}
		}
		lastExitNum = 0;
	}
	
	private boolean copy(boolean force, boolean remove_destination, boolean link, boolean recursive, boolean interactive, boolean no_clobber, boolean update, boolean verbose, boolean hidden, boolean executable,
		boolean read_only, Path source,
		Path target) {
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
					boolean ret = copy(force, remove_destination, false, true, interactive, no_clobber, update, verbose, hidden, executable, read_only, sourceChild, newTarget);
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
					Files.setAttribute(target, PFSFileSystemImpl.ATTR_VIEW_PATR + ':' + PFSFileSystemProviderImpl.PATR_VIEW_ATTR_EXECUTABLE, true);
					if (verbose) {
						out.print("marked " + target + " as executable\n");
					}
				}
				if (hidden) {
					Files.setAttribute(target, PFSFileSystemImpl.ATTR_VIEW_PATR + ':' + PFSFileSystemProviderImpl.PATR_VIEW_ATTR_HIDDEN, true);
					if (verbose) {
						out.print("marked " + target + " as hidden\n");
					}
				}
				if (read_only) {
					Files.setAttribute(target, PFSFileSystemImpl.ATTR_VIEW_PATR + ':' + PFSFileSystemProviderImpl.PATR_VIEW_ATTR_READ_ONLY, true);
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
				boolean ret = copy(false, false, link, recursive, interactive, no_clobber, update, verbose, hidden, executable, read_only, source, target);
				if (ret) return true;
			}
		}
		return false;
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
			Path newpfs = getPath(false, false, cmd[i]);
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
	
	public void help(String[] cmd) {
		if (cmd.length == 1) {
			out.print(GENERAL_HELP_MSG);
		}
		for (int i = 1; i < cmd.length; i ++ ) {
			boolean all = false;
			switch (cmd[1].toLowerCase()) {
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
				out.print(ConsoleColors.RED + "unknown help topic: '" + cmd[1] + "'\n" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			}
		}
		lastExitNum = 0;
	}
	
	public void pfs(String[] cmd) {
		boolean formatt = false, block_size = false, block_count = false, free_blocks = false, used_blocks = false, free_space = false, used_space = false, total_space = false;
		int block_size_num = -1, block_count_num = -1;
		for (int i = 1; i < cmd.length; i ++) {
			switch(cmd[i]) {
			
			}
		}
		// TODO Auto-generated method stub
		
	}
	
	public void execute(String[] cmd) {
		if (inPfs) {
			if (patrFileSyss.size() != 1) {
				out.print(ConsoleColors.RED + "error: can not execute from nested patr file system!" + ConsoleColors.RESET);
				lastExitNum = 1;
				return;
			}
			String[] pvmcmd = new String[cmd.length + 2];
			pvmcmd[0] = pvm;
			pvmcmd[1] = "-pfs=" + pathToPfs.toString();
			String command = getPath(true, true, cmd[0]).normalize().toString();
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
	
	private Path getPath(boolean pfsIsDefault, boolean executable, String path) {
		boolean isPfsPath = pfsIsDefault;
		if (path.startsWith("//pfs/")) {
			isPfsPath = true;
			path = path.substring("//pfs/".length());
		} else if (path.startsWith("//nfs/")) {
			isPfsPath = false;
			path = path.substring("//nfs/".length());
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
