package de.hechler.patrick.pfs.objects.bfs;

import java.io.IOException;
import java.lang.ref.WeakReference;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrLink;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingBooleanSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingIntSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingLongSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingRunnable;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingSupplier;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;


public class BufferedFileSysElementImpl implements PatrFileSysElement {
	
	public static final int TYPE_NONE        = 0;
	public static final int TYPE_FILE        = 1;
	public static final int TYPE_FOLDER      = 2;
	public static final int TYPE_LINK        = 4;
	public static final int TYPE_ROOT_FOLDER = 8;
	public static final int TYPE_EXE         = 16;
	public static final int TYPE_HIDDEN      = 32;
	public static final int TYPE_READ_ONLY   = 64;
	
	protected final PatrFileSysElementBuffer buffer;
	
	public BufferedFileSysElementImpl(PatrFileSysElementBuffer buffer) {
		this.buffer = buffer;
	}
	
	@Override
	public PatrFolder getParent() throws IllegalStateException, IOException {
		synchronized (buffer) {
			if (buffer.parent != null) {
				PatrFileSysElementBuffer p = buffer.parent.get();
				if (p != null) {
					if (p.me != null) {
						BufferedFileSysElementImpl pe = p.me.get();
						if (pe == null || ! (pe instanceof PatrFolder)) {
							pe = new BufferedFolderImpl(p);
							pe.folder();
							p.me = new WeakReference <>(pe);
						} else {
							pe.folder();
						}
						return (PatrFolder) pe;
					}
				}
			}
			PatrFolder p = buffer.element.getParent();
			PatrFileSysElementBuffer pbuf = new PatrFileSysElementBuffer(buffer.fs, p);
			BufferedFolderImpl bufpf = new BufferedFolderImpl(pbuf);
			pbuf.me = new WeakReference <>(bufpf);
			pbuf.folder = new WeakReference <>(p);
			buffer.parent = new WeakReference <>(pbuf);
			return bufpf;
		}
	}
	
	@Override
	public void setParent(PatrFolder newParent, long myLock, long oldParentLock, long newParentLock) throws IllegalStateException, IllegalArgumentException, NullPointerException, IOException, ElementLockedException {
		if ( ! (newParent instanceof BufferedFolderImpl)) {
			throw new IllegalArgumentException("the parent is not from an unknown type!");
		}
		BufferedFolderImpl newP = (BufferedFolderImpl) newParent;
		if (newP.buffer.fs != buffer.fs) {
			throw new IllegalArgumentException("the parent is from an other file system!");
		}
		newParent = (PatrFolder) newP.buffer.element;
		synchronized (buffer.parent) {
			synchronized (this) {
				buffer.element.setParent(newParent, myLock, oldParentLock, newParentLock);
				if (buffer.parent != null) {
					PatrFileSysElementBuffer buf = buffer.parent.get();
					if (buf != null) {
						buf.dataChanged = true;
					}
				}
				buffer.parent = new WeakReference <PatrFileSysElementBuffer>(newP.buffer);
				newP.buffer.dataChanged = true;
			}
		}
	}
	
	@Override
	public PatrFolder getFolder() throws IllegalStateException, IOException {
		if (this instanceof PatrFolder) {
			return (PatrFolder) this;
		} else {
			if (buffer.me != null) {
				BufferedFileSysElementImpl bufMe = buffer.me.get();
				if (bufMe != null) {
					if (bufMe instanceof PatrFolder) {
						return (PatrFolder) bufMe;
					}
				}
			}
			PatrFolder folder;
			if (buffer.folder == null || buffer.elementTypeChanged) {
				folder = buffer.element.getFolder();
				buffer.folder = new WeakReference <>(folder);
			} else {
				folder = buffer.folder.get();
				if (folder == null) {
					folder = buffer.element.getFolder();
					buffer.folder = new WeakReference <>(folder);
				}
			}
			BufferedFolderImpl bufFolder = new BufferedFolderImpl(buffer);
			buffer.me = new WeakReference <>(bufFolder);
			return bufFolder;
		}
	}
	
