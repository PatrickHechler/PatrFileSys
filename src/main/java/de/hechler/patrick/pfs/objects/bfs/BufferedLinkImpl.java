package de.hechler.patrick.pfs.objects.bfs;

import java.io.IOException;
import java.lang.ref.WeakReference;

import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrLink;


public class BufferedLinkImpl extends BufferedFileSysElementImpl implements PatrLink {
	
	public BufferedLinkImpl(PatrFileSysElementBuffer buffer) {
		super(buffer);
	}
	
	@Override
	public PatrFileSysElement getTarget() throws IOException {
		if (buffer.target != null && !buffer.dataChanged) {
			PatrFileSysElementBuffer buf = buffer.target.get();
			if (buf != null) {
				BufferedFileSysElementImpl te;
				if (buf.me != null) {
					te = buf.me.get();
					if (te != null) {
						return te;
					}
				}
				te = new BufferedFileSysElementImpl(buf);
				buf.me = new WeakReference <>(te);
				return te;
			}
		}
		PatrFileSysElement target = link().getTarget();
		PatrFileSysElementBuffer buf = new PatrFileSysElementBuffer(buffer.fs, target);
		BufferedFileSysElementImpl te = new BufferedFileSysElementImpl(buf);
		buf.me = new WeakReference <>(te);
		return te;
	}
	
	@Override
	public void setTarget(PatrFileSysElement newTarget, long lock) throws IOException, IllegalArgumentException {
		synchronized (buffer) {
			link().setTarget(newTarget, lock);
			buffer.target = null;
		}
	}
	
}
