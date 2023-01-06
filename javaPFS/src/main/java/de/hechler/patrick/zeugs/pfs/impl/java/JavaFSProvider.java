package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;

public class JavaFSProvider extends FSProvider {
	
	private final Map<Path, JavaFS> loaded;
	
	protected JavaFSProvider() {
		super(FSProvider.JAVA_FS_PROVIDER_NAME, Integer.MAX_VALUE);
		
		this.loaded = new HashMap<>();
	}
	
	@Override
	public FS loadFS(FSOptions opts) throws IOException {
		return null;
	}
	
	@Override
	public Collection<? extends FS> loadedFS() {
		return this.loaded.values();
	}
	
}
