package de.hechler.patrick.ownfs.objects;

import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.*;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.*;

import java.io.IOException;

import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFile;


public class PatrFileImpl extends PatrFileSysElementImpl implements PatrFile {
	
	public PatrFileImpl(long startTime, BlockManager bm, long block, int pos) {
		super(startTime, bm, block, pos);
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
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToLong(bytes, pos + FILE_OFFSET_FILE_LENGTH);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void delete() throws IOException {
		bm.getBlock(block);
		try {
			removeContent(0, length());
			deleteFromParent();
			reallocate(block, pos, FILE_OFFSET_FILE_DATA_TABLE, 0, false);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
}
