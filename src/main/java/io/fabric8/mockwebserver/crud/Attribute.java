/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.mockwebserver.crud;

import java.util.Objects;

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
        return Objects.equals(key, attribute.key) && Objects.equals(value, attribute.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
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
