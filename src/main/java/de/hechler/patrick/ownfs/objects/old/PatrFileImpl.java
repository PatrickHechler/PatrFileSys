package de.hechler.patrick.ownfs.objects.old;

import java.io.IOException;

import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFile;


public class PatrFileImpl extends PatrFSElement implements PatrFile {

	public PatrFileImpl(BlockManager bm, long block, int pos) {
		super(bm, block, pos);
	}

	@Override
	public void getContent(byte[] bytes, long offset, int bytesOff, int length) throws IllegalArgumentException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeContent(long offset, long length) throws IllegalArgumentException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContent(byte[] bytes, long offset, int bytesOff, int length) throws IllegalArgumentException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void appendContent(byte[] bytes, int bytesOff, int length) throws IllegalArgumentException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long length() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void delete() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}