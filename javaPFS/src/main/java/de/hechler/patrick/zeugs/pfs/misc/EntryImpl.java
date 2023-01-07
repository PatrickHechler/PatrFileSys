package de.hechler.patrick.zeugs.pfs.misc;

import java.util.Map.Entry;

/**
 * this class provides a simple unmodifiable implementation of the {@link Entry}
 * interface
 * 
 * @author pat
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class EntryImpl<K, V> implements Entry<K, V> {
	
	/**
	 * the key value
	 * 
	 * @see #getKey()
	 */
	public final K key;
	/**
	 * the value value
	 * 
	 * @see #getValue()
	 */
	public final V val;
	
	/**
	 * creates a new {@link EntryImpl} with the given key and value
	 * 
	 * @param key the key value
	 * @param val the value value
	 */
	public EntryImpl(K key, V val) {
		this.key = key;
		this.val = val;
	}
	
	@Override
	public K getKey() { return key; }
	
	@Override
	public V getValue() { return val; }
	
	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException("setValue");
	}
	
}
