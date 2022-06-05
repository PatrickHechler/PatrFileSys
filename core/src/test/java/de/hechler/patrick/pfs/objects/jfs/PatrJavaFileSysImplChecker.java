package de.hechler.patrick.pfs.objects.jfs;

import static de.hechler.patrick.pfs.utils.JavaPFSConsants.ATTR_VIEW_BASIC;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.ATTR_VIEW_PATR;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_DIRECTORY;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_OTHER;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_REGULAR_FILE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_SIZE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_DO_FORMATT;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_FILE_SYS;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.PATR_VIEW_ATTR_EXECUTABLE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.PATR_VIEW_ATTR_HIDDEN;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.PATR_VIEW_ATTR_READ_ONLY;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.URI_SHEME;
import static de.hechler.patrick.zeugs.check.Assert.assertArrayEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertSame;
import static de.hechler.patrick.zeugs.check.Assert.assertTrue;
import static de.hechler.patrick.zeugs.check.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImpl;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImplChecker;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.MethodParam;
import de.hechler.patrick.zeugs.check.anotations.Start;
import de.hechler.patrick.zeugs.check.objects.CheckResult;
import de.hechler.patrick.zeugs.check.objects.Checker;

@CheckClass
public class PatrJavaFileSysImplChecker {
	
