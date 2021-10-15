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

    protected final Map<AttributeSet, String> map = new HashMap<>();

    protected final Context context;
    protected final AttributeExtractor attributeExtractor;
    protected final ResponseComposer responseComposer;

    public CrudDispatcher(Context context, AttributeExtractor attributeExtractor, ResponseComposer responseComposer) {
        this.context = context;
        this.attributeExtractor = attributeExtractor;
        this.responseComposer = responseComposer;
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) {
        String path = request.getPath();
        String method = request.getMethod();

        switch (method.toUpperCase()) {
            case POST:
                return handleCreate(path, request.getBody().readUtf8());
            case PUT:
                return handleUpdate(path, request.getBody().readUtf8());
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
     * @param path for the request.
     * @param body Request body as String (UTF-8).
     * @return a MockResponse to be dispatched.
     */
    public MockResponse handleCreate(String path, String body) {
        MockResponse response = new MockResponse();
        AttributeSet features = AttributeSet.merge(attributeExtractor.fromPath(path), attributeExtractor.fromResource(body));
        map.put(features, body);
        response.setBody(body);
        response.setResponseCode(202);
        return response;
    }

    /**
     * Patches the specified object to the in-memory db.
     *
     * @param path for the request.
     * @param body Request body as String (UTF-8).
     * @return a MockResponse to be dispatched.
     */
    public MockResponse handlePatch(String path, String body) {
        MockResponse response = new MockResponse();
        String existingObjectBody = doGet(path);
        if (existingObjectBody == null) {
            response.setResponseCode(404);
        } else {
            try {
                JsonNode patch = context.getMapper().readTree(body);
                JsonNode source = context.getMapper().readTree(existingObjectBody);
                JsonNode updated = JsonPatch.apply(patch, source);
                String updatedAsString = context.getMapper().writeValueAsString(updated);
                AttributeSet features = AttributeSet.merge(attributeExtractor.fromPath(path),
                        attributeExtractor.fromResource(updatedAsString));
                map.put(features, updatedAsString);
                response.setResponseCode(202);
                response.setBody(updatedAsString);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        return response;
    }

    /**
     * Updates the specified object to the in-memory db.
     *
     * @param path for the request.
     * @param body Request body as String (UTF-8).
     * @return a MockResponse to be dispatched.
     */
    public MockResponse handleUpdate(String path, String body) {
        return handleCreate(path, body);
    }

    /**
     * Performs a get for the corresponding object from the in-memory db.
     *
     * @param path for the request.
     * @return a MockResponse to be dispatched.
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
     * @param path for the request.
     * @return a MockResponse to be dispatched.
     */
    public MockResponse handleDelete(String path) {
        MockResponse response = new MockResponse();
        List<AttributeSet> items = new ArrayList<>();
        AttributeSet query = attributeExtractor.fromPath(path);

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
        AttributeSet query = attributeExtractor.fromPath(path);
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
