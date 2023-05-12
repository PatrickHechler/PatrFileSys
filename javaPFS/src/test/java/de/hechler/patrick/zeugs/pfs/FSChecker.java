//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.zeugs.pfs;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertNull;
import static de.hechler.patrick.zeugs.check.Assert.assertThrows;
import static de.hechler.patrick.zeugs.check.ParamCreaterHelp.SPLIT_COMMA_OF_INFO;

import java.io.IOException;
import java.lang.foreign.Arena;
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
@SuppressWarnings({ "static-method", "javadoc" })
public class FSChecker {
	
	private static final String ALL_PROVIDERS   =  FSProvider.PATR_FS_PROVIDER_NAME + ',' + FSProvider.JAVA_FS_PROVIDER_NAME;
	private static final Path   TEST_ROOT       = Paths.get("./testout/");
	private static final Path   JAVA_TEST_ROOT  = Paths.get("./testout/java-fs/");
	private static final String PATR_FS_TESTOUT = "./testout/patr-fs/";
	private static final Path   PFS_TEST_ROOT   = Paths.get(PATR_FS_TESTOUT);
	
	private JavaFSOptions          javaOpts;
	private PatrFSOptions          patrOpts;
	private Map<String, FSOptions> providerOpts;
	
	@Start(onlyOnce = true)
	private void init() throws IOException {
		Files.createDirectories(TEST_ROOT);
		deleteChildren(TEST_ROOT);
		Files.createDirectory(JAVA_TEST_ROOT);
		Files.createDirectory(PFS_TEST_ROOT);
		System.out.println(PatrFSProvider.class.getModule());
		System.out.println(FSProvider.providerMap());
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
	private void start(@MethodParam Method met, @MethodParamsParam Object[] params) throws IOException {
		Path jtr = JAVA_TEST_ROOT.resolve(met.getName());
		if (!Files.exists(jtr)) Files.createDirectory(jtr);
		this.javaOpts     = new JavaFSOptions(jtr);
		this.patrOpts     = new PatrFSOptions(PFS_TEST_ROOT + met.getName() + ".pfs", true, 1024L, 1024);
		this.providerOpts = Map.of(FSProvider.JAVA_FS_PROVIDER_NAME, this.javaOpts, FSProvider.PATR_FS_PROVIDER_NAME, this.patrOpts);
		LogHandler.LOG.info(() -> {
			StringBuilder b = new StringBuilder();
			b.append("start now check: ");
			appendMethod(met, params, b);
			b.append(')');
			return b.toString();
		});
	}
	
	@End
	private void end(@MethodParam Method met, @MethodParamsParam Object[] params, @ResultParam Result res) {
		LogHandler.LOG.info(() -> {
			StringBuilder b = new StringBuilder();
			b.append("finished check: ");
			appendMethod(met, params, b);
			b.append(")\nresult: ");
			b.append(res);
			if (res.badResult()) res.getErr().printStackTrace();
			return b.toString();
		});
	}
	
	private void appendMethod(Method met, Object[] params, StringBuilder b) {
		b.append(met.getName());
		b.append('(');
		for (int i = 0; i < params.length; i++) {
			if (i != 0) {
				b.append(", ");
			}
			b.append(params[i]);
		}
	}
	
	@Check
	private void
		simpleCheck(@ParamCreater(clas = ParamCreaterHelp.class, method = SPLIT_COMMA_OF_INFO, params = Parameter.class) @ParamInfo(ALL_PROVIDERS) String prov)
			throws IOException, NoSuchProviderException {
		try (FS fs = fs(prov)) {
			try (Folder root = fs.folder("/")) {
				assertNull(root.childCount());
				assertThrows(true, IllegalStateException.class, () -> root.parent());
				try (File file = root.createFile("testfile.txt"); WriteStream str = file.openAppend(); Arena mem = Arena.openConfined()) {
					assertEquals(1, root.childCount());
					str.write(mem.allocateUtf8String("this file ends with a '\\0'"));
				}
				try (File file = root.createFile("testfile2.txt"); WriteStream str = file.openAppend()) {
					assertEquals(2, root.childCount());
					str.write("this file ends with a '\\n' and not with a '\\0'\n".getBytes(StandardCharsets.UTF_8));
				}
			}
		}
	}
	
	@Check
	private void
		writeCheck(@ParamCreater(clas = ParamCreaterHelp.class, method = SPLIT_COMMA_OF_INFO, params = Parameter.class) @ParamInfo(ALL_PROVIDERS) String prov)
			throws IOException, NoSuchProviderException {
		try (FS fs = fs(prov); Folder root = fs.folder("/"); File file = root.createFile("file")) {
			byte[] fl = "this is a text\n".getBytes(StandardCharsets.UTF_8);
			byte[] sl = "this is the second line".getBytes(StandardCharsets.UTF_8);
			try (WriteStream write = file.openWrite()) {
				write.write(fl);
				write.write(sl);
			}
			assertEquals(fl.length + (long) sl.length, file.length());
			file.delete();
		}
	}
	
	private FS fs(String provName) throws IOException, NoSuchProviderException {
		FSProvider prov = FSProvider.ofName(provName);
		FSOptions  opts = this.providerOpts.get(provName);
		return prov.loadFS(opts);
	}
	
}