	private static final long BLOCK_COUNT = 414777L;
	private static final int  BLOCK_SIZE  = 1024;
	FileSystemProvider        provider;
	
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
		BlockAccessor ba = new SeekablePathBlockAccessor(path, BLOCK_SIZE);
		PatrFileSystem pfs = new PatrFileSysImpl(ba);
		pfs.format(BLOCK_COUNT, BLOCK_SIZE);
		PFSFileSystemProviderImpl prov = new PFSFileSystemProviderImpl();
		prov.initDefault(pfs);
		provider = prov;
		System.out.println("start check: " + getClass().getSimpleName() + ": " + met.getName());
	}
	
	@End
	private void end() throws URISyntaxException, IOException {
		FileSystemProvider prov = provider;
		provider = null;
		if (prov != null) {
			FileSystem fs = prov.getPath(new URI(URI_SHEME, null, "/", null, null)).getFileSystem();
			fs.close();
		}
	}
	
	@End(onlyOnce = true)
	private void finish() {
		System.out.println("finished checks of class: " + getClass().getSimpleName());
	}
	
	@Check
	private void checkInstalledProvider() throws IOException, URISyntaxException {
		search: {
			for (FileSystemProvider prov : FileSystemProvider.installedProviders()) {
				if (prov instanceof PFSFileSystemProviderImpl) {
					break search;
				}
			}
			fail("provider not found");
		}
		URI uri = URI.create(URI_SHEME + "://res");
		FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap(), PatrJavaFileSysImplChecker.class.getClassLoader());
		FileSystem fs2 = FileSystems.getFileSystem(uri);
		assertSame(fs, fs2);
		fs.close();
		uri = new URI(URI_SHEME + "://res2");
		fs = FileSystems.newFileSystem(uri,
			Map.of(NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT, Long.valueOf(BLOCK_COUNT), NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE, Integer.valueOf(BLOCK_SIZE), NEW_FILE_SYS_ENV_ATTR_DO_FORMATT, Boolean.TRUE,
				NEW_FILE_SYS_ENV_ATTR_FILE_SYS, new PatrFileSysImpl(new SeekablePathBlockAccessor(Paths.get("testout", getClass().getSimpleName(), "checkInstalledProvider-second.pfs"), BLOCK_SIZE))));
		FileStore store = fs.getFileStores().iterator().next();
		assertEquals(BLOCK_COUNT * BLOCK_SIZE, store.getTotalSpace());
		fs2 = FileSystems.getFileSystem(uri);
		assertSame(fs2, fs);
		fs.close();
	}
	
	@Check
	private void checkMeta() throws URISyntaxException, IOException {
		Path root = provider.getPath(new URI(URI_SHEME, null, "/", null));
		Path bin = Files.createDirectories(root.resolve("bin"));
		Path cat = Files.createFile(bin.resolve("cat"), new PFSFileAttributeImpl <Boolean>(ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_EXECUTABLE, Boolean.TRUE));
		assertEquals(Boolean.TRUE, Files.getAttribute(cat, ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_EXECUTABLE));
		assertEquals(Boolean.FALSE, Files.getAttribute(cat, ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_HIDDEN));
		assertEquals(Boolean.FALSE, Files.getAttribute(cat, ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_READ_ONLY));
		assertEquals(Boolean.TRUE, Files.getAttribute(cat, ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_IS_REGULAR_FILE));
		assertEquals(Boolean.TRUE, Files.getAttribute(cat, BASIC_ATTRIBUTE_IS_REGULAR_FILE));
		assertEquals(Boolean.FALSE, Files.getAttribute(cat, ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK));
		assertEquals(Boolean.FALSE, Files.getAttribute(cat, BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK));
		assertEquals(Boolean.FALSE, Files.getAttribute(cat, ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_IS_DIRECTORY));
		assertEquals(Boolean.FALSE, Files.getAttribute(cat, BASIC_ATTRIBUTE_IS_DIRECTORY));
		assertEquals(Boolean.FALSE, Files.getAttribute(cat, ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_IS_OTHER));
		assertEquals(Boolean.FALSE, Files.getAttribute(cat, BASIC_ATTRIBUTE_IS_OTHER));
		assertEquals(Long.valueOf(0L), Files.getAttribute(cat, ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_SIZE));
		assertEquals(Long.valueOf(0L), Files.getAttribute(cat, BASIC_ATTRIBUTE_SIZE));
		// TODO more
	}
	
	@Check
	private void checkChanges() {
		// TODO Auto-generated method stub
		
	}
	
	@Check
	private void checkFromRealFS() throws IOException, URISyntaxException {
		final Path testOut = Paths.get("testout", getClass().getSimpleName(), "checkFromRealFS");
		Files.createDirectory(testOut);
		final Path testIn = Paths.get("./testin/checkFromRealFileSys/");
		if ( !Files.exists(testIn)) {
			fail("the input data of this test does not exist! (testfolder='" + testIn + "')");
		}
		Path pfsroot = provider.getPath(new URI(URI_SHEME, null, "/", null));
		deepCopy(testIn, pfsroot);
		System.out.println("[" + getClass().getSimpleName() + ".checkFromRealFS]: finished read");
		deepCopy(pfsroot, testOut);
		System.out.println("[" + getClass().getSimpleName() + ".checkFromRealFS]: finished write");
		deepCompare(testIn, testOut);
	}
	
	private void deepCopy(Path from, Path to) throws IOException {
		try (DirectoryStream <Path> dirStr = Files.newDirectoryStream(from)) {
			for (Path copySource : dirStr) {
				Path copyTarget = to.resolve(copySource.getFileName().toString());
				if (Files.isDirectory(copySource)) {
					Files.createDirectory(copyTarget);
					deepCopy(copySource, copyTarget);
				} else {
					long sourceSize = Files.size(copySource);
					try (InputStream in = Files.newInputStream(copySource)) {
						try (OutputStream out = Files.newOutputStream(copyTarget, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND)) {
							byte[] bytes = new byte[ (Integer.MAX_VALUE > sourceSize) ? (1 << 30) : ((int) sourceSize)];
							long cnt = 0L;
							for (int cpy = in.read(bytes, 0, bytes.length); cpy != -1; cpy = in.read(bytes, 0, bytes.length)) {
								out.write(bytes, 0, cpy);
								cnt += cpy;
							}
							assertEquals(sourceSize, cnt);
						}
					}
					assertEquals(sourceSize, Files.size(copyTarget));
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
	
	public static void main(String[] args) {
		CheckResult res = Checker.check(PatrJavaFileSysImplChecker.class);
		res.detailedPrint(System.out, 4);
		if (res.wentUnexpected()) throw new Error(res.toString());
	}
	
}
