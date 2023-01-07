package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;
import de.hechler.patrick.zeugs.pfs.opts.JavaFSOptions;

public class JavaFSProvider extends FSProvider {
	
	private final Function<Path, JavaFS> mapFunc = p -> new JavaFS(this, p);
	private final Map<Path, JavaFS>      loaded  = new HashMap<>();
	
	public JavaFSProvider() {
		super(FSProvider.JAVA_FS_PROVIDER_NAME, Integer.MAX_VALUE);
	}
	
	@Override
	public FS loadFS(FSOptions opts) throws IOException {
		return switch (opts) {
		case JavaFSOptions o -> loaded.computeIfAbsent(o.root(), mapFunc);
		default -> throw new IllegalArgumentException("option type is not supported: " + opts.getClass());
		};
	}
	
	@Override
	public Collection<? extends FS> loadedFS() {
		return this.loaded.values();
	}
	
	void unload(JavaFS fs) {
		JavaFS old = loaded.remove(fs.root);
		if (old != fs) {
			loaded.put(fs.root, old);
			throw new AssertionError();
		}
	}
	
}
