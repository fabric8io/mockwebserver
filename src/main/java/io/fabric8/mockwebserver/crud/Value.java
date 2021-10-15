package io.fabric8.mockwebserver.crud;

public class Value {

    private static final String ANY = "*";

    private final String val;

    public Value(String value) {
        this.val = value;
    }

    @Override
    // TODO: There's a BUG here, equals({val: "*"} is true but might have different hashCode
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (ANY.equals(val)) {
            return true;
        }

        Value key = (Value) o;

        if (ANY.equals(key.val)) {
            return true;
        }
        return val != null ? val.equals(key.val) : key.val == null;
    }

    @Override
    public int hashCode() {
        return val != null ? val.hashCode() : 0;
    }

    @Override
    public String toString() {
        return val;
    }
}
