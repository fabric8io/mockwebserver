package io.fabric8.mockwebserver.crud;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrudDispatcher extends Dispatcher {

    private static final String POST = "post";
    private static final String UPDATE = "update";
    private static final String GET = "get";
    private static final String DELETE = "delete";

    private Map<AttributeSet, String> map = new HashMap<>();

    private final AttributeExtractor attributeExtractor;
    private final ResponseComposer responseComposer;

    public CrudDispatcher(AttributeExtractor attributeExtractor, ResponseComposer responseComposer) {
        this.attributeExtractor = attributeExtractor;
        this.responseComposer = responseComposer;
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        String path = request.getPath();
        String method = request.getMethod();


        if (POST.equalsIgnoreCase(method)) {
            return handlePost(path, request.getBody().readUtf8());
        } else if (UPDATE.equalsIgnoreCase(path)) {
            return handlePost(path, request.getBody().readUtf8());
        } else if (GET.equalsIgnoreCase(method)) {
            return handleGet(path);
        } else if (DELETE.equalsIgnoreCase(method)) {
            return handleDelete(path);
        }
        return null;
    }

    /**
     * Adds the specified object to the in-memory db.
     *
     * @param path
     * @param s
     * @return
     */
    public MockResponse handlePost(String path, String s) {
        MockResponse response = new MockResponse();
        AttributeSet features = AttributeSet.merge(attributeExtractor.extract(path), attributeExtractor.extract(s));
        map.put(features, s);
        response.setBody(s);
        response.setResponseCode(202);
        return response;
    }

     /**
     * Updates the specified object to the in-memory db.
     * @param path
     * @param s
     * @return
     */
    public MockResponse handleUpdate(String path, String s) {
        return handlePost(path, s);
    }

    /**
     * Performs a get for the corresponding object from the in-memory db.
     *
     * @param path The path.
     * @return The {@link MockResponse}
     */
    public MockResponse handleGet(String path) {
        MockResponse response = new MockResponse();
        List<String> items = new ArrayList<>();
        AttributeSet query = attributeExtractor.extract(path);

        for (Map.Entry<AttributeSet, String> entry : map.entrySet()) {
            if (entry.getKey().matches(query)) {
                items.add(entry.getValue());
            }
        }
        if (!items.isEmpty()) {
            response.setBody(responseComposer.compose(items));
            response.setResponseCode(200);
        } else {
            response.setResponseCode(404);
        }
        return response;
    }


    /**
     * Performs a delete for the corresponding object from the in-memory db.
     * @param path
     * @return
     */
    public MockResponse handleDelete(String path) {
        MockResponse response = new MockResponse();
        List<AttributeSet> items = new ArrayList<>();
        AttributeSet query = attributeExtractor.extract(path);

        for (Map.Entry<AttributeSet, String> entry : map.entrySet()) {
            if (entry.getKey().matches(query)) {
                items.add(entry.getKey());
            }
        }
        if (!items.isEmpty()) {
            for (AttributeSet item: items)  {
                map.remove(item);
            }
            response.setResponseCode(200);
        } else {
            response.setResponseCode(404);
        }
        return response;
    }
}
