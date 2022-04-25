package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.zeugs.check.Assert.assertArrayEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertFalse;
import static de.hechler.patrick.zeugs.check.Assert.assertGreatherEqual;
import static de.hechler.patrick.zeugs.check.Assert.assertLowerEqual;
import static de.hechler.patrick.zeugs.check.Assert.assertNotEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertNull;
import static de.hechler.patrick.zeugs.check.Assert.assertTrue;
import static de.hechler.patrick.zeugs.check.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.ba.BlockManagerImpl;
import de.hechler.patrick.pfs.objects.ba.FileBlockAccessor;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.MethodParam;
import de.hechler.patrick.zeugs.check.anotations.ParamCreater;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass(disabled = PatrFileSysImplChecker.DISABLE_OTHERS)
class PatrFileSysImplDiffrentBlocksChecker extends PatrFileSysImplChecker {
	
	@Override
	protected int startsize() {
		return 5131;
	}
	
}


@CheckClass(disabled = PatrFileSysImplChecker.DISABLE_OTHERS)
class PatrFileSysImplSmallDiffrentBlocksChecker extends PatrFileSysImplChecker {
	
	@Override
	protected int startsize() {
		return 321;
	}
	
}


@CheckClass(disabled = PatrFileSysImplChecker.DISABLE_OTHERS)
class PatrFileSysImplBigBlocksChecker extends PatrFileSysImplChecker {
	
	@Override
	protected int startsize() {
		return 1 << 30;
	}
	
}


@CheckClass
class PatrFileSysImplNormalBlocksChecker extends PatrFileSysImplChecker {
	
	@Override
	protected int startsize() {
		return 1 << 15;
	}
	
}


@CheckClass(disabled = PatrFileSysImplChecker.DISABLE_OTHERS)
public class PatrFileSysImplChecker {
	
	public static final boolean DISABLE_OTHERS = false;
	
	private static final long NO_LOCK  = PatrFileSysConstants.LOCK_NO_LOCK;
	private static final long NO_OWNER = PatrFileSysConstants.OWNER_NO_OWNER;
	private static final long NO_TIME  = PatrFileSysConstants.NO_TIME;
	
	private PatrFileSystem fs;
	
	@Start(onlyOnce = true)
	private void init() throws IOException {
		Path path = Paths.get("./testout/" + getClass().getSimpleName() + "/");
		if (Files.exists(path)) {
			deepDeleteChildren(path);
		} else {
			Files.createDirectories(path);
		}
	}
	
	@Start
	private void start(@MethodParam Method met, @ParamCreater(method = "startsize") int startSize) throws IOException {
		Path path = Paths.get("./testout/" + getClass().getSimpleName() + "/" + met.getName() + "/");
		Files.createDirectory(path);
		RandomAccessFile raf = new RandomAccessFile(path.resolve("./myFileSys.pfs").toFile(), "rw");
		FileBlockAccessor ba = new FileBlockAccessor(startSize, raf);
		BlockManagerImpl bm = new BlockManagerImpl(ba);
		fs = new PatrFileSysImpl(bm);
		fs.format();
	}
	
	protected int startsize() {
		return 512;
	}
	
