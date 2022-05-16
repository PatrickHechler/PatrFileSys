package de.hechler.patrick.pfs.objects;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * like {@link HashMap}, bug as keys must be non <code>null</code> {@link Integer} objects.
 * 
 * @see HashMap
 */
public class IntHashMap <V> extends AbstractMap <Integer, V> implements Cloneable, Serializable, Map <Integer, V> {
	
	/** UID */
	private static final long serialVersionUID = -357344950706681798L;
	
	// Views
	
	/**
	 * Each of these fields are initialized to contain an instance of the appropriate view the first time this view is requested. The views are stateless, so there's no reason to create more than one
	 * of each.
	 *
	 * <p>
	 * Since there is no synchronization performed while accessing these fields, it is expected that java.util.Map view classes using these fields have no non-final fields (or any fields at all except
	 * for outer-this). Adhering to this rule would make the races on these fields benign.
	 *
	 * <p>
	 * It is also imperative that implementations read the field only once, as in:
	 *
	 * <pre>
	 *  {@code
	 * public Set<K> keySet() {
	 *   Set<K> ks = keySet;  // single racy read
	 *   if (ks == null) {
	 *     ks = new KeySet();
	 *     keySet = ks;
	 *   }
	 *   return ks;
	 * }
	 *}
	 * </pre>
	 */
	private transient Set <Integer>  keySet;
	private transient Collection <V> values;
	
	/*
	 * Implementation notes.
	 *
	 * This map usually acts as a binned (bucketed) hash table, but when bins get too large, they are transformed into bins of TreeNodes, each structured similarly to those in java.util.TreeMap. Most
	 * methods try to use normal bins, but relay to TreeNode methods when applicable (simply by checking instanceof a node). Bins of TreeNodes may be traversed and used like any others, but
	 * additionally support faster lookup when overpopulated. However, since the vast majority of bins in normal use are not overpopulated, checking for existence of tree bins may be delayed in the
	 * course of table methods.
	 *
	 * Tree bins (i.e., bins whose elements are all TreeNodes) are ordered primarily by hashCode, but in the case of ties, if two elements are of the same "class C implements Comparable<C>", type then
	 * their compareTo method is used for ordering. (We conservatively check generic types via reflection to validate this -- see method comparableClassFor). The added complexity of tree bins is
	 * worthwhile in providing worst-case O(log n) operations when keys either have distinct hashes or are orderable, Thus, performance degrades gracefully under accidental or malicious usages in
	 * which hashCode() methods return values that are poorly distributed, as well as those in which many keys share a hashCode, so long as they are also Comparable. (If neither of these apply, we may
	 * waste about a factor of two in time and space compared to taking no precautions. But the only known cases stem from poor user programming practices that are already so slow that this makes
	 * little difference.)
	 *
	 * Because TreeNodes are about twice the size of regular nodes, we use them only when bins contain enough nodes to warrant use (see TREEIFY_THRESHOLD). And when they become too small (due to
	 * removal or resizing) they are converted back to plain bins. In usages with well-distributed user hashCodes, tree bins are rarely used. Ideally, under random hashCodes, the frequency of nodes in
	 * bins follows a Poisson distribution (http://en.wikipedia.org/wiki/Poisson_distribution) with a parameter of about 0.5 on average for the default resizing threshold of 0.75, although with a
	 * large variance because of resizing granularity. Ignoring variance, the expected occurrences of list size k are (exp(-0.5) * pow(0.5, k) / factorial(k)). The first values are:
	 *
	 * 0: 0.60653066 1: 0.30326533 2: 0.07581633 3: 0.01263606 4: 0.00157952 5: 0.00015795 6: 0.00001316 7: 0.00000094 8: 0.00000006 more: less than 1 in ten million
	 *
	 * The root of a tree bin is normally its first node. However, sometimes (currently only upon Iterator.remove), the root might be elsewhere, but can be recovered following parent links (method
	 * TreeNode.root()).
	 *
	 * All applicable internal methods accept a hash code as an argument (as normally supplied from a public method), allowing them to call each other without recomputing user hashCodes. Most internal
	 * methods also accept a "tab" argument, that is normally the current table, but may be a new or old one when resizing or converting.
	 *
	 * When bin lists are treeified, split, or untreeified, we keep them in the same relative access/traversal order (i.e., field Node.next) to better preserve locality, and to slightly simplify
	 * handling of splits and traversals that invoke iterator.remove. When using comparators on insertion, to keep a total ordering (or as close as is required here) across rebalancings, we compare
	 * classes and identityHashCodes as tie-breakers.
	 *
	 * The use and transitions among plain vs tree modes is complicated by the existence of subclass LinkedHashMap. See below for hook methods defined to be invoked upon insertion, removal and access
	 * that allow LinkedHashMap internals to otherwise remain independent of these mechanics. (This also requires that a map instance be passed to some utility methods that may create new nodes.)
	 *
	 * The concurrent-programming-like SSA-based coding style helps avoid aliasing errors amid all of the twisty pointer operations.
	 */
	
	/**
	 * The default initial capacity - MUST be a power of two.
	 */
	private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
	
	/**
	 * The maximum capacity, used if a higher value is implicitly specified by either of the constructors with arguments. MUST be a power of two <= 1<<30.
	 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;
	
	/**
	 * The load factor used when none specified in constructor.
	 */
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	/**
	 * The bin count threshold for using a tree rather than list for a bin. Bins are converted to trees when adding an element to a bin with at least this many nodes. The value must be greater than 2
	 * and should be at least 8 to mesh with assumptions in tree removal about conversion back to plain bins upon shrinkage.
	 */
	private static final int TREEIFY_THRESHOLD = 8;
	
	/**
	 * The bin count threshold for untreeifying a (split) bin during a resize operation. Should be less than TREEIFY_THRESHOLD, and at most 6 to mesh with shrinkage detection under removal.
	 */
	private static final int UNTREEIFY_THRESHOLD = 6;
	
	/**
	 * The smallest table capacity for which bins may be treeified. (Otherwise the table is resized if too many nodes in a bin.) Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts between
	 * resizing and treeification thresholds.
	 */
	private static final int MIN_TREEIFY_CAPACITY = 64;
	
	/**
	 * Basic hash bin node, used for most entries. (See below for TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
	 */
	private static class HashMapNode <V> implements Map.Entry <Integer, V> {
		
		final int       hash;
		final int       key;
		V               value;
		HashMapNode <V> next;
		
		private HashMapNode(int hash, int key, V value, HashMapNode <V> next) {
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}
		
		public final Integer getKey() {
			return key;
		}
		
		public final V getValue() {
			return value;
		}
		
		public final String toString() {
			return key + "=" + value;
		}
		
		public final int hashCode() {
			if (value == null) {
				return key;
			} else {
				return key ^ value.hashCode();
			}
		}
		
		public final V setValue(V newValue) {
			V oldValue = value;
			value = newValue;
			return oldValue;
		}
		
		public final boolean equals(Object o) {
			if (o == this)
				return true;
			if (o instanceof Map.Entry) {
				Map.Entry <?, ?> e = (Map.Entry <?, ?>) o;
				if (Objects.equals(key, e.getKey()) &&
					Objects.equals(value, e.getValue()))
					return true;
			}
			return false;
		}
		
	}
	
	/* ---------------- utilities -------------- */
	
	/**
	 * Computes key.hashCode() and spreads (XORs) higher bits of hash to lower. Because the table uses power-of-two masking, sets of hashes that vary only in bits above the current mask will always
	 * collide. (Among known examples are sets of Float keys holding consecutive whole numbers in small tables.) So we apply a transform that spreads the impact of higher bits downward. There is a
	 * tradeoff between speed, utility, and quality of bit-spreading. Because many common sets of hashes are already reasonably distributed (so don't benefit from spreading), and because we use trees
	 * to handle large sets of collisions in bins, we just XOR some shifted bits in the cheapest possible way to reduce systematic lossage, as well as to incorporate impact of the highest bits that
	 * would otherwise never be used in index calculations because of table bounds.
	 */
	private static final int hash(int key) {
		return key ^ (key >>> 16);
	}
	
	/**
	 * Returns k.compareTo(x) if x matches kc (k's screened comparable class), else 0.
	 */
	// @SuppressWarnings({"rawtypes", "unchecked" }) // for cast to Comparable
	private static int compare(int k, int x) {
		return (k < x) ? -1 : ( (k == x) ? 0 : 1);
	}
	
	/**
	 * Returns a power of two size for the given target capacity.
	 */
	private static final int tableSizeFor(int cap) {
		int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
		return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
	}
	
	/* ---------------- Fields -------------- */
	
	/**
	 * The table, initialized on first use, and resized as necessary. When allocated, length is always a power of two. (We also tolerate length zero in some operations to allow bootstrapping mechanics
	 * that are currently not needed.)
	 */
	private transient HashMapNode <V>[] table;
	
	/**
	 * Holds cached entrySet(). Note that AbstractMap fields are used for keySet() and values().
	 */
	private transient Set <Map.Entry <Integer, V>> entrySet;
	
