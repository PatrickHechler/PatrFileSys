package de.hechler.patrick.zeugs.pfs.misc;

import java.util.Map.Entry;

public class EntryImpl<K, V> implements Entry<K, V> {

	public final K key;
	public final V val;

	public EntryImpl(K key, V val) {
		this.key = key;
		this.val = val;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return val;
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException("setValue");
	}

}
