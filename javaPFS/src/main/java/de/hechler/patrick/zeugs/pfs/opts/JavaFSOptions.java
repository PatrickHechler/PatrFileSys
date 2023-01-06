package de.hechler.patrick.zeugs.pfs.opts;

import java.nio.file.Path;

import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;

public record JavaFSOptions(Path root) implements FSOptions {
	
}
