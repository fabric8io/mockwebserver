package io.fabric8.mockwebserver.crud;

import static io.fabric8.mockwebserver.crud.AttributeType.WITH;

public class Attribute {

    private final Key key;
    private final Value value;
    private final AttributeType type;


    public Attribute(Key key, Value value, AttributeType type) {
    	this.key = key;
    	this.value = value;
    	this.type = type;
    }
    
    public Attribute(String key, String value, AttributeType type) {
    	this(new Key(key), new Value(value), type);
    }

    public Attribute(Key key, Value value) {
        this(key,value,WITH);
    }

    public Attribute(String key, String value) {
    	this(new Key(key), new Value(value));
    }
    

    public Key getKey() {
        return key;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (key != null ? !key.equals(attribute.key) : attribute.key != null) return false;
        return value != null ? value.equals(attribute.value) : attribute.value == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "{" +
                "key:" + key +
                ", value:" + value +
                '}';
    }

	public AttributeType getType() {
		return type;
	}
}
