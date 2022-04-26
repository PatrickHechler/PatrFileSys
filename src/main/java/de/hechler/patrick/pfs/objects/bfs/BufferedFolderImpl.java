package de.hechler.patrick.pfs.objects.bfs;

import java.io.IOException;
import java.lang.ref.WeakReference;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrLink;
import de.hechler.patrick.pfs.objects.IntHashMap;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;


public class BufferedFolderImpl extends BufferedFileSysElementImpl implements PatrFolder {
	
	public BufferedFolderImpl(PatrFileSysElementBuffer buffer) {
		super(buffer);
	}
	
	@Override
	public PatrFolder addFolder(String name, long lock) throws IOException, NullPointerException, ElementLockedException {
		synchronized (buffer) {
			PatrFolder added = folder().addFolder(name, lock);
			buffer.dataChanged = true;
			buffer.fs.changeSize();
			return added;
		}
	}
	
	@Override
	public PatrFile addFile(String name, long lock) throws IOException, NullPointerException, ElementLockedException {
		synchronized (buffer) {
			PatrFile added = folder().addFile(name, lock);
			buffer.dataChanged = true;
			buffer.fs.changeSize();
			return added;
		}
	}
	
	@Override
	public PatrLink addLink(String name, PatrFileSysElement target, long lock) throws IOException, NullPointerException, ElementLockedException {
		if ( ! (target instanceof BufferedFileSysElementImpl)) {
			throw new IllegalArgumentException("target is from an unknown type!");
		}
		BufferedFileSysElementImpl e = (BufferedFileSysElementImpl) target;
		if (e.buffer.fs != buffer.fs) {
			throw new IllegalArgumentException("target is from a diffrent file system!");
		}
		synchronized (buffer) {
			PatrLink added = folder().addLink(name, target, lock);
			buffer.dataChanged = true;
			buffer.fs.changeSize();
			return added;
		}
	}
	
	@Override
	public boolean isRoot() throws IOException {
		return isTypeFlagSet(TYPE_ROOT_FOLDER, buffer.elementTypeChanged);
	}
	
	@Override
	public int elementCount(long lock) throws ElementLockedException, IOException {
		synchronized (buffer) {
			if (buffer.elementCount == 0) {
				buffer.elementCount = folder().elementCount(lock);
			} else {
				ensureAccess(lock, PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK, false);
			}
			return buffer.elementCount;
		}
	}
	
	@Override
	public PatrFileSysElement getElement(int index, long lock) throws ElementLockedException, IOException {
		synchronized (buffer) {
			if (buffer.dataChanged) {
				buffer.childs.clear();
				buffer.dataChanged = false;
			}
			if (buffer.childs == null) {
				buffer.childs = new IntHashMap <>(elementCount(lock));
			} else {
				WeakReference <PatrFileSysElementBuffer> ref = buffer.childs.get(index);
				if (ref != null) {
					PatrFileSysElementBuffer childBuf = ref.get();
					if (childBuf != null) {
						BufferedFileSysElementImpl child = childBuf.me.get();
						if (child == null) {
							child = new BufferedFileSysElementImpl(childBuf);
							childBuf.me = new WeakReference <>(child);
						}
						ensureAccess(lock, PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK, false);
						return child;
					}
				}
			}
			PatrFileSysElement child = folder().getElement(index, lock);
			PatrFileSysElementBuffer childBuf = new PatrFileSysElementBuffer(buffer.fs, child);
			childBuf.parent = new WeakReference <>(buffer);
			BufferedFileSysElementImpl bufChild = new BufferedFileSysElementImpl(childBuf);
			buffer.childs.put(index, new WeakReference <>(childBuf));
			return bufChild;
		}
	}
	
}
