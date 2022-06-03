package de.hechler.patrick.pfs.objects.jfs;

import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.*;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_DO_FORMATT;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_FILE_SYS;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.URI_SHEME;
import static de.hechler.patrick.zeugs.check.Assert.assertArrayEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertSame;
import static de.hechler.patrick.zeugs.check.Assert.assertTrue;
import static de.hechler.patrick.zeugs.check.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.objects.ba.FileBlockAccessor;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor;
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
		Path metpath = Paths.get("testout", getClass().getSimpleName());
		if (Files.exists(metpath)) {
			PatrFileSysImplChecker.deepDeleteChildren(metpath);
		} else {
			Files.createDirectories(metpath);
		}
		Path path = metpath.resolve(met.getName() + ".pfs");
		RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
		BlockAccessor ba = new FileBlockAccessor(1 << 14, raf);
		PatrFileSystem fs = new PatrFileSysImpl(ba);
		fs.format(1 << 10, 1 << 14);
		provider = new PFSFileSystemProviderImpl(fs);
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
				Map.of(NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT, Long.valueOf(1L << 9), NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE, Integer.valueOf(1 << 11), NEW_FILE_SYS_ENV_ATTR_DO_FORMATT,
						Boolean.TRUE, NEW_FILE_SYS_ENV_ATTR_FILE_SYS,
						new PatrFileSysImpl(new SeekablePathBlockAccessor(Paths.get(getClass().getSimpleName(), "checkInstalledProvider", "test-fs.pfs"), 1 << 11))));
		FileStore store = fs.getFileStores().iterator().next();
		assertEquals( (1 << 11) * (1L << 9), store.getTotalSpace());
		fs2 = FileSystems.getFileSystem(uri);
		assertSame(fs2, fs);
		fs.close();
	}
	
	@Check
	private void checkMeta() throws URISyntaxException, IOException {
		Path root = provider.getPath(new URI(URI_SHEME, null, "/", null));
		Path bin = Files.createDirectories(root.resolve("bin"));
		Path cat = Files.createFile(bin.resolve("cat"), new FileAttribute <Boolean>() {
			
			@Override
			public String name() {
				return ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_EXECUTABLE;
			}
			
			@Override
			public Boolean value() {
				return Boolean.TRUE;
			}
			
		});
		assertEquals(Boolean.TRUE, Files.getAttribute(cat, ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_EXECUTABLE));
		// TODO more
	}
	//TODO check changes
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
			for (Path path : dirStr) {
				Path p = to.resolve(path.getFileName().toString());
				if (Files.isDirectory(path)) {
					Files.createDirectory(p);
					deepCopy(path, p);
				} else {
					Files.copy(path, p);
					assertEquals(Files.size(path), Files.size(p));
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
	
}
