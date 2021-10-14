package io.fabric8.mockwebserver.crud;

public interface AttributeExtractor<T> {

    AttributeSet fromPath(String path);

    AttributeSet fromResource(String resource);

}
