package de.hechler.patrick.pfs.objects.fs;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_DELETE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_META_CHANGE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_TIME;
import static de.hechler.patrick.zeugs.check.Assert.assertArrayEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertFalse;
import static de.hechler.patrick.zeugs.check.Assert.assertGreatherEqual;
import static de.hechler.patrick.zeugs.check.Assert.assertLowerEqual;
import static de.hechler.patrick.zeugs.check.Assert.assertNotEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertNull;
import static de.hechler.patrick.zeugs.check.Assert.assertThrows;
import static de.hechler.patrick.zeugs.check.Assert.assertTrue;
import static de.hechler.patrick.zeugs.check.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.exception.ElementReadOnlyException;
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
import de.hechler.patrick.zeugs.check.exceptions.CheckerException;

@CheckClass(disabled = PatrFileSysImplChecker.DISABLE_DIFFRENT_SIZE)
class PatrFileSysImplDiffrentBlocksChecker extends PatrFileSysImplChecker {
	
	@Override
	protected int startsize() {
		return 513761;
	}
	
}


@CheckClass(disabled = PatrFileSysImplChecker.DISABLE_DIFFRENT_SIZE)
class PatrFileSysImplSmallDiffrentBlocksChecker extends PatrFileSysImplChecker {
	
	@Override
	protected int startsize() {
		return 12541;
	}
	
}


@CheckClass(disabled = PatrFileSysImplChecker.DISABLE_SLOW)
class PatrFileSysImplBigBlocksChecker extends PatrFileSysImplChecker {
	
	@Override
	protected int startsize() {
		return 1 << 30;
	}
	
}


@CheckClass(disabled = PatrFileSysImplChecker.DISABLE_NORMAL)
class PatrFileSysImplNormalBlocksChecker extends PatrFileSysImplChecker {
	
	@Override
	protected int startsize() {
		return 1 << 14;
	}
	
}


@CheckClass(disabled = PatrFileSysImplChecker.DISABLE_ME)
public class PatrFileSysImplChecker {
	
