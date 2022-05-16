package de.hechler.patrick.pfs.objects.jfs;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.objects.ba.FileBlockAccessor;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImpl;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImplChecker;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.MethodParam;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass
public class PatrJavaFileSysImplChecker {
	
	FileSystemProvider provider;
	
	@Start(onlyOnce = true)
	private void init() throws IOException {
		Path path = Paths.get("testout", getClass().getSimpleName());
		if (Files.exists(path)) {
			PatrFileSysImplChecker.deepDeleteChildren(path);
		} else {
			Files.createDirectories(path);
		}
	}
	@Start
	private void start(@MethodParam Method met) throws IOException {
		Path path = Paths.get("testout", getClass().getSimpleName(), met.getName() + ".pfs");
		RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
		BlockAccessor ba = new FileBlockAccessor(1 << 14, raf);
		PatrFileSystem fs = new PatrFileSysImpl(ba);
		fs.format(1 << 10, 1 << 14);
		provider = new PFSFileSystemProviderImpl(fs);
	}
	
	@End
	private void end() throws URISyntaxException, IOException {
		FileSystemProvider prov = provider;
		provider = null;
		if (prov != null) {
			prov.getPath(new URI(null, null, "/", null, null)).getFileSystem().close();
		}
	}
	
	@Check
	private void check() {
		// TODO
	}
	
}
