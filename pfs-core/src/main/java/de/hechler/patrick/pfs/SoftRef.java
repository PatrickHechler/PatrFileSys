package de.hechler.patrick.pfs;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class SoftRef <T> extends SoftReference <T> {
	
	public SoftRef(T referent) {
		super(referent);
	}
	
	public SoftRef(T referent, ReferenceQueue <? super T> q) {
		super(referent, q);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		T obj = get();
		result = prime * result + ( (obj == null) ? 0 : obj.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		T myRef = get();
		if (getClass() != obj.getClass()) {
			if (myRef == null) {
				return false;
			}
			return myRef.equals(obj);
		}
		SoftRef <?> other = (SoftRef <?>) obj;
		Object otherRef = other.get();
		if (myRef == null) {
			if (otherRef != null) {
				return false;
			}
		} else if ( !myRef.equals(otherRef)) {
			return false;
		}
		return true;
	}
	
}
