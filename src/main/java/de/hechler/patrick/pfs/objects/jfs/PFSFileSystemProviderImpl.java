package de.hechler.patrick.pfs.objects.jfs;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_LOCKED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_DELETE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
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
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.ba.ByteArrayArrayBlockAccessor;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImpl;
import de.hechler.patrick.pfs.objects.jfs.PFSPathImpl.Name;

public class PFSFileSystemProviderImpl extends FileSystemProvider {
	
	/**
	 * name of the basic attribute view for the file key
	 * <p>
	 * the corresponding value must be of type {@link Object}
	 */
	public static final String BASIC_ATTRIBUTE_FILE_KEY           = "fileKey";
	/**
	 * name of the basic attribute view for is other (not file,dir,symbol-link)
	 * <p>
	 * the corresponding value must be of type {@link Boolean}
	 */
	public static final String BASIC_ATTRIBUTE_IS_OTHER           = "isOther";
	/**
	 * name of the basic attribute view for is symbol-link
	 * <p>
	 * the corresponding value must be of type {@link Boolean}
	 */
	public static final String BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK   = "isSymbolicLink";
	/**
	 * name of the basic attribute view for is directory
	 * <p>
	 * the corresponding value must be of type {@link Boolean}
	 */
	public static final String BASIC_ATTRIBUTE_IS_DIRECTORY       = "isDirectory";
	/**
	 * name of the basic attribute view for is file
	 * <p>
	 * the corresponding value must be of type {@link Boolean}
	 */
	public static final String BASIC_ATTRIBUTE_IS_REGULAR_FILE    = "isRegularFile";
	/**
	 * name of the basic attribute view for the size
	 * <p>
	 * the corresponding value must be of type {@link Long}
	 */
	public static final String BASIC_ATTRIBUTE_SIZE               = "size";
	/**
	 * name of the basic attribute view for the creation time
	 * <p>
	 * the corresponding value must be of type {@link FileTime}
	 */
	public static final String BASIC_ATTRIBUTE_CREATION_TIME      = "creationTime";
	/**
	 * name of the basic attribute view for the last access time
	 * <p>
	 * the corresponding PFSFileSystemProviderImplvalue must be of type {@link FileTime}
	 */
	public static final String BASIC_ATTRIBUTE_LAST_ACCESS_TIME   = "lastAccessTime";
	/**
	 * name of the basic attribute view for the last modified time
	 * <p>
	 * the corresponding value must be of type {@link FileTime}
	 */
	public static final String BASIC_ATTRIBUTE_LAST_MODIFIED_TIME = "lastModifiedTime";
	/**
	 * {@link FileAttribute} name for the hidden flag.<br>
	 * {@link FileAttribute#value()} must be of the {@link Boolean} type<br>
	 * <code>true</code> to mark the element as executable
	 */
	public static final String PATR_VIEW_ATTR_HIDDEN              = "hidden";
	/**
	 * {@link FileAttribute} name for the executable flag.<br>
	 * {@link FileAttribute#value()} must be of the {@link Boolean} type<br>
	 * <code>true</code> to mark the element as executable
	 */
	public static final String PATR_VIEW_ATTR_EXECUTABLE          = "executable";
	/**
	 * {@link FileAttribute} name for the read only flag.<br>
	 * {@link FileAttribute#value()} must be of the {@link Boolean} type<br>
	 * <code>true</code> to mark the element as executable
	 */
	public static final String PATR_VIEW_ATTR_READ_ONLY           = "read_only";
	