	@Override
	public PatrFile getFile() throws IllegalStateException, IOException {
		if (this instanceof PatrFile) {
			return (PatrFile) this;
		} else {
			if (buffer.me != null) {
				BufferedFileSysElementImpl bufMe = buffer.me.get();
				if (bufMe != null) {
					if (bufMe instanceof PatrFile) {
						return (PatrFile) bufMe;
					}
				}
			}
			PatrFile file;
			if (buffer.link == null || buffer.elementTypeChanged) {
				file = buffer.element.getFile();
				buffer.file = new WeakReference <>(file);
			} else {
				file = buffer.file.get();
				if (file == null) {
					file = buffer.element.getFile();
					buffer.file = new WeakReference <>(file);
				}
			}
			BufferedFileImpl bufFile = new BufferedFileImpl(buffer);
			buffer.me = new WeakReference <>(bufFile);
			return bufFile;
		}
	}
	
	@Override
	public PatrLink getLink() throws IllegalStateException, IOException {
		if (this instanceof PatrLink) {
			return (PatrLink) this;
		} else {
			if (buffer.me != null) {
				BufferedFileSysElementImpl bufMe = buffer.me.get();
				if (bufMe != null) {
					if (bufMe instanceof PatrLink) {
						return (PatrLink) bufMe;
					}
				}
			}
			PatrLink link;
			if (buffer.link == null || buffer.elementTypeChanged) {
				link = buffer.element.getLink();
				buffer.link = new WeakReference <>(link);
			} else {
				link = buffer.link.get();
				if (link == null) {
					link = buffer.element.getLink();
					buffer.link = new WeakReference <>(link);
				}
			}
			BufferedLinkImpl bufLink = new BufferedLinkImpl(buffer);
			buffer.me = new WeakReference <>(bufLink);
			return bufLink;
		}
	}
	
	@Override
	public Object getID() throws IOException {
		return buffer.element.getID();
	}
	
	@Override
	public boolean isFolder() throws IOException {
		return isTypeFlagSet(TYPE_FOLDER, buffer.elementTypeChanged);
	}
	
	@Override
	public boolean isFile() throws IOException {
		return isTypeFlagSet(TYPE_FILE, buffer.elementTypeChanged);
	}
	
	@Override
	public boolean isLink() throws IOException {
		return isTypeFlagSet(TYPE_LINK, buffer.elementTypeChanged);
	}
	
	@Override
	public boolean isExecutable() throws IOException {
		return isTypeFlagSet(TYPE_EXE, buffer.metaChanged);
	}
	
	@Override
	public boolean isHidden() throws IOException {
		return isTypeFlagSet(TYPE_HIDDEN, buffer.metaChanged);
	}
	
	@Override
	public boolean isReadOnly() throws IOException {
		return isTypeFlagSet(TYPE_READ_ONLY, buffer.metaChanged);
	}
	
	protected boolean isTypeFlagSet(int t, boolean changed) throws IOException {
		synchronized (buffer) {
			if (buffer.type == TYPE_NONE || changed) {
				doType();
			}
			return (buffer.type & t) != 0;
		}
	}
	
	private void doType() throws IOException {
		buffer.element.withLock(() -> {
			buffer.type = 0;
			if (buffer.element.isFile()) {
				buffer.type |= TYPE_FILE;
			}
			if (buffer.element.isFolder()) {
				buffer.type |= TYPE_FOLDER;
				if (folder().isRoot()) {
					buffer.type |= TYPE_ROOT_FOLDER;
				}
			}
			if (buffer.element.isLink()) {
				buffer.type |= TYPE_LINK;
			}
			if (buffer.element.isExecutable()) {
				buffer.type |= TYPE_EXE;
			}
			if (buffer.element.isHidden()) {
				buffer.type |= TYPE_HIDDEN;
			}
			if (buffer.element.isReadOnly()) {
				buffer.type |= TYPE_READ_ONLY;
			}
		});
		buffer.metaChanged = false;
		buffer.elementTypeChanged = false;
	}
	
	@Override
	public void setExecutable(boolean isExecutale, long lock) throws IOException, ElementLockedException {
		setType(lock, TYPE_EXE, isExecutale, () -> buffer.element.setExecutable(isExecutale, lock));
	}
	
	@Override
	public void setHidden(boolean isHidden, long lock) throws IOException, ElementLockedException {
		setType(lock, TYPE_EXE, isHidden, () -> buffer.element.setHidden(isHidden, lock));
	}
	
