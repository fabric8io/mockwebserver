package io.fabric8.mockwebserver.crud;

public class Value {

    private static final String ANY = "*";

    private final String value;

    public Value(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (ANY.equals(value)) {
            return true;
        }

        Value key = (Value) o;

        if (ANY.equals(key.value)) {
            return true;
        }
        return value != null ? value.equals(key.value) : key.value == null;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return value;
    }
}
