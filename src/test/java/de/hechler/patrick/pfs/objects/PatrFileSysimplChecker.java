package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertFalse;
import static de.hechler.patrick.zeugs.check.Assert.assertGreatherEqual;
import static de.hechler.patrick.zeugs.check.Assert.assertLowerEqual;
import static de.hechler.patrick.zeugs.check.Assert.assertNotEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertNull;
import static de.hechler.patrick.zeugs.check.Assert.assertTrue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.ba.BlockManagerImpl;
import de.hechler.patrick.pfs.objects.ba.FileBlockAccessor;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.MethodParam;
import de.hechler.patrick.zeugs.check.anotations.Start;

public class PatrFileSysimplChecker {
	
	private static final long NO_LOCK  = PatrFileSysConstants.LOCK_NO_LOCK;
	private static final long NO_OWNER = PatrFileSysConstants.OWNER_NO_OWNER;
	private static final long NO_TIME  = PatrFileSysConstants.NO_TIME;
	
	private PatrFileSystem fs;
	
	@Start(onlyOnce = true)
	private void init() throws IOException {
		Path path = Paths.get("./testout/" + getClass().getSimpleName() + "/");
		Files.createDirectories(path);
	}
	
	@Start
	private void start(@MethodParam Method met) throws IOException {
		Path path = Paths.get("./testout/" + getClass().getSimpleName() + "/" + met.getName() + "/");
		if (Files.exists(path)) {
			deepDelete(path);
		} else {
			Files.createDirectory(path);
		}
		RandomAccessFile raf = new RandomAccessFile(path.resolve("./myFileSys.pfs").toFile(), "rw");
		FileBlockAccessor ba = new FileBlockAccessor(1 << 13, raf);
		BlockManagerImpl bm = new BlockManagerImpl(ba);
		fs = new PatrFileSysImpl(bm);
		fs.format();
	}
	
	private void deepDelete(Path path) throws IOException {
		try (DirectoryStream <Path> dirStr = Files.newDirectoryStream(path)) {
			for (Path p : dirStr) {
				if (Files.isDirectory(p)) {
					deepDelete(p);
				}
				Files.delete(p);
			}
		}
	}
	
	@End
	private void end() throws IOException {
		if (fs != null) {
			fs.close();
		}
		fs = null;
	}
	
	@Check
	private void checkWithEmptyFiles() throws IOException {
		PatrFolder root = fs.getRoot();
		long startCreate = System.currentTimeMillis();
		root.addFile("myName", NO_LOCK);
		long endCreate = System.currentTimeMillis();
		PatrFileSysElement e = root.getElement(0, NO_LOCK);
		assertTrue(e.isFile());
		assertFalse(e.isExecutable());
		assertFalse(e.isFolder());
		assertFalse(e.isHidden());
		assertFalse(e.isReadOnly());
		assertEquals(NO_OWNER, e.getOwner());
		assertEquals(NO_LOCK, e.getLockData());
		assertEquals(NO_TIME, e.getLockTime());
		assertGreatherEqual(startCreate, e.getCreateTime());
		assertLowerEqual(endCreate, e.getCreateTime());
		assertEquals(e.getLastModTime(), e.getCreateTime());
		assertEquals(e.getLastMetaModTime(), e.getCreateTime());
		assertEquals("myName", e.getName());
		assertEquals(root, e.getParent());
		assertNull(e.getFile().length());
		PatrFolder f = root.addFolder("myFolder", NO_LOCK);
		assertTrue(f.isFolder());
		assertFalse(f.isFile());
		assertFalse(f.isExecutable());
		assertFalse(f.isHidden());
		assertFalse(f.isReadOnly());
		assertEquals(NO_OWNER, f.getOwner());
		assertEquals(NO_LOCK, f.getLockData());
		assertEquals(NO_TIME, f.getLockTime());
		assertGreatherEqual(startCreate, f.getCreateTime());
		assertLowerEqual(endCreate, f.getCreateTime());
		assertEquals(f.getLastModTime(), f.getCreateTime());
		assertEquals(f.getLastMetaModTime(), f.getCreateTime());
		assertEquals("myFolder", f.getName());
		assertEquals(root, f.getParent());
		assertNotEquals(e, f);
		assertNull(f.getFolder().elementCount(NO_LOCK));
		PatrFolder sd = f.addFolder("subFolder", NO_LOCK);
		assertEquals(1, f.getFolder().elementCount(NO_LOCK));
		assertTrue(sd.isFolder());
		assertFalse(sd.isFile());
		assertFalse(sd.isExecutable());
		assertFalse(sd.isHidden());
		assertFalse(sd.isReadOnly());
		assertEquals(NO_OWNER, sd.getOwner());
		assertEquals(NO_LOCK, sd.getLockData());
		assertEquals(NO_TIME, sd.getLockTime());
		assertGreatherEqual(startCreate, sd.getCreateTime());
		assertLowerEqual(endCreate, sd.getCreateTime());
		assertEquals(sd.getLastModTime(), sd.getCreateTime());
		assertEquals(sd.getLastMetaModTime(), sd.getCreateTime());
		assertEquals("myFolder", sd.getName());
		assertEquals(root, sd.getParent());
		assertNotEquals(e, sd);
		assertNull(sd.getFolder().elementCount(NO_LOCK));
		PatrFile sf = f.addFile("subFile", NO_LOCK);
		assertEquals(2, f.getFolder().elementCount(NO_LOCK));
		assertTrue(sf.isFile());
		assertFalse(sf.isExecutable());
		assertFalse(sf.isFolder());
		assertFalse(sf.isHidden());
		assertFalse(sf.isReadOnly());
		assertEquals(NO_OWNER, sf.getOwner());
		assertEquals(NO_LOCK, sf.getLockData());
		assertEquals(NO_TIME, sf.getLockTime());
		assertGreatherEqual(startCreate, sf.getCreateTime());
		assertLowerEqual(endCreate, sf.getCreateTime());
		assertEquals(sf.getLastModTime(), sf.getCreateTime());
		assertEquals(sf.getLastMetaModTime(), sf.getCreateTime());
		assertEquals("myName", sf.getName());
		assertEquals(root, sf.getParent());
		assertNull(sf.getFile().length());
	}
	
	@Check
	private void checkFromRealFS() throws IOException {
		final Path testOut = Paths.get("./testout/" + getClass().getSimpleName() + "/checkFromRealFS/realFileSys/");
		Files.createDirectory(testOut);
		final Path testIn = Paths.get("./testin/checkFromRealFileSys/");
		deepCopy(testIn, fs.getRoot());//TODO add some bigger files to the folder
		deepCompare(testIn, fs.getRoot());
	}
	
	private void deepCompare(Path testIn, PatrFolder root) {
		// TODO Auto-generated method stub
		
	}

	private void deepCopy(Path realFSFolder, PatrFolder patrFSFolder) {
		// TODO Auto-generated method stub
		
	}
	
	
}
