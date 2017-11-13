package io.fabric8.mockwebserver.crud;

public interface AttributeExtractor<T> {

   AttributeSet extract(String path);

    AttributeSet extract(T object);
}
