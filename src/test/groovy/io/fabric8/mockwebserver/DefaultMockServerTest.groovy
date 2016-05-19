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

import com.squareup.okhttp.*
import com.squareup.okhttp.ws.WebSocket
import com.squareup.okhttp.ws.WebSocketCall
import com.squareup.okhttp.ws.WebSocketListener
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

public class DefaultMockServerTest extends Specification {

    @Shared
    DefaultMockServer server
    @Shared
    OkHttpClient client

    def setup() {
        server = new DefaultMockServer()
        client = new OkHttpClient()
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
        response1.code() == 200
        response1.body().string() == "admin"
        response2.code() == 404
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
        response1.code() == 200
        response1.body().string() == "admin"
        response2.code() == 200
        response2.body().string() == "admin"
        response3.code() == 200
        response3.body().string() == "admin"
        response4.code() == 404
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
        response1.code() == 200
        response1.body().string() == "admin"
        response2.code() == 200
        response2.body().string() == "admin"
        response3.code() == 200
        response3.body().string() == "admin"
        response4.code() == 200
        response4.body().string() == "admin"
    }

    def "when setting an expectation as an object it should be serialized to json"() {
        given:
        User root = new User(0, "root", true)

        server.expect().get().withPath("/api/v1/users").andReturn(200, root).always()

        when:
        Request request = new Request.Builder().url(server.url("/api/v1/users")).get().build()
        Response response1 = client.newCall(request).execute()

        then:
        response1.code() == 200
        response1.body().string() == "{\"id\":0,\"username\":\"root\",\"enabled\":true}"
    }

    def "when setting a timed websocket message it should be fire at the specified time"() {
        given:
        CountDownLatch closed = new CountDownLatch(1)
        Queue<String> messages = new ArrayBlockingQueue<String>(1)
        WebSocketListener listener = new AbstractWebSocketListener() {
            void onMessage(ResponseBody message) throws IOException {
                messages.add(message.string())
            }
            void onClose(int code, String reason) {
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
        WebSocketCall call = WebSocketCall.create(client, request)
        call.enqueue(listener)

        then:
        messages.poll(2, TimeUnit.SECONDS) == "DELETED"
        closed.await(2, TimeUnit.SECONDS)
    }

    def "when setting a request/response websocket message it should be fired when the event is triggered"() {
        given:
        CountDownLatch opened = new CountDownLatch(1)
        CountDownLatch closed = new CountDownLatch(1)
        Queue<String> messages = new ArrayBlockingQueue<String>(1)
        AtomicReference<WebSocket> webSocketRef = new AtomicReference<>()

        WebSocketListener listener = new AbstractWebSocketListener() {
            @Override
            void onOpen(WebSocket webSocket, Response response) {
                webSocketRef.set(webSocket)
                opened.countDown()
            }

            @Override
            void onMessage(ResponseBody message) throws IOException {
                messages.add(message.string())
            }

            @Override
            void onClose(int code, String reason) {
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
        WebSocketCall call = WebSocketCall.create(client, request)
        call.enqueue(listener)

        then:
        opened.await(1, TimeUnit.SECONDS)
        WebSocket ws = webSocketRef.get()
        ws.sendMessage(RequestBody.create(WebSocket.TEXT, "create root"))
        messages.poll(2, TimeUnit.SECONDS) == "CREATED"
        ws.sendMessage(RequestBody.create(WebSocket.TEXT, "delete root"))
        messages.poll(2, TimeUnit.SECONDS) == "DELETED"
        ws.close(1000, "just close")
        closed.await(2, TimeUnit.SECONDS)
    }
}