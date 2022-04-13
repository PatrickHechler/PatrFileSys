package de.hechler.patrick.pfs.objects.java;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_LOCKED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_DELETE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
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
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.nio.file.ExtendedOpenOption;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.PatrFileSysImpl;
import de.hechler.patrick.pfs.objects.ba.ByteArrayArrayBlockAccessor;
import de.hechler.patrick.pfs.objects.java.PFSPathImpl.Name;

public class PFSFileSystemProviderImpl extends FileSystemProvider {
	
	/**
	 * the attribute key for the block size of each block.<br>
	 * 
	 * the value must to be of the type {@link Integer}
	 * <p>
	 * if a block manager is specified and format is set to <code>false</code> (default value), this value must be the block size value saved from the block manager.
	 * 
	 * @see #newFileSystem(URI, Map)
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE    = "block-size";
	/**
	 * the attribute key for the block count.<br>
	 * 
	 * the value must be of the type {@link Long}
	 * <p>
	 * if a block manager is specified and format is set to <code>false</code> (default value), this value must be the block count value saved from the block manager.
	 * 
	 * @see #newFileSystem(URI, Map)
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT   = "block-count";
	/**
	 * the attribute key to set the underling block manager.
	 * <p>
	 * the value must implement the interface {@link BlockManager}
	 * <p>
	 * if no value is set a virtual block manager will be generated.<br>
	 * This automatic generated block manager will not be saved.
	 * 
	 * @see #newFileSystem(URI, Map)
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_BLOCK_MANAGER = "block-manager";
	/**
	 * the attribute key to specify if the file system should be formatted.
	 * <p>
	 * if no value is set, but {@link #NEW_FILE_SYS_ENV_ATTR_BLOCK_MANAGER} is set, the block manager will be interpreted as already formatted.<br>
	 * if a value is set, but {@link #NEW_FILE_SYS_ENV_ATTR_BLOCK_MANAGER} is not set, the value will be ignored.
	 * <p>
	 * the value must be of type {@link Boolean}.<br>
	 * if the {@link Boolean#booleanValue()} is <code>true</code>, the block manager will be formatted. If the {@link Boolean#booleanValue()} is <code>false</code> the block manager will not be.
	 * formatted
	 */
	public static final String NEW_FILE_SYS_ENV_ATTR_DO_FORMATT    = "do-formatt";
	
	private final Map <URI, PFSFileSystemImpl> created;
	private PFSFileSystemImpl                  def;
	
	
	public PFSFileSystemProviderImpl(PatrFileSystem fs) {
		this.def = new PFSFileSystemImpl(this, fs);
		this.created = new HashMap <>();
	}
	
	
	@Override
	public String getScheme() {
		return PFSPathImpl.URI_SHEME;
	}
	