	/**
	 * The number of key-value mappings contained in this map.
	 */
	private transient int size;
	
	/**
	 * The number of times this HashMap has been structurally modified Structural modifications are those that change the number of mappings in the HashMap or otherwise modify its internal structure
	 * (e.g., rehash). This field is used to make iterators on Collection-views of the HashMap fail-fast. (See ConcurrentModificationException).
	 */
	private transient int modCount;
	
	/**
	 * The next size value at which to resize (capacity * load factor).
	 *
	 * @serial
	 */
	// (The javadoc description is true upon serialization.
	// Additionally, if the table array has not been allocated, this
	// field holds the initial array capacity, or zero signifying
	// DEFAULT_INITIAL_CAPACITY.)
	private int threshold;
	
	/**
	 * The load factor for the hash table.
	 *
	 * @serial
	 */
	private final float loadFactor;
	
	/* ---------------- Public operations -------------- */
	
	/**
	 * Constructs an empty {@code HashMap} with the specified initial capacity and load factor.
	 *
	 * @param initialCapacity
	 *            the initial capacity
	 * @param loadFactor
	 *            the load factor
	 * @throws IllegalArgumentException
	 *             if the initial capacity is negative or the load factor is nonpositive
	 */
	public IntHashMap(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal initial capacity: " +
				initialCapacity);
		if (initialCapacity > MAXIMUM_CAPACITY)
			initialCapacity = MAXIMUM_CAPACITY;
		if (loadFactor <= 0 || Float.isNaN(loadFactor))
			throw new IllegalArgumentException("Illegal load factor: " +
				loadFactor);
		this.loadFactor = loadFactor;
		this.threshold = tableSizeFor(initialCapacity);
	}
	
	/**
	 * Constructs an empty {@code HashMap} with the specified initial capacity and the default load factor (0.75).
	 *
	 * @param initialCapacity
	 *            the initial capacity.
	 * @throws IllegalArgumentException
	 *             if the initial capacity is negative.
	 */
	public IntHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}
	
	/**
	 * Constructs an empty {@code HashMap} with the default initial capacity (16) and the default load factor (0.75).
	 */
	public IntHashMap() {
		this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
	}
	
	/**
	 * Constructs a new {@code HashMap} with the same mappings as the specified {@code Map}. The {@code HashMap} is created with default load factor (0.75) and an initial capacity sufficient to hold
	 * the mappings in the specified {@code Map}.
	 *
	 * @param m
	 *            the map whose mappings are to be placed in this map
	 * @throws NullPointerException
	 *             if the specified map is null
	 */
	public IntHashMap(Map <Integer, ? extends V> m) {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		putMapEntries(m, false);
	}
	
	/**
	 * Implements Map.putAll and Map constructor.
	 *
	 * @param m
	 *            the map
	 * @param evict
	 *            false when initially constructing this map, else true (relayed to method afterNodeInsertion).
	 */
	private final void putMapEntries(Map <? extends Integer, ? extends V> m, boolean evict) {
		int s = m.size();
		if (s > 0) {
			if (table == null) { // pre-size
				float ft = ((float) s / loadFactor) + 1.0F;
				int t = ( (ft < (float) MAXIMUM_CAPACITY) ? (int) ft : MAXIMUM_CAPACITY);
				if (t > threshold)
					threshold = tableSizeFor(t);
			} else if (s > threshold)
				resize();
			for (Map.Entry <? extends Integer, ? extends V> e : m.entrySet()) {
				Integer key = e.getKey();
				V value = e.getValue();
				putVal(hash(key), key, value, false, evict);
			}
		}
	}
	
	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size() {
		return size;
	}
	
	/**
	 * Returns {@code true} if this map contains no key-value mappings.
	 *
	 * @return {@code true} if this map contains no key-value mappings
	 */
	public boolean isEmpty() {
		return size == 0;
	}
	
	/**
	 * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
	 *
	 * <p>
	 * More formally, if this map contains a mapping from a key {@code k} to a value {@code v} such that {@code (key==null ? k==null :
	 * key.equals(k))}, then this method returns {@code v}; otherwise it returns {@code null}. (There can be at most one such mapping.)
	 *
	 * <p>
	 * A return value of {@code null} does not <i>necessarily</i> indicate that the map contains no mapping for the key; it's also possible that the map explicitly maps the key to {@code null}. The
	 * {@link #containsKey containsKey} operation may be used to distinguish these two cases.
	 *
	 * @see #put(Object, Object)
	 */
	public V get(Object key) {
		if (key == null) {
			throw new NullPointerException("only non null Integer keys are allowed");
		}
		if ( ! (key instanceof Integer)) {
			throw new ClassCastException("can not use " + key.getClass() + " as int key!");
		}
		HashMapNode <V> e;
		int k = (int) key;
		return (e = getNode(hash(k), k)) == null ? null : e.value;
	}
	
	public V get(int k) {
		HashMapNode <V> e;
		return (e = getNode(hash(k), k)) == null ? null : e.value;
	}
	
	/**
	 * Implements Map.get and related methods.
	 *
	 * @param hash
	 *            hash for key
	 * @param key
	 *            the key
	 * @return the node, or null if none
	 */
	private final HashMapNode <V> getNode(int hash, int key) {
		HashMapNode <V>[] tab;
		HashMapNode <V> first, e;
		int n;
		if ( (tab = table) != null && (n = tab.length) > 0 &&
			(first = tab[ (n - 1) & hash]) != null) {
			if (first.hash == hash && // always check first node
				(first.key == key))
				return first;
			if ( (e = first.next) != null) {
				if (first instanceof TreeNode)
					return ((TreeNode <V>) first).getTreeNode(hash, key);
				do {
					if (e.hash == hash &&
						(e.key == key))
						return e;
				} while ( (e = e.next) != null);
			}
		}
		return null;
	}
	
	/**
	 * Returns {@code true} if this map contains a mapping for the specified key.
	 *
	 * @param key
	 *            The key whose presence in this map is to be tested
	 * @return {@code true} if this map contains a mapping for the specified key.
	 */
	public boolean containsKey(Object key) {
		if (key == null) return false;
		else if ( ! (key instanceof Integer)) return false;
		int k = (int) (Integer) key;
		return getNode(hash(k), k) != null;
	}
	
	/**
	 * Associates the specified value with the specified key in this map. If the map previously contained a mapping for the key, the old value is replaced.
	 *
	 * @param key
	 *            key with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the specified key
	 * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}. (A {@code null} return can also indicate that the map previously associated
	 *         {@code null} with {@code key}.)
	 */
	public V put(Integer key, V value) {
		int k = key;
		return putVal(hash(k), k, value, false, true);
	}
	
	public V put(int k, V value) {
		return putVal(hash(k), k, value, false, true);
	}
	
	
	
	/**
	 * Implements Map.put and related methods.
	 *
	 * @param hash
	 *            hash for key
	 * @param key
	 *            the key
	 * @param value
	 *            the value to put
	 * @param onlyIfAbsent
	 *            if true, don't change existing value
	 * @param evict
	 *            if false, the table is in creation mode.
	 * @return previous value, or null if none
	 */
	private final V putVal(int hash, int key, V value, boolean onlyIfAbsent,
		boolean evict) {
		HashMapNode <V>[] tab;
		HashMapNode <V> p;
		int n, i;
		if ( (tab = table) == null || (n = tab.length) == 0)
			n = (tab = resize()).length;
		if ( (p = tab[i = (n - 1) & hash]) == null)
			tab[i] = new HashMapNode <>(hash, key, value, null);
		else {
			HashMapNode <V> e;
			if (p.hash == hash &&
				(p.key == key))
				e = p;
			else if (p instanceof TreeNode)
				e = ((TreeNode <V>) p).putTreeVal(this, tab, hash, key, value);
			else {
				for (int binCount = 0;; ++ binCount) {
					if ( (e = p.next) == null) {
						p.next = newNode(hash, key, value, null);
						if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
							treeifyBin(tab, hash);
						break;
					}
					if (e.hash == hash &&
						(e.key == key))
						break;
					p = e;
				}
			}
			if (e != null) { // existing mapping for key
				V oldValue = e.value;
				if ( !onlyIfAbsent || oldValue == null)
					e.value = value;
				afterNodeAccess(e);
				return oldValue;
			}
		}
		++ modCount;
		if ( ++ size > threshold)
			resize();
		afterNodeInsertion(evict);
		return null;
	}
	
	/**
	 * Initializes or doubles table size. If null, allocates in accord with initial capacity target held in field threshold. Otherwise, because we are using power-of-two expansion, the elements from
	 * each bin must either stay at same index, or move with a power of two offset in the new table.
	 *
	 * @return the table
	 */
	private final HashMapNode <V>[] resize() {
		HashMapNode <V>[] oldTab = table;
		int oldCap = (oldTab == null) ? 0 : oldTab.length;
		int oldThr = threshold;
		int newCap, newThr = 0;
		if (oldCap > 0) {
			if (oldCap >= MAXIMUM_CAPACITY) {
				threshold = Integer.MAX_VALUE;
				return oldTab;
			} else if ( (newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
				oldCap >= DEFAULT_INITIAL_CAPACITY)
				newThr = oldThr << 1; // double threshold
		} else if (oldThr > 0) // initial capacity was placed in threshold
			newCap = oldThr;
		else { // zero initial threshold signifies using defaults
			newCap = DEFAULT_INITIAL_CAPACITY;
			newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
		}
		if (newThr == 0) {
			float ft = (float) newCap * loadFactor;
			newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY ? (int) ft : Integer.MAX_VALUE);
		}
		threshold = newThr;
		@SuppressWarnings({"unchecked" })
		HashMapNode <V>[] newTab = (HashMapNode <V>[]) new HashMapNode[newCap];
		table = newTab;
		if (oldTab != null) {
			for (int j = 0; j < oldCap; ++ j) {
				HashMapNode <V> e;
				if ( (e = oldTab[j]) != null) {
					oldTab[j] = null;
					if (e.next == null)
						newTab[e.hash & (newCap - 1)] = e;
					else if (e instanceof TreeNode)
						((TreeNode <V>) e).split(this, newTab, j, oldCap);
					else { // preserve order
						HashMapNode <V> loHead = null, loTail = null;
						HashMapNode <V> hiHead = null, hiTail = null;
						HashMapNode <V> next;
						do {
							next = e.next;
							if ( (e.hash & oldCap) == 0) {
								if (loTail == null)
									loHead = e;
								else
									loTail.next = e;
								loTail = e;
							} else {
								if (hiTail == null)
									hiHead = e;
								else
									hiTail.next = e;
								hiTail = e;
							}
						} while ( (e = next) != null);
						if (loTail != null) {
							loTail.next = null;
							newTab[j] = loHead;
						}
						if (hiTail != null) {
							hiTail.next = null;
							newTab[j + oldCap] = hiHead;
						}
					}
				}
			}
		}
		return newTab;
	}
	
	/**
	 * Replaces all linked nodes in bin at index for given hash unless table is too small, in which case resizes instead.
	 */
	private final void treeifyBin(HashMapNode <V>[] tab, int hash) {
		int n, index;
		HashMapNode <V> e;
		if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
			resize();
		else if ( (e = tab[index = (n - 1) & hash]) != null) {
			TreeNode <V> hd = null, tl = null;
			do {
				TreeNode <V> p = replacementTreeNode(e, null);
				if (tl == null)
					hd = p;
				else {
					p.prev = tl;
					tl.next = p;
				}
				tl = p;
			} while ( (e = e.next) != null);
			if ( (tab[index] = hd) != null)
				hd.treeify(tab);
		}
	}
	
	/**
	 * Copies all of the mappings from the specified map to this map. These mappings will replace any mappings that this map had for any of the keys currently in the specified map.
	 *
	 * @param m
	 *            mappings to be stored in this map
	 * @throws NullPointerException
	 *             if the specified map is null
	 */
	public void putAll(Map <? extends Integer, ? extends V> m) {
		putMapEntries(m, true);
	}
	
	/**
	 * Removes the mapping for the specified key from this map if present.
	 *
	 * @param key
	 *            key whose mapping is to be removed from the map
	 * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}. (A {@code null} return can also indicate that the map previously associated
	 *         {@code null} with {@code key}.)
	 */
	public V remove(Object key) {
		HashMapNode <V> e;
		if (key == null) {
			throw new NullPointerException("key is null");
		}
		int k = (Integer) key;
		return (e = removeNode(hash(k), k, null, false, true)) == null ? null : e.value;
	}
	
	/**
	 * Implements Map.remove and related methods.
	 *
	 * @param hash
	 *            hash for key
	 * @param key
	 *            the key
	 * @param value
	 *            the value to match if matchValue, else ignored
	 * @param matchValue
	 *            if true only remove if value is equal
	 * @param movable
	 *            if false do not move other nodes while removing
	 * @return the node, or null if none
	 */
	final HashMapNode <V> removeNode(int hash, int key, Object value,
		boolean matchValue, boolean movable) {
		HashMapNode <V>[] tab;
		HashMapNode <V> p;
		int n, index;
		if ( (tab = table) != null && (n = tab.length) > 0 &&
			(p = tab[index = (n - 1) & hash]) != null) {
			HashMapNode <V> node = null, e;
			V v;
			if (p.hash == hash &&
				(p.key == key))
				node = p;
			else if ( (e = p.next) != null) {
				if (p instanceof TreeNode)
					node = ((TreeNode <V>) p).getTreeNode(hash, key);
				else {
					do {
						if (e.hash == hash &&
							(e.key == key)) {
							node = e;
							break;
						}
						p = e;
					} while ( (e = e.next) != null);
				}
			}
			if (node != null && ( !matchValue || (v = node.value) == value ||
				(value != null && value.equals(v)))) {
				if (node instanceof TreeNode)
					((TreeNode <V>) node).removeTreeNode(this, tab, movable);
				else if (node == p)
					tab[index] = node.next;
				else
					p.next = node.next;
				++ modCount;
				-- size;
				afterNodeRemoval(node);
				return node;
			}
		}
		return null;
	}
	
	/**
	 * Removes all of the mappings from this map. The map will be empty after this call returns.
	 */
	public void clear() {
		HashMapNode <V>[] tab;
		modCount ++ ;
		if ( (tab = table) != null && size > 0) {
			size = 0;
			for (int i = 0; i < tab.length; ++ i)
				tab[i] = null;
		}
	}
	
	/**
	 * Returns {@code true} if this map maps one or more keys to the specified value.
	 *
	 * @param value
	 *            value whose presence in this map is to be tested
	 * @return {@code true} if this map maps one or more keys to the specified value
	 */
	public boolean containsValue(Object value) {
		HashMapNode <V>[] tab;
		V v;
		if ( (tab = table) != null && size > 0) {
			for (HashMapNode <V> e : tab) {
				for (; e != null; e = e.next) {
					if ( (v = e.value) == value ||
						(value != null && value.equals(v)))
						return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns a {@link Set} view of the keys contained in this map. The set is backed by the map, so changes to the map are reflected in the set, and vice-versa. If the map is modified while an
	 * iteration over the set is in progress (except through the iterator's own {@code remove} operation), the results of the iteration are undefined. The set supports element removal, which removes
	 * the corresponding mapping from the map, via the {@code Iterator.remove}, {@code Set.remove}, {@code removeAll}, {@code retainAll}, and {@code clear} operations. It does not support the
	 * {@code add} or {@code addAll} operations.
	 *
	 * @return a set view of the keys contained in this map
	 */
	public Set <Integer> keySet() {
		Set <Integer> ks = keySet;
		if (ks == null) {
			ks = new KeySet();
			keySet = ks;
		}
		return ks;
	}
	
	final class KeySet extends AbstractSet <Integer> {
		
		public final int size() {
			return size;
		}
		
		public final void clear() {
			IntHashMap.this.clear();
		}
		
		public final Iterator <Integer> iterator() {
			return new KeyIterator();
		}
		
		public final boolean contains(Object o) {
			return containsKey(o);
		}
		
		public final boolean remove(Object key) {
			if (key == null || ! (key instanceof Integer)) return false;
			int k = (int) (Integer) key;
			return removeNode(hash(k), k, null, false, true) != null;
		}
		
		public final Spliterator <Integer> spliterator() {
			return new KeySpliterator <>(IntHashMap.this, 0, -1, 0, 0);
		}
		
		public final void forEach(Consumer <? super Integer> action) {
			HashMapNode <V>[] tab;
			if (action == null)
				throw new NullPointerException();
			if (size > 0 && (tab = table) != null) {
				int mc = modCount;
				for (HashMapNode <V> e : tab) {
					for (; e != null; e = e.next)
						action.accept(e.key);
				}
				if (modCount != mc)
					throw new ConcurrentModificationException();
			}
		}
		
	}
	
	/**
	 * Returns a {@link Collection} view of the values contained in this map. The collection is backed by the map, so changes to the map are reflected in the collection, and vice-versa. If the map is
	 * modified while an iteration over the collection is in progress (except through the iterator's own {@code remove} operation), the results of the iteration are undefined. The collection supports
	 * element removal, which removes the corresponding mapping from the map, via the {@code Iterator.remove}, {@code Collection.remove}, {@code removeAll}, {@code retainAll} and {@code clear}
	 * operations. It does not support the {@code add} or {@code addAll} operations.
	 *
	 * @return a view of the values contained in this map
	 */
	public Collection <V> values() {
		Collection <V> vs = values;
		if (vs == null) {
			vs = new Values();
			values = vs;
		}
		return vs;
	}
	
	final class Values extends AbstractCollection <V> {
		
		public final int size() {
			return size;
		}
		
		public final void clear() {
			IntHashMap.this.clear();
		}
		
		public final Iterator <V> iterator() {
			return new ValueIterator();
		}
		
		public final boolean contains(Object o) {
			return containsValue(o);
		}
		
		public final Spliterator <V> spliterator() {
			return new ValueSpliterator <>(IntHashMap.this, 0, -1, 0, 0);
		}
		
		public final void forEach(Consumer <? super V> action) {
			HashMapNode <V>[] tab;
			if (action == null)
				throw new NullPointerException();
			if (size > 0 && (tab = table) != null) {
				int mc = modCount;
				for (HashMapNode <V> e : tab) {
					for (; e != null; e = e.next)
						action.accept(e.value);
				}
				if (modCount != mc)
					throw new ConcurrentModificationException();
			}
		}
		
	}
	
	/**
	 * Returns a {@link Set} view of the mappings contained in this map. The set is backed by the map, so changes to the map are reflected in the set, and vice-versa. If the map is modified while an
	 * iteration over the set is in progress (except through the iterator's own {@code remove} operation, or through the {@code setValue} operation on a map entry returned by the iterator) the results
	 * of the iteration are undefined. The set supports element removal, which removes the corresponding mapping from the map, via the {@code Iterator.remove}, {@code Set.remove}, {@code removeAll},
	 * {@code retainAll} and {@code clear} operations. It does not support the {@code add} or {@code addAll} operations.
	 *
	 * @return a set view of the mappings contained in this map
	 */
	public Set <Map.Entry <Integer, V>> entrySet() {
		Set <Map.Entry <Integer, V>> es;
		return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
	}
	
	final class EntrySet extends AbstractSet <Map.Entry <Integer, V>> {
		
		public final int size() {
			return size;
		}
		
		public final void clear() {
			IntHashMap.this.clear();
		}
		
		public final Iterator <Map.Entry <Integer, V>> iterator() {
			return new EntryIterator();
		}
		
		public final boolean contains(Object o) {
			if ( ! (o instanceof Map.Entry))
				return false;
			Map.Entry <?, ?> e = (Map.Entry <?, ?>) o;
			Object key = e.getKey();
			if (key == null || ! (key instanceof Integer)) return false;
			int k = (int) key;
			HashMapNode <V> candidate = getNode(hash(k), k);
			return candidate != null && candidate.equals(e);
		}
		
		public final boolean remove(Object o) {
			if (o instanceof Map.Entry) {
				Map.Entry <?, ?> e = (Map.Entry <?, ?>) o;
				Object key = e.getKey();
				Object value = e.getValue();
				if (key == null || ! (key instanceof Integer)) return false;
				int k = (int) key;
				return removeNode(hash(k), k, value, true, true) != null;
			}
			return false;
		}
		
		public final Spliterator <Map.Entry <Integer, V>> spliterator() {
			return new EntrySpliterator <>(IntHashMap.this, 0, -1, 0, 0);
		}
		
		public final void forEach(Consumer <? super Map.Entry <Integer, V>> action) {
			HashMapNode <V>[] tab;
			if (action == null)
				throw new NullPointerException();
			if (size > 0 && (tab = table) != null) {
				int mc = modCount;
				for (HashMapNode <V> e : tab) {
					for (; e != null; e = e.next)
						action.accept(e);
				}
				if (modCount != mc)
					throw new ConcurrentModificationException();
			}
		}
		
	}
	
	// Overrides of JDK8 Map extension methods
	
	@Override
	public V getOrDefault(Object key, V defaultValue) {
		HashMapNode <V> e;
		if (key == null || ! (key instanceof Integer)) return defaultValue;
		int k = (int) key;
		return (e = getNode(hash(k), k)) == null ? defaultValue : e.value;
	}
	
	@Override
	public V putIfAbsent(Integer key, V value) {
		if (key == null) throw new NullPointerException("null key");
		int k = key;
		return putVal(hash(k), k, value, true, true);
	}
	
	@Override
	public boolean remove(Object key, Object value) {
		if (key == null || ! (key instanceof Integer)) return false;
		int k = (int) key;
		return removeNode(hash(k), k, value, true, true) != null;
	}
	
	@Override
	public boolean replace(Integer key, V oldValue, V newValue) {
		if (key == null) throw new NullPointerException("null key");
		HashMapNode <V> e;
		V v;
		int k = key;
		if ( (e = getNode(hash(k), k)) != null &&
			( (v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
			e.value = newValue;
			afterNodeAccess(e);
			return true;
		}
		return false;
	}
	
	@Override
	public V replace(Integer key, V value) {
		if (key == null) throw new NullPointerException("null key");
		HashMapNode <V> e;
		int k = key;
		if ( (e = getNode(hash(k), k)) != null) {
			V oldValue = e.value;
			e.value = value;
			afterNodeAccess(e);
			return oldValue;
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This method will, on a best-effort basis, throw a {@link ConcurrentModificationException} if it is detected that the mapping function modifies this map during computation.
	 *
	 * @throws ConcurrentModificationException
	 *             if it is detected that the mapping function modified this map
	 */
	@Override
	public V computeIfAbsent(Integer key, Function <? super Integer, ? extends V> mappingFunction) {
		if (mappingFunction == null || key == null) throw new NullPointerException("key=" + key + " mappingFunction=" + mappingFunction);
		int _k = key;
		int hash = hash(_k);
		HashMapNode <V>[] tab;
		HashMapNode <V> first;
		int n, i;
		int binCount = 0;
		TreeNode <V> t = null;
		HashMapNode <V> old = null;
		if (size > threshold || (tab = table) == null ||
			(n = tab.length) == 0)
			n = (tab = resize()).length;
		if ( (first = tab[i = (n - 1) & hash]) != null) {
			if (first instanceof TreeNode)
				old = (t = (TreeNode <V>) first).getTreeNode(hash, _k);
			else {
				HashMapNode <V> e = first;
				do {
					if (e.hash == hash &&
						(e.key == _k)) {
						old = e;
						break;
					}
					++ binCount;
				} while ( (e = e.next) != null);
			}
			V oldValue;
			if (old != null && (oldValue = old.value) != null) {
				afterNodeAccess(old);
				return oldValue;
			}
		}
		int mc = modCount;
		V v = mappingFunction.apply(key);
		if (mc != modCount) {
			throw new ConcurrentModificationException();
		}
		if (v == null) {
			return null;
		} else if (old != null) {
			old.value = v;
			afterNodeAccess(old);
			return v;
		} else if (t != null)
			t.putTreeVal(this, tab, hash, _k, v);
		else {
			tab[i] = new HashMapNode <>(hash, _k, v, first);
			if (binCount >= TREEIFY_THRESHOLD - 1)
				treeifyBin(tab, hash);
		}
		modCount = mc + 1;
		++ size;
		afterNodeInsertion(true);
		return v;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This method will, on a best-effort basis, throw a {@link ConcurrentModificationException} if it is detected that the remapping function modifies this map during computation.
	 *
	 * @throws ConcurrentModificationException
	 *             if it is detected that the remapping function modified this map
	 */
	@Override
	public V computeIfPresent(Integer key, BiFunction <? super Integer, ? super V, ? extends V> remappingFunction) {
		if (remappingFunction == null)
			throw new NullPointerException();
		int k = key;
		HashMapNode <V> e;
		V oldValue;
		int hash = hash(k);
		if ( (e = getNode(hash, k)) != null &&
			(oldValue = e.value) != null) {
			int mc = modCount;
			V v = remappingFunction.apply(k, oldValue);
			if (mc != modCount) {
				throw new ConcurrentModificationException();
			}
			if (v != null) {
				e.value = v;
				afterNodeAccess(e);
				return v;
			} else
				removeNode(hash, key, null, false, true);
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This method will, on a best-effort basis, throw a {@link ConcurrentModificationException} if it is detected that the remapping function modifies this map during computation.
	 *
	 * @throws ConcurrentModificationException
	 *             if it is detected that the remapping function modified this map
	 */
	@Override
	public V compute(Integer key, BiFunction <? super Integer, ? super V, ? extends V> remappingFunction) {
		if (remappingFunction == null || key == null) throw new NullPointerException("key=" + key + " remappingFunction=" + remappingFunction);
		int _k = key;
		int hash = hash(_k);
		HashMapNode <V>[] tab;
		HashMapNode <V> first;
		int n, i;
		int binCount = 0;
		TreeNode <V> t = null;
		HashMapNode <V> old = null;
		if (size > threshold || (tab = table) == null ||
			(n = tab.length) == 0)
			n = (tab = resize()).length;
		if ( (first = tab[i = (n - 1) & hash]) != null) {
			if (first instanceof TreeNode)
				old = (t = (TreeNode <V>) first).getTreeNode(hash, key);
			else {
				HashMapNode <V> e = first;
				do {
					if (e.hash == hash &&
						(e.key == _k)) {
						old = e;
						break;
					}
					++ binCount;
				} while ( (e = e.next) != null);
			}
		}
		V oldValue = (old == null) ? null : old.value;
		int mc = modCount;
		V v = remappingFunction.apply(key, oldValue);
		if (mc != modCount) {
			throw new ConcurrentModificationException();
		}
		if (old != null) {
			if (v != null) {
				old.value = v;
				afterNodeAccess(old);
			} else
				removeNode(hash, _k, null, false, true);
		} else if (v != null) {
			if (t != null)
				t.putTreeVal(this, tab, hash, _k, v);
			else {
				tab[i] = new HashMapNode <>(hash, _k, v, first);
				if (binCount >= TREEIFY_THRESHOLD - 1)
					treeifyBin(tab, hash);
			}
			modCount = mc + 1;
			++ size;
			afterNodeInsertion(true);
		}
		return v;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This method will, on a best-effort basis, throw a {@link ConcurrentModificationException} if it is detected that the remapping function modifies this map during computation.
	 *
	 * @throws ConcurrentModificationException
	 *             if it is detected that the remapping function modified this map
	 */
	@Override
	public V merge(Integer key, V value, BiFunction <? super V, ? super V, ? extends V> remappingFunction) {
		if (key == null || value == null || remappingFunction == null) throw new NullPointerException("key=" + key + " val=" + value + " func=" + remappingFunction);
		int _k = key;
		int hash = hash(_k);
		HashMapNode <V>[] tab;
		HashMapNode <V> first;
		int n, i;
		int binCount = 0;
		TreeNode <V> t = null;
		HashMapNode <V> old = null;
		if (size > threshold || (tab = table) == null ||
			(n = tab.length) == 0)
			n = (tab = resize()).length;
		if ( (first = tab[i = (n - 1) & hash]) != null) {
			if (first instanceof TreeNode)
				old = (t = (TreeNode <V>) first).getTreeNode(hash, _k);
			else {
				HashMapNode <V> e = first;
				do {
					if (e.hash == hash &&
						(e.key == _k)) {
						old = e;
						break;
					}
					++ binCount;
				} while ( (e = e.next) != null);
			}
		}
		if (old != null) {
			V v;
			if (old.value != null) {
				int mc = modCount;
				v = remappingFunction.apply(old.value, value);
				if (mc != modCount) {
					throw new ConcurrentModificationException();
				}
			} else {
				v = value;
			}
			if (v != null) {
				old.value = v;
				afterNodeAccess(old);
			} else
				removeNode(hash, _k, null, false, true);
			return v;
		}
		if (value != null) {
			if (t != null)
				t.putTreeVal(this, tab, hash, _k, value);
			else {
				tab[i] = new HashMapNode <>(hash, _k, value, first);
				if (binCount >= TREEIFY_THRESHOLD - 1)
					treeifyBin(tab, hash);
			}
			++ modCount;
			++ size;
			afterNodeInsertion(true);
		}
		return value;
	}
	
	@Override
	public void forEach(BiConsumer <? super Integer, ? super V> action) {
		HashMapNode <V>[] tab;
		if (action == null)
			throw new NullPointerException();
		if (size > 0 && (tab = table) != null) {
			int mc = modCount;
			for (HashMapNode <V> e : tab) {
				for (; e != null; e = e.next)
					action.accept(e.key, e.value);
			}
			if (modCount != mc)
				throw new ConcurrentModificationException();
		}
	}
	
	@Override
	public void replaceAll(BiFunction <? super Integer, ? super V, ? extends V> function) {
		HashMapNode <V>[] tab;
		if (function == null)
			throw new NullPointerException();
		if (size > 0 && (tab = table) != null) {
			int mc = modCount;
			for (HashMapNode <V> e : tab) {
				for (; e != null; e = e.next) {
					e.value = function.apply(e.key, e.value);
				}
			}
			if (modCount != mc)
				throw new ConcurrentModificationException();
		}
	}
	
	/* ------------------------------------------------------------ */
	// Cloning and serialization
	
	/**
	 * Returns a shallow copy of this {@code HashMap} instance: the keys and values themselves are not cloned.
	 *
	 * @return a shallow copy of this map
	 */
	@Override
	@SuppressWarnings("unchecked")
	public IntHashMap <V> clone() {
		IntHashMap <V> result;
		try {
			result = (IntHashMap <V>) super.clone();
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError(e);
		}
		result.reinitialize();
		result.putMapEntries(this, false);
		return result;
	}
	
	// These methods are also used when serializing HashSets
	final float loadFactor() {
		return loadFactor;
	}
	
	final int capacity() {
		return (table != null) ? table.length : (threshold > 0) ? threshold : DEFAULT_INITIAL_CAPACITY;
	}
	
	/**
	 * Saves this map to a stream (that is, serializes it).
	 *
	 * @param s
	 *            the stream
	 * @throws IOException
	 *             if an I/O error occurs
	 * @serialData The <i>capacity</i> of the HashMap (the length of the bucket array) is emitted (int), followed by the <i>size</i> (an int, the number of key-value mappings), followed by the key
	 *             (Object) and value (Object) for each key-value mapping. The key-value mappings are emitted in no particular order.
	 */
	private void writeObject(java.io.ObjectOutputStream s) throws IOException {
		int buckets = capacity();
		// Write out the threshold, loadfactor, and any hidden stuff
		s.defaultWriteObject();
		s.writeInt(buckets);
		s.writeInt(size);
		internalWriteEntries(s);
	}
	
	/**
	 * Reconstitutes this map from a stream (that is, deserializes it).
	 * 
	 * @param s
	 *            the stream
	 * @throws ClassNotFoundException
	 *             if the class of a serialized object could not be found
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private void readObject(java.io.ObjectInputStream s)
		throws IOException, ClassNotFoundException {
		// Read in the threshold (ignored), loadfactor, and any hidden stuff
		s.defaultReadObject();
		reinitialize();
		if (loadFactor <= 0 || Float.isNaN(loadFactor))
			throw new InvalidObjectException("Illegal load factor: " + loadFactor);
		s.readInt(); // Read and ignore number of buckets
		int mappings = s.readInt(); // Read number of mappings (size)
		if (mappings < 0)
			throw new InvalidObjectException("Illegal mappings count: " +
				mappings);
		else if (mappings > 0) { // (if zero, use defaults)
			// Size the table using given load factor only if within
			// range of 0.25...4.0
			float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
			float fc = (float) mappings / lf + 1.0f;
			int cap = ( (fc < DEFAULT_INITIAL_CAPACITY) ? DEFAULT_INITIAL_CAPACITY : (fc >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : tableSizeFor((int) fc));
			float ft = (float) cap * lf;
			threshold = ( (cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ? (int) ft : Integer.MAX_VALUE);
			
			// Check Map.Entry[].class since it's the nearest public type to
			// what we're actually creating.
			// SharedSecrets.getJavaObjectInputStreamAccess().checkArray(s, Map.Entry[].class, cap);
			@SuppressWarnings({"unchecked" })
			HashMapNode <V>[] tab = (HashMapNode <V>[]) new HashMapNode[cap];
			table = tab;
			
			// Read the keys and values, and put the mappings in the HashMap
			for (int i = 0; i < mappings; i ++ ) {
				int key = s.readInt();
				@SuppressWarnings("unchecked")
				V value = (V) s.readObject();
				putVal(hash(key), key, value, false, false);
			}
		}
	}
	
	/* ------------------------------------------------------------ */
	// iterators
	
	abstract class HashIterator {
		
		HashMapNode <V> next;             // next entry to return
		HashMapNode <V> current;          // current entry
		int             expectedModCount; // for fast-fail
		int             index;            // current slot
		
		HashIterator() {
			expectedModCount = modCount;
			HashMapNode <V>[] t = table;
			current = next = null;
			index = 0;
			if (t != null && size > 0) { // advance to first entry
				do {} while (index < t.length && (next = t[index ++ ]) == null);
			}
		}
		
		public final boolean hasNext() {
			return next != null;
		}
		
		final HashMapNode <V> nextNode() {
			HashMapNode <V>[] t;
			HashMapNode <V> e = next;
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			if (e == null)
				throw new NoSuchElementException("no more elements");
			if ( (next = (current = e).next) == null && (t = table) != null) {
				do {} while (index < t.length && (next = t[index ++ ]) == null);
			}
			return e;
		}
		
		public final void remove() {
			HashMapNode <V> p = current;
			if (p == null)
				throw new IllegalStateException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			current = null;
			removeNode(p.hash, p.key, null, false, false);
			expectedModCount = modCount;
		}
		
	}
	
	final class KeyIterator extends HashIterator
		implements Iterator <Integer> {
		
		public final Integer next() {
			return nextNode().key;
		}
		
	}
	
	final class ValueIterator extends HashIterator
		implements Iterator <V> {
		
		public final V next() {
			return nextNode().value;
		}
		
	}
	
	final class EntryIterator extends HashIterator
		implements Iterator <Map.Entry <Integer, V>> {
		
		public final Map.Entry <Integer, V> next() {
			return nextNode();
		}
		
	}
	
	/* ------------------------------------------------------------ */
	// spliterators
	
	static class HashMapSpliterator <V> {
		
		final IntHashMap <V> map;
		HashMapNode <V>      current;          // current node
		int                  index;            // current index, modified on advance/split
		int                  fence;            // one past last index
		int                  est;              // size estimate
		int                  expectedModCount; // for comodification checks
		
		HashMapSpliterator(IntHashMap <V> m, int origin,
			int fence, int est,
			int expectedModCount) {
			this.map = m;
			this.index = origin;
			this.fence = fence;
			this.est = est;
			this.expectedModCount = expectedModCount;
		}
		
		final int getFence() { // initialize fence and size on first use
			int hi;
			if ( (hi = fence) < 0) {
				IntHashMap <V> m = map;
				est = m.size;
				expectedModCount = m.modCount;
				HashMapNode <V>[] tab = m.table;
				hi = fence = (tab == null) ? 0 : tab.length;
			}
			return hi;
		}
		
		public final long estimateSize() {
			getFence(); // force init
			return (long) est;
		}
		
	}
	
	static final class KeySpliterator <V>
		extends HashMapSpliterator <V>
		implements Spliterator <Integer> {
		
		KeySpliterator(IntHashMap <V> m, int origin, int fence, int est,
			int expectedModCount) {
			super(m, origin, fence, est, expectedModCount);
		}
		
		public KeySpliterator <V> trySplit() {
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo >= mid || current != null) ? null
				: new KeySpliterator <>(map, lo, index = mid, est >>>= 1,
					expectedModCount);
		}
		
		public void forEachRemaining(Consumer <? super Integer> action) {
			int i, hi, mc;
			if (action == null)
				throw new NullPointerException();
			IntHashMap <V> m = map;
			HashMapNode <V>[] tab = m.table;
			if ( (hi = fence) < 0) {
				mc = expectedModCount = m.modCount;
				hi = fence = (tab == null) ? 0 : tab.length;
			} else
				mc = expectedModCount;
			if (tab != null && tab.length >= hi &&
				(i = index) >= 0 && (i < (index = hi) || current != null)) {
				HashMapNode <V> p = current;
				current = null;
				do {
					if (p == null)
						p = tab[i ++ ];
					else {
						action.accept(p.key);
						p = p.next;
					}
				} while (p != null || i < hi);
				if (m.modCount != mc)
					throw new ConcurrentModificationException();
			}
		}
		
		public boolean tryAdvance(Consumer <? super Integer> action) {
			int hi;
			if (action == null)
				throw new NullPointerException();
			HashMapNode <V>[] tab = map.table;
			if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
				while (current != null || index < hi) {
					if (current == null)
						current = tab[index ++ ];
					else {
						int k = current.key;
						current = current.next;
						action.accept(k);
						if (map.modCount != expectedModCount)
							throw new ConcurrentModificationException();
						return true;
					}
				}
			}
			return false;
		}
		
		public int characteristics() {
			return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
				Spliterator.DISTINCT;
		}
		
	}
	
	static final class ValueSpliterator <V>
		extends HashMapSpliterator <V>
		implements Spliterator <V> {
		
		ValueSpliterator(IntHashMap <V> m, int origin, int fence, int est,
			int expectedModCount) {
			super(m, origin, fence, est, expectedModCount);
		}
		
		public ValueSpliterator <V> trySplit() {
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo >= mid || current != null) ? null
				: new ValueSpliterator <>(map, lo, index = mid, est >>>= 1,
					expectedModCount);
		}
		
		public void forEachRemaining(Consumer <? super V> action) {
			int i, hi, mc;
			if (action == null)
				throw new NullPointerException();
			IntHashMap <V> m = map;
			HashMapNode <V>[] tab = m.table;
			if ( (hi = fence) < 0) {
				mc = expectedModCount = m.modCount;
				hi = fence = (tab == null) ? 0 : tab.length;
			} else
				mc = expectedModCount;
			if (tab != null && tab.length >= hi &&
				(i = index) >= 0 && (i < (index = hi) || current != null)) {
				HashMapNode <V> p = current;
				current = null;
				do {
					if (p == null)
						p = tab[i ++ ];
					else {
						action.accept(p.value);
						p = p.next;
					}
				} while (p != null || i < hi);
				if (m.modCount != mc)
					throw new ConcurrentModificationException();
			}
		}
		
		public boolean tryAdvance(Consumer <? super V> action) {
			int hi;
			if (action == null)
				throw new NullPointerException();
			HashMapNode <V>[] tab = map.table;
			if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
				while (current != null || index < hi) {
					if (current == null)
						current = tab[index ++ ];
					else {
						V v = current.value;
						current = current.next;
						action.accept(v);
						if (map.modCount != expectedModCount)
							throw new ConcurrentModificationException();
						return true;
					}
				}
			}
			return false;
		}
		
		public int characteristics() {
			return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
		}
		
	}
	
	static final class EntrySpliterator <V>
		extends HashMapSpliterator <V>
		implements Spliterator <Map.Entry <Integer, V>> {
		
		EntrySpliterator(IntHashMap <V> m, int origin, int fence, int est,
			int expectedModCount) {
			super(m, origin, fence, est, expectedModCount);
		}
		
		public EntrySpliterator <V> trySplit() {
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo >= mid || current != null) ? null
				: new EntrySpliterator <>(map, lo, index = mid, est >>>= 1,
					expectedModCount);
		}
		
		public void forEachRemaining(Consumer <? super Map.Entry <Integer, V>> action) {
			int i, hi, mc;
			if (action == null)
				throw new NullPointerException();
			IntHashMap <V> m = map;
			HashMapNode <V>[] tab = m.table;
			if ( (hi = fence) < 0) {
				mc = expectedModCount = m.modCount;
				hi = fence = (tab == null) ? 0 : tab.length;
			} else
				mc = expectedModCount;
			if (tab != null && tab.length >= hi &&
				(i = index) >= 0 && (i < (index = hi) || current != null)) {
				HashMapNode <V> p = current;
				current = null;
				do {
					if (p == null)
						p = tab[i ++ ];
					else {
						action.accept(p);
						p = p.next;
					}
				} while (p != null || i < hi);
				if (m.modCount != mc)
					throw new ConcurrentModificationException();
			}
		}
		
		public boolean tryAdvance(Consumer <? super Map.Entry <Integer, V>> action) {
			int hi;
			if (action == null)
				throw new NullPointerException();
			HashMapNode <V>[] tab = map.table;
			if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
				while (current != null || index < hi) {
					if (current == null)
						current = tab[index ++ ];
					else {
						HashMapNode <V> e = current;
						current = current.next;
						action.accept(e);
						if (map.modCount != expectedModCount)
							throw new ConcurrentModificationException();
						return true;
					}
				}
			}
			return false;
		}
		
		public int characteristics() {
			return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
				Spliterator.DISTINCT;
		}
		
	}
	
	/* ------------------------------------------------------------ */
	// LinkedHashMap support
	
	
	/*
	 * The following package-protected methods are designed to be overridden by LinkedHashMap, but not by any other subclass. Nearly all other internal methods are also package-protected but are
	 * declared final, so can be used by LinkedHashMap, view classes, and HashSet.
	 */
	
	// Create a regular (non-tree) node
	HashMapNode <V> newNode(int hash, int key, V value, HashMapNode <V> next) {
		return new HashMapNode <>(hash, key, value, next);
	}
	
	// For conversion from TreeNodes to plain nodes
	HashMapNode <V> replacementNode(HashMapNode <V> p, HashMapNode <V> next) {
		return new HashMapNode <>(p.hash, p.key, p.value, next);
	}
	
	// Create a tree bin node
	TreeNode <V> newTreeNode(int hash, int key, V value, HashMapNode <V> next) {
		return new TreeNode <>(hash, key, value, next);
	}
	
	// For treeifyBin
	TreeNode <V> replacementTreeNode(HashMapNode <V> p, HashMapNode <V> next) {
		return new TreeNode <>(p.hash, p.key, p.value, next);
	}
	
	/**
	 * Reset to initial default state. Called by clone and readObject.
	 */
	void reinitialize() {
		table = null;
		entrySet = null;
		keySet = null;
		values = null;
		modCount = 0;
		threshold = 0;
		size = 0;
	}
	
	// Callbacks to allow LinkedHashMap post-actions
	void afterNodeAccess(HashMapNode <V> p) {}
	
	void afterNodeInsertion(boolean evict) {}
	
	void afterNodeRemoval(HashMapNode <V> p) {}
	
	// Called only from writeObject, to ensure compatible ordering.
	void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
		HashMapNode <V>[] tab;
		if (size > 0 && (tab = table) != null) {
			for (HashMapNode <V> e : tab) {
				for (; e != null; e = e.next) {
					s.writeObject(e.key);
					s.writeObject(e.value);
				}
			}
		}
	}
	
	/* ------------------------------------------------------------ */
	// Tree bins
	
	// /**
	// * Basic hash bin node, used for most entries. (See below for TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
	// */
	// static class HashMapNode < V> implements Map.Entry <Integer, V> {
	//
	// final int hash;
	// final K key;
	// V value;
	// HashMapNode <K, V> next;
	//
	// HashMapNode(int hash, K key, V value, HashMapNode <K, V> next) {
	// this.hash = hash;
	// this.key = key;
	// this.value = value;
	// this.next = next;
	// }
	//
	// public final K getKey() {
	// return key;
	// }
	//
	// public final V getValue() {
	// return value;
	// }
	//
	// public final String toString() {
	// return key + "=" + value;
	// }
	//
	// public final int hashCode() {
	// return Objects.hashCode(key) ^ Objects.hashCode(value);
	// }
	//
	// public final V setValue(V newValue) {
	// V oldValue = value;
	// value = newValue;
	// return oldValue;
	// }
	//
	// public final boolean equals(Object o) {
	// if (o == this)
	// return true;
	// if (o instanceof Map.Entry) {
	// Map.Entry <?, ?> e = (Map.Entry <?, ?>) o;
	// if (Objects.equals(key, e.getKey()) &&
	// Objects.equals(value, e.getValue()))
	// return true;
	// }
	// return false;
	// }
	//
	// }
	
	/**
	 * HashMap.Node subclass for normal LinkedHashMap entries.
	 */
	static class LinkedHashMapEntry <V> extends HashMapNode <V> {
		
		Entry <Integer, V> before, after;
		
		LinkedHashMapEntry(int hash, int key, V value, HashMapNode <V> next) {
			super(hash, key, value, next);
		}
		
	}
	
	/**
	 * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn extends Node) so can be used as extension of either regular or linked node.
	 */
	static final class TreeNode <V> extends LinkedHashMapEntry <V> {
		
		TreeNode <V> parent; // red-black tree links
		TreeNode <V> left;
		TreeNode <V> right;
		TreeNode <V> prev;   // needed to unlink next upon deletion
		boolean      red;
		
		TreeNode(int hash, int key, V val, HashMapNode <V> next) {
			super(hash, key, val, next);
		}
		
		/**
		 * Returns root of tree containing this node.
		 */
		final TreeNode <V> root() {
			for (TreeNode <V> r = this, p;;) {
				if ( (p = r.parent) == null)
					return r;
				r = p;
			}
		}
		
		/**
		 * Ensures that the given root is the first node of its bin.
		 */
		static <K, V> void moveRootToFront(HashMapNode <V>[] tab, TreeNode <V> root) {
			int n;
			if (root != null && tab != null && (n = tab.length) > 0) {
				int index = (n - 1) & root.hash;
				TreeNode <V> first = (TreeNode <V>) tab[index];
				if (root != first) {
					HashMapNode <V> rn;
					tab[index] = root;
					TreeNode <V> rp = root.prev;
					if ( (rn = root.next) != null)
						((TreeNode <V>) rn).prev = rp;
					if (rp != null)
						rp.next = rn;
					if (first != null)
						first.prev = root;
					root.next = first;
					root.prev = null;
				}
				assert checkInvariants(root);
			}
		}
		
		/**
		 * Finds the node starting at root p with the given hash and key. The kc argument caches comparableClassFor(key) upon first use comparing keys.
		 */
		final TreeNode <V> find(int h, int k) {
			TreeNode <V> p = this;
			do {
				int ph, dir;
				int pk;
				TreeNode <V> pl = p.left, pr = p.right, q;
				if ( (ph = p.hash) > h)
					p = pl;
				else if (ph < h)
					p = pr;
				else if ( (pk = p.key) == k)
					return p;
				else if (pl == null)
					p = pr;
				else if (pr == null)
					p = pl;
				else if ( (dir = compare(k, pk)) != 0)
					p = (dir < 0) ? pl : pr;
				else if ( (q = pr.find(h, k)) != null)
					return q;
				else
					p = pl;
			} while (p != null);
			return null;
		}
		
		/**
		 * Calls find for root node.
		 */
		final TreeNode <V> getTreeNode(int h, int k) {
			return ( (parent != null) ? root() : this).find(h, k);
		}
		
		/**
		 * Tie-breaking utility for ordering insertions when equal hashCodes and non-comparable. We don't require a total order, just a consistent insertion rule to maintain equivalence across
		 * rebalancings. Tie-breaking further than necessary simplifies testing a bit.
		 */
		static int tieBreakOrder(int a, int b) {
			// int d;
			// if (a == null || b == null ||
			// (d = a.getClass().getName().compareTo(b.getClass().getName())) == 0)
			// d = (System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1);
			// return d;
			assert a == b;
			return -1;
		}
		
		/**
		 * Forms tree of the nodes linked from this node.
		 */
		final void treeify(HashMapNode <V>[] tab) {
			TreeNode <V> root = null;
			for (TreeNode <V> x = this, next; x != null; x = next) {
				next = (TreeNode <V>) x.next;
				x.left = x.right = null;
				if (root == null) {
					x.parent = null;
					x.red = false;
					root = x;
				} else {
					int k = x.key;
					int h = x.hash;
					for (TreeNode <V> p = root;;) {
						int dir, ph;
						int pk = p.key;
						if ( (ph = p.hash) > h)
							dir = -1;
						else if (ph < h)
							dir = 1;
						else if ( (dir = compare(k, pk)) == 0)
							dir = tieBreakOrder(k, pk);
						
						TreeNode <V> xp = p;
						if ( (p = (dir <= 0) ? p.left : p.right) == null) {
							x.parent = xp;
							if (dir <= 0)
								xp.left = x;
							else
								xp.right = x;
							root = balanceInsertion(root, x);
							break;
						}
					}
				}
			}
			moveRootToFront(tab, root);
		}
		
		/**
		 * Returns a list of non-TreeNodes replacing those linked from this node.
		 */
		final HashMapNode <V> untreeify(IntHashMap <V> map) {
			HashMapNode <V> hd = null, tl = null;
			for (HashMapNode <V> q = this; q != null; q = q.next) {
				HashMapNode <V> p = map.replacementNode(q, null);
				if (tl == null)
					hd = p;
				else
					tl.next = p;
				tl = p;
			}
			return hd;
		}
		
		/**
		 * Tree version of putVal.
		 */
		final TreeNode <V> putTreeVal(IntHashMap <V> map, HashMapNode <V>[] tab,
			int h, int k, V v) {
			boolean searched = false;
			TreeNode <V> root = (parent != null) ? root() : this;
			for (TreeNode <V> p = root;;) {
				int dir, ph;
				int pk;
				if ( (ph = p.hash) > h)
					dir = -1;
				else if (ph < h)
					dir = 1;
				else if ( (pk = p.key) == k)
					return p;
				else if ( (dir = compare(k, pk)) == 0) {
					if ( !searched) {
						TreeNode <V> q, ch;
						searched = true;
						if ( ( (ch = p.left) != null &&
							(q = ch.find(h, k)) != null) ||
							( (ch = p.right) != null &&
								(q = ch.find(h, k)) != null))
							return q;
					}
					dir = tieBreakOrder(k, pk);
				}
				
				TreeNode <V> xp = p;
				if ( (p = (dir <= 0) ? p.left : p.right) == null) {
					HashMapNode <V> xpn = xp.next;
					TreeNode <V> x = map.newTreeNode(h, k, v, xpn);
					if (dir <= 0)
						xp.left = x;
					else
						xp.right = x;
					xp.next = x;
					x.parent = x.prev = xp;
					if (xpn != null)
						((TreeNode <V>) xpn).prev = x;
					moveRootToFront(tab, balanceInsertion(root, x));
					return null;
				}
			}
		}
		
		/**
		 * Removes the given node, that must be present before this call. This is messier than typical red-black deletion code because we cannot swap the contents of an interior node with a leaf
		 * successor that is pinned by "next" pointers that are accessible independently during traversal. So instead we swap the tree linkages. If the current tree appears to have too few nodes, the
		 * bin is converted back to a plain bin. (The test triggers somewhere between 2 and 6 nodes, depending on tree structure).
		 */
		final void removeTreeNode(IntHashMap <V> map, HashMapNode <V>[] tab,
			boolean movable) {
			int n;
			if (tab == null || (n = tab.length) == 0)
				return;
			int index = (n - 1) & hash;
			TreeNode <V> first = (TreeNode <V>) tab[index], root = first, rl;
			TreeNode <V> succ = (TreeNode <V>) next, pred = prev;
			if (pred == null)
				tab[index] = first = succ;
			else
				pred.next = succ;
			if (succ != null)
				succ.prev = pred;
			if (first == null)
				return;
			if (root.parent != null)
				root = root.root();
			if (root == null
				|| (movable
					&& (root.right == null
						|| (rl = root.left) == null
						|| rl.left == null))) {
				tab[index] = first.untreeify(map); // too small
				return;
			}
			TreeNode <V> p = this, pl = left, pr = right, replacement;
			if (pl != null && pr != null) {
				TreeNode <V> s = pr, sl;
				while ( (sl = s.left) != null) // find successor
					s = sl;
				boolean c = s.red;
				s.red = p.red;
				p.red = c; // swap colors
				TreeNode <V> sr = s.right;
				TreeNode <V> pp = p.parent;
				if (s == pr) { // p was s's direct parent
					p.parent = s;
					s.right = p;
				} else {
					TreeNode <V> sp = s.parent;
					if ( (p.parent = sp) != null) {
						if (s == sp.left)
							sp.left = p;
						else
							sp.right = p;
					}
					if ( (s.right = pr) != null)
						pr.parent = s;
				}
				p.left = null;
				if ( (p.right = sr) != null)
					sr.parent = p;
				if ( (s.left = pl) != null)
					pl.parent = s;
				if ( (s.parent = pp) == null)
					root = s;
				else if (p == pp.left)
					pp.left = s;
				else
					pp.right = s;
				if (sr != null)
					replacement = sr;
				else
					replacement = p;
			} else if (pl != null)
				replacement = pl;
			else if (pr != null)
				replacement = pr;
			else
				replacement = p;
			if (replacement != p) {
				TreeNode <V> pp = replacement.parent = p.parent;
				if (pp == null)
					root = replacement;
				else if (p == pp.left)
					pp.left = replacement;
				else
					pp.right = replacement;
				p.left = p.right = p.parent = null;
			}
			
			TreeNode <V> r = p.red ? root : balanceDeletion(root, replacement);
			
			if (replacement == p) { // detach
				TreeNode <V> pp = p.parent;
				p.parent = null;
				if (pp != null) {
					if (p == pp.left)
						pp.left = null;
					else if (p == pp.right)
						pp.right = null;
				}
			}
			if (movable)
				moveRootToFront(tab, r);
		}
		
		/**
		 * Splits nodes in a tree bin into lower and upper tree bins, or untreeifies if now too small. Called only from resize; see above discussion about split bits and indices.
		 *
		 * @param map
		 *            the map
		 * @param tab
		 *            the table for recording bin heads
		 * @param index
		 *            the index of the table being split
		 * @param bit
		 *            the bit of hash to split on
		 */
		final void split(IntHashMap <V> map, HashMapNode <V>[] tab, int index, int bit) {
			TreeNode <V> b = this;
			// Relink into lo and hi lists, preserving order
			TreeNode <V> loHead = null, loTail = null;
			TreeNode <V> hiHead = null, hiTail = null;
			int lc = 0, hc = 0;
			for (TreeNode <V> e = b, next; e != null; e = next) {
				next = (TreeNode <V>) e.next;
				e.next = null;
				if ( (e.hash & bit) == 0) {
					if ( (e.prev = loTail) == null)
						loHead = e;
					else
						loTail.next = e;
					loTail = e;
					++ lc;
				} else {
					if ( (e.prev = hiTail) == null)
						hiHead = e;
					else
						hiTail.next = e;
					hiTail = e;
					++ hc;
				}
			}
			
			if (loHead != null) {
				if (lc <= UNTREEIFY_THRESHOLD)
					tab[index] = loHead.untreeify(map);
				else {
					tab[index] = loHead;
					if (hiHead != null) // (else is already treeified)
						loHead.treeify(tab);
				}
			}
			if (hiHead != null) {
				if (hc <= UNTREEIFY_THRESHOLD)
					tab[index + bit] = hiHead.untreeify(map);
				else {
					tab[index + bit] = hiHead;
					if (loHead != null)
						hiHead.treeify(tab);
				}
			}
		}
		
		/* ------------------------------------------------------------ */
		// Red-black tree methods, all adapted from CLR
		
		static <V> TreeNode <V> rotateLeft(TreeNode <V> root, TreeNode <V> p) {
			TreeNode <V> r, pp, rl;
			if (p != null && (r = p.right) != null) {
				if ( (rl = p.right = r.left) != null)
					rl.parent = p;
				if ( (pp = r.parent = p.parent) == null)
					(root = r).red = false;
				else if (pp.left == p)
					pp.left = r;
				else
					pp.right = r;
				r.left = p;
				p.parent = r;
			}
			return root;
		}
		
		static <V> TreeNode <V> rotateRight(TreeNode <V> root, TreeNode <V> p) {
			TreeNode <V> l, pp, lr;
			if (p != null && (l = p.left) != null) {
				if ( (lr = p.left = l.right) != null)
					lr.parent = p;
				if ( (pp = l.parent = p.parent) == null)
					(root = l).red = false;
				else if (pp.right == p)
					pp.right = l;
				else
					pp.left = l;
				l.right = p;
				p.parent = l;
			}
			return root;
		}
		
		static <V> TreeNode <V> balanceInsertion(TreeNode <V> root, TreeNode <V> x) {
			x.red = true;
			for (TreeNode <V> xp, xpp, xppl, xppr;;) {
				if ( (xp = x.parent) == null) {
					x.red = false;
					return x;
				} else if ( !xp.red || (xpp = xp.parent) == null)
					return root;
				if (xp == (xppl = xpp.left)) {
					if ( (xppr = xpp.right) != null && xppr.red) {
						xppr.red = false;
						xp.red = false;
						xpp.red = true;
						x = xpp;
					} else {
						if (x == xp.right) {
							root = rotateLeft(root, x = xp);
							xpp = (xp = x.parent) == null ? null : xp.parent;
						}
						if (xp != null) {
							xp.red = false;
							if (xpp != null) {
								xpp.red = true;
								root = rotateRight(root, xpp);
							}
						}
					}
				} else {
					if (xppl != null && xppl.red) {
						xppl.red = false;
						xp.red = false;
						xpp.red = true;
						x = xpp;
					} else {
						if (x == xp.left) {
							root = rotateRight(root, x = xp);
							xpp = (xp = x.parent) == null ? null : xp.parent;
						}
						if (xp != null) {
							xp.red = false;
							if (xpp != null) {
								xpp.red = true;
								root = rotateLeft(root, xpp);
							}
						}
					}
				}
			}
		}
		
		static <V> TreeNode <V> balanceDeletion(TreeNode <V> root, TreeNode <V> x) {
			for (TreeNode <V> xp, xpl, xpr;;) {
				if (x == null || x == root)
					return root;
				else if ( (xp = x.parent) == null) {
					x.red = false;
					return x;
				} else if (x.red) {
					x.red = false;
					return root;
				} else if ( (xpl = xp.left) == x) {
					if ( (xpr = xp.right) != null && xpr.red) {
						xpr.red = false;
						xp.red = true;
						root = rotateLeft(root, xp);
						xpr = (xp = x.parent) == null ? null : xp.right;
					}
					if (xpr == null)
						x = xp;
					else {
						TreeNode <V> sl = xpr.left, sr = xpr.right;
						if ( (sr == null || !sr.red) &&
							(sl == null || !sl.red)) {
							xpr.red = true;
							x = xp;
						} else {
							if (sr == null || !sr.red) {
								if (sl != null)
									sl.red = false;
								xpr.red = true;
								root = rotateRight(root, xpr);
								xpr = (xp = x.parent) == null ? null : xp.right;
							}
							if (xpr != null) {
								xpr.red = (xp == null) ? false : xp.red;
								if ( (sr = xpr.right) != null)
									sr.red = false;
							}
							if (xp != null) {
								xp.red = false;
								root = rotateLeft(root, xp);
							}
							x = root;
						}
					}
				} else { // symmetric
					if (xpl != null && xpl.red) {
						xpl.red = false;
						xp.red = true;
						root = rotateRight(root, xp);
						xpl = (xp = x.parent) == null ? null : xp.left;
					}
					if (xpl == null)
						x = xp;
					else {
						TreeNode <V> sl = xpl.left, sr = xpl.right;
						if ( (sl == null || !sl.red) &&
							(sr == null || !sr.red)) {
							xpl.red = true;
							x = xp;
						} else {
							if (sl == null || !sl.red) {
								if (sr != null)
									sr.red = false;
								xpl.red = true;
								root = rotateLeft(root, xpl);
								xpl = (xp = x.parent) == null ? null : xp.left;
							}
							if (xpl != null) {
								xpl.red = (xp == null) ? false : xp.red;
								if ( (sl = xpl.left) != null)
									sl.red = false;
							}
							if (xp != null) {
								xp.red = false;
								root = rotateRight(root, xp);
							}
							x = root;
						}
					}
				}
			}
		}
		
		/**
		 * Recursive invariant check
		 */
		static <V> boolean checkInvariants(TreeNode <V> t) {
			TreeNode <V> tp = t.parent, tl = t.left, tr = t.right,
				tb = t.prev, tn = (TreeNode <V>) t.next;
			if (tb != null && tb.next != t)
				return false;
			if (tn != null && tn.prev != t)
				return false;
			if (tp != null && t != tp.left && t != tp.right)
				return false;
			if (tl != null && (tl.parent != t || tl.hash > t.hash))
				return false;
			if (tr != null && (tr.parent != t || tr.hash < t.hash))
				return false;
			if (t.red && tl != null && tl.red && tr != null && tr.red)
				return false;
			if (tl != null && !checkInvariants(tl))
				return false;
			if (tr != null && !checkInvariants(tr))
				return false;
			return true;
		}
		
	}
	
}
