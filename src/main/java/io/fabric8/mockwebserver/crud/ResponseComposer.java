package io.fabric8.mockwebserver.crud;

import java.util.Collection;

public interface ResponseComposer {

    String compose(Collection<String> items);
}
