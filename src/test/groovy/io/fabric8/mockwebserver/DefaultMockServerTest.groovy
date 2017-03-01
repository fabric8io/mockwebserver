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

package io.fabric8.mockwebserver

import okhttp3.*
import okio.ByteString
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class DefaultMockServerTest extends Specification {

    DefaultMockServer server

    @Shared
    OkHttpClient client = new OkHttpClient()

    def setup() {
        server = new DefaultMockServer()
        server.start()
    }

    def cleanup() {
        server.shutdown()
    }

    def "when setting an expectation with once it should be met only the first time"() {
        given:
        server.expect().get().withPath("/api/v1/users").andReturn(200, "admin").once()

        when:
        Request request = new Request.Builder().url(server.url("/api/v1/users")).get().build()
        Response response1 = client.newCall(request).execute()
        Response response2 = client.newCall(request).execute()

        then:
        try {
            response1.code() == 200
            response1.body().string() == "admin"
            response2.code() == 404
        } finally {
            response1.close()
            response2.close()
        }
    }

    def "when setting an expectation with n-th times it should be met only the for the first n-th times"() {
        given:
        server.expect().get().withPath("/api/v1/users").andReturn(200, "admin").times(3)

        when:
        Request request = new Request.Builder().url(server.url("/api/v1/users")).get().build()
        Response response1 = client.newCall(request).execute()
        Response response2 = client.newCall(request).execute()
        Response response3 = client.newCall(request).execute()
        Response response4 = client.newCall(request).execute()

        then:
        try {
            response1.code() == 200
            response1.body().string() == "admin"
            response2.code() == 200
            response2.body().string() == "admin"
            response3.code() == 200
            response3.body().string() == "admin"
            response4.code() == 404
        } finally {
            response1.close()
            response2.close()
            response3.close()
            response4.close()
        }
    }

    def "when setting an expectation with alwyas it should be met only alwayas"() {
        given:
        server.expect().get().withPath("/api/v1/users").andReturn(200, "admin").always()

        when:
        Request request = new Request.Builder().url(server.url("/api/v1/users")).get().build()
        Response response1 = client.newCall(request).execute()
        Response response2 = client.newCall(request).execute()
        Response response3 = client.newCall(request).execute()
        Response response4 = client.newCall(request).execute()

        then:
        try {
            response1.code() == 200
            response1.body().string() == "admin"
            response2.code() == 200
            response2.body().string() == "admin"
            response3.code() == 200
            response3.body().string() == "admin"
            response4.code() == 200
            response4.body().string() == "admin"
        } finally {
            response1.close()
            response2.close()
            response3.close()
            response4.close()
        }
    }

    def "when setting an expectation as an object it should be serialized to json"() {
        given:
        User root = new User(0, "root", true)

        server.expect().get().withPath("/api/v1/users").andReturn(200, root).always()

        when:
        Request request = new Request.Builder().url(server.url("/api/v1/users")).get().build()
        Response response1 = client.newCall(request).execute()

        then:
        try {
            response1.code() == 200
            response1.body().string() == "{\"id\":0,\"username\":\"root\",\"enabled\":true}"
        } finally {
            response1.close()
        }
    }

    def "when setting a timed websocket message it should be fire at the specified time"() {
        given:
        CountDownLatch closed = new CountDownLatch(1)
        Queue<String> messages = new ArrayBlockingQueue<String>(1)
        AtomicReference<WebSocket> webSocketRef = new AtomicReference<>()
        WebSocketListener listener = new WebSocketListener() {
            @Override
            void onMessage(WebSocket webSocket, String text) {
                messages.add(text)
            }

            @Override
            void onMessage(WebSocket webSocket, ByteString bytes) {
                onMessage(webSocket, bytes.utf8())
            }

            @Override
            void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(code, reason);
            }

            @Override
            void onClosed(WebSocket webSocket, int code, String reason) {
                closed.countDown()
            }
        }

        server.expect().get().withPath("/api/v1/users/watch")
            .andUpgradeToWebSocket()
                .open()
                .waitFor(1000).andEmit("DELETED")
                .done()
            .once()

        when:
        Request request = new Request.Builder().url(server.url("/api/v1/users/watch")).get().build()
        webSocketRef.set(client.newWebSocket(request, listener))

        then:
        messages.poll(2, TimeUnit.SECONDS) == "DELETED"
        webSocketRef.get().close(1000, "just close")
        closed.await(2, TimeUnit.SECONDS)
    }

    def "when setting a request/response websocket message it should be fired when the event is triggered"() {
        given:
        CountDownLatch opened = new CountDownLatch(1)
        CountDownLatch closed = new CountDownLatch(1)
        Queue<String> messages = new ArrayBlockingQueue<String>(1)
        AtomicReference<WebSocket> webSocketRef = new AtomicReference<>()

        WebSocketListener listener = new WebSocketListener() {
            @Override
            void onOpen(WebSocket webSocket, Response response) {
                webSocketRef.set(webSocket)
                opened.countDown()
            }

            @Override
            void onMessage(WebSocket webSocket, String text) {
                messages.add(text)
            }

            @Override
            void onMessage(WebSocket webSocket, ByteString bytes) {
                onMessage(webSocket, bytes.utf8());
            }

            @Override
            void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(code, reason);
            }

            @Override
            void onClosed(WebSocket webSocket, int code, String reason) {
                closed.countDown()
            }
        }

        server.expect().get().withPath("/api/v1/users/watch")
                .andUpgradeToWebSocket()
                .open()
                    .expect("create root").andEmit("CREATED").once()
                    .expect("delete root").andEmit("DELETED").once()
                .done()
                .once()


        when:
        Request request = new Request.Builder().url(server.url("/api/v1/users/watch")).get().build()
        webSocketRef.set(client.newWebSocket(request, listener))

        then:
        opened.await(1, TimeUnit.SECONDS)
        WebSocket ws = webSocketRef.get()
        ws.send("create root")
        ws.send("delete root")
        messages.poll(2, TimeUnit.SECONDS) == "CREATED"
        messages.poll(2, TimeUnit.SECONDS) == "DELETED"
        ws.close(1000, "just close")
        closed.await(2, TimeUnit.SECONDS)
    }

    def "when setting a delayed response it should be delayed for the specified duration"() {
        given:
        server.expect().get().withPath("/api/v1/users").delay(100, TimeUnit.MILLISECONDS).andReturn(200, "admin").once()

        when:
        Request request = new Request.Builder().url(server.url("/api/v1/users")).get().build()
        long startTime = System.currentTimeMillis()
        Response response1 = client.newCall(request).execute()

        then:
        try {
            response1.code() == 200
            response1.body().string() == "admin"
            System.currentTimeMillis() - startTime >= 100
        } finally {
            response1.close()
        }
    }
}
