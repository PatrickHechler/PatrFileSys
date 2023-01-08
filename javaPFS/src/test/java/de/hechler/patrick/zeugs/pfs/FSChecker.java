package de.hechler.patrick.zeugs.pfs;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertNull;
import static de.hechler.patrick.zeugs.check.Assert.assertThrows;
import static de.hechler.patrick.zeugs.check.ParamCreaterHelp.SPLIT_COMMA_OF_INFO;

import java.io.IOException;
import java.lang.foreign.MemorySession;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchProviderException;
import java.util.Map;

import de.hechler.patrick.zeugs.check.ParamCreaterHelp;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.MethodParam;
import de.hechler.patrick.zeugs.check.anotations.MethodParamsParam;
import de.hechler.patrick.zeugs.check.anotations.ParamCreater;
import de.hechler.patrick.zeugs.check.anotations.ParamInfo;
import de.hechler.patrick.zeugs.check.anotations.ResultParam;
import de.hechler.patrick.zeugs.check.anotations.Start;
import de.hechler.patrick.zeugs.check.objects.LogHandler;
import de.hechler.patrick.zeugs.check.objects.Result;
import de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;
import de.hechler.patrick.zeugs.pfs.opts.JavaFSOptions;
import de.hechler.patrick.zeugs.pfs.opts.PatrFSOptions;

@CheckClass
@SuppressWarnings("static-method")
public class FSChecker {
	
	private static final Path                   JAVA_TEST_ROOT = Paths.get("./testout/java.fs/");
	private static final String                 ALL_PROVIDERS  = FSProvider.JAVA_FS_PROVIDER_NAME + "," + FSProvider.PATR_FS_PROVIDER_NAME;
	private static final JavaFSOptions          JAVA_OPTS      = new JavaFSOptions(JAVA_TEST_ROOT);
	private static final PatrFSOptions          PATR_OPTS      = new PatrFSOptions("./testout/patr-fs.pfs", true, 1024L, 1024);
	private static final Map<String, FSOptions> PROVIDER_OPTS  = Map.of(FSProvider.JAVA_FS_PROVIDER_NAME, JAVA_OPTS, FSProvider.PATR_FS_PROVIDER_NAME,
			PATR_OPTS);
	
	@Start(onlyOnce = true)
	private void init() throws IOException {
		Files.createDirectories(JAVA_TEST_ROOT);
		deleteChildren(JAVA_TEST_ROOT);
		System.out.println(PatrFSProvider.class.getModule());
		System.out.println(FSProvider.providerMap());
	}
	
	static {
		try {
			Path src  = Paths.get("/data/git/PatrFileSys/javaPFS/target/");
			Path dest = Path.of("/data/git/PatrFileSys/javaPFS/target-snapshot/");
			deepCopy(src, dest);
			System.out.println("copied successfully");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void deepCopy(Path src, Path dest) throws IOException {
		Files.createDirectory(dest);
		try (DirectoryStream<Path> str = Files.newDirectoryStream(src)) {
			for (Path subSrc : str) {
				Path subDest = dest.resolve(subSrc.getFileName());
				if (Files.isDirectory(subSrc)) {
					deepCopy(subSrc, subDest);
				} else {
					Files.copy(subSrc, subDest);
				}
			}
		}
	}
	
	private void deleteChildren(Path folder) throws IOException {
		try (DirectoryStream<Path> str = Files.newDirectoryStream(folder)) {
			for (Path p : str) {
				if (Files.isDirectory(p)) {
					deleteChildren(p);
				}
				Files.delete(p);
			}
		}
	}
	
	@Start
	private void start(@MethodParam Method met, @MethodParamsParam Object[] params) {
		LogHandler.LOG.info(() -> {
			StringBuilder b = new StringBuilder();
			b.append("start now check: ");
			b.append(met.getName());
			b.append('(');
			for (int i = 0; i < params.length; i++) {
				if (i != 0) {
					b.append(", ");
				}
				b.append(params[i]);
			}
			b.append(')');
			return b.toString();
		});
	}
	
	@End
	private void end(@MethodParam Method met, @MethodParamsParam Object[] params, @ResultParam Result res) {
		LogHandler.LOG.info(() -> {
			StringBuilder b = new StringBuilder();
			b.append("finished check: ");
			b.append(met.getName());
			b.append('(');
			for (int i = 0; i < params.length; i++) {
				if (i != 0) {
					b.append(", ");
				}
				b.append(params[i]);
			}
			b.append(")\nresult: ");
			b.append(res);
			return b.toString();
		});
	}
	
	@Check
	private void simpleCheck( // FIXME
			@ParamCreater(clas = ParamCreaterHelp.class, method = SPLIT_COMMA_OF_INFO, params = Parameter.class) @ParamInfo(ALL_PROVIDERS) String prov)
			throws IOException, NoSuchProviderException {
		try (FS fs = fs(prov)) {
			try (Folder root = fs.cwd()) {
				assertNull(root.childCount());
				assertThrows(true, IllegalStateException.class, () -> root.parent());
				try (File file = root.createFile("testfile.txt");
						WriteStream str = file.openAppend();
						MemorySession mem = MemorySession.openConfined()) {
					assertEquals(1, root.childCount());
					str.write(mem.allocateUtf8String("this file ends with a '\\0'"));
				}
				try (File file = root.createFile("testfile2.txt"); WriteStream str = file.openAppend()) {
					assertEquals(1, root.childCount());
					str.write("this file ends with a '\\n' and not with a '\\0'\n".getBytes(StandardCharsets.UTF_8));
				}
			}
		}
	}
	
	private static FS fs(String provName) throws IOException, NoSuchProviderException {
		FSProvider prov = FSProvider.ofName(provName);
		FSOptions  opts = PROVIDER_OPTS.get(provName);
		return prov.loadFS(opts);
	}
	
}
