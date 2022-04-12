package de.hechler.patrick.pfs.objects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.objects.old.ba.PatrFileSys.FolderElement;
import de.hechler.patrick.pfs.objects.old.ba.PatrFileSys.PatrFile;
import de.hechler.patrick.pfs.objects.old.ba.PatrFileSys.PatrFolder;
import de.hechler.patrick.zeugs.check.Checker;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass
public class PatrFileSysCompareingChecker extends Checker {
	
	PatrFileSys fs;
	
	@Start
	private void start() throws IOException {
		BlockAccessor ba = new BlockAccessorByteArrayArrayImpl(1 << 20, 1024);
		PatrFileSys.format(ba, 1024);
		fs = new PatrFileSys(ba);
	}
	
	@End
	private void end() {
		fs.close();
		fs = null;
	}
	
	@Check
	private void fileSysChecks() throws IOException {
		fileSysCheck("empty");
		end();
		start();
		fileSysCheck("fileSysCheck0");
		end();
		start();
		fileSysCheck("fileSysCheck1");
		end();
		start();
		fileSysCheck("fileSysCheck2");
		end();
		start();
		fileSysCheck("fileSysCheck3");
	}
	
	private void fileSysCheck(String name) {
		System.err.println("name='" + name + "'");
		try {
			Files.createDirectories(Paths.get("./testout/" + name + "/"));
			File dest = Paths.get("./testout/" + name + "/").toFile();
			File source = getResource("/" + name + "/").toFile();
			removeSubs(dest);
			copyToPFS(source, fs.rootFolder(), false);
			copyFromPFS(dest, fs.rootFolder());
			assertEquals(source, dest);
		} catch (IOException e) {
			throw new IOError(e);
		}
	}
	
	private void removeSubs(File f) {
		if (f.isDirectory()) {
			File[] subs = f.listFiles();
			for (int i = 0; i < subs.length; i ++ ) {
				removeSubs(subs[i]);
				subs[i].delete();
			}
		}
	}
	
	private void assertEquals(File a, File b) throws IOException {
		assertEquals(a.getName(), b.getName());
		if (a.isDirectory()) {
			assertTrue(b.isDirectory());
			File[] asubs = a.listFiles();
			File[] bsubs = b.listFiles();
			assertEquals(asubs.length, bsubs.length);
			Arrays.sort(asubs, (af, bf) -> af.getName().compareTo(bf.getName()));
			Arrays.sort(bsubs, (af, bf) -> af.getName().compareTo(bf.getName()));
			for (int i = 0; i < bsubs.length; i ++ ) {
				assertEquals(asubs[i], bsubs[i]);
			}
		} else {
			assertFalse(b.isDirectory());
			byte[] abytes = new byte[1024];
			byte[] bbytes = new byte[1024];
			try (InputStream ain = new FileInputStream(a)) {
				try (InputStream bin = new FileInputStream(b)) {
					while (true) {
						int aread = readBytes(abytes, ain);
						int bread = readBytes(bbytes, bin);
						assertEquals(aread, bread);
						assertArrayEquals(abytes, bbytes);
						int abyte = ain.read();
						int bbyte = bin.read();
						assertEquals(abyte, bbyte);
						if (abyte == -1) {
							break;
						}
					}
				}
			}
		}
	}
	
	private void copyFromPFS(File dest, PatrFolder patrFolder) throws IOException {
		patrFolder.forEachElement(fe -> {
			try {
				int flags = fe.getFlags();
				String name = fe.getName();
				if ( (flags & FolderElement.ELEMENT_FLAG_FILE) != 0) {
					PatrFile file = fe.getFile();
					File destf = new File(dest, name);
					try (OutputStream out = new FileOutputStream(destf)) {
						byte[] bytes = new byte[1024];
						for (long len = file.size(), off = 0; off < len; off += 1024) {
							int read = 1024;
							if ( ((long) read) + off > len) {
								read = (int) (len - off);
							}
							file.read(bytes, 0, off, read);
							out.write(bytes, 0, read);
						}
					}
				} else {
					PatrFolder folder = fe.getFolder();
					File sub = new File(dest, name + "/");
					assertTrue(sub.mkdir());
					copyFromPFS(sub, folder);
				}
			} catch (IOException e) {
				throw new IOError(e);
			}
		});
	}
	
	private void copyToPFS(File source, PatrFolder patrFolder, boolean includeThis) throws OutOfMemoryError, IOException {
		if (source.isDirectory()) {
			PatrFolder folder;
			if (includeThis) {
				FolderElement element = patrFolder.addElement(source.getName(), false);
				folder = element.getFolder();
			} else {
				folder = patrFolder;
			}
			File[] subs = source.listFiles();
			for (int i = 0; i < subs.length; i ++ ) {
				copyToPFS(subs[i], folder, true);
			}
		} else if (includeThis) {
			FolderElement element = patrFolder.addElement(source.getName(), true);
			PatrFile newFile = element.getFile();
			InputStream in = new FileInputStream(source);
			byte[] bytes = new byte[4096];
			while (true) {
				int read = readBytes(bytes, in);
				newFile.append(bytes, 0, 4096);
				if (read < 0) {
					break;
				}
			}
		}
	}
	
	private int readBytes(byte[] bytes, InputStream in) throws IOException {
		int rread = 0;
		while (rread < 1024) {
			int read = in.read(bytes, 0, bytes.length);
			if (read == -1) {
				rread = -read - 1;
				break;
			}
			rread += read;
		}
		return rread;
	}
	
	private Path getResource(String name) {
		URL res = getClass().getResource(name);
		if (res == null) {
			res = getClass().getResource("/resourses" + name);
		}
		String path = res.getPath();
		System.err.println(path);
		return Paths.get(path.substring(1));
	}
	
}
