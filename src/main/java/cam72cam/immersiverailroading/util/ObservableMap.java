package cam72cam.immersiverailroading.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;

/**
 * An HashMap that triggers an event if a value is changed <br>
 * It only works if the Key is a String
 * @author poizzy
 */
public class ObservableMap<K, V> extends HashMap<K, V> {
    private PropertyChangeSupport propertySupport;

    public ObservableMap() {
        super();
        propertySupport = new PropertyChangeSupport(this);
    }

    @Override
    public V put(K key, V value) {
        V old = super.get(key);
        boolean changed = (old == null && value != null) || (old != null && !old.equals(value));
        if (changed) {
            propertySupport.firePropertyChange((String) key, old, value);
        }
        return super.put(key, value);
    }

    public ObservableMap<K, V> addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
        return this;
    }

    public ObservableMap<K, V> removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
        return this;

    }
}
