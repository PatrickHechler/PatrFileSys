package de.hechler.patrick.pfs.objects.java;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class PFSPathImpl implements Path {
	
	public static final String URI_SHEME = "patr_file_sys_uri_sheme";
	
	private final PFSFileSystemImpl fs;
	private final PFSPathImpl       relativeTo;
	private final Name[]            path;
	
	/**
	 * creates an root path
	 * 
	 * @param fs
	 *            the file system of the root
	 */
	public PFSPathImpl(PFSFileSystemImpl fs) {
		this.fs = fs;
		this.path = new Name[0];
		this.relativeTo = this;
	}
	
	public PFSPathImpl(PFSFileSystemImpl fs, PFSPathImpl relativeTo, Name... path) {
		this.fs = fs;
		this.path = path;
		this.relativeTo = relativeTo;
	}
	
	
	public Name[] getNames() {
		return this.path.clone();
	}
	
	@Override
	public PFSFileSystemImpl getFileSystem() {
		return this.fs;
	}
	
	@Override
	public boolean isAbsolute() {
		return this.path[0] == null;
	}
	
	@Override
	public PFSPathImpl getRoot() {
		if (this.relativeTo == this.relativeTo.relativeTo) {
			return this.relativeTo;
		}
		return new PFSPathImpl(fs);
	}
	
	@Override
	public PFSPathImpl getFileName() {
		if (this.path.length == 0) return null;
		else return new PFSPathImpl(this.fs, getParent(), this.path[this.path.length - 1]);
	}
	
	@Override
	public PFSPathImpl getParent() {
		if (this.path.length == 0) return null;
		else return new PFSPathImpl(this.fs, this.relativeTo, Arrays.copyOf(this.path, this.path.length - 1));
	}
	
	@Override
	public int getNameCount() {
		return this.path.length;
	}
	
	@Override
	public Path getName(int index) {
		return new PFSPathImpl(this.fs, new PFSPathImpl(this.fs, this.relativeTo, Arrays.copyOf(this.path, index)), this.path[index]);
	}
	
	@Override
	public Path subpath(int beginIndex, int endIndex) {
		return new PFSPathImpl(this.fs, new PFSPathImpl(this.fs, this.relativeTo, Arrays.copyOf(this.path, beginIndex)), Arrays.copyOfRange(this.path, beginIndex, endIndex));
	}
	
	@Override
	public boolean startsWith(Path other) {
		PFSPathImpl p = getMyPath(other, true);
		if ( !p.fs.equals(this.fs)) {
			return false;
		}
		if (this.path.length < p.path.length) {
			return false;
		}
		for (int i = 0; i < p.path.length; i ++ ) {
			if ( !p.path[i].equals(this.path[i])) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean endsWith(Path other) {
		PFSPathImpl p = getMyPath(other, true);
		if (this.path.length < p.path.length) {
			return false;
		}
		for (int i = 0; i < p.path.length; i ++ ) {
			if ( !p.path[p.path.length - i - 1].equals(this.path[this.path.length - i - 1])) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public PFSPathImpl normalize() {
		List <Name> names = new ArrayList <>(Arrays.asList(this.path));
		boolean changed;
		do {
			changed = false;
			for (int i = 0, s = names.size(); i < s; i ++ ) {
				Name n = names.get(i);
				if (n.name != null) {
					continue;
				} else if (n == Name.NOTHING) {
					names.remove(i);
					i -- ;
				} else if (n == Name.BACK) {
					names.remove(i);
					names.remove(i - 1);
					i -= 2;
				} else {
					throw new InternalError("unknown special name: " + n);
				}
				changed = true;
			}
		} while (changed);
		return new PFSPathImpl(this.fs, this.relativeTo, names.toArray(new Name[names.size()]));
	}
	
	@Override
	public Path resolve(Path other) {
		PFSPathImpl myPath = getMyPath(other, false);
		if (myPath.relativeTo.relativeTo == myPath.relativeTo) {
			return myPath;
		} else if (myPath.path.length == 0) {
			return this;
		} else {
			Name[] arr = Arrays.copyOf(this.path, this.path.length + myPath.path.length);
			System.arraycopy(myPath.path, 0, arr, this.path.length, myPath.path.length);
			return new PFSPathImpl(this.fs, this.relativeTo, arr);
		}
	}
	
	@Override
	public PFSPathImpl relativize(Path other) {
		PFSPathImpl p = getMyPath(other, false);
		int commonStart;
		for (commonStart = 0; commonStart < this.path.length && commonStart < p.path.length; commonStart ++ ) {
			if ( !p.path[commonStart].equals(this.path[commonStart])) {
				break;
			}
		}
		Name[] names = new Name[this.path.length - commonStart + p.path.length - commonStart];
		for (int i = 0; i < commonStart; i ++ ) {
			names[i] = Name.BACK;
		}
		System.arraycopy(p, commonStart, names, commonStart, names.length - commonStart);
		return new PFSPathImpl(this.fs, this, names);
	}
	
	@Override
	public URI toUri() {
		try {
			PFSPathImpl abs = toAbsolutePath();
			String pathStr = abs.toString();
			URI uri = new URI(URI_SHEME, null, pathStr, null);
			return uri;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public PFSPathImpl toAbsolutePath() {
		List <PFSPathImpl> paths = new ArrayList <>();
		PFSPathImpl p = this;
		while (p.relativeTo != p) {
			paths.add(p);
			p = p.relativeTo;
		}
		List <Name> names = new ArrayList <>();
		for (int i = paths.size() - 1; i >= 0; i -- ) {
			names.addAll(Arrays.asList(paths.get(i).path));
		}
		return new PFSPathImpl(this.fs, p, names.toArray(new Name[names.size()]));
	}
	
	@Override
	public PFSPathImpl toRealPath(LinkOption... options) throws IOException {
		PFSPathImpl abs = toAbsolutePath();
		PFSPathImpl result = abs.normalize();
		return result;
	}
	
	@Override
	public WatchKey register(WatchService watcher, Kind <?>[] events, Modifier... modifiers) throws IOException {
		System.err.println("register(...) is called!");
		System.err.println("  this: " + this);
		System.err.println("  stack Trace:");
		new Throwable().printStackTrace(System.err);
		return null;
	}
	
	@Override
	public int compareTo(Path other) {
		PFSPathImpl p = getMyPath(other, true);
		for (int i = 0; i < this.path.length && i < p.path.length; i ++ ) {
			int cmp = this.path[i].compareTo(p.path[i]);
			if (cmp != 0) {
				return cmp;
			}
		}
		if (this.path.length > p.path.length) {
			return -1;
		} else if (this.path.length < p.path.length) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public PFSPathImpl getMyPath(Path other, boolean checkFS) {
		PFSPathImpl myPath = getMyPath(other);
		if (checkFS) {
			if ( !myPath.fs.equals(this.fs)) {
				throw new IllegalArgumentException("I can not work with a path from a different file system!");
			}
		}
		return myPath;
	}
	
	public static PFSPathImpl getMyPath(Path path) {
		if ( ! (path instanceof PFSPathImpl)) {
			throw new IllegalArgumentException("I can not work with an unknown path impl! " + path.getClass());
		}
		PFSPathImpl myPath = (PFSPathImpl) path;
		return myPath;
	}
	
	@Override
	public String toString() {
		StringBuilder build = new StringBuilder();
		if (this.relativeTo == this.relativeTo.relativeTo) {
			build.append('/');
		}
		if (this.path.length > 0) {
			build.append(this.path[0]);
			for (int i = 1; i < this.path.length; i ++ ) {
				build.append('/').append(this.path[i]);
			}
		}
		return build.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( (fs == null) ? 0 : fs.hashCode());
		result = prime * result + Arrays.hashCode(path);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PFSPathImpl other = (PFSPathImpl) obj;
		if (fs == null) {
			if (other.fs != null) return false;
		} else if ( !fs.equals(other.fs)) return false;
		if ( !Arrays.equals(path, other.path)) return false;
		return true;
	}
	
	public static final class Name implements Comparable <Name> {
		
		public static final Name BACK    = new Name(null);
		public static final Name NOTHING = new Name(null);
		
		public final String name;
		
		private Name(String name) {
			this.name = name;
		}
		
		public static Name create(String name) {
			Objects.requireNonNull(name, "no custom null names are allowed");
			return new Name(name);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( (name == null) ? 0 : name.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Name other = (Name) obj;
			if (name == null) return false;
			else if (other.name == null) return false;
			else if ( !name.equals(other.name)) return false;
			return true;
		}
		
		@Override
		public String toString() {
			if (name != null) return name;
			else if (this == BACK) return "..";
			else if (this == NOTHING) return ".";
			else throw new InternalError("I don't know what I am!");
		}
		
		@Override
		public int compareTo(Name o) {
			if (this == o) {
				return 0;
			} else if (name != null) {
				if (o.name != null) {
					return name.compareTo(o.name);
				} else {
					return 1;
				}
			} else if (o.name != null) {
				return -1;
			} else if (this == NOTHING) {
				assert o == BACK;
				return 1;
			} else {
				assert this == BACK;
				assert o == NOTHING;
				return 1;
			}
		}
		
	}
	
}
