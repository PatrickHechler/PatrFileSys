package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;

import de.hechler.patrick.zeugs.pfs.objects.misc.EntryImpl;

/**
 * the {@link Folder} provides methods:
 * <ul>
 * <li>to get handles for the children.
 * <ul>
 * <li>for generic children (over the {@link #iter(boolean)} method)</li>
 * <li>for special children (over the {@link #childElement(String)},
 * {@link #childFile(String)}, {@link #childFolder(String)} and
 * {@link #childPipe(String)} methods).</li>
 * </ul>
 * <li>add children to the folder (with the {@link #createFile(String)},
 * {@link #createFolder(String)} and {@link #createPipe(String)} methods)</li>
 * <li>remove children #re
 * </ul>
 * 
 * @author pat
 */
public interface Folder extends FSElement, Iterable<FSElement>, Map<String, FSElement> {

	/**
	 * returns an {@link FolderIter} which iterates over the children of this folder
	 * <p>
	 * if {@code showHidden} is <code>true</code> the iterator will return all child
	 * elements of this folder and <code>false</code> if the iterator should only
	 * the elements which are not marked with {@link FSElement#FLAG_HIDDEN}.
	 * 
	 * @param showHidden <code>true</code> if all elements should be returned by the
	 *                   returned {@link FolderIter} and <code>false</code> if only
	 *                   non hidden elements should be returned by the returned
	 *                   {@link FolderIter}
	 * @return an {@link FolderIter} which iterates over the children of this folder
	 * @throws IOException
	 */
	FolderIter iter(boolean showHidden) throws IOException;