	@Override
	@SuppressWarnings("resource")
	public PFSFileSystemImpl newFileSystem(URI uri, Map <String, ?> env) throws IOException, FileSystemAlreadyExistsException, IllegalArgumentException {
		synchronized (this.created) {
			if (this.created.containsKey(uri)) {
				throw new FileSystemAlreadyExistsException("uri: '" + uri + "' env: '" + env + "'");
			}
			Object blockManager = env.get(NEW_FILE_SYS_ENV_ATTR_BLOCK_MANAGER);
			Object blockCount = env.get(NEW_FILE_SYS_ENV_ATTR_BLOCK_COUNT);
			Object blockSize = env.get(NEW_FILE_SYS_ENV_ATTR_BLOCK_SIZE);
			PFSFileSystemImpl fileSys;
			if (blockManager != null) {
				Object doFormatt = env.get(NEW_FILE_SYS_ENV_ATTR_DO_FORMATT);
				BlockManager bm;
				if ( ! (blockManager instanceof BlockManager)) {
					throw new IllegalArgumentException("the block manager is not of type block manger! cls: " + blockManager.getClass().getName() + " blockManager: '" + blockManager + "'");
				}
				bm = (BlockManager) blockManager;
				bm.getBlock(0L);
				try {
					bm.getBlock(1L);
					PatrFileSystem fs = new PatrFileSysImpl(bm);
					try {
						if (doFormatt != null) {
							if ( ! (doFormatt instanceof Boolean)) {
								throw new IllegalArgumentException("do formatt is not of type Boolean! cls: " + doFormatt.getClass().getName() + " doFormatt: '" + doFormatt + "'");
							}
							if ((boolean) (Boolean) doFormatt) {
								boolean formatted = false;
								if (blockCount != null) {
									if ( ! (blockCount instanceof Long)) {
										throw new IllegalArgumentException("block count is not of type Long! cls: " + blockCount.getClass().getName() + " blockCount: '" + blockCount + "'");
									}
									if (fs instanceof PatrFileSysImpl) {
										((PatrFileSysImpl) fs).format((long) (Long) blockCount);
										formatted = true;
									}
								}
								if ( !formatted) {
									fs.format();
								}
							}
						}
						if (blockCount != null) {
							if ( !blockCount.equals(fs.blockCount())) {
								throw new IllegalArgumentException("the given block count does not match the saved block count!");
							}
						}
						if (blockSize != null) {
							if ( !blockSize.equals(fs.blockCount())) {
								throw new IllegalArgumentException("the given block size does not match the saved block size!");
							}
						}
						fileSys = new PFSFileSystemImpl(this, fs);
					} finally {
						bm.ungetBlock(1L);
					}
				} finally {
					bm.ungetBlock(0L);
				}
			} else {
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
			if (fs != null) return fs;
			else throw new FileSystemNotFoundException("uri: " + uri);
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
	public SeekableByteChannel newByteChannel(Path path, Set <? extends OpenOption> options, FileAttribute <?>... attrs) throws IOException {
		PFSPathImpl p = PFSPathImpl.getMyPath(path);
		String[] strs = getPath(p);
		PatrFileSystem fs = p.getFileSystem().getFileSys();
		PatrFolder directParent = getFolder(fs.getRoot(), strs, strs.length - 1);
		long lock = LOCK_NO_LOCK;
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
		PatrFile file = getFile(options, strs, directParent, MODE_READ, mode);
		if (lock != LOCK_NO_LOCK) {
			lock = file.lock(lock);
		}
		if (options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
			if ( (mode & MODE_READ) != mode) {
				file.removeContent(0L, file.length(), lock);
			}
		}
		if (options.contains(StandardOpenOption.DELETE_ON_CLOSE)) {
			final long finallock = lock;
			return new PFSSeekableByteChannelImpl(lock, strs, file, (patrFile) -> {
				patrFile.delete(finallock);
			}, mode);
		} else {
			return new PFSSeekableByteChannelImpl(lock, strs, file, (patrFile) -> {}, mode);
		}
		/* @formatter:off TODO:
DELETE_ON_CLOSE
	When this option is present then the implementation makes a best effort attempt to delete the file when closed by the close method.
	If the close method is not invoked then a best effort attempt is made to delete the file when the Java virtual machine terminates.
SPARSE
	When creating a new file this option is a hint that the new file will be sparse. This option is ignored when not creating a new file.
SYNC
	Requires that every update to the file's content or metadata be written synchronously to the underlying storage device. (see  Synchronized I/O file integrity).
DSYNC
	Requires that every update to the file's content be written synchronously to the underlying storage device. (see  Synchronized I/O file integrity).
		 @formatter:on */
	}
	
	
	private PatrFile getFile(Set <? extends OpenOption> options, String[] strs, PatrFolder directParent, final int mode_read, int mode)
		throws IOException, NotDirectoryException, FileAlreadyExistsException, ElementLockedException, NoSuchFileException {
		PatrFile file;
		if (options.contains(StandardOpenOption.CREATE_NEW)) {
			if ( (mode & mode_read) != mode) {
				try {
					getElement(directParent, new String[] {strs[strs.length - 1] }, 1);
					throw new FileAlreadyExistsException(buildName(strs, strs.length - 1), null, "the file exists already!");
				} catch (NoSuchFileException e) {
					file = directParent.addFile(strs[strs.length - 1], LOCK_NO_LOCK);
				}
			} else {
				file = getFile(directParent, new String[] {strs[strs.length - 1] }, 1);
			}
		} else {
			if (options.contains(StandardOpenOption.CREATE)) {
				if ( (mode & mode_read) != mode) {
					boolean rethrow = false;
					try {
						PatrFileSysElement element = getElement(directParent, new String[] {strs[strs.length - 1] }, 1);
						if (element.isFile()) {
							file = element.getFile();
						} else {
							rethrow = true;
							throw new NoSuchFileException(buildName(strs, strs.length - 1), null, "the file is a folder and no file!");
						}
					} catch (NoSuchFileException e) {
						if (rethrow) {
							throw e;
						} else {
							file = directParent.addFile(strs[strs.length - 1], LOCK_NO_LOCK);
						}
					}
				} else {
					file = getFile(directParent, new String[] {strs[strs.length - 1] }, 1);
				}
			} else {
				file = getFile(directParent, new String[] {strs[strs.length - 1] }, 1);
			}
		}
		return file;
	}
	
	@Override
	public DirectoryStream <Path> newDirectoryStream(Path dir, Filter <? super Path> filter) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void createDirectory(Path dir, FileAttribute <?>... attrs) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void delete(Path path) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		return path.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
	}
	
	@Override
	public boolean isHidden(Path path) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public FileStore getFileStore(Path path) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class <V> type, LinkOption... options) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class <A> type, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Map <String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	
	private PatrFolder getFolder(PatrFolder folder, String[] path, int len) throws IOException, NoSuchFileException, NotDirectoryException {
		PatrFileSysElement element = getElement(folder, path, len - 1);
		if (element.isFolder()) {
			return element.getFolder();
		} else {
			throw new NotDirectoryException(buildName(path, path.length - 1));
		}
	}
	
	private PatrFile getFile(PatrFolder folder, String[] path, int len) throws IOException, NoSuchFileException, NotDirectoryException {
		PatrFileSysElement element = getElement(folder, path, len - 1);
		if (element.isFile()) {
			return element.getFile();
		} else {
			throw new NoSuchFileException(buildName(path, path.length - 1), null, "the element is a folder, but a file was expected!");
		}
	}
	
	private static PatrFileSysElement getElement(PatrFolder folder, String[] path, int len) throws IOException, NoSuchFileException, NotDirectoryException {
		PatrFolder parent = folder;
		for (int i = 0; i < len - 1; i ++ ) {
			PatrFileSysElement otherParent = findChildWithName(parent, path, i);
			if (otherParent.isFolder()) {
				parent = otherParent.getFolder();
			} else {
				throw new NotDirectoryException(buildName(path, i));
			}
		}
		return findChildWithName(parent, path, path.length - 1);
	}
	
	private static PatrFileSysElement findChildWithName(PatrFolder parent, String[] path, int i) throws IOException, NoSuchFileException {
		for (PatrFileSysElement otherParent : parent) {
			if (path[i].equals(otherParent.getName())) {
				return otherParent;
			}
		}
		throw new NoSuchFileException(buildName(path, i));
	}
	
	private static String buildName(String[] path, int i) {
		StringBuilder build = new StringBuilder();
		for (int si = 0; si <= i; si ++ ) {
			build.append('/').append(path[si]);
		}
		String file = build.toString();
		return file;
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
	
}