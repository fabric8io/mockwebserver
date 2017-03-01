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

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import io.fabric8.mockwebserver.Context;
import okio.ByteString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class WebSocketSession extends WebSocketListener {

    private final List<WebSocketMessage> open;
    private final WebSocketMessage failure;
    private final Exception cause;

    private final Map<Object, Queue<WebSocketMessage>> requestEvents = new HashMap<>();
    private final List<WebSocketMessage> timedEvents = new ArrayList<>();

    private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final Context context;

    public WebSocketSession(Context context, List<WebSocketMessage> open, WebSocketMessage failure, Exception cause) {
        this.context = context;
        this.open = open;
        this.failure = failure;
        this.cause = cause;
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
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    }

    @Override
    public void onMessage(WebSocket webSocket,  ByteString bytes) {
        String s = new String(bytes.toByteArray());
        onMessage(webSocket, s);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text)  {
        Queue<WebSocketMessage> queue = requestEvents.get(text);
        if (queue != null && !queue.isEmpty()) {
            WebSocketMessage msg = queue.peek();
            send(msg);
            if (msg.isToBeRemoved()) {
                queue.remove();
            }
            checkIfShouldClose();
        } else {
            webSocketRef.get().close(0, "Unexpected message:" + text);
        }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocketRef.get().close(code, reason);
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

    private void checkIfShouldClose() {
        if (requestEvents.isEmpty()) {
            try {
                executor.shutdown();
                if (executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    webSocketRef.get().close(1000, "Closing...");
                } else {
                    executor.shutdownNow();
                    webSocketRef.get().close(1000, "Closing...");
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    private void send(final WebSocketMessage message) {
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                WebSocket ws = webSocketRef.get();
                if (ws != null) {
                    ws.send(message.getBody());
                }
            }
        }, message.getDelay(), TimeUnit.MILLISECONDS);
    }
}
