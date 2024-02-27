package dev.bithole.debugrenderers;

import java.util.Map;
import java.util.WeakHashMap;

public class IDMapper<T> {

    private final Map<T, Integer> ids = new WeakHashMap<>();
    private int nextID = 0;

    public int getID(T object) {
        Integer id = ids.get(object);
        if(id == null) {
            id = nextID++;
            ids.put(object, id);
        }
        return id;
    }

}
