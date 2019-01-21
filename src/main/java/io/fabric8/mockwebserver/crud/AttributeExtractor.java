package io.fabric8.mockwebserver.crud;

public interface AttributeExtractor<T> {

    @Deprecated //to be replaced with fromPath
    AttributeSet extract(String path);

    @Deprecated //to be replaced with fromResource.
    AttributeSet extract(T object);


    AttributeSet fromPath(String path);

    AttributeSet fromResource(String resource);

}
