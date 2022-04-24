package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FILE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LINK_OFFSET_TARGET_ID;

import java.io.IOException;

import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrLink;


public class PatrLinkImpl extends PatrFileSysElementImpl implements PatrLink {
	
	public PatrLinkImpl(PatrFileSysImpl fs, long startTime, BlockManager bm, long id) {
		super(fs, startTime, bm, id);
	}
	
	@Override
	public PatrFileSysElement getTarget() throws IOException {
		return simpleWithLock(() -> {
			updatePosAndBlock();
			return executeGetTarget();
		});
	}
	
	private PatrFileSysElement executeGetTarget() throws IOException {
		updatePosAndBlock();
		byte[] bytes = bm.getBlock(block);
		try {
			long tid = byteArrToLong(bytes, pos + LINK_OFFSET_TARGET_ID);
			return new PatrFileSysElementImpl(fs, startTime, bm, tid);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public PatrFile getTargetFile() throws IOException, IllegalStateException {
		return simpleWithLock(() -> {
			updatePosAndBlock();
			return executeGetTargetFile();
		});
	}
	
	private PatrFile executeGetTargetFile() throws IOException {
		if ( !isFile()) {
			throw new IllegalStateException("this is no file link!");
		}
		byte[] bytes = bm.getBlock(block);
		try {
			long tid = byteArrToLong(bytes, pos + LINK_OFFSET_TARGET_ID);
			return new PatrFileImpl(fs, startTime, bm, tid);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public PatrFolder getTargetFolder() throws IOException, IllegalStateException {
		return simpleWithLock(() -> {
			updatePosAndBlock();
			return executeGetTargetFolder();
		});
	}
	
	private PatrFolder executeGetTargetFolder() throws IOException {
		if ( !isFile()) {
			throw new IllegalStateException("this is no file link!");
		}
		byte[] bytes = bm.getBlock(block);
		try {
			long tid = byteArrToLong(bytes, pos + LINK_OFFSET_TARGET_ID);
			return new PatrFolderImpl(fs, startTime, bm, tid);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void setTarget(PatrFileSysElement newTarget, long lock) throws IOException, IllegalArgumentException, NullPointerException {
		if (newTarget == null) {
			throw new NullPointerException("newTarget is null");
		}
		if ( ! (newTarget instanceof PatrFileSysElementImpl)) {
			throw new IllegalArgumentException("the newTarget is unknown! newTarget.class: " + newTarget.getClass() + " newTarget.tos: '" + newTarget + "'");
		}
		PatrFileSysElementImpl nt = (PatrFileSysElementImpl) newTarget;
		if (nt.fs != fs) {
			throw new IllegalArgumentException("the target has aa diffrent file system!");
		}
		withLock(() -> executeSetTarget(nt));
	}
	
	private void executeSetTarget(PatrFileSysElementImpl nt) throws IOException {
		if (nt.isLink()) {
			throw new IllegalArgumentException("i will not link a link!");
		}
		updatePosAndBlock();
		byte[] bytes = bm.getBlock(block);
		try {
			longToByteArr(bytes, pos + LINK_OFFSET_TARGET_ID, nt.id);
			if (nt.isFolder()) {
				flag(ELEMENT_FLAG_FILE, ELEMENT_FLAG_FOLDER);
			} else {
				flag(ELEMENT_FLAG_FOLDER, ELEMENT_FLAG_FILE);
			}
		} finally {
			bm.setBlock(block);
		}
	}
	
}