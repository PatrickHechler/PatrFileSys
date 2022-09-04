package de.hechler.patrick.pfs.objects.jfs;

import static de.hechler.patrick.pfs.utils.JavaPFSConsants.*;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.ATTR_VIEW_PATR;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_CREATION_TIME;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_FILE_KEY;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_DIRECTORY;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_OTHER;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_REGULAR_FILE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_LAST_ACCESS_TIME;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_LAST_MODIFIED_TIME;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_SIZE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_DO_FORMATT;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.NEW_FILE_SYS_ENV_ATTR_FILE_SYS;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.PATR_VIEW_ATTR_EXECUTABLE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.PATR_VIEW_ATTR_HIDDEN;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.PATR_VIEW_ATTR_READ_ONLY;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.URI_SHEME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_LOCKED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_DELETE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.nio.file.ExtendedCopyOption;
import com.sun.nio.file.ExtendedOpenOption;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileAttributeView;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingConsumer;
import de.hechler.patrick.pfs.objects.ba.ByteArrayArrayBlockAccessor;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImpl;
import de.hechler.patrick.pfs.objects.jfs.PFSPathImpl.Name;

@SuppressWarnings("restriction")
public class PFSFileSystemProviderImpl extends FileSystemProvider {
	
	
	private final Map <URI, PFSFileSystemImpl> created;
	private PFSFileSystemImpl                  def;
	private Set <PatrFile>                     delOnClose;
	private final boolean                      allowAutoDefault;
	private final boolean                      firstIsDefault;
	
	public PFSFileSystemProviderImpl() {
		this(null);
	}
	
	public PFSFileSystemProviderImpl(PatrFileSystem fs) {
		this(fs, false, true);
	}
	
