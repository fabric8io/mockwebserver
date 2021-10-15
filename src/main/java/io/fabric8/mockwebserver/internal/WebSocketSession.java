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

package io.fabric8.mockwebserver.internal;

import io.fabric8.mockwebserver.dsl.HttpMethod;
import okhttp3.Response;
import io.fabric8.mockwebserver.Context;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.RecordedRequest;
import okio.ByteString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class WebSocketSession extends WebSocketListener {

    private final List<WebSocketMessage> open;
    private final WebSocketMessage failure;
    private final Exception cause;

    private final Map<Object, Queue<WebSocketMessage>> requestEvents = new HashMap<>();
    private final Map<Object, Queue<WebSocketMessage>> sentWebSocketMessagesRequestEvents = new HashMap<>();
    private final Map<SimpleRequest, Queue<WebSocketMessage>> httpRequestEvents = new HashMap<>();
    private final List<WebSocketMessage> timedEvents = new ArrayList<>();

    private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();
    private final ScheduledExecutorService executor;

    private final Context context;

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocketRef.get().close(code, reason);
    }

    public WebSocketSession(Context context, ScheduledExecutorService executor, List<WebSocketMessage> open, WebSocketMessage failure, Exception cause) {
        this.context = context;
        this.open = open;
        this.failure = failure;
        this.cause = cause;
        this.executor = executor;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        webSocketRef.set(webSocket);
        //Schedule all timed events
        for (WebSocketMessage msg : open) {
            send(msg);
        }

        for (WebSocketMessage msg : timedEvents) {
            send(msg);
        }

        checkIfShouldClose();
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        onMessage(webSocket, bytes.utf8());
    }

    @Override
    public void onMessage(WebSocket webSocket, String in) {
        Queue<WebSocketMessage> queue = requestEvents.get(in);
        send(queue, in);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
    }

    private void send(Queue<WebSocketMessage> queue, String in) {
        if (queue != null && !queue.isEmpty()) {
            WebSocketMessage msg = queue.peek();
            send(msg);
            if (msg.isToBeRemoved()) {
                queue.remove();
            }
            checkIfShouldSendAgain(msg);
            checkIfShouldClose();
        } else {
            webSocketRef.get().close(1002, "Unexpected message:" + in);
        }
    }

    private void checkIfShouldSendAgain(WebSocketMessage msg) {
        String text = msg.isBinary() ? ByteString.of(msg.getBytes()).utf8() : msg.getBody();
        if (sentWebSocketMessagesRequestEvents.containsKey(text)) {
            Queue<WebSocketMessage> queue = sentWebSocketMessagesRequestEvents.get(text);
            send(queue, text);
        }
    }

    public void dispatch(RecordedRequest request) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String path = request.getPath();
        SimpleRequest key = new SimpleRequest(method, path);
        SimpleRequest keyForAnyMethod = new SimpleRequest(path);
        if (httpRequestEvents.containsKey(key)) {
            Queue<WebSocketMessage> queue = httpRequestEvents.get(key);
            send(queue, "from http " + path);
        } else if (httpRequestEvents.containsKey(keyForAnyMethod)) {
            Queue<WebSocketMessage> queue = httpRequestEvents.get(keyForAnyMethod);
            send(queue, "from http " + path);
        }
    }

    public List<WebSocketMessage> getOpen() {
        return open;
    }

    public WebSocketMessage getFailure() {
        return failure;
    }

    public Exception getCause() {
        return cause;
    }

    public Map<Object, Queue<WebSocketMessage>> getRequestEvents() {
        return requestEvents;
    }

    public List<WebSocketMessage> getTimedEvents() {
        return timedEvents;
    }

    public Map<Object, Queue<WebSocketMessage>> getSentWebSocketMessagesRequestEvents() {
        return sentWebSocketMessagesRequestEvents;
    }

    public Map<SimpleRequest, Queue<WebSocketMessage>> getHttpRequestEvents() {
        return httpRequestEvents;
    }

    private void checkIfShouldClose() {
        if (requestEvents.isEmpty() && httpRequestEvents.isEmpty() && sentWebSocketMessagesRequestEvents.isEmpty()) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    executor.shutdownNow();
                }
                webSocketRef.get().close(1000, "Closing...");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void send(final WebSocketMessage message) {
        executor.schedule(() -> {
            WebSocket ws = webSocketRef.get();
            if (ws != null) {
                if (message.isBinary()) {
                    ws.send(ByteString.of(message.getBytes()));
                } else {
                    ws.send(message.getBody());
                }
            }
        }, message.getDelay(), TimeUnit.MILLISECONDS);
    }
}