	/**
	 * the attribute key for the block size of each block.<br>
	 * 
	 * the value must to be of the type {@link Integer}
	 * <p>
	 * if a file system is specified and format is set to <code>false</code> (default value), this value must be the block size value saved by the file system.
	 * 
	 * @see #newFileSystem(URI, Map)
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE  = "block-size";
	/**
	 * the attribute key for the block count.<br>
	 * 
	 * the value must be of the tyPFSFileSystemProviderImplpe {@link Long}
	 * <p>
	 * if a block manager is specified and format is set to <code>false</code> (default value), this value must be the block count value saved from the block manager.
	 * 
	 * @see #newFileSystem(URI, Map)
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT = "block-count";
	/**
	 * the attribute key to set the underling file system.
	 * <p>
	 * the value must implement the interface {@link PatrFileSystem}
	 * <p>
	 * if no value is set a {@link PatrFileSysImpl} will be created.<br>
	 * if no value the generated file system will use virtual block manager.<br>
	 * This automatic generated block manager and thus the file system will not be saved.
	 * 
	 * @see #newFileSystem(URI, Map)
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_FILE_SYS    = "file-sys";
	/**
	 * the attribute key to specify if the file system should be formatted.
	 * <p>
	 * if no value is set, but {@link #NEW_FILE_SYS_ENV_ATTR_FILE_SYS} is set, the file system will be interpreted as already formatted.<br>
	 * if a value is set, but {@link #NPFSFileSystemProviderImplEW_FILE_SYS_ENV_ATTR_FILE_SYS} is not set, the value will be ignored.
	 * <p>
	 * the value must be of type {@link Boolean}.<br>
	 * if the {@link Boolean#booleanValue()} is <code>true</code>, the block manager will be formatted. If the {@link Boolean#booleanValue()} is <code>false</code> the block manager will not be.
	 * formatted
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_DO_FORMATT  = "do-formatt";
	
	private final Map <URI, PFSFileSystemImpl> created;
	private PFSFileSystemImpl                  def;
	private Set <PatrFile>                     delOnClose;
	
	public PFSFileSystemProviderImpl() {
		this(new PatrFileSysImpl(new ByteArrayArrayBlockAccessor(1 << 10, 1 << 10)));
	}
	
	public PFSFileSystemProviderImpl(PatrFileSystem fs) {
		this.def = new PFSFileSystemImpl(this, fs);
		this.created = new HashMap <>();
		this.delOnClose = new HashSet <>();
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
							if ( (parent.getLockData() & LOCK_NO_DELETE_ALLOWED_LOCK) != 0) {
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
	
	@Override
	public String getScheme() {
		return PFSPathImpl.URI_SHEME;
	}
	
	@Override
	@SuppressWarnings("resource")
	public PFSFileSystemImpl newFileSystem(URI uri, Map <String, ?> env)
		throws IOException, FileSystemAlreadyExistsException, IllegalArgumentException {
		synchronized (this.created) {
			if (this.created.containsKey(uri)) {
				throw new FileSystemAlreadyExistsException("uri: '" + uri + "' env: '" + env + "'");
			}
			Object fileSystem = env.get(NEW_FILE_SYS_ENV_ATTR_FILE_SYS);
			Object blockCount = env.get(NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT);
			Object blockSize = env.get(NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE);
			PFSFileSystemImpl fileSys;
			if (fileSystem != null) {
				Object doFormatt = env.get(NEW_FILE_SYS_ENV_ATTR_DO_FORMATT);
				if ( ! (fileSystem instanceof BlockManager)) {
					throw new IllegalArgumentException("the file system is not of type PatrFileSystem! cls: "
						+ fileSystem.getClass().getName() + " fileSystem: '" + fileSystem + "'");
				}
				boolean formatt = false;
				if (doFormatt != null) {
					if ( ! (doFormatt instanceof Boolean)) {
						throw new IllegalArgumentException("do formatt is not of type Boolean! cls: "
							+ doFormatt.getClass().getName() + " doFormatt: '" + doFormatt + "'");
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
				int bs;
				if (blockSize != null) {
					if ( ! (blockSize instanceof Integer)) {
						throw new IllegalArgumentException("block size is not of type Integer! cls: "
							+ blockSize.getClass().getName() + " blockSize: '" + blockSize + "'");
					}
					bs = (int) blockSize;
				} else {
					bs = 1 << 12;
				}
				int bc;
				if (blockCount != null) {
					if ( ! (blockCount instanceof Integer)) {
						throw new IllegalArgumentException("block count is not of type Long! cls: "
							+ blockCount.getClass().getName() + " blockCount: '" + blockCount + "'");
					}
					bc = (int) blockCount;
				} else {
					bc = 1 << 8;
				}
				BlockAccessor ba = new ByteArrayArrayBlockAccessor(bc, bs);
				PatrFileSystem fs = new PatrFileSysImpl(ba);
				fileSys = new PFSFileSystemImpl(this, fs);
			}
			this.created.put(uri, fileSys);
			return fileSys;
		}
	}
	
	@Override
	public PFSFileSystemImpl getFileSystem(URI uri) throws FileSystemNotFoundException {
		synchronized (this.created) {
			PFSFileSystemImpl fs = this.created.get(uri);
			if (fs != null)
				return fs;
			else
				throw new FileSystemNotFoundException("uri: " + uri);
		}
	}
	
	@Override
	public Path getPath(URI uri) {
		String str = uri.getRawPath();
		Path path = def.getPath(str);
		return path;
	}
	
	public static final int MODE_READ = 1, MODE_WRITE = 2, MODE_APPEND = 4;
	
	@Override
	public SeekableByteChannel newByteChannel(Path path, Set <? extends OpenOption> options, FileAttribute <?>... attrs)
		throws IOException {
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFileSystem fs = p.getFileSystem().getFileSys();
		PatrFolder directParent = getFolder(fs.getRoot(), strs, strs.length - 1);
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
			throw new IllegalArgumentException("no mode defined! (read,write,append)");
		}
		if (mode == MODE_READ) {
			options.remove(StandardOpenOption.CREATE);
			options.remove(StandardOpenOption.CREATE_NEW);
		} else if (options.contains(StandardOpenOption.CREATE_NEW)) {
			options.remove(StandardOpenOption.CREATE);
		}
		PatrFile file;
		file = getFile(options, strs, directParent, mode);
		if (lock != NO_LOCK) {
			lock = file.lock(lock);
		}
		if (options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
			if ( (mode & MODE_READ) != mode) {
				file.removeContent(0L, file.length(), lock);
			}
		}
		if (options.contains(StandardOpenOption.DELETE_ON_CLOSE)) {
			final long finallock = lock;
			synchronized (delOnClose) {
				delOnClose.add(file);
			}
			return new PFSSeekableByteChannelImpl(lock, strs, file, (patrFile) -> {
				synchronized (delOnClose) {
					patrFile.delete(finallock, NO_LOCK);
					delOnClose.remove(patrFile);
				}
			}, mode);
		} else {
			return new PFSSeekableByteChannelImpl(lock, strs, file, (patrFile) -> {}, mode);
		}
	}
	
	private PatrFile getFile(Set <? extends OpenOption> options, String[] strs, PatrFolder directParent, int mode)
		throws IOException, NotDirectoryException, FileAlreadyExistsException, ElementLockedException,
		NoSuchFileException {
		PatrFile file;
		if (options.contains(StandardOpenOption.CREATE_NEW)) {
			try {
				getElement(directParent, new String[] {strs[strs.length - 1] }, 1);
				throw new FileAlreadyExistsException(buildName(strs, strs.length - 1), null,
					"the file exists already!");
			} catch (NoSuchFileException e) {
				file = directParent.addFile(strs[strs.length - 1], NO_LOCK);
			}
		} else if (options.contains(StandardOpenOption.CREATE)) {
			boolean rethrow = false;
			try {
				PatrFileSysElement element = getElement(directParent, new String[] {strs[strs.length - 1] }, 1);
				if (element.isFile()) {
					file = element.getFile();
				} else {
					rethrow = true;
					throw new NoSuchFileException(buildName(strs, strs.length - 1), null,
						"the file is a folder and no file!");
				}
			} catch (NoSuchFileException e) {
				if (rethrow) {
					throw e;
				} else {
					file = directParent.addFile(strs[strs.length - 1], NO_LOCK);
				}
			}
		} else {
			file = getFile(directParent, new String[] {strs[strs.length - 1] }, 1);
		}
		return file;
	}
	
	@Override
	public DirectoryStream <Path> newDirectoryStream(Path dir, Filter <? super Path> filter) throws IOException {
		PFSPathImpl p = PFSPathImpl.getMyPath(dir);
		String[] names = getPath(p);
		PatrFolder folder = getFolder(p.getFileSystem().getFileSys().getRoot(), names, names.length);
		long lock = LOCK_LOCKED_LOCK | LOCK_SHARED_LOCK | LOCK_NO_DELETE_ALLOWED_LOCK;
		try {
			folder.lock(lock);
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
	
	private void executeCreateDirectory(Set <?> set, String[] names, PatrFolder folder, FileAttribute <?>... attrs)
		throws IOException, ElementLockedException {
		try {
			getFolder(folder, new String[] {names[names.length - 1] }, 1);
			throw new FileAlreadyExistsException(buildName(names, names.length - 1), null,
				"the folder exists already!");
		} catch (IOException ignore) {}
		PatrFolder added = folder.addFolder(names[names.length - 1], NO_LOCK);
		for (int i = 0; i < attrs.length; i ++ ) {
			FileAttribute <?> attr = attrs[i];
			switch (attr.name()) {
			case PATR_VIEW_ATTR_READ_ONLY:
				added.setReadOnly(booleanValue(attr), NO_LOCK);
				break;
			case PATR_VIEW_ATTR_HIDDEN:
				added.setHidden(booleanValue(attr), NO_LOCK);
				break;
			case PATR_VIEW_ATTR_EXECUTABLE:
				added.setExecutable(booleanValue(attr), NO_LOCK);
				break;
			default:
				throw new UnsupportedOperationException("unknown file attribute: name='" + attr.name() + "'");
			}
		}
	}
	
	private boolean booleanValue(FileAttribute <?> attr) {
		Object val = attr.value();
		if (val != null && val instanceof Boolean) {
			return (boolean) (Boolean) val;
		} else {
			throw new UnsupportedOperationException("unknown file attribute value: name='" + attr.name() + "' value='"
				+ val + "' (expected non null Boolean value)");
		}
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
	
	private void executeCopy(PatrFile copyTarget, PatrFile src, String[] targetstrs, PatrFolder newParent,
		CopyOption[] options, CopyOptions opts) throws IOException, ElementLockedException {
		long sourceLock;
		try {
			sourceLock = src.lock(
				LOCK_LOCKED_LOCK | LOCK_SHARED_LOCK | LOCK_NO_WRITE_ALLOWED_LOCK | LOCK_NO_DELETE_ALLOWED_LOCK);
		} catch (ElementLockedException e) {
			sourceLock = NO_LOCK;
		}
		try {
			long targetLock;
			try {
				targetLock = copyTarget
					.lock(LOCK_LOCKED_LOCK | LOCK_NO_WRITE_ALLOWED_LOCK | LOCK_NO_DELETE_ALLOWED_LOCK);
			} catch (ElementLockedException e) {
				targetLock = NO_LOCK;
			}
			try {
				long length = src.length(), copied = 0L;
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
				throw new FileAlreadyExistsException(buildName(targetstrs, targetstrs.length - 1),
					buildName(sourcestrs, sourcestrs.length - 1),
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
					throw new AccessDeniedException(buildName(strs, strs.length - 1), null,
						"the element is not marked as executable!");
				}
			case READ:
				element.ensureAccess(NO_LOCK, LOCK_NO_READ_ALLOWED_LOCK, false);
				break;
			case WRITE:
				element.ensureAccess(NO_LOCK, LOCK_NO_WRITE_ALLOWED_LOCK, true);
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
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class <A> type, LinkOption... options)
		throws IOException {
		if ( !type.isAssignableFrom(PFSBasicFileAttributesImpl.class)) {
			return null;
		}
		try {
			PFSPathImpl p = PFSPathImpl.getMyPath(path);
			String[] strs = getPath(p);
			PatrFolder root = p.getFileSystem().getFileSys().getRoot();
			PatrFileSysElement element;
			element = getElement(root, strs, strs.length - 1);
			PFSBasicFileAttributesImpl attributes = PFSBasicFileAttributeViewImpl.readAttributes(element);
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
		PatrFileSysElement element = getElement(root, strs, strs.length - 1);
		Map <String, Object> result = new HashMap <>();
		for (String attr : attributes.split("\\,")) {
			boolean skip = false;
			switch (attr) {
			case "*":
				skip = true;
			case BASIC_ATTRIBUTE_LAST_MODIFIED_TIME:
				result.put(attr, FileTime.fromMillis(element.getLastModTime()));
				if ( !skip)
					break;
			case BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
				if ( !skip)
					break;
			case BASIC_ATTRIBUTE_CREATION_TIME:
				result.put(attr, FileTime.fromMillis(element.getCreateTime()));
				if ( !skip)
					break;
			case BASIC_ATTRIBUTE_SIZE: {
				Object val;
				if (element.isFile()) {
					val = element.getFile().length();
				} else {
					val = PFSBasicFileAttributeViewImpl.sizeOf(element.getFolder());
				}
				result.put(attr, val);
				if ( !skip)
					break;
			}
			case BASIC_ATTRIBUTE_IS_REGULAR_FILE:
				result.put(attr, (Boolean) element.isFile());
				if ( !skip)
					break;
			case BASIC_ATTRIBUTE_IS_DIRECTORY:
				result.put(attr, (Boolean) element.isFolder());
				if ( !skip)
					break;
			case BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK:
				result.put(attr, (Boolean) false);
				if ( !skip)
					break;
			case BASIC_ATTRIBUTE_IS_OTHER:
				result.put(attr, (Boolean) false);
				if ( !skip)
					break;
			case BASIC_ATTRIBUTE_FILE_KEY:
				if ( !skip)
					break;
			case PATR_VIEW_ATTR_EXECUTABLE:
				result.put(attr, (Boolean) element.isExecutable());
				if ( !skip)
					break;
			case PATR_VIEW_ATTR_HIDDEN:
				result.put(attr, (Boolean) element.isHidden());
				if ( !skip)
					break;
			case PATR_VIEW_ATTR_READ_ONLY:
				result.put(attr, (Boolean) element.isReadOnly());
				if ( !skip)
					break;
			}
		}
		return result;
	}
	
	private String supportsView(String attributes) {
		int index = attributes.indexOf(':');
		if (index >= 0) {
			String view = attributes.substring(0, index);
			if (PFSFileSystemImpl.ATTR_VIEW_BASIC.equalsIgnoreCase(view)
				|| PFSFileSystemImpl.ATTR_VIEW_PATR.equalsIgnoreCase(view)) {
				throw new UnsupportedOperationException("unsupported view: " + view);
			}
			attributes = attributes.substring(index + 1);
		}
		return attributes;
	}
	
	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		if (value == null) {
			throw new IllegalArgumentException("null values are not permitted");
		}
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFolder root = p.getFileSystem().getFileSys().getRoot();
		PatrFileSysElement element = getElement(root, strs, strs.length - 1);
		attribute = supportsView(attribute);
		switch (attribute) {
		case BASIC_ATTRIBUTE_LAST_MODIFIED_TIME:
		case BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
		case BASIC_ATTRIBUTE_CREATION_TIME:
		case BASIC_ATTRIBUTE_SIZE:
		case BASIC_ATTRIBUTE_IS_REGULAR_FILE:
		case BASIC_ATTRIBUTE_IS_DIRECTORY:
		case BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK:
		case BASIC_ATTRIBUTE_IS_OTHER:
		case BASIC_ATTRIBUTE_FILE_KEY:
		default:
			throw new IllegalArgumentException("illegal attribut to set: attribute='" + attribute + "'");
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
	
	private static PatrFolder getFolder(PatrFolder folder, String[] path, int len)
		throws IOException, NoSuchFileException, NotDirectoryException {
		for (int i = 0; i < len; i ++ ) {
			PatrFileSysElement other = folder.getElement(path[i], NO_LOCK);
			if (other.isFolder()) folder = other.getFolder();
			else throw new NotDirectoryException(buildName(path, i));
		}
		return folder;
	}
	
	private static PatrFile getFile(PatrFolder folder, String[] path, int len) throws IOException, NoSuchFileException, NotDirectoryException {
		PatrFolder parent = getFolder(folder, path, len - 1);
		PatrFileSysElement e = parent.getElement(path[len - 1], NO_LOCK);
		if (e.isFile()) return e.getFile();
		else throw new NoSuchFileException(buildName(path, len - 1));
	}
	
	private static PatrFileSysElement getElement(PatrFolder folder, String[] path, int len) throws IOException, NoSuchFileException, NotDirectoryException {
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