	private void deepDeleteChildren(Path path) throws IOException {
		try (DirectoryStream <Path> dirStr = Files.newDirectoryStream(path)) {
			for (Path p : dirStr) {
				if ( !Files.isWritable(path)) {
					path.toFile().setWritable(true);
				}
				if (Files.isDirectory(p)) {
					deepDeleteChildren(p);
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
	private void checkWithNonEmptyFiles() throws IOException {
		PatrFolder root = fs.getRoot();
		PatrFile file1 = root.addFile("myName", NO_LOCK);
		PatrFile file2 = root.addFile("mySecondName", NO_LOCK);
		byte[] bytes1 = ("hello world!\n"
			+ "this is some texxt file.\n"
			+ "\n"
			+ "here comes some text.\n"
			+ "this is some more text.\n"
			+ "now the there comes no more text in this file!").getBytes(StandardCharsets.UTF_8);
		file1.appendContent(bytes1, 0, bytes1.length, NO_LOCK);
		Random rnd = new Random();
		byte[] bytes2 = new byte[rnd.nextInt(1000) + 200];
		rnd.nextBytes(bytes2);
		file2.appendContent(bytes2, 0, bytes2.length, NO_LOCK);
		assertEquals(bytes1.length, file1.length());
		assertEquals(bytes2.length, file2.length());
		byte[] r1 = new byte[bytes1.length];
		byte[] r2 = new byte[bytes2.length];
		file1.getContent(r1, 0, 0, r1.length, NO_LOCK);
		file2.getContent(r2, 0, 0, r2.length, NO_LOCK);
		assertArrayEquals(bytes1, r1);
		assertArrayEquals(bytes2, r2);
		System.arraycopy(bytes2, 50, bytes2, 10, 100);
		file2.getContent(r2, 50L, 10, 100, NO_LOCK);
		assertArrayEquals(bytes2, r2);
		System.arraycopy(bytes1, 30, bytes2, 150, 20);
		file2.setContent(bytes1, 150L, 30, 20, NO_LOCK);
		file2.getContent(r2, 140L, 140, 40, NO_LOCK);
		assertArrayEquals(bytes2, r2);
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
		assertLowerEqual(startCreate, e.getCreateTime());
		assertGreatherEqual(endCreate, e.getCreateTime());
		assertEquals(e.getLastModTime(), e.getCreateTime());
		assertEquals(e.getLastMetaModTime(), e.getCreateTime());
		assertEquals("myName", e.getName());
		assertEquals(root, e.getParent());
		assertNull(e.getFile().length());
		startCreate = System.currentTimeMillis();
		PatrFolder f = root.addFolder("myFolder", NO_LOCK);
		endCreate = System.currentTimeMillis();
		assertTrue(f.isFolder());
		assertFalse(f.isFile());
		assertFalse(f.isExecutable());
		assertFalse(f.isHidden());
		assertFalse(f.isReadOnly());
		assertEquals(NO_OWNER, f.getOwner());
		assertEquals(NO_LOCK, f.getLockData());
		assertEquals(NO_TIME, f.getLockTime());
		assertLowerEqual(startCreate, f.getCreateTime());
		assertGreatherEqual(endCreate, f.getCreateTime());
		assertEquals(f.getLastModTime(), f.getCreateTime());
		assertEquals(f.getLastMetaModTime(), f.getCreateTime());
		assertEquals("myFolder", f.getName());
		assertEquals(root, f.getParent());
		assertNotEquals(e, f);
		assertNull(f.getFolder().elementCount(NO_LOCK));
		startCreate = System.currentTimeMillis();
		PatrFolder sd = f.addFolder("subFolder", NO_LOCK);
		endCreate = System.currentTimeMillis();
		assertEquals(1, f.elementCount(NO_LOCK));
		assertTrue(sd.isFolder());
		assertFalse(sd.isFile());
		assertFalse(sd.isExecutable());
		assertFalse(sd.isHidden());
		assertFalse(sd.isReadOnly());
		assertEquals(NO_OWNER, sd.getOwner());
		assertEquals(NO_LOCK, sd.getLockData());
		assertEquals(NO_TIME, sd.getLockTime());
		assertLowerEqual(startCreate, sd.getCreateTime());
		assertGreatherEqual(endCreate, sd.getCreateTime());
		assertEquals(sd.getLastModTime(), sd.getCreateTime());
		assertEquals(sd.getLastMetaModTime(), sd.getCreateTime());
		assertEquals("subFolder", sd.getName());
		assertEquals(f, sd.getParent());
		assertNotEquals(e, sd);
		assertNull(sd.getFolder().elementCount(NO_LOCK));
		startCreate = System.currentTimeMillis();
		PatrFile sf = f.addFile("subFile", NO_LOCK);
		endCreate = System.currentTimeMillis();
		assertEquals(2, f.getFolder().elementCount(NO_LOCK));
		assertTrue(sf.isFile());
		assertFalse(sf.isExecutable());
		assertFalse(sf.isFolder());
		assertFalse(sf.isHidden());
		assertFalse(sf.isReadOnly());
		assertEquals(NO_OWNER, sf.getOwner());
		assertEquals(NO_LOCK, sf.getLockData());
		assertEquals(NO_TIME, sf.getLockTime());
		assertLowerEqual(startCreate, sf.getCreateTime());
		assertGreatherEqual(endCreate, sf.getCreateTime());
		assertEquals(sf.getLastModTime(), sf.getCreateTime());
		assertEquals(sf.getLastMetaModTime(), sf.getCreateTime());
		assertEquals("subFile", sf.getName());
		assertEquals(f, sf.getParent());
		assertNull(sf.getFile().length());
	}
	
	@Check
	private void checkFromRealFS() throws IOException {
		final Path testOut = Paths.get("./testout/" + getClass().getSimpleName() + "/checkFromRealFS/realFileSys/");
		Files.createDirectory(testOut);
		final Path testIn = Paths.get("./testin/checkFromRealFileSys/");
		if ( !Files.exists(testIn)) {
			fail("the input data of this test does not exist! (testfolder='" + testIn + "')");
		}
		deepRead(testIn, fs.getRoot());
		deepWrite(testOut, fs.getRoot());
		deepCompare(testIn, testOut);
	}
	
	private void deepRead(Path realFSFolder, PatrFolder patrFSFolder) throws IOException {
		try (DirectoryStream <Path> dirStr = Files.newDirectoryStream(realFSFolder)) {
			for (Path path : dirStr) {
				String name = path.getFileName().toString();
				PatrFileSysElement e;
				if (Files.isDirectory(path)) {
					e = patrFSFolder.addFolder(name, NO_LOCK);
					deepRead(path, e.getFolder());
				} else {
					e = patrFSFolder.addFile(name, NO_LOCK);
					PatrFile f = e.getFile();
					try (InputStream in = Files.newInputStream(path)) {
						byte[] buff = new byte[1 << 30];
						while (true) {
							int r = in.read(buff, 0, buff.length);
							if (r == -1) {
								break;
							}
							f.appendContent(buff, 0, r, NO_LOCK);
						}
					}
					assertEquals(Files.size(path), f.length());
				}
				if (!Files.isWritable(path)) {
					e.setReadOnly(true, NO_LOCK);
				}
				if (Files.isHidden(path)) {
					e.setHidden(true, NO_LOCK);
				}
			}
		}
	}
	
	private void deepWrite(Path realFSFolder, PatrFolder patrFSFolder) throws IOException {
		for (PatrFileSysElement child : patrFSFolder) {
			Path path = realFSFolder.resolve(child.getName());
			if (child.isFolder()) {
				Files.createDirectory(path);
				deepWrite(path, child.getFolder());
			} else {
				try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND)) {
					byte[] buffer = new byte[1 << 30];
					PatrFile file = child.getFile();
					long len = file.length();
					int cpy;
					for (long copied = 0L; copied < len; copied += cpy) {
						cpy = (int) Math.min(buffer.length, len - copied);
						file.getContent(buffer, copied, 0, cpy, NO_LOCK);
						out.write(buffer, 0, cpy);
					}
				}
			}
			File file = path.toFile();
			file.setExecutable(child.isExecutable());
			file.setWritable( !child.isReadOnly());
			if (child.isHidden()) {
				Files.setAttribute(path, "dos:hidden", true);
			} else {
				Files.setAttribute(path, "dos:hidden", false);
			}
		}
	}
	
	private void deepCompare(Path a, Path b) throws IOException {
		assertTrue(Files.exists(a));
		assertTrue(Files.exists(b));
		if (Files.isDirectory(a)) {
			assertTrue(Files.isDirectory(b));
			Set <String> names = new HashSet <>();
			try (DirectoryStream <Path> da = Files.newDirectoryStream(a)) {
				for (Path path : da) {
					Path lastPathSeg = path.getFileName();
					deepCompare(path, b.resolve(lastPathSeg));
					names.add(lastPathSeg.toString());
				}
			}
			try (DirectoryStream <Path> db = Files.newDirectoryStream(b)) {
				for (Path path : db) {
					names.remove(path.getFileName().toString());
				}
			}
			assertTrue(names.isEmpty());
		} else {
			assertTrue(Files.isRegularFile(a));
			assertTrue(Files.isRegularFile(b));
			InputStream ina = Files.newInputStream(a, StandardOpenOption.READ);
			InputStream inb = Files.newInputStream(b, StandardOpenOption.READ);
			byte[] bufferA = new byte[1 << 30];
			byte[] bufferB = new byte[1 << 30];
			while (true) {
				int readA = ina.read(bufferA, 0, bufferA.length);
				int readB = inb.read(bufferB, 0, readA == -1 ? 1 : readA);
				assertEquals(readA, readB);
				assertArrayEquals(bufferA, bufferB);
				if (readA == -1) {
					break;
				}
			}
		}
	}
	
	
}
