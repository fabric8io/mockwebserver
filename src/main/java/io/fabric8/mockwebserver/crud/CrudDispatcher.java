package io.fabric8.mockwebserver.crud;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.mockwebserver.Context;
import io.fabric8.zjsonpatch.JsonPatch;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrudDispatcher extends Dispatcher {

    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String PATCH = "PATCH";
    private static final String GET = "GET";
    private static final String DELETE = "DELETE";

    protected Map<AttributeSet, String> map = new HashMap<>();

    protected final Context context;
    protected final AttributeExtractor attributeExtractor;
    protected final ResponseComposer responseComposer;

    public CrudDispatcher(Context context, AttributeExtractor attributeExtractor, ResponseComposer responseComposer) {
        this.context = context;
        this.attributeExtractor = attributeExtractor;
        this.responseComposer = responseComposer;
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        String path = request.getPath();
        String method = request.getMethod();

        switch (method.toUpperCase()) {
            case POST:
            case PUT:
                return handleCreate(path, request.getBody().readUtf8());
            case PATCH:
                return handlePatch(path, request.getBody().readUtf8());
            case GET:
                return handleGet(path);
            case DELETE:
                return handleDelete(path);
            default:
                return null;
        }
    }

    /**
     * Adds the specified object to the in-memory db.
     *
     * @param path
     * @param s
     * @return
     */
    public MockResponse handleCreate(String path, String s) {
        MockResponse response = new MockResponse();
        AttributeSet features = AttributeSet.merge(attributeExtractor.fromPath(path), attributeExtractor.fromResource(s));
        map.put(features, s);
        response.setBody(s);
        response.setResponseCode(202);
        return response;
    }

    /**
     * Patches the specified object to the in-memory db.
     *
     * @param path
     * @param s
     * @return
     */
    public MockResponse handlePatch(String path, String s) {
        MockResponse response = new MockResponse();
        String body = doGet(path);
        if (body == null) {
            response.setResponseCode(404);
        } else {
            try {
                JsonNode patch = context.getMapper().readTree(s);
                JsonNode source = context.getMapper().readTree(body);
                JsonNode updated = JsonPatch.apply(patch, source);
                response.setResponseCode(202);
                response.setBody(context.getMapper().writeValueAsString(updated));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        return response;
    }

    /**
     * Updates the specified object to the in-memory db.
     *
     * @param path
     * @param s
     * @return
     */
    public MockResponse handleUpdate(String path, String s) {
        return handleCreate(path, s);
    }

    /**
     * Performs a get for the corresponding object from the in-memory db.
     *
     * @param path The path.
     * @return The {@link MockResponse}
     */
    public MockResponse handleGet(String path) {
        MockResponse response = new MockResponse();

        String body = doGet(path);
        if (body == null) {
            response.setResponseCode(404);
        } else {
            response.setResponseCode(200);
            response.setBody(body);
        }
        return response;
    }


    /**
     * Performs a delete for the corresponding object from the in-memory db.
     *
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
            for (AttributeSet item : items) {
                map.remove(item);
            }
            response.setResponseCode(200);
        } else {
            response.setResponseCode(404);
        }
        return response;
    }

    public Map<AttributeSet, String> getMap() {
        return map;
    }

    public AttributeExtractor getAttributeExtractor() {
        return attributeExtractor;
    }

    public ResponseComposer getResponseComposer() {
        return responseComposer;
    }


    private String doGet(String path) {
        List<String> items = new ArrayList<>();
        AttributeSet query = attributeExtractor.extract(path);
        for (Map.Entry<AttributeSet, String> entry : map.entrySet()) {
            if (entry.getKey().matches(query)) {
                items.add(entry.getValue());
            }
        }

        if (items.isEmpty()) {
            return null;
        } else if (items.size() == 1) {
            return items.get(0);
        } else {
            return responseComposer.compose(items);
        }
    }
}
