package de.hechler.patrick.zeugs.pfs.java.impl;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatrPath implements Path {

	private final PatrFileSystem fs;
	private final String[] segs;
	private final boolean abs;

	public PatrPath(PatrFileSystem fs, String[] path, boolean absolute) {
		this.fs = fs;
		this.segs = path;
		this.abs = absolute;
	}

	public static Path create(PatrFileSystem fs, String path) {
		if (path.isEmpty()) {
			return new PatrPath(fs, new String[0], false);
		}
		boolean abs = path.charAt(0) == '/';
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < path.length();) {
			int slash = path.indexOf('/', i);
			slash = slash == -1 ? path.length() : slash;
			paths.add(path.substring(i, slash));
			i = slash + 1;
		}
		return new PatrPath(fs, paths.toArray(new String[paths.size()]), abs);
	}

	public static PatrPath path(Path path) {
		if (path == null) {
			throw new NullPointerException("path is null");
		} else if (path instanceof PatrPath res) {
			return res;
		} else {
			throw new ProviderMismatchException("unknown path class: " + path.getClass().getName());
		}
	}

	@Override
	public FileSystem getFileSystem() {
		return fs;
	}

	@Override
	public boolean isAbsolute() {
		return abs;
	}

	@Override
	public Path getRoot() {
		if (abs) {
			return new PatrPath(fs, new String[0], abs);
		} else {
			return null;
		}
	}

	@Override
	public Path getFileName() {
		if (segs.length > 0) {
			return new PatrPath(fs, new String[] { segs[segs.length - 1] }, false);
		} else {
			return null;
		}
	}

	@Override
	public Path getParent() {
		if (segs.length > (abs ? 0 : 1)) {
			return new PatrPath(fs, Arrays.copyOf(segs, segs.length - 1), abs);
		} else {
			return null;
		}
	}

	@Override
	public int getNameCount() {
		return abs ? segs.length + 1 : segs.length;
	}

	@Override
	public Path getName(int index) {
		if (index < 0) {
			throw new IndexOutOfBoundsException("negative index: " + index);
		}
		if (abs) {
			if (index == 0) {
				return new PatrPath(fs, new String[0], true);
			} else {
				return new PatrPath(fs, new String[] { segs[index - 1] }, false);
			}
		} else {
			return new PatrPath(fs, new String[] { segs[index] }, false);
		}
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		if (beginIndex > endIndex || beginIndex < 0) {
			throw new IndexOutOfBoundsException("beginIndex=" + beginIndex + " endIndex=" + endIndex);
		}
		String[] res = new String[abs && beginIndex == 0 ? endIndex - beginIndex : (beginIndex - endIndex + 1)];
		System.arraycopy(segs, Math.min(0, abs ? beginIndex - 1 : beginIndex), res, 0, res.length);
		boolean a = abs && beginIndex == 0;
		return new PatrPath(fs, res, a);
	}

	@Override
	public boolean startsWith(Path other) {
		try {
			PatrPath pp = path(other);
			if (!pp.abs && this.abs) {
				return false;
			} else if (this.segs.length > pp.segs.length) {
				return false;
			} else {
				return Arrays.equals(this.segs, 0, pp.segs.length, pp.segs, 0, pp.segs.length);
			}
		} catch (ProviderMismatchException pme) {
			return false;
		}
	}

	@Override
	public boolean endsWith(Path other) {
		try {
			PatrPath pp = path(other);
			if (pp.abs && !this.abs) {
				return false;
			} else if (this.segs.length < pp.segs.length) {
				return false;
			} else {
				return Arrays.equals(this.segs, this.segs.length - pp.segs.length, pp.segs.length, pp.segs, 0,
						pp.segs.length);
			}
		} catch (ProviderMismatchException pme) {
			return false;
		}
	}

	@Override
	public Path normalize() {
		String[] result = new String[segs.length];
		int ri = 0;
		for (int i = 0; i < segs.length; i++) {
			switch (segs[i]) {
			case ".":
			case "":
				break;
			case "..":
				if (--ri > 0) {
					if (abs) {
						throw new IllegalStateException("the root folder has no parent! (" + this + ")");
					} else {
						ri++;
					}
				} else {
					break;
				}
			default:
				result[ri++] = segs[i];
			}
		}
		if (ri < result.length) {
			result = Arrays.copyOf(result, ri);
		} else {
			assert ri == result.length;
		}
		return new PatrPath(fs, result, abs);
	}

	@Override
	public Path resolve(Path other) {
		PatrPath pp = path(other);
		if (pp.abs) {
			return pp;
		}
		String[] result = new String[segs.length + pp.segs.length];
		System.arraycopy(segs, 0, result, 0, segs.length);
		System.arraycopy(pp.segs, 0, result, segs.length, pp.segs.length);
		return new PatrPath(fs, result, abs);
	}

	@Override
	public Path relativize(Path other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI toUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path toAbsolutePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int compareTo(Path other) {
		// TODO Auto-generated method stub
		return 0;
	}

}
