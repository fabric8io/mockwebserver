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

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketSession extends WebSocketListener {

    private final List<WebSocketMessage> open;
    private final WebSocketMessage failure;
    private final Exception cause;

    private final Map<Object, Queue<WebSocketMessage>> requestEvents = new HashMap<>();
    private final List<WebSocketMessage> timedEvents = new ArrayList<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public WebSocketSession(List<WebSocketMessage> open, WebSocketMessage failure, Exception cause) {
        this.open = open;
        this.failure = failure;
        this.cause = cause;
    }

    @Override
    public void onOpen(WebSocket ws, Response response) {
        //Schedule all timed events
        for (WebSocketMessage msg : open) {
            send(ws, msg);
        }

        for (WebSocketMessage msg : timedEvents) {
            send(ws, msg);
        }

        //checkIfShouldClose(ws);
    }

    @Override
    public void onMessage(WebSocket ws, String text) {
        Queue<WebSocketMessage> queue = requestEvents.get(text);
        if (queue != null && !queue.isEmpty()) {
            WebSocketMessage msg = queue.peek();
            send(ws, msg);
            if (msg.isToBeRemoved()) {
                queue.remove();
            }
            checkIfShouldClose(ws);
        } else {
            ws.close(0, "Unexpected message:" + text);
        }
    }

    @Override
    public void onMessage(WebSocket ws, ByteString text) {
        System.out.println("other on message");
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

    void registerRequestEvent(Object req, WebSocketMessage resp) {
        Queue<WebSocketMessage> responses = requestEvents.get(req);
        if (responses == null) {
            responses = new ArrayDeque<>();
            requestEvents.put(req, responses);
        }
        responses.add(resp);
    }

    public void addTimedEvent(WebSocketMessage msg) {
        timedEvents.add(msg);
    }

    private void checkIfShouldClose(WebSocket ws) {
        if (requestEvents.isEmpty()) {
            try {
                executor.shutdown();
                if (executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    ws.close(1000, "Closing...");
                } else {
                    executor.shutdownNow();
                    ws.close(1000, "Closing...");
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    private void send(final WebSocket ws, final WebSocketMessage message) {
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                if (ws != null) {
                    ws.send(message.getBody());
                }
            }
        }, message.getDelay(), TimeUnit.MILLISECONDS);
    }
}
