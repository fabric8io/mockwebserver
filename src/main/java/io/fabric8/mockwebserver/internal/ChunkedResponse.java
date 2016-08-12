/*
 *   Copyright (C) 2016 Red Hat, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.fabric8.mockwebserver.internal;

import io.fabric8.mockwebserver.ServerResponse;
import okhttp3.mockwebserver.MockResponse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChunkedResponse implements ServerResponse {

    private static final int DEFAULT_MAX_CHUNK_SIZE = 204800;
    private final int statusCode;
    private final List<String> body;
    private final boolean repeatable;
    private final long responseDelay;
    private final TimeUnit responseDelayUnit;

    public ChunkedResponse(boolean repeatable, int statusCode, String... body) {
        this(repeatable, statusCode, 0, TimeUnit.MILLISECONDS, body);
    }

    public ChunkedResponse(boolean repeatable, int statusCode, long responseDelay, TimeUnit responseDelayUnit, String... body) {
        this.statusCode = statusCode;
        this.body = Arrays.asList(body);
        this.repeatable = repeatable;
        this.responseDelay = responseDelay;
        this.responseDelayUnit = responseDelayUnit;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public List<String> getBody() {
        return body;
    }

    public MockResponse toMockResponse() {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setChunkedBody(concatBody(), DEFAULT_MAX_CHUNK_SIZE);
        mockResponse.setResponseCode(statusCode);

        if (responseDelay > 0) {
            mockResponse.setBodyDelay(responseDelay, responseDelayUnit);
        }

        return mockResponse;
    }

    private String concatBody() {
        StringBuilder sb = new StringBuilder();
        for (String s : body) {
            sb.append(s);
        }
        return sb.toString();
    }

    public boolean isRepeatable() {
        return repeatable;
    }
}
