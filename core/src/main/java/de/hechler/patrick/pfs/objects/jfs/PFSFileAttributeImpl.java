package de.hechler.patrick.pfs.objects.jfs;

import java.nio.file.attribute.FileAttribute;

public class PFSFileAttributeImpl <T> implements FileAttribute <T> {
	
	public final String name;
	public final T      value;
	
	public PFSFileAttributeImpl(String name, T value) {
		this.name = name;
		this.value = value;
	}
	
	@Override
	public String name() {
		return name;
	}
	
	@Override
	public T value() {
		return value;
	}
	
}
