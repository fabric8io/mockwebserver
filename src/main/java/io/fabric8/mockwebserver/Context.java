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

package io.fabric8.mockwebserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.mockwebserver.internal.DefaultWebSocketReader;
import io.fabric8.mockwebserver.internal.DefaultWebSocketWriter;
import io.sundr.builder.annotations.Buildable;

public class Context {

    private final ObjectMapper mapper;
    private final WebSocketReader reader;
    private final WebSocketWriter writer;

    public Context() {
        this(new ObjectMapper(), new DefaultWebSocketReader(), new DefaultWebSocketWriter());
    }

    @Buildable(builderPackage = "io.fabric8.mockwebserver.builder", generateBuilderPackage = true)
    public Context(ObjectMapper mapper, WebSocketReader reader, WebSocketWriter writer) {
        this.mapper = mapper;
        this.reader = reader;
        this.writer = writer;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public WebSocketReader getReader() {
        return reader;
    }

    public WebSocketWriter getWriter() {
        return writer;
    }
}
