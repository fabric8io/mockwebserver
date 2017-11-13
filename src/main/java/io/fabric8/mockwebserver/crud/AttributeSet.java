package io.fabric8.mockwebserver.crud;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class AttributeSet {

    private final Set<Attribute> attributes;


    public static AttributeSet merge(AttributeSet... attributeSets) {
        Set<Attribute> all = new LinkedHashSet<>();
        for (AttributeSet f : attributeSets) {
            all.addAll(f.attributes);
        }
        return new AttributeSet(all);
    }

    public AttributeSet(Attribute... attributes) {
        this(Arrays.asList(attributes));
    }

    public AttributeSet(Collection<Attribute> attributes) {
        this.attributes = attributes == null ? new LinkedHashSet<Attribute>() : new LinkedHashSet<>(attributes);
    }

    public AttributeSet add(Attribute... attr) {
        Set<Attribute> all = new LinkedHashSet<>(attributes);
        for(Attribute a : attr) {
            all.add(a);
        }
        return new AttributeSet(all);
    }

    public boolean matches(AttributeSet candidate) {
        for (Attribute c : candidate.attributes) {
            boolean found = false;
            for (Attribute a : attributes) {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeSet that = (AttributeSet) o;

        return attributes != null ? attributes.equals(that.attributes) : that.attributes == null;
    }

    @Override
    public int hashCode() {
        return attributes != null ? attributes.hashCode() : 0;
    }
}
