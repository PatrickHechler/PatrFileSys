//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