	private static final boolean RUN_ALL               = false;
	public static final boolean  DISABLE_DIFFRENT_SIZE = ( !RUN_ALL) & true;
	public static final boolean  DISABLE_OTHERS        = ( !RUN_ALL) & true;
	public static final boolean  DISABLE_NORMAL        = ( !RUN_ALL) & true;
	public static final boolean  DISABLE_ME            = ( !RUN_ALL) & false;
	public static final boolean  DISABLE_SLOW          = ( !RUN_ALL) & true;
	public static final boolean  DELETE_AFTER_CHECKS   = false;
	
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
		long blockCount = 1 << 15;
		blockCount += (2487926 / startSize) + 1L;
		blockCount += (1607082 / startSize) + 1L;
		blockCount += (2283687 / startSize) + 1L;
		blockCount += (384794875L / startSize) + 1L;
		blockCount += (33 / startSize) + 1L;
		fs.format(blockCount, startSize);
		System.out.println("start check: " + getClass().getSimpleName() + ": " + met.getName());
	}
	
	@End(onlyOnce = true)
	private void finish() throws IOException {
		if (DELETE_AFTER_CHECKS) {
			Path path = Paths.get("./testout/" + getClass().getSimpleName());
			deepDeleteChildren(path);
			Files.delete(path);
		}
		System.out.println("finished checks of class: " + getClass().getSimpleName());
	}
	
	protected int startsize() {
		return 1024;
	}
	
	public static void deepDeleteChildren(Path path) throws IOException {
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
		System.out.println("[" + getClass().getSimpleName() + ".checkFromRealFS]: finished read");
		deepWrite(testOut, fs.getRoot());
		System.out.println("[" + getClass().getSimpleName() + ".checkFromRealFS]: finished write");
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
		}
	}
	
	public static void deepCompare(Path a, Path b) throws IOException {
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
	
	@Check
	void checkLock() throws IOException {
		PatrFolder root = fs.getRoot();
		long lock = root.lock(LOCK_NO_WRITE_ALLOWED_LOCK);
		assertThrows(ElementLockedException.class, () -> root.addFile("locked", NO_LOCK));
		PatrFile legal = root.addFile("legal", lock);
		assertEquals(legal, root.getElement("legal", lock));
		assertEquals(legal, root.getElement("legal", NO_LOCK));
		assertThrows(ElementLockedException.class, () -> root.ensureAccess(NO_LOCK, LOCK_NO_WRITE_ALLOWED_LOCK, false));
		root.removeLock(lock);
		lock = root.lock(LOCK_NO_READ_ALLOWED_LOCK);
		root.addFolder("2", lock);
		root.addFolder("3", NO_LOCK);
		root.getElement("2", lock);
		assertThrows(ElementLockedException.class, () -> root.getElement(0, NO_LOCK));
		assertThrows(ElementLockedException.class, () -> root.addFile("4", 3245454L));
		root.removeLock(lock);
		lock = root.lock(LOCK_NO_META_CHANGE_ALLOWED_LOCK);
		root.setName("new name", lock);
		assertThrows(ElementLockedException.class, () -> root.setName("", NO_LOCK));
		root.setCreateTime(5L, lock);
		assertThrows(ElementLockedException.class, () -> root.setCreateTime(5L, NO_LOCK));
		root.setExecutable(true, lock);
		assertThrows(ElementLockedException.class, () -> root.setExecutable(true, NO_LOCK));
		root.setHidden(true, lock);
		assertThrows(ElementLockedException.class, () -> root.setHidden(true, NO_LOCK));
		root.setLastMetaModTime(5L, lock);
		assertThrows(ElementLockedException.class, () -> root.setLastMetaModTime(5L, NO_LOCK));
		root.setLastModTime(5L, lock);
		assertThrows(ElementLockedException.class, () -> root.setLastModTime(5L, NO_LOCK));
		root.setReadOnly(true, lock);
		assertThrows(ElementLockedException.class, () -> root.setReadOnly(true, NO_LOCK));
		root.setReadOnly(false, lock);
		root.removeLock(lock);
		lock = root.lock(LOCK_NO_WRITE_ALLOWED_LOCK);
		deleteAndCheckChild(lock, root.getElement(0, lock));
		root.removeLock(lock);
		lock = root.lock(LOCK_NO_READ_ALLOWED_LOCK);
		deleteAndCheckChild(NO_LOCK, root.getElement(0, lock));
		root.removeLock(lock);
		deleteAndCheckChild(NO_LOCK, root.getElement(0, NO_LOCK));
		assertNull(root.elementCount(NO_LOCK));
	}
	
	private void deleteAndCheckChild(final long rlock, final PatrFileSysElement c1) throws IOException, ElementLockedException, CheckerException {
		long clock = c1.lock(LOCK_NO_DELETE_ALLOWED_LOCK);
		assertThrows(ElementLockedException.class, () -> c1.delete(NO_LOCK, NO_LOCK));
		if (rlock != NO_LOCK) {
			assertThrows(ElementLockedException.class, () -> c1.delete(clock, NO_LOCK));
			assertThrows(ElementLockedException.class, () -> c1.delete(NO_LOCK, rlock));
		}
		c1.delete(clock, rlock);
	}
	
	@Check
	void checkMeta() throws IOException {
		PatrFolder root = fs.getRoot();
		assertThrows(IllegalStateException.class, () -> root.delete(NO_LOCK, NO_LOCK));
		assertFalse(root.isExecutable());
		assertFalse(root.isFile());
		assertTrue(root.isFolder());
		assertFalse(root.isHidden());
		assertFalse(root.isLink());
		assertFalse(root.isReadOnly());
		assertTrue(root.isRoot());
		root.addFolder("name", NO_LOCK);
		root.setReadOnly(true, NO_LOCK);
		assertThrows(ElementLockedException.class, () -> root.addFile("other file", 87L));
		root.setReadOnly(false, NO_LOCK);
		root.addFile("other file", NO_LOCK);
		assertThrows(FileAlreadyExistsException.class, () -> root.addFile("other file", NO_LOCK));
		assertThrows(FileAlreadyExistsException.class, () -> root.addFolder("other file", NO_LOCK));
		assertEquals("", root.getName());
		root.setName("root folder", NO_LOCK);
		assertEquals("root folder", root.getName());
		PatrFolder folder = root.getElement(0, NO_LOCK).getFolder();
		PatrFolder inner = folder.addFolder("inner folder", NO_LOCK);
		inner.addFile("file name", NO_LOCK);
		inner.addFolder("folder name", NO_LOCK).addFile("name", NO_LOCK).appendContent("value".getBytes(StandardCharsets.UTF_8), 0, 5, NO_LOCK);
		assertNotEquals(NO_TIME, inner.getCreateTime());
		inner.setCreateTime(NO_TIME, NO_LOCK);
		assertEquals(NO_TIME, inner.getCreateTime());
		assertThrows(IllegalStateException.class, () -> root.deepDelete(NO_LOCK, NO_LOCK));
		assertThrows(IllegalStateException.class, () -> folder.delete(NO_LOCK, NO_LOCK));
		root.setReadOnly(true, NO_LOCK);
		assertThrows(ElementReadOnlyException.class, () -> folder.deepDelete(e -> NO_LOCK));
		folder.setReadOnly(true, NO_LOCK);
		assertThrows(ElementReadOnlyException.class, () -> folder.deepDelete(e -> NO_LOCK));
		root.setReadOnly(false, NO_LOCK);
		assertThrows(ElementReadOnlyException.class, () -> folder.deepDelete(e -> NO_LOCK));
		folder.setReadOnly(false, NO_LOCK);
		folder.deepDelete(e -> NO_LOCK);
		assertNotEquals(NO_TIME, root.getCreateTime());
		root.setCreateTime(NO_TIME, NO_LOCK);
		assertEquals(NO_TIME, root.getCreateTime());
	}
	
	@Check(disabled = false)
	private void checkBlockChange() throws IOException {
		PatrFolder root = fs.getRoot();
		long start = System.currentTimeMillis();
		PatrFolder folder = root.addFolder(" ", NO_LOCK);
		long end = System.currentTimeMillis();
		int need = fs.blockSize() - PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
		need -= 20;
		need -= 8;
		need -= 8;
		need -= need / 2;
		char[] chars = new char[need];
		Arrays.fill(chars, '-');
		String name = new String(chars);
		folder.setName(name, NO_LOCK);
		assertEquals(name, folder.getName());
		assertTrue(folder.isFolder());
		assertFalse(folder.isFile());
		assertFalse(folder.isLink());
		assertFalse(folder.isExecutable());
		assertFalse(folder.isReadOnly());
		assertFalse(folder.isHidden());
		assertNull(folder.getLockData());
		assertLowerEqual(start, folder.getCreateTime());
		assertGreatherEqual(end, folder.getCreateTime());
		assertEquals(folder.getCreateTime(), folder.getLastModTime());
		assertLowerEqual(folder.getCreateTime(), folder.getLastMetaModTime());
		assertFalse(folder.isRoot());
		start = System.currentTimeMillis();
		folder = folder.addFolder(" ", NO_LOCK);
		end = System.currentTimeMillis();
		assertEquals(" ", folder.getName());
		assertTrue(folder.isFolder());
		assertFalse(folder.isFile());
		assertFalse(folder.isLink());
		assertFalse(folder.isExecutable());
		assertFalse(folder.isReadOnly());
		assertFalse(folder.isHidden());
		assertNull(folder.getLockData());
		assertLowerEqual(start, folder.getCreateTime());
		assertGreatherEqual(end, folder.getCreateTime());
		assertEquals(folder.getCreateTime(), folder.getLastModTime());
		assertEquals(folder.getCreateTime(), folder.getLastMetaModTime());
		assertFalse(folder.isRoot());
	}
	
}