	public PFSFileSystemProviderImpl(PatrFileSystem fs, boolean allowAutoDefault, boolean firstIsDefault) {
		this.def = fs == null ? null : new PFSFileSystemImpl(this, fs);
		this.created = new HashMap <>();
		this.delOnClose = new HashSet <>();
		this.allowAutoDefault = allowAutoDefault;
		this.firstIsDefault = firstIsDefault;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			synchronized (delOnClose) {
				IOException err = null;
				for (PatrFile patrFile : delOnClose) {
					try {
						patrFile.withLock(() -> {
							if ( (patrFile.getLockData() & LOCK_NO_DELETE_ALLOWED_LOCK) != 0) {
								patrFile.removeLock(NO_LOCK);
							}
							PatrFolder parent = patrFile.getParent();
							if ( (parent.getLockData() & LOCK_NO_WRITE_ALLOWED_LOCK) != 0) {
								parent.removeLock(NO_LOCK);
							}
							patrFile.delete(NO_LOCK, NO_LOCK);
						});
					} catch (IOException e) {
						if (err != null) {
							err.addSuppressed(e);
						} else {
							err = e;
						}
					}
				}
				if (err != null) {
					throw new RuntimeException(err);
				}
			}
		}));
	}
	
	private final PatrFileSysImpl newRamFS() {
		ByteArrayArrayBlockAccessor ba = new ByteArrayArrayBlockAccessor(1 << 10, 1 << 10);
		PatrFileSysImpl fs = new PatrFileSysImpl(ba);
		try {
			fs.format(1 << 10, 1 << 10);
		} catch (IOException e) {
			assert false;
			throw new RuntimeException(e);
		}
		return fs;
	}
	
	public FileSystem initDefault(PatrFileSystem pfs) throws NullPointerException, IllegalStateException {
		if (pfs == null) {
			throw new NullPointerException("patr-file-system is null");
		}
		synchronized (this) {
			if (this.def != null) {
				throw new IllegalStateException("a default file system is already spezified!");
			}
			this.def = new PFSFileSystemImpl(this, pfs);
		}
		return this.def;
	}
	
	@Override
	public String getScheme() {
		return URI_SHEME;
	}
	
	@Override
	@SuppressWarnings("resource")
	public PFSFileSystemImpl newFileSystem(URI uri, Map <String, ?> env) throws IOException, FileSystemAlreadyExistsException, IllegalArgumentException {
		synchronized (this.created) {
			if (this.created.containsKey(uri)) {
				throw new FileSystemAlreadyExistsException("uri: '" + uri + "' env: '" + env + "'");
			}
			Object fileSystem = env.get(NEW_FILE_SYS_ENV_ATTR_FILE_SYS);
			Object blockCount = env.get(NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT);
			Object blockSize = env.get(NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE);
			PFSFileSystemImpl fileSys;
			if (fileSystem != null) {
				if ( ! (fileSystem instanceof PatrFileSystem)) {
					throw new IllegalArgumentException(
						"the file system is not of type PatrFileSystem! cls: " + fileSystem.getClass().getName() + " fileSystem: '" + fileSystem + "'");
				}
				Object doFormatt = env.get(NEW_FILE_SYS_ENV_ATTR_DO_FORMATT);
				boolean formatt = false;
				if (doFormatt != null) {
					if ( ! (doFormatt instanceof Boolean)) {
						throw new IllegalArgumentException("do formatt is not of type Boolean! cls: " + doFormatt.getClass().getName() + " doFormatt: '" + doFormatt + "'");
					}
					formatt = (boolean) doFormatt;
				}
				PatrFileSystem fs = (PatrFileSystem) fileSystem;
				long bc = -1L;
				if (blockCount != null) {
					if ( ! (blockCount instanceof Long)) {
						throw new IllegalArgumentException("block count is not of type Long! cls: " + blockCount.getClass().getName() + " blockCount: '" + blockCount + "'");
					}
					bc = (long) blockCount;
					if ( !formatt) {
						if (bc != fs.blockCount()) {
							throw new IllegalArgumentException("block count is set, format is not set to true and the spezified file system has a diffrent block count!");
						}
					}
				}
				int bs = -1;
				if (blockSize != null) {
					if ( ! (blockSize instanceof Integer)) {
						throw new IllegalArgumentException("block size is not of type Integer! cls: " + blockCount.getClass().getName() + " blockCount: '" + blockCount + "'");
					}
					bs = (int) blockSize;
					if ( !formatt) {
						if (bs != fs.blockSize()) {
							throw new IllegalArgumentException("block size is set, format is not set to true and the spezified file system has a diffrent block size!");
						}
					}
				}
				if (formatt) {
					fs.format(bc, bs);
				}
				fileSys = new PFSFileSystemImpl(this, fs);
			} else {
				Object ramfs = env.get(NEW_FILE_SYS_ENV_ATTR_RAM_FS);
				boolean rfs = false;
				if (ramfs != null) {
					if ( ! (ramfs instanceof Boolean)) {
						throw new IllegalArgumentException("ram-fs is not of type Boolean! cls: " + ramfs.getClass().getName() + " ramfs: '" + ramfs + "'");
					}
					rfs = (boolean) ((Boolean) ramfs);
				}
				int bs;
				if (blockSize != null) {
					if ( ! (blockSize instanceof Integer)) {
						throw new IllegalArgumentException("block size is not of type Integer! cls: " + blockSize.getClass().getName() + " blockSize: '" + blockSize + "'");
					}
					bs = (int) blockSize;
				} else {
					bs = 1 << 12;
				}
				int bc;
				if (blockCount != null) {
					if ( ! (blockCount instanceof Integer)) {
						throw new IllegalArgumentException("block count is not of type Long! cls: " + blockCount.getClass().getName() + " blockCount: '" + blockCount + "'");
					}
					bc = (int) blockCount;
				} else {
					bc = 1 << 16;
				}
				BlockAccessor ba;
				boolean formatt;
				if (rfs) {
					ba = new ByteArrayArrayBlockAccessor(bc, bs);
					formatt = true;
				} else {
					formatt = false;
					Object doFormatt = env.get(NEW_FILE_SYS_ENV_ATTR_DO_FORMATT);
					if (doFormatt != null) {
						if ( ! (doFormatt instanceof Boolean)) {
							throw new IllegalArgumentException("do formatt is not of type Boolean! cls: " + doFormatt.getClass().getName() + " doFormatt: '" + doFormatt + "'");
						}
						formatt = (boolean) (Boolean) doFormatt;
					}
					String up = uri.getPath();
					Path path;
					if (up.indexOf(':') == -1) {
						path = Paths.get(up);
					} else {
						path = Paths.get(URI.create(up.substring(1)));
					}
					SeekableByteChannel channel;
					if (formatt || doFormatt == null && !Files.exists(path)) {
						formatt = true;
						channel = Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
					} else {
						channel = Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
					}
					ba = new SeekablePathBlockAccessor(channel, bs);
				}
				PatrFileSystem fs = new PatrFileSysImpl(ba);
				if (formatt) {
					fs.format(bc, bs);
				}
				fileSys = new PFSFileSystemImpl(this, fs);
			}
			this.created.put(uri, fileSys);
			if (firstIsDefault && def == null) {
				synchronized (this) {
					if (def == null) {
						def = fileSys;
					}
				}
			}
			return fileSys;
		}
	}
	
	@Override
	public PFSFileSystemImpl getFileSystem(URI uri) throws FileSystemNotFoundException {
		synchronized (this.created) {
			PFSFileSystemImpl fs = this.created.get(uri);
			if (fs != null) return fs;
			else throw new FileSystemNotFoundException("uri: " + uri);
		}
	}
	
	@Override
	public Path getPath(URI uri) {
		String str = uri.getPath();
		if (def != null) {
			synchronized (this) {
				if (def == null) {
					if (allowAutoDefault) {
						def = new PFSFileSystemImpl(this, newRamFS());
					} else {
						throw new UnsupportedOperationException("no default file system is spezified");
					}
				}
			}
		}
		Path path = def.getPath(str);
		return path;
	}
	
	public static final int MODE_READ = 1, MODE_WRITE = 2, MODE_APPEND = 4;
	
	@Override
	public SeekableByteChannel newByteChannel(Path path, Set <? extends OpenOption> options, FileAttribute <?>... attrs) throws IOException {
		options = new HashSet <>(options);
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFileSystem fs = p.getFileSystem().getFileSys();
		PatrFolder directParent = getFolder(fs.getRoot(), strs, strs.length - 1);
		int mode = 0;
		if (options.contains(StandardOpenOption.WRITE)) {
			mode = MODE_WRITE;
		}
		if (options.contains(StandardOpenOption.APPEND)) {
			mode |= MODE_APPEND | MODE_WRITE;
		}
		if (options.contains(StandardOpenOption.READ)) {
			mode |= MODE_READ;
		}
		if (mode == 0) {
			mode = MODE_READ;
		}
		if (mode == MODE_READ) {
			options.remove(StandardOpenOption.CREATE);
			options.remove(StandardOpenOption.CREATE_NEW);
			options.remove(StandardOpenOption.TRUNCATE_EXISTING);
		} else if (options.contains(StandardOpenOption.CREATE_NEW)) {
			options.remove(StandardOpenOption.CREATE);
		}
		PatrFile file = extractFile(options, strs, directParent, attrs);
		long lock = extractLock(options);
		if (lock != NO_LOCK) {
			lock = file.lock(lock);
		}
		if (options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
			file.removeContent(lock);
		}
		ThrowingConsumer <? extends IOException, PatrFile> onClose;
		if (options.contains(StandardOpenOption.DELETE_ON_CLOSE)) {
			final long finallock = lock;
			synchronized (delOnClose) {
				delOnClose.add(file);
			}
			onClose = (patrFile) -> {
				synchronized (delOnClose) {
					patrFile.delete(finallock, NO_LOCK);
					delOnClose.remove(patrFile);
				}
			};
		} else {
			onClose = (patrFile) -> {};
		}
		return new PFSByteChannelImpl(lock, strs, file, onClose, mode);
	}
	
	private long extractLock(Set <? extends OpenOption> options) {
		long lock = NO_LOCK;
		if (options.contains(ExtendedOpenOption.NOSHARE_READ)) {
			lock |= LOCK_LOCKED_LOCK | LOCK_NO_READ_ALLOWED_LOCK;
		}
		if (options.contains(ExtendedOpenOption.NOSHARE_WRITE)) {
			lock |= LOCK_LOCKED_LOCK | LOCK_NO_WRITE_ALLOWED_LOCK;
		}
		if (options.contains(ExtendedOpenOption.NOSHARE_DELETE)) {
			lock |= LOCK_LOCKED_LOCK | LOCK_NO_DELETE_ALLOWED_LOCK;
		}
		return lock;
	}
	
	private PatrFile extractFile(Set <? extends OpenOption> options, String[] strs, PatrFolder directParent, FileAttribute <?>... attrs)
		throws IOException, ElementLockedException, FileAlreadyExistsException, NoSuchFileException {
		PatrFile file;
		try {
			file = directParent.getElement(strs[strs.length - 1], NO_LOCK).getFile();
			if (options.contains(StandardOpenOption.CREATE_NEW)) {
				throw new FileAlreadyExistsException(buildName(strs, strs.length - 1));
			}
			if (options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
				file.removeContent(NO_LOCK);
			}
		} catch (NoSuchFileException e) {
			if (options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW)) {
				file = directParent.addFile(strs[strs.length - 1], NO_LOCK);
				for (FileAttribute <?> attr : attrs) {
					String atr = supportsView(attr.name());
					switch (atr) {
					case PATR_VIEW_ATTR_EXECUTABLE:
						file.setExecutable(booleanValue(attr), NO_LOCK);
						break;
					case PATR_VIEW_ATTR_HIDDEN:
						file.setHidden(booleanValue(attr), NO_LOCK);
						break;
					case PATR_VIEW_ATTR_READ_ONLY:
						file.setReadOnly(booleanValue(attr), NO_LOCK);
						break;
					case BASIC_ATTRIBUTE_CREATION_TIME: {
						FileTime time = value(attr, FileTime.class);
						file.setCreateTime(time.toMillis(), NO_LOCK);
						break;
					}
					case BASIC_ATTRIBUTE_FILE_KEY:
						throw new UnsupportedOperationException("can't set file to file-key");
					case BASIC_ATTRIBUTE_IS_DIRECTORY:
						if (booleanValue(attr)) {
							throw new UnsupportedOperationException("can't set file to folder");
						}
						break;
					case BASIC_ATTRIBUTE_IS_OTHER:
						if (booleanValue(attr)) {
							throw new UnsupportedOperationException("can't set file to other");
						}
						break;
					case BASIC_ATTRIBUTE_IS_REGULAR_FILE:
						if ( !booleanValue(attr)) {
							throw new UnsupportedOperationException("can't set file to non file");
						}
						break;
					case BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK:
						if (booleanValue(attr)) {
							throw new UnsupportedOperationException("can't set file to sym-link");
						}
						break;
					case BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
						throw new UnsupportedOperationException("can't set last-access-time");
					case BASIC_ATTRIBUTE_LAST_MODIFIED_TIME: {
						FileTime time = value(attr, FileTime.class);
						file.setLastMetaModTime(time.toMillis(), NO_LOCK);
						file.setLastModTime(time.toMillis(), NO_LOCK);
						break;
					}
					case BASIC_ATTRIBUTE_SIZE:
						throw new UnsupportedOperationException("can't set size");
					default:
						throw new UnsupportedOperationException("unknown attribute type: " + attr.name());
					}
				}
			} else {
				throw e;
			}
		}
		return file;
	}
	
	@Override
	public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
		Set <OpenOption> opts = new HashSet <>(Arrays.asList(options));
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFileSystem fs = p.getFileSystem().getFileSys();
		PatrFolder directParent = getFolder(fs.getRoot(), strs, strs.length - 1);
		if (opts.contains(StandardOpenOption.WRITE)) {}
		boolean append = opts.contains(StandardOpenOption.APPEND);
		if (opts.contains(StandardOpenOption.READ)) {
			throw new IllegalArgumentException("READ not allowed");
		}
		if (opts.contains(StandardOpenOption.CREATE_NEW)) {
			opts.remove(StandardOpenOption.CREATE);
		}
		PatrFile file = extractFile(opts, strs, directParent);
		long _lock = extractLock(opts);
		final long lock;
		if (_lock == NO_LOCK) lock = NO_LOCK;
		else lock = file.lock(_lock);
		if (opts.contains(StandardOpenOption.DELETE_ON_CLOSE)) {
			synchronized (delOnClose) {
				delOnClose.add(file);
			}
			return new OutputStream() {
				
				final OutputStream f = file.openOutput(append, lock);
				boolean            c;
				
				@Override
				public void write(int b) throws IOException {
					f.write(b);
				}
				
				@Override
				public void write(byte[] b) throws IOException {
					f.write(b);
				}
				
				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					f.write(b, off, len);
				}
				
				@Override
				public void flush() throws IOException {
					f.flush();
				}
				
				@Override
				public void close() throws IOException {
					if (c) return;
					c = true;
					try {
						f.close();
					} catch (IOException e) {
						c = false;
						throw e;
					}
					synchronized (delOnClose) {
						file.delete(lock, NO_LOCK);
						delOnClose.remove(file);
					}
				}
				
			};
		}
		return file.openOutput(append, lock);
	}
	
	@Override
	public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
		Set <OpenOption> opts = new HashSet <>(Arrays.asList(options));
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFileSystem fs = p.getFileSystem().getFileSys();
		PatrFolder directParent = getFolder(fs.getRoot(), strs, strs.length - 1);
		if (opts.contains(StandardOpenOption.WRITE)) {
			throw new UnsupportedOperationException("WRITE not allowed");
		}
		if (opts.contains(StandardOpenOption.APPEND)) {
			throw new UnsupportedOperationException("APPEND not allowed");
		}
		if (opts.contains(StandardOpenOption.READ)) {}
		if (opts.contains(StandardOpenOption.CREATE_NEW)) {
			opts.remove(StandardOpenOption.CREATE);
		}
		PatrFile file = extractFile(opts, strs, directParent);
		long _lock = extractLock(opts);
		final long lock;
		if (_lock == NO_LOCK) lock = NO_LOCK;
		else lock = file.lock(_lock);
		if (opts.contains(StandardOpenOption.DELETE_ON_CLOSE)) {
			synchronized (delOnClose) {
				delOnClose.add(file);
			}
			return new InputStream() {
				
				final InputStream f = file.openInput(lock);
				boolean           c;
				
				@Override
				public int read() throws IOException {
					return f.read();
				}
				
				@Override
				public int read(byte[] b) throws IOException {
					return f.read(b);
				}
				
				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					return f.read(b, off, len);
				}
				
				@Override
				public int available() throws IOException {
					return f.available();
				}
				
				@Override
				public synchronized void mark(int readlimit) {
					f.mark(readlimit);
				}
				
				@Override
				public synchronized void reset() throws IOException {
					f.reset();
				}
				
				@Override
				public long skip(long n) throws IOException {
					return f.skip(n);
				}
				
				@Override
				public boolean markSupported() {
					return f.markSupported();
				}
				
				@Override
				public void close() throws IOException {
					if (c) return;
					c = true;
					try {
						f.close();
					} catch (IOException e) {
						c = false;
						throw e;
					}
					synchronized (delOnClose) {
						file.delete(lock, NO_LOCK);
						delOnClose.remove(file);
					}
				}
				
			};
		}
		return file.openInput(lock);
	}
	
	private static boolean booleanValue(FileAttribute <?> attr) {
		Object val = attr.value();
		if (val != null && val instanceof Boolean) {
			return (boolean) (Boolean) val;
		} else {
			throw new UnsupportedOperationException("unknown file attribute value: name='" + attr.name() + "' value='" + val + "' (expected non null Boolean value)");
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <R, T extends Throwable> R value(FileAttribute <?> attr, Class <R> cls) throws T, NullPointerException {
		Object value = attr.value();
		if (value == null) {
			throw new NullPointerException("value is null (name=" + attr.name() + ')');
		}
		if (cls.isInstance(value)) {
			return (R) value;
		}
		throw new UnsupportedOperationException("no " + cls.getSimpleName() + " value: " + value.getClass().getName() + " name=" + attr.name() + " toString: '" + value + "'");
	}
	
	@Override
	public DirectoryStream <Path> newDirectoryStream(Path dir, Filter <? super Path> filter) throws IOException {
		PFSPathImpl p = PFSPathImpl.getMyPath(dir);
		String[] names = getPath(p);
		PatrFolder folder = getFolder(p.getFileSystem().getFileSys().getRoot(), names, names.length);
		long lock = LOCK_SHARED_LOCK | LOCK_NO_DELETE_ALLOWED_LOCK;
		try {
			lock = folder.lock(lock);
		} catch (ElementLockedException e) {
			lock = NO_LOCK;
		}
		return new PFSDirectoryStreamImpl(p.getFileSystem(), lock, folder, filter, p.getNames());
	}
	
	@Override
	public void createDirectory(Path dir, FileAttribute <?>... attrs) throws IOException {
		Set <?> set = new HashSet <>(Arrays.asList(attrs));
		PFSPathImpl p = PFSPathImpl.getMyPath(dir);
		String[] names = getPath(p);
		PatrFolder folder = getFolder(p.getFileSystem().getFileSys().getRoot(), names, names.length - 1);
		folder.withLock(() -> executeCreateDirectory(set, names, folder, attrs));
	}
	
	private void executeCreateDirectory(Set <?> set, String[] names, PatrFolder folder, FileAttribute <?>... attrs) throws IOException, ElementLockedException {
		PatrFolder added = folder.addFolder(names[names.length - 1], NO_LOCK);
		for (int i = 0; i < attrs.length; i ++ ) {
			FileAttribute <?> attr = attrs[i];
			switch (attr.name()) {
			case ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_CREATION_TIME:
			case BASIC_ATTRIBUTE_CREATION_TIME:
				added.setCreateTime(value(attr, FileTime.class).toMillis(), NO_LOCK);
				break;
			case ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_FILE_KEY:
			case BASIC_ATTRIBUTE_FILE_KEY:
				throw new UnsupportedOperationException("file key can not be set");
			case ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_IS_DIRECTORY:
			case BASIC_ATTRIBUTE_IS_DIRECTORY:
				if ( !booleanValue(attr)) {
					throw new UnsupportedOperationException("can not set directory to non dir");
				}
				break;
			case ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_IS_OTHER:
			case BASIC_ATTRIBUTE_IS_OTHER:
				if (booleanValue(attr)) {
					throw new UnsupportedOperationException("can not set directory to other");
				}
				break;
			case ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_IS_REGULAR_FILE:
			case BASIC_ATTRIBUTE_IS_REGULAR_FILE:
				if (booleanValue(attr)) {
					throw new UnsupportedOperationException("can not set directory to file");
				}
				break;
			case ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK:
			case BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK:
				if (booleanValue(attr)) {
					throw new UnsupportedOperationException("can not set directory to sym-link");
				}
				break;
			case ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
			case BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
				throw new UnsupportedOperationException("last-access-time is not supported");
			case ATTR_VIEW_BASIC + ':' + BASIC_ATTRIBUTE_LAST_MODIFIED_TIME:
			case BASIC_ATTRIBUTE_LAST_MODIFIED_TIME:
				added.setLastModTime(value(attr, FileTime.class).toMillis(), NO_LOCK);
				added.setLastMetaModTime(value(attr, FileTime.class).toMillis(), NO_LOCK);
				break;
			case PATR_VIEW_ATTR_READ_ONLY + ':' + PATR_VIEW_ATTR_READ_ONLY:
				added.setReadOnly(booleanValue(attr), NO_LOCK);
				break;
			case ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_HIDDEN:
				added.setHidden(booleanValue(attr), NO_LOCK);
				break;
			case ATTR_VIEW_PATR + ':' + PATR_VIEW_ATTR_EXECUTABLE:
				added.setExecutable(booleanValue(attr), NO_LOCK);
				break;
			default:
				throw new UnsupportedOperationException("unknown file attribute: name='" + attr.name() + "'");
			}
		}
	}
	
	@Override
	public void createLink(Path link, Path existing) throws IOException {
		PFSPathImpl lp = PFSPathImpl.getMyPath(link);
		PFSPathImpl tp = PFSPathImpl.getMyPath(existing);
		String[] linknames = getPath(lp);
		PatrFolder folder = getFolder(lp.getFileSystem().getFileSys().getRoot(), linknames, linknames.length - 1);
		String[] targetnames = getPath(tp);
		PatrFileSysElement target = getElement(tp.getFileSystem().getFileSys().getRoot(), targetnames, targetnames.length);
		folder.withLock(() -> executeCreateLink(folder, linknames[linknames.length - 1], target));
	}
	
	private void executeCreateLink(PatrFolder folder, String linkname, PatrFileSysElement target)
		throws ElementLockedException, FileAlreadyExistsException, NullPointerException, IOException {
		folder.addLink(linkname, target, NO_LOCK);
	}
	
	@Override
	public void delete(Path path) throws IOException {
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFolder root = p.getFileSystem().getFileSys().getRoot();
		PatrFileSysElement element = getElement(root, strs, strs.length);
		if (element.isFile()) {
			element.getFile().delete(NO_LOCK, NO_LOCK);
		} else {
			element.getFolder().delete(NO_LOCK, NO_LOCK);
		}
	}
	
	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		CopyOptions opts = CopyOptions.create(options);
		PFSPathImpl from = PFSPathImpl.getMyPath(source);
		String[] sourcestrs = getPath(from);
		PatrFolder root = from.getFileSystem().getFileSys().getRoot();
		PatrFile src = getFile(root, sourcestrs, sourcestrs.length);
		PFSPathImpl to = PFSPathImpl.getMyPath(target);
		String[] targetstrs = getPath(to);
		if (Arrays.deepEquals(sourcestrs, targetstrs)) {
			return;
		}
		PatrFolder newParent = getFolder(root, targetstrs, targetstrs.length - 1);
		checkCopyMoveTarget(opts, sourcestrs, targetstrs, newParent);
		PatrFile copyTarget = newParent.addFile(targetstrs[targetstrs.length - 1], NO_LOCK);
		checkInterrupted(opts, 0L);
		src.withLock(() -> executeCopy(copyTarget, src, targetstrs, newParent, options, opts));
	}
	
	private void checkInterrupted(CopyOptions opts, long progress) throws InterruptedIOException {
		if (opts.interruptable && Thread.interrupted()) {
			InterruptedIOException err = new InterruptedIOException("interrupted");
			err.bytesTransferred = (int) Math.min(Integer.MAX_VALUE, progress);
			throw err;
		}
	}
	
	private void executeCopy(PatrFile copyTarget, PatrFile src, String[] targetstrs, PatrFolder newParent, CopyOption[] options, CopyOptions opts)
		throws IOException, ElementLockedException {
		long sourceLock;
		try {
			sourceLock = src.lock(LOCK_LOCKED_LOCK | LOCK_SHARED_LOCK | LOCK_NO_WRITE_ALLOWED_LOCK | LOCK_NO_DELETE_ALLOWED_LOCK);
		} catch (ElementLockedException e) {
			sourceLock = NO_LOCK;
		}
		try {
			long targetLock;
			try {
				targetLock = copyTarget.lock(LOCK_LOCKED_LOCK | LOCK_NO_WRITE_ALLOWED_LOCK | LOCK_NO_DELETE_ALLOWED_LOCK);
			} catch (ElementLockedException e) {
				targetLock = NO_LOCK;
			}
			try {
				long length = src.length(sourceLock), copied = 0L;
				byte[] buffer = new byte[(int) Math.min(1 << 16, length)];
				for (int cpy; length > copied; length -= cpy) {
					checkInterrupted(opts, copied);
					cpy = (int) Math.min(buffer.length, length);
					src.getContent(buffer, copied, 0, cpy, sourceLock);
					copyTarget.appendContent(buffer, 0, cpy, targetLock);
				}
				assert copied == length;
				if (opts.copyAttributes) {
					copyTarget.setReadOnly(src.isReadOnly(), targetLock);
					copyTarget.setExecutable(src.isExecutable(), targetLock);
					copyTarget.setHidden(src.isHidden(), targetLock);
				}
			} finally {
				if (targetLock != NO_LOCK) {
					copyTarget.removeLock(targetLock);
				}
			}
		} finally {
			if (sourceLock != NO_LOCK) {
				src.removeLock(sourceLock);
			}
		}
	}
	
	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		CopyOptions opts = CopyOptions.create(options);
		PFSPathImpl from = PFSPathImpl.getMyPath(source);
		String[] sourcestrs = getPath(from);
		PatrFolder root = from.getFileSystem().getFileSys().getRoot();
		PatrFileSysElement copyTarget = getElement(root, sourcestrs, sourcestrs.length);
		PFSPathImpl to = PFSPathImpl.getMyPath(target);
		String[] targetstrs = getPath(to);
		if (Arrays.deepEquals(sourcestrs, targetstrs)) {
			return;
		}
		PatrFolder newParent = getFolder(root, targetstrs, targetstrs.length - 1);
		checkCopyMoveTarget(opts, sourcestrs, targetstrs, newParent);
		copyTarget.withLock(() -> {
			copyTarget.setParent(newParent, NO_LOCK, NO_LOCK, NO_LOCK);
			copyTarget.setName(targetstrs[targetstrs.length - 1], NO_LOCK);
			if ( !opts.copyAttributes) {
				copyTarget.setReadOnly(false, NO_LOCK);
				copyTarget.setExecutable(false, NO_LOCK);
				copyTarget.setHidden(false, NO_LOCK);
			}
		});
	}
	
	private void checkCopyMoveTarget(CopyOptions opts, String[] sourcestrs, String[] targetstrs, PatrFolder newParent) {
		try {
			PatrFileSysElement f = getElement(newParent, new String[] {targetstrs[targetstrs.length - 1] }, 1);
			if (opts.replaceExisting) {
				if (f.isFile()) {
					f.getFile().delete(NO_LOCK, NO_LOCK);
				} else {
					f.getFolder().delete(NO_LOCK, NO_LOCK);
				}
			} else {
				throw new FileAlreadyExistsException(buildName(targetstrs, targetstrs.length - 1), buildName(sourcestrs, sourcestrs.length - 1),
					"the target of the move operation exists already!");
			}
		} catch (IOException ignore) {}
	}
	
	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		return path.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
	}
	
	@Override
	public boolean isHidden(Path path) throws IOException {
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFileSysElement element = getElement(p.getFileSystem().getFileSys().getRoot(), strs, strs.length);
		return element.isHidden();
	}
	
	@Override
	public FileStore getFileStore(Path path) throws IOException {
		return path.getFileSystem().getFileStores().iterator().next();
	}
	
	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFolder root = p.getFileSystem().getFileSys().getRoot();
		PatrFileSysElement element = getElement(root, strs, strs.length);
		for (int i = 0; i < modes.length; i ++ ) {
			switch (modes[i]) {
			case EXECUTE:
				if ( !element.isExecutable()) {
					throw new AccessDeniedException(buildName(strs, strs.length - 1), null, "the element is not marked as executable!");
				}
			case READ:
				element.ensureAccess(NO_LOCK, LOCK_NO_READ_ALLOWED_LOCK);
				break;
			case WRITE:
				element.ensureAccess(NO_LOCK, LOCK_NO_WRITE_ALLOWED_LOCK);
				break;
			default:
				throw new InternalError("unknown AccessMode: " + modes[i].name());
			}
		}
	}
	
	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class <V> type, LinkOption... options) {
		if ( !type.isAssignableFrom(PFSBasicFileAttributeViewImpl.class)) {
			return null;
		}
		try {
			PFSPathImpl p = PFSPathImpl.getMyPath(path);
			String[] strs = getPath(p);
			PatrFolder root = p.getFileSystem().getFileSys().getRoot();
			PatrFileSysElement element;
			element = getElement(root, strs, strs.length - 1);
			return type.cast(new PFSBasicFileAttributeViewImpl(element));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class <A> type, LinkOption... options) throws IOException {
		if ( !type.isAssignableFrom(PFSFileAttributesImpl.class)) {
			return null;
		}
		try {
			PFSPathImpl p = PFSPathImpl.getMyPath(path);
			String[] strs = getPath(p);
			PatrFolder root = p.getFileSystem().getFileSys().getRoot();
			PatrFileSysElement element;
			element = getElement(root, strs, strs.length);
			PatrFileAttributeView attributes = PFSBasicFileAttributeViewImpl.readAttributes(element);
			return type.cast(attributes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Map <String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		attributes = supportsView(attributes);
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFolder root = p.getFileSystem().getFileSys().getRoot();
		PatrFileSysElement element = getElement(root, strs, strs.length);
		Map <String, Object> result = new HashMap <>();
		for (String attr : attributes.split("\\,")) {
			boolean skip = false;
			switch (attr) {
			case "*":
				skip = true;
			case BASIC_ATTRIBUTE_LAST_MODIFIED_TIME:
				result.put(attr, FileTime.fromMillis(element.getLastModTime()));
				if ( !skip) break;
			case BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
				if ( !skip) break;
			case BASIC_ATTRIBUTE_CREATION_TIME:
				result.put(attr, FileTime.fromMillis(element.getCreateTime()));
				if ( !skip) break;
			case BASIC_ATTRIBUTE_SIZE: {
				Object val;
				if (element.isFile()) {
					val = element.getFile().length(NO_LOCK);
				} else {
					val = PFSBasicFileAttributeViewImpl.sizeOf(element.getFolder());
				}
				result.put(attr, val);
				if ( !skip) break;
			}
			case BASIC_ATTRIBUTE_IS_REGULAR_FILE:
				result.put(attr, (Boolean) element.isFile());
				if ( !skip) break;
			case BASIC_ATTRIBUTE_IS_DIRECTORY:
				result.put(attr, (Boolean) element.isFolder());
				if ( !skip) break;
			case BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK:
				result.put(attr, (Boolean) false);
				if ( !skip) break;
			case BASIC_ATTRIBUTE_IS_OTHER:
				result.put(attr, (Boolean) false);
				if ( !skip) break;
			case BASIC_ATTRIBUTE_FILE_KEY:
				if ( !skip) break;
			case PATR_VIEW_ATTR_EXECUTABLE:
				result.put(attr, (Boolean) element.isExecutable());
				if ( !skip) break;
			case PATR_VIEW_ATTR_HIDDEN:
				result.put(attr, (Boolean) element.isHidden());
				if ( !skip) break;
			case PATR_VIEW_ATTR_READ_ONLY:
				result.put(attr, (Boolean) element.isReadOnly());
				if ( !skip) break;
			}
		}
		return result;
	}
	
	private String supportsView(String attributes) {
		if (attributes == null) {
			throw new NullPointerException("null attribute");
		} else {
			int index = attributes.indexOf(':');
			if (index != -1) {
				String view = attributes.substring(0, index);
				switch (view) {
				case ATTR_VIEW_BASIC:
				case ATTR_VIEW_PATR:
					attributes = attributes.substring(index + 1);
					break;
				default:
					throw new UnsupportedOperationException("unsupported view: " + view);
				}
			}
		}
		return attributes;
	}
	
	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		if (value == null) {
			throw new NullPointerException("null values are not permitted");
		}
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFolder root = p.getFileSystem().getFileSys().getRoot();
		PatrFileSysElement element = getElement(root, strs, strs.length - 1);
		attribute = supportsView(attribute);
		switch (attribute) {
		case BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
		case BASIC_ATTRIBUTE_SIZE:
		case BASIC_ATTRIBUTE_IS_REGULAR_FILE:
		case BASIC_ATTRIBUTE_IS_DIRECTORY:
		case BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK:
		case BASIC_ATTRIBUTE_IS_OTHER:
		case BASIC_ATTRIBUTE_FILE_KEY:
		default:
			throw new IllegalArgumentException("illegal attribut to set: attribute='" + attribute + "'");
		case BASIC_ATTRIBUTE_CREATION_TIME: {
			FileTime ft = (FileTime) value;
			long val = ft.toMillis();
			element.setCreateTime(val, NO_LOCK);
			break;
		}
		case BASIC_ATTRIBUTE_LAST_MODIFIED_TIME: {
			FileTime ft = (FileTime) value;
			long val = ft.toMillis();
			element.setLastModTime(val, NO_LOCK);
			break;
		}
		case PATR_VIEW_ATTR_EXECUTABLE: {
			boolean bval = (boolean) (Boolean) value;
			element.setExecutable(bval, NO_LOCK);
			break;
		}
		case PATR_VIEW_ATTR_HIDDEN: {
			boolean bval = (boolean) (Boolean) value;
			element.setHidden(bval, NO_LOCK);
			break;
		}
		case PATR_VIEW_ATTR_READ_ONLY: {
			boolean bval = (boolean) (Boolean) value;
			element.setReadOnly(bval, NO_LOCK);
			break;
		}
		}
	}
	
	private static PatrFolder getFolder(PatrFolder folder, String[] path, int len) throws IOException, NoSuchFileException, NotDirectoryException {
		for (int i = 0; i < len; i ++ ) {
			PatrFileSysElement other = folder.getElement(path[i], NO_LOCK);
			if (other.isFolder()) folder = other.getFolder();
			else throw new NotDirectoryException(buildName(path, i));
		}
		return folder;
	}
	
	private static PatrFile getFile(PatrFolder folder, String[] path, int len) throws IOException, NoSuchFileException, NotDirectoryException {
		PatrFileSysElement e = getElement(folder, path, len);
		// PatrFolder parent = getFolder(folder, path, len - 1);
		// PatrFileSysElement e = parent.getElement(path[len - 1], NO_LOCK);
		if (e.isFile()) return e.getFile();
		else throw new NoSuchFileException(buildName(path, len - 1));
	}
	
	private static PatrFileSysElement getElement(PatrFolder folder, String[] path, int len) throws IOException, NoSuchFileException, NotDirectoryException {
		if (len == 0) return folder;
		PatrFolder parent = getFolder(folder, path, len - 1);
		return parent.getElement(path[len - 1], NO_LOCK);
	}
	
	public static String buildName(PatrFileSysElement element) throws IOException {
		List <String> strs = new ArrayList <>();
		if (element.isFolder() && element.getFolder().isRoot()) {
			return "/";
		}
		strs.add(element.getName());
		PatrFolder p = element.getParent();
		while ( !p.isRoot()) {
			strs.add(p.getName());
			p = p.getParent();
		}
		StringBuilder build = new StringBuilder();
		for (int i = strs.size() - 1; i >= 0; i -- ) {
			build.append('/').append(strs.get(i));
		}
		return build.toString();
	}
	
	public static String buildName(String[] path, int imax) {
		StringBuilder build = new StringBuilder();
		for (int si = 0; si <= imax; si ++ ) {
			build.append('/').append(path[si]);
		}
		return build.toString();
	}
	
	private static String[] getPath(PFSPathImpl p) {
		p = p.toAbsolutePath().normalize();
		Name[] names = p.getNames();
		String[] result = new String[names.length];
		for (int i = 0; i < result.length; i ++ ) {
			result[i] = names[i].name;
		}
		return result;
	}
	
	public static class CopyOptions {
		
		public boolean interruptable;
		public boolean replaceExisting = false;
		public boolean copyAttributes  = false;
		
		public static CopyOptions create(CopyOption... options) {
			CopyOptions opts = new CopyOptions();
			for (CopyOption option : options) {
				if (option instanceof StandardCopyOption) {
					switch ((StandardCopyOption) option) {
					case ATOMIC_MOVE:
						throw new UnsupportedOperationException("atomic move/copy is not supported");
					case COPY_ATTRIBUTES:
						opts.copyAttributes = true;
						break;
					case REPLACE_EXISTING:
						opts.replaceExisting = true;
						break;
					default:
						throw new UnsupportedOperationException("unknown copy option: " + option);
					}
				} else if (option instanceof ExtendedCopyOption) {
					switch ((ExtendedCopyOption) option) {
					case INTERRUPTIBLE:
						opts.interruptable = true;
						break;
					default:
						throw new UnsupportedOperationException("unknown copy option: " + option);
					}
				} else if (option instanceof LinkOption) {
					if (option != LinkOption.NOFOLLOW_LINKS) {
						throw new UnsupportedOperationException("unknown copy option: " + option);
					}
				} else {
					throw new UnsupportedOperationException("unknown copy option: " + option);
				}
			}
			return opts;
		}
		
	}
	
}