	@Override
	public void setReadOnly(boolean isReadOnly, long lock) throws IOException, ElementLockedException {
		setType(lock, TYPE_EXE, isReadOnly, () -> buffer.element.setReadOnly(isReadOnly, lock));
	}
	
	private void setType(long lock, int t, boolean isSet, ThrowingRunnable <IOException> set) throws IOException, ElementLockedException {
		synchronized (buffer) {
			if (buffer.type != TYPE_NONE) {
				if ( (buffer.type & t) != 0 == isSet) {
					return;
				}
				set.execute();
				if (isSet) {
					buffer.type |= t;
				} else {
					buffer.type &= ~t;
				}
				buffer.metaModTime = 0L;
			} else {
				buffer.element.withLock(() -> {
					set.execute();
					doType();
					buffer.metaModTime = 0L;
				});
			}
		}
	}
	
	@Override
	public int getOwner() throws IOException {
		synchronized (buffer) {
			if (buffer.owner == 0 || buffer.metaChanged) {
				buffer.owner = buffer.element.getOwner();
			}
			return buffer.owner;
		}
	}
	
	@Override
	public void setOwner(int owner, long lock) throws IOException, ElementLockedException {
		synchronized (buffer) {
			buffer.element.setOwner(owner, lock);
			buffer.owner = owner;
			buffer.metaModTime = 0L;
		}
	}
	
	@Override
	public long getCreateTime() throws IOException {
		synchronized (buffer) {
			if (buffer.createTime == 0L) {
				buffer.createTime = buffer.element.getCreateTime();
			}
			return buffer.createTime;
		}
	}
	
	@Override
	public long getLastModTime() throws IOException {
		synchronized (buffer) {
			if (buffer.dataModTime == 0L) {
				buffer.dataModTime = buffer.element.getCreateTime();
			}
			return buffer.dataModTime;
		}
	}
	
	@Override
	public long getLastMetaModTime() throws IOException {
		synchronized (buffer) {
			if (buffer.metaModTime == 0L) {
				buffer.metaModTime = buffer.element.getCreateTime();
			}
			return buffer.metaModTime;
		}
	}
	
	@Override
	public long getLockData() throws IOException {
		synchronized (buffer) {
			if (buffer.lockData == 0L || buffer.metaChanged) {
				buffer.lockData = buffer.element.getLockData();
			}
			return buffer.lockData;
		}
	}
	
	@Override
	public long getLockTime() throws IOException {
		synchronized (buffer) {
			if (buffer.lockTime == 0L) {
				buffer.lockTime = buffer.element.getLockTime();
			}
			return buffer.lockData;
		}
	}
	
	@Override
	public void ensureAccess(long lock, long forbiddenBits, boolean readOnlyForbidden) throws IOException, ElementLockedException, IllegalArgumentException {
		synchronized (buffer) {
			buffer.element.ensureAccess(lock, forbiddenBits, readOnlyForbidden);
		}
	}
	
	@Override
	public void removeLock(long lock) throws IOException, ElementLockedException {
		synchronized (buffer) {
			buffer.element.removeLock(lock);
			buffer.lockData = 0L;
			buffer.lockTime = 0L;
			buffer.metaChanged = true;
		}
	}
	
	@Override
	public long lock(long lock) throws IOException, IllegalStateException, ElementLockedException {
		synchronized (buffer) {
			long l = buffer.element.lock(lock);
			buffer.lockData = 0L;
			buffer.lockTime = 0L;
			buffer.metaChanged = true;
			return l;
		}
	}
	
	@Override
	public void delete(long myLock, long parentLock) throws IOException, IllegalStateException, ElementLockedException {
		synchronized (buffer) {
			buffer.element.delete(myLock, parentLock);
		}
	}
	
	@Override
	public void setName(String name, long lock) throws IOException, NullPointerException, IllegalStateException, ElementLockedException {
		synchronized (buffer) {
			if (buffer.name != null && name != null) {
				String n = buffer.name.get();
				if (n != null && name.equals(n)) {
					buffer.element.ensureAccess(lock, PatrFileSysConstants.LOCK_NO_META_CHANGE_ALLOWED_LOCK, false);
					return;
				}
			}
			buffer.element.setName(name, lock);
			buffer.name = null;
		}
	}
	
