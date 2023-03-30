package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapStuffData extends StuffDataBase implements Map<String, String> {
    public final static int IDENTIFIER = 1;

    private Map<String, String> map = new HashMap<>();

    protected MapStuffData() {}

    public MapStuffData(HashMap<String, String> map) {
        this.map = map == null ? new HashMap<>() : map.entrySet().stream().filter(e -> e.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, HashMap::new));
    }

    public MapStuffData(int uniqueSerialNumber, int uniqueSerialSize, HashMap<String, String> map) {
        super(uniqueSerialNumber, uniqueSerialSize);
        this.map = map == null ? new HashMap<>() : map.entrySet().stream().filter(e -> e.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, HashMap::new));
    }

    @Override
    protected void initialize(HPacket packet) {
        int size = packet.readInteger();
        this.clear();
        for (int i = 0; i < size; i++) {
            this.put(packet.readString(), packet.readString());
        }
        super.initialize(packet);
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendInt(IDENTIFIER | this.getFlags());
        packet.appendInt(this.size());
        for (Entry<String, String> entry : this.entrySet()) {
            packet.appendObjects(
                    entry.getKey(),
                    entry.getValue() == null ? "" : entry.getValue()
            );
        }
        super.appendToPacket(packet);
    }

    @Override
    public String getLegacyString() {
        return this.getOrDefault("state", "");
    }

    @Override
    public void setLegacyString(String legacyString) {
        this.put("state", legacyString == null ? "" : legacyString);
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return this.map.get(key);
    }

    @Override
    public String put(String key, String value) {
        return this.map.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return this.map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        this.map.putAll(m);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Set<String> keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection<String> values() {
        return this.map.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return this.map.entrySet();
    }

    @Override
    public String getOrDefault(Object key, String defaultValue) {
        return this.map.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super String> action) {
        this.map.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super String, ? extends String> function) {
        this.map.replaceAll(function);
    }

    @Override
    public String putIfAbsent(String key, String value) {
        return this.map.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return this.map.remove(key, value);
    }

    @Override
    public boolean replace(String key, String oldValue, String newValue) {
        return this.map.replace(key, oldValue, newValue);
    }

    @Override
    public String replace(String key, String value) {
        return this.map.replace(key, value);
    }

    @Override
    public String computeIfAbsent(String key, Function<? super String, ? extends String> mappingFunction) {
        return this.map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public String computeIfPresent(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
        return this.map.computeIfPresent(key, remappingFunction);
    }

    @Override
    public String compute(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
        return this.map.compute(key, remappingFunction);
    }

    @Override
    public String merge(String key, String value, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
        return this.map.merge(key, value, remappingFunction);
    }
}
