package io.fabric8.mockwebserver.crud;

import static io.fabric8.mockwebserver.crud.AttributeType.WITHOUT;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AttributeSet {

	private final Map<Key, Attribute> attributes;

	public static AttributeSet merge(AttributeSet... attributeSets) {
		Map<Key, Attribute> all = new HashMap<>();
		if (attributeSets != null) {
			for (AttributeSet f : attributeSets) {
				if (f != null && f.attributes != null) {
					all.putAll(f.attributes);
				}
			}
		}
		return new AttributeSet(all);
	}

	public static AttributeSet map(Attribute... attributes) {
		Map<Key, Attribute> all = new HashMap<>();
		if (attributes != null) {
			for (Attribute a : attributes) {
				all.put(a.getKey(), a);
			}
		}
		return new AttributeSet(all);
	}

	public AttributeSet(Attribute... attributes) {
		this(Arrays.asList(attributes));
	}

	public AttributeSet(Collection<Attribute> attributes) {
		this(AttributeSet.map(attributes.toArray(new Attribute[attributes.size()])).attributes);
	}

	public AttributeSet(Map<Key, Attribute> attributes) {
		this.attributes = attributes;
	}

	public AttributeSet add(Attribute... attr) {
		Map<Key, Attribute> all = new HashMap(attributes);
		for (Attribute a : attr) {
			all.put(a.getKey(), a);
		}
		return new AttributeSet(all);
	}

	public boolean containsKey(String key) {
		return containsKey(new Key(key));
	}

	public boolean containsKey(Key key) {
		return attributes.containsKey(key);
	}

	public boolean matches(AttributeSet candidate) {
		for (Attribute c : candidate.attributes.values()) {
			if (c.getType().equals(WITHOUT)) {
				return without(c);
			}
			boolean found = false;
			for (Attribute a : attributes.values()) {
				if (c.equals(a)) {
					found = true;
				}
			}

			if (!found) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		AttributeSet that = (AttributeSet) o;

		return attributes != null ? attributes.equals(that.attributes) : that.attributes == null;
	}

	@Override
	public int hashCode() {
		return attributes != null ? attributes.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "{" + "attributes: " + attributes + '}';
	}

	private boolean without(Attribute attribute) {
//    	Key key = new Key(attribute.getKey().toString().replace("without_", ""));
//    	Attribute c = new Attribute(key, attribute.getValue());
		for (Attribute a : attributes.values()) {
			if (attribute.equals(a)) {
				return false;
			}
		}
		return true;
	}
}