	@Override
	public String getName() throws IOException {
		synchronized (buffer) {
			String name;
			if (buffer.name == null || buffer.metaChanged) {
				name = buffer.element.getName();
				buffer.name = new WeakReference <>(name);
			} else {
				name = buffer.name.get();
				if (name == null) {
					name = buffer.element.getName();
					buffer.name = new WeakReference <>(name);
				}
			}
			return name;
		}
	}
	
	@Override
	public <T extends Throwable> void withLock(ThrowingRunnable <T> exec) throws T, IOException {
		synchronized (buffer) {
			buffer.element.withLock(exec);
		}
	}
	
	@Override
	public <T extends Throwable, R> R withLock(ThrowingSupplier <T, R> exec) throws T, IOException {
		synchronized (buffer) {
			return buffer.element.withLock(exec);
		}
	}
	
	@Override
	public <T extends Throwable> int withLockInt(ThrowingIntSupplier <T> exec) throws T, IOException {
		synchronized (buffer) {
			return buffer.element.withLockInt(exec);
		}
	}
	
	@Override
	public <T extends Throwable> long withLockLong(ThrowingLongSupplier <T> exec) throws T, IOException {
		synchronized (buffer) {
			return buffer.element.withLockLong(exec);
		}
	}
	
	@Override
	public <T extends Throwable> boolean withLockBoolean(ThrowingBooleanSupplier <T> exec) throws T, IOException {
		synchronized (buffer) {
			return buffer.element.withLockBoolean(exec);
		}
	}
	
	@Override
	public <T extends Throwable> void simpleWithLock(ThrowingRunnable <T> exec) throws T {
		synchronized (buffer) {
			buffer.element.simpleWithLock(exec);
		}
	}
	
	@Override
	public <T extends Throwable, R> R simpleWithLock(ThrowingSupplier <T, R> exec) throws T {
		synchronized (buffer) {
			return buffer.element.simpleWithLock(exec);
		}
	}
	
	@Override
	public <T extends Throwable> int simpleWithLockInt(ThrowingIntSupplier <T> exec) throws T {
		synchronized (buffer) {
			return buffer.element.simpleWithLockInt(exec);
		}
	}
	
	@Override
	public <T extends Throwable> long simpleWithLockLong(ThrowingLongSupplier <T> exec) throws T {
		synchronized (buffer) {
			return buffer.element.simpleWithLockLong(exec);
		}
	}
	
	@Override
	public <T extends Throwable> boolean simpleWithLockBoolean(ThrowingBooleanSupplier <T> exec) throws T {
		synchronized (buffer) {
			return buffer.element.simpleWithLockBoolean(exec);
		}
	}
	
	protected PatrFolder folder() throws IOException {
		PatrFolder folder;
		if (buffer.folder != null) {
			folder = buffer.folder.get();
			if (folder != null) {
				return folder;
			}
		}
		if (buffer.element instanceof PatrFolder) {
			folder = (PatrFolder) buffer.element;
		} else {
			if (buffer.folder != null) {
				folder = buffer.folder.get();
				if (folder != null) {
					return folder;
				}
			}
			folder = buffer.element.getFolder();
			if ( !isLink()) {
				buffer.element = folder;
			}
		}
		buffer.folder = new WeakReference <>(folder);
		return folder;
	}
	
	protected PatrFile file() throws IOException {
		PatrFile file;
		if (buffer.file != null) {
			file = buffer.file.get();
			if (file != null) {
				return file;
			}
		}
		if (buffer.element instanceof PatrFile) {
			file = (PatrFile) buffer.element;
		} else {
			if (buffer.file != null) {
				file = buffer.file.get();
				if (file != null) {
					return file;
				}
			}
			file = buffer.element.getFile();
			if ( !isLink()) {
				buffer.element = file;
			}
		}
		buffer.file = new WeakReference <>(file);
		return file;
	}
	
	protected PatrLink link() throws IOException {
		PatrLink link;
		if (buffer.link != null) {
			link = buffer.link.get();
			if (link != null) {
				return link;
			}
		}
		if (buffer.element instanceof PatrLink) {
			link = (PatrLink) buffer.element;
		} else {
			if (buffer.link != null) {
				link = buffer.link.get();
				if (link != null) {
					return link;
				}
			}
			link = buffer.element.getLink();
			buffer.element = link;
		}
		buffer.link = new WeakReference <>(link);
		return link;
	}
	
}
