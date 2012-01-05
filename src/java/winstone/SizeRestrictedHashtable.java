package winstone;

import java.util.Hashtable;
import java.util.Map;

/**
 * {@link Hashtable} that sets the upper bound in the total number of keys.
 *
 * This is to protect against the DoS attack based on the hash key collision.
 * See http://www.ocert.org/advisories/ocert-2011-003.html
 *
 * @author Kohsuke Kawaguchi
 */
public class SizeRestrictedHashtable<K,V> extends Hashtable<K,V> {
    private final int cap;

    public SizeRestrictedHashtable(int initialCapacity, float loadFactor, int cap) {
        super(initialCapacity, loadFactor);
        this.cap = cap;
    }

    public SizeRestrictedHashtable(int initialCapacity, int cap) {
        super(initialCapacity);
        this.cap = cap;
    }

    public SizeRestrictedHashtable(int cap) {
        this.cap = cap;
    }

    public SizeRestrictedHashtable(Map<? extends K, ? extends V> t, int cap) {
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
