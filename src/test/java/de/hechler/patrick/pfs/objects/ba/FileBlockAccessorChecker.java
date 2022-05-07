package de.hechler.patrick.pfs.objects.ba;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.MethodParam;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass
public class FileBlockAccessorChecker extends BlockAccessorChecker {
	
	private static final int START_SIZE = 1 << 15;
	
	protected RandomAccessFile raf;
	
	@Start(onlyOnce = true)
	private void init() throws IOException {
		Path path = Paths.get("./testout/" + this.getClass().getSimpleName() + "/");
		if (!Files.exists(path)) {
			Files.createDirectories(path);
		}
	}
	
	@Start
	protected void start(@MethodParam Method met) throws IOException {
		Path path = Paths.get("./testout/" + this.getClass().getSimpleName() + "/fba-" + met.getName() + ".data");
		if ( !Files.exists(path)) {
			Files.createFile(path);
		}
		raf = new RandomAccessFile(path.toFile(), "rw");
		newFileBlockAccessor(START_SIZE);
	}
	
	@Check
	protected void checBlockkSize() throws IOException {
		assertEquals(ba.blockSize(), START_SIZE);
		assertEquals(ba.blockSize(), START_SIZE);
		assertEquals(ba.blockSize(), START_SIZE);
		newFileBlockAccessor(16);
		assertEquals(ba.blockSize(), 16);
		assertEquals(ba.blockSize(), 16);
		assertEquals(ba.blockSize(), 16);
		newFileBlockAccessor(1024);
		assertEquals(ba.blockSize(), 1024);
		newFileBlockAccessor(1024);
		assertEquals(ba.blockSize(), 1024);
		newFileBlockAccessor(1);
		assertEquals(ba.blockSize(), 1);
	}
	
	protected void newFileBlockAccessor(int blockSize) {
		ba = new FileBlockAccessor(blockSize, raf);
	}
	
}
