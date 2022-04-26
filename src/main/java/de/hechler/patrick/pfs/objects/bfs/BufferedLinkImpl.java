package de.hechler.patrick.pfs.objects.bfs;

import java.io.IOException;

import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrLink;


public class BufferedLinkImpl extends BufferedFileSysElementImpl implements PatrLink {
	
	public BufferedLinkImpl(PatrFileSysElementBuffer buffer) {
		super(buffer);
	}
	
	@Override
	public PatrFileSysElement getTarget() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PatrFile getTargetFile() throws IOException, IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PatrFolder getTargetFolder() throws IOException, IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setTarget(PatrFileSysElement newTarget, long lock) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}
