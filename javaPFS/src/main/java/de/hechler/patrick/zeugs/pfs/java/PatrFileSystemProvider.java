package de.hechler.patrick.zeugs.pfs.java;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;
import de.hechler.patrick.zeugs.pfs.java.impl.PatrFileSystem;
import de.hechler.patrick.zeugs.pfs.java.impl.PatrPath;
import de.hechler.patrick.zeugs.pfs.opts.PatrFSOptions;
import de.hechler.patrick.zeugs.pfs.opts.PatrRamFSOpts;

public class PatrFileSystemProvider extends FileSystemProvider {

	public static void main(String[] args) {
		System.out.println(Runtime.version());
		System.out.println(FSProvider.providerMap());
		System.out.println(FSProvider.providers());
		System.out.println(FileSystemProvider.installedProviders());
	}

	private final FSProvider provider;
	private final Map<String, PatrFileSystem> loadedFSs = new HashMap<>();

	/**
	 * creates a new {@link PatrFileSystemProvider}.<br>
	 * the provide will use the {@link PatrFSProvider} as {@link FSProvider}.
	 */
	public PatrFileSystemProvider() {
		super();
		try {
			this.provider = FSProvider.ofName(FSProvider.PATR_FS_PROVIDER_NAME);
		} catch (NoSuchProviderException e) {
			throw new InternalError(e);
		}
	}

	/**
	 * creates a new {@link PatrFileSystemProvider} which uses the given
	 * {@link FSProvider}.
	 * <p>
	 * note that the {@link FSProvider} has to be compatible with the
	 * {@link PatrFSProvider}.<br>
	 * {@link PatrFSOptions} and {@link PatrRamFSOpts} has to be valid
	 * {@link FSOptions}.
	 * <p>
	 * note that the {@code providers} {@link FSProvider#name() name} is used as
	 * {@link #getScheme() scheme}, so "file" is an invalid name
	 * 
	 * @param provider the backing provider of this provider
	 */
	public PatrFileSystemProvider(FSProvider provider) {
		super();
		this.provider = provider;
	}

	@Override
	public String getScheme() {
		return provider.name();
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		if (!provider.name().equals(uri.getScheme())) {
			throw new IllegalArgumentException(
					"not my scheme! (uri.scheme='" + uri.getScheme() + "', me.scheme='" + provider.name() + "')");
		}
		String host = uri.getHost();
		String path = uri.getPath();
		if (host == null || path == null) {
			throw new IllegalArgumentException("host or path is not set! host='" + host + "', path='" + path + "'");
		}
		PatrFileSystem fs = loadedFSs.get(host);
		if (fs == null) {
			throw new FileSystemNotFoundException("no file system found (" + host + ")");
		}
		return fs;
	}

	@Override
	public Path getPath(URI uri) {
		if (!provider.name().equals(uri.getScheme())) {
			throw new IllegalArgumentException(
					"not my scheme! (uri.scheme='" + uri.getScheme() + "', me.scheme='" + provider.name() + "')");
		}
		String host = uri.getHost();
		String path = uri.getPath();
		if (host == null || path == null) {
			throw new IllegalArgumentException("host or path is not set! host='" + host + "', path='" + path + "'");
		}
		PatrFileSystem fs = loadedFSs.get(host);
		if (fs == null) {
			throw new FileSystemNotFoundException("no file system found (" + host + ")");
		}
		return PatrPath.create(fs, path);
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
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
		// TODO Auto-generated method stub
		return false;
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
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub

	}
}
