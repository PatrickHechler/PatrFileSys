package de.hechler.patrick.zeugs.pfs.records;

public record StreamOptions(boolean read, boolean write, boolean append, boolean seekable) {
	public StreamOptions(boolean read, boolean write) {
		this(read, write, false, false);
	}

	public StreamOptions(boolean read, boolean write, boolean append) {
		this(read, write || append, append, false);
	}

	/**
	 * returns <code>true</code> if {@link #write} or {@link #append} is set and
	 * <code>false</code> if {@link #write} and {@link #append} is
	 * <code>false</code>
	 * 
	 * @return <code>true</code> if {@link #write} or {@link #append} is set and
	 *         <code>false</code> if {@link #write} and {@link #append} is
	 *         <code>false</code>
	 */
	public boolean write() {
		return this.write || this.append;
	}

	@Override
	public String toString() {
		return "StreamOptions [read=" + read + ", write=" + write() + ", append=" + append + ", seekable=" + seekable
				+ "]";
	}

}