	/**
	 * returns an {@link FolderIter} which iterates over all non
	 * {@link FSElement#FLAG_HIDDEN hidden} children of this Folder
	 * <p>
	 * this method works like {@link #iter(boolean)} with <code>false</code> as
	 * argument
	 * 
	 * @return an {@link FolderIter} which iterates over all non
	 *         {@link FSElement#FLAG_HIDDEN hidden} children of this Folder
	 */
	@Override
	default FolderIter iterator() {
		try {
			return iter(false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * returns the number of child elements this folder has
	 * 
	 * @return the number of child elements this folder has
	 * @throws IOException
	 */
	long childCount() throws IOException;

	/**
	 * returns the child element with the given name.<br>
	 * this method will fail if this folder does not has a child with the given name
	 * 
	 * @param name the name of the child
	 * @return the child element with the given name
	 * @throws IOException
	 */
	FSElement childElement(String name) throws IOException;

	/**
	 * returns the child element with the given name.<br>
	 * this method will fail
	 * <ul>
	 * <li>if this folder does not has a child with the given name</li>
	 * <li>if the child with the given name is no folder</li>
	 * </ul>
	 * 
	 * @param name the name of the child
	 * @return the child element with the given name
	 * @throws IOException
	 */
	Folder childFolder(String name) throws IOException;

	/**
	 * returns the child element with the given name.<br>
	 * this method will fail
	 * <ul>
	 * <li>if this folder does not has a child with the given name</li>
	 * <li>if the child with the given name is no file</li>
	 * </ul>
	 * 
	 * @param name the name of the child
	 * @return the child element with the given name
	 * @throws IOException
	 */
	File childFile(String name) throws IOException;

	/**
	 * returns the child element with the given name.<br>
	 * this method will fail
	 * <ul>
	 * <li>if this folder does not has a child with the given name</li>
	 * <li>if the child with the given name is no pipe</li>
	 * </ul>
	 * 
	 * @param name the name of the child
	 * @return the child element with the given name
	 * @throws IOException
	 */
	Pipe childPipe(String name) throws IOException;

	/**
	 * creates a new child folder with the given name and adds the new child to this
	 * folder
	 * <p>
	 * the child folder will be initially empty
	 * 
	 * @param name the name of the new child
	 * @return the newly created child
	 * @throws IOException
	 */
	Folder createFolder(String name) throws IOException;

	/**
	 * creates a new child file with the given name and adds the new child to this
	 * folder
	 * <p>
	 * the child file will be initially empty
	 * 
	 * @param name the name of the new child
	 * @return the newly created child
	 * @throws IOException
	 */
	File createFile(String name) throws IOException;

	/**
	 * creates a new child pipe with the given name and adds the new child to this
	 * folder
	 * <p>
	 * the child pipe will be initially empty
	 * 
	 * @param name the name of the new child
	 * @return the newly created child
	 * @throws IOException
	 */
	Pipe createPipe(String name) throws IOException;

	/**
	 * this interface describes an {@link Iterator} which returns {@link FSElement}
	 * objects
	 * <p>
	 * in addition to the {@link Iterator} methods {@link #next()}
	 * {@link #hasNext()} and {@link #remove()} this interface adds the three
	 * methods {@link #nextElement()}, {@link #hasNextElement()} and
	 * {@link #delete()} which do the same work, but are allowed to throw an
	 * {@link IOException}
	 * 
	 * @author pat
	 */
	static interface FolderIter extends Iterator<FSElement> {

		/**
		 * returns the next element
		 * 
		 * @return the next element
		 * @throws IOException
		 */
		FSElement nextElement() throws IOException;

		/**
		 * returns <code>true</code> if there is a next element and <code>false</code>
		 * if not.
		 * 
		 * @return <code>true</code> if there is a next element and <code>false</code>
		 *         if not
		 * @throws IOException
		 */
		boolean hasNextElement() throws IOException;

		/**
		 * {@link FSElement#delete() deletes} the element which has been returned from
		 * the last {@link #nextElement()} or {@link #next()} call.
		 * 
		 * @throws IOException
		 */
		void delete() throws IOException;

		/**
		 * returns the next element
		 * 
		 * @return the next element
		 */
		@Override
		default FSElement next() {
			try {
				return nextElement();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * returns <code>true</code> if there is a next element and <code>false</code>
		 * if not.
		 * 
		 * @return <code>true</code> if there is a next element and <code>false</code>
		 *         if not
		 */
		@Override
		default boolean hasNext() {
			try {
				return hasNextElement();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * {@link FSElement#delete() deletes} the element which has been returned from
		 * the last {@link #nextElement()} or {@link #next()} call.
		 */
		@Override
		default void remove() {
			try {
				delete();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * returns the number of child elements this folder has.
	 * <p>
	 * if the folder has more children than {@link Integer#MAX_VALUE},
	 * {@link Integer#MAX_VALUE} is returned instead
	 * 
	 * @return the number of child elements this folder has
	 */
	@Override
	default int size() {
		try {
			long cc = childCount();
			return (int) (cc <= Integer.MAX_VALUE ? cc : Integer.MAX_VALUE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * deletes all children this folder has.
	 */
	@Override
	default void clear() {
		try {
			for (FolderIter iter = this.iter(true); iter.hasNextElement();) {
				iter.nextElement();
				iter.delete();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * returns <code>true</code> if this folder contains a child with the given name
	 * and <code>false</code> if not
	 * 
	 * @param name the name of the child
	 * @return <code>true</code> if this folder contains a child with the given name
	 *         and <code>false</code> if not
	 */
	default boolean contains(String name) {
		try {
			childElement(name);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * returns <code>true</code> if this folder is the (direct) parent folder of the
	 * given element and <code>false</code> if not
	 * 
	 * @param element the potential (direct) child element of this folder
	 * @return <code>true</code> if this folder is the (direct) parent folder of the
	 *         given element and <code>false</code> if not
	 */
	default boolean contains(FSElement element) {
		try {
			return equals(element.parent());
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * returns <code>true</code> if this folder contains a child with the given name
	 * and <code>false</code> if not
	 * 
	 * @param key the name of the child
	 * @return <code>true</code> if this folder contains a child with the given name
	 *         and <code>false</code> if not
	 */
	@Override
	default boolean containsKey(Object key) {
		if (key instanceof String str) {
			return contains(str);
		}
		return false;
	}

	/**
	 * returns <code>true</code> if this folder is the (direct) parent of the given
	 * object and <code>false</code> if not
	 * 
	 * @param value the child
	 * @return <code>true</code> if this folder is the (direct) parent of the given
	 *         object and <code>false</code> if not
	 */
	@Override
	default boolean containsValue(Object value) {
		if (value instanceof FSElement val) {
			return contains(val);
		}
		return false;
	}

	@Override
	default Set<Entry<String, FSElement>> entrySet() {
		return new FolderSet(this);
	}

	static class FolderSet implements Set<Entry<String, FSElement>> {

		protected final Folder f;

		public FolderSet(Folder f) {
			this.f = f;
		}

		@Override
		public int size() {
			return f.size();
		}

		@Override
		public boolean isEmpty() {
			return f.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof Entry<?, ?> e) {
				Object k = e.getKey();
				if (k == null) {
					return false;
				} else if (k instanceof String) {
					Object v = e.getValue();
					if (v == null) {
						return false;
					} else if (v instanceof FSElement ce) {
						return f.contains(ce);
					}
					return false;
				}
			}
			return false;
		}

		@Override
		public Iterator<Entry<String, FSElement>> iterator() {
			return new FolderSetIter(f);
		}

		public static class FolderSetIter implements Iterator<Map.Entry<String, FSElement>> {

			private final Iterator<FSElement> iter;

			public FolderSetIter(Folder folder) {
				this.iter = folder.iterator();
			}

			public FolderSetIter(Iterator<FSElement> iter) {
				this.iter = iter;
			}

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public Entry<String, FSElement> next() {
				FSElement fse = iter.next();
				try {
					return new EntryImpl<>(fse.name(), fse);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void remove() {
				iter.remove();
			}

		}

		@Override
		public Object[] toArray() {
			return toArray(new Object[0]);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T[] toArray(final T[] arg) {
			try {
				long ls = f.childCount();
				if (ls > Integer.MAX_VALUE) {
					throw new IndexOutOfBoundsException("I have more children than Integer.MAX_VALUE");
				}
				int s = (int) ls;
				T[] res = arg.length > s ? (T[]) Array.newInstance(arg.getClass().getComponentType(), s) : arg;
				for (FolderIter iter = f.iter(true); iter.hasNextElement();) {
					if (s <= 0) {
						assert s == 0;
						ls = f.childCount();
						if (ls > Integer.MAX_VALUE) {
							throw new IndexOutOfBoundsException("I have more children than Integer.MAX_VALUE");
						}
						T[] res2 = arg.length > ls
								? (T[]) Array.newInstance(arg.getClass().getComponentType(), (int) ls)
								: arg;
						s = (int) (ls - res.length);
						System.arraycopy(res, 0, res2, s, res.length);

					}
					res[--s] = (T) iter.nextElement();
				}
				if (res.length > ls) {
					if (res == arg) {
						res[(int) ls] = null;
					} else {
						T[] res2 = (T[]) Array.newInstance(arg.getClass().getComponentType(), (int) ls);
						System.arraycopy(res, 0, res2, 0, (int) ls);
						res = res2;
					}
				}
				return res;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean add(Entry<String, FSElement> e) {
			throw new UnsupportedOperationException("add");
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Entry<?, ?> e) {
				Object k = e.getKey();
				if (k == null) {
					return false;
				} else if (k instanceof String name) {
					Object v = e.getValue();
					if (v == null) {
						return false;
					} else if (v instanceof FSElement fse) {
						if (f.contains(fse)) {
							try {
								fse.delete();
							} catch (IOException e1) {
								throw new RuntimeException(e1);
							}
						}
					}
				}
			}
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object object : c) {
				if (!contains(object)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends Entry<String, FSElement>> c) {
			throw new UnsupportedOperationException("addAll");
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			try {
				boolean mod = false;
				for (FolderIter iter = f.iter(true); iter.hasNextElement();) {
					FSElement e = iter.nextElement();
					if (c.contains(e)) {
						iter.delete();
						mod = true;
					}
				}
				return mod;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean res = false;
			for (Object obj : c) {
				res |= remove(obj);
			}
			return res;
		}

		@Override
		public void clear() {
			try {
				for (FolderIter iter = f.iter(true); iter.hasNextElement();) {
					iter.nextElement().delete();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	default FSElement get(Object key) {
		if (key instanceof String name) {
			try {
				return childElement(name);
			} catch (IOException e) {
			}
		}
		return null;
	}

	@Override
	default FSElement getOrDefault(Object key, FSElement defaultValue) {
		if (key instanceof String str) {
			try {
				return childElement(str);
			} catch (IOException e) {
			}
		}
		return defaultValue;
	}

	@Override
	default FSElement put(String key, FSElement value) {
		throw new UnsupportedOperationException("put");
	}

	@Override
	default void putAll(Map<? extends String, ? extends FSElement> m) {
		throw new UnsupportedOperationException("put");
	}

	@Override
	default FSElement putIfAbsent(String key, FSElement value) {
		throw new UnsupportedOperationException("put");
	}

	@Override
	default FSElement remove(Object key) {
		if (key instanceof String name) {
			try {
				FSElement e = childElement(name);
				e.delete();
				return e;
			} catch (IOException e) {
				return null;
			}
		}
		return null;
	}

	@Override
	default boolean remove(Object key, Object value) {
		if (key instanceof String name) {
			try {
				FSElement ce = childElement(name);
				if (value.equals(ce)) {
					try {
						ce.delete();
						return true;
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			} catch (IOException e1) {
			}
		}
		return false;
	}

	@Override
	default FSElement compute(String key,
			BiFunction<? super String, ? super FSElement, ? extends FSElement> remappingFunction) {
		throw new UnsupportedOperationException("compute");
	}

	@Override
	default FSElement computeIfAbsent(String key, Function<? super String, ? extends FSElement> mappingFunction) {
		throw new UnsupportedOperationException("compute");
	}

	@Override
	default FSElement computeIfPresent(String key,
			BiFunction<? super String, ? super FSElement, ? extends FSElement> remappingFunction) {
		throw new UnsupportedOperationException("compute");
	}

	@Override
	default boolean replace(String key, FSElement oldValue, FSElement newValue) {
		throw new UnsupportedOperationException("replace");
	}

	@Override
	default FSElement replace(String key, FSElement value) {
		throw new UnsupportedOperationException("replace");
	}

	@Override
	default void replaceAll(BiFunction<? super String, ? super FSElement, ? extends FSElement> function) {
		throw new UnsupportedOperationException("replace");
	}

	@Override
	default FSElement merge(String key, FSElement value,
			BiFunction<? super FSElement, ? super FSElement, ? extends FSElement> remappingFunction) {
		throw new UnsupportedOperationException("merge");
	}

	@Override
	default Spliterator<FSElement> spliterator() {
		try {
			return Spliterators.spliterator(iterator(), childCount(),
					Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.SIZED);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	default Set<String> keySet() {
		return new AbstractSet<String>() {
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					private FolderIter i = Folder.this.iterator();

					public boolean hasNext() {
						return i.hasNext();
					}

					public String next() {
						try {
							return i.nextElement().name();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}

					public void remove() {
						i.remove();
					}
				};
			}

			public int size() {
				return Folder.this.size();
			}

			public boolean isEmpty() {
				return Folder.this.isEmpty();
			}

			public void clear() {
				Folder.this.clear();
			}

			public boolean contains(Object k) {
				return Folder.this.containsKey(k);
			}
		};
	}

	@Override
	default Collection<FSElement> values() {
		return new AbstractCollection<FSElement>() {

			@Override
			public Iterator<FSElement> iterator() {
				return Folder.this.iterator();
			}

			@Override
			public int size() {
				return Folder.this.size();
			}

			@Override
			public boolean isEmpty() {
				return Folder.this.isEmpty();
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof FSElement ce) {
					if (Folder.this.contains(ce)) {
						try {
							ce.delete();
							return true;
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
				return false;
			}

		};
	}
}
