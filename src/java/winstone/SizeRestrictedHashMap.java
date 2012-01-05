package winstone;

import java.util.Hashtable;
import java.util.Map;

/**
 * @see SizeRestrictedHashtable
 * @author Kohsuke Kawaguchi
 */
public class SizeRestrictedHashMap<K,V> extends Hashtable<K,V> {
    private final int cap;

    public SizeRestrictedHashMap(int initialCapacity, float loadFactor, int cap) {
        super(initialCapacity, loadFactor);
        this.cap = cap;
    }

    public SizeRestrictedHashMap(int initialCapacity, int cap) {
        super(initialCapacity);
        this.cap = cap;
    }

    public SizeRestrictedHashMap(int cap) {
        this.cap = cap;
    }

    public SizeRestrictedHashMap(Map<? extends K, ? extends V> t, int cap) {
        super(t);
        this.cap = cap;
    }

    @Override
    public V put(K key, V value) {
        if (size()>cap)
            throw new IllegalStateException("Hash table size limit exceeded");
        return super.put(key, value);
    }
}

