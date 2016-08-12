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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;
import io.fabric8.mockwebserver.dsl.DelayPathable;
import io.fabric8.mockwebserver.dsl.Function;
import io.fabric8.mockwebserver.dsl.HttpMethod;
import io.fabric8.mockwebserver.dsl.MockServerExpectation;
import io.fabric8.mockwebserver.dsl.Pathable;
import io.fabric8.mockwebserver.dsl.ReturnOrWebsocketable;
import io.fabric8.mockwebserver.dsl.TimesOrOnceable;
import io.fabric8.mockwebserver.dsl.WebSocketSessionBuilder;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class MockServerExpectationImpl implements MockServerExpectation {

  private final Context context;
  private final HttpMethod method;
  private final String path;
  private final int statusCode;
  private final String body;
  private final String[] chunks;
  private final long delay;
  private final TimeUnit delayUnit;
  private final int times;

  private final Map<ServerRequest, Queue<ServerResponse>> responses;

  public MockServerExpectationImpl(Map<ServerRequest, Queue<ServerResponse>> responses, Context context) {
    this(context, HttpMethod.ANY, null, 200, null, null, 0, TimeUnit.SECONDS, 1, responses);
  }
  public MockServerExpectationImpl(Context context, HttpMethod method, String path, int statusCode, String body, String[] chunks, long delay, TimeUnit delayUnit, int times, Map<ServerRequest, Queue<ServerResponse>> responses) {
    this.context = context;
    this.method = method;
    this.path = path;
    this.statusCode = statusCode;
    this.body = body;
    this.chunks = chunks;
    this.delay = delay;
    this.delayUnit = delayUnit;
    this.times = times;
    this.responses = responses;
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> any() {
    return new MockServerExpectationImpl(context, HttpMethod.ANY, path, statusCode, body, chunks, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> post() {
    return new MockServerExpectationImpl(context, HttpMethod.POST, path, statusCode, body, chunks, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> get() {
    return new MockServerExpectationImpl(context, HttpMethod.GET, path, statusCode, body, chunks, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> put() {
    return new MockServerExpectationImpl(context, HttpMethod.PUT, path, statusCode, body, chunks, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> delete() {
    return new MockServerExpectationImpl(context, HttpMethod.DELETE, path, statusCode, body, chunks, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> patch() {
    return new MockServerExpectationImpl(context, HttpMethod.PATCH, path, statusCode, body, chunks, delay, delayUnit, times, responses);
  }

  @Override
  public ReturnOrWebsocketable<TimesOrOnceable<Void>> withPath(String path) {
    return new MockServerExpectationImpl(context, method, path, statusCode, body, chunks, delay, delayUnit, times, responses);
  }

  @Override
  public TimesOrOnceable<Void> andReturn(int statusCode, Object content) {
    return new MockServerExpectationImpl(context, method, path, statusCode, toString(content), chunks, delay, delayUnit, times, responses);
  }

  @Override
  @Deprecated
  public TimesOrOnceable<Void> andReturnChucked(int statusCode, Object... contents) {
    return andReturnChunked(statusCode, contents);
  }

  @Override
  public TimesOrOnceable<Void> andReturnChunked(int statusCode, Object... contents) {
    return new MockServerExpectationImpl(context, method, path, statusCode, body, toString(contents), delay, delayUnit, times, responses);
  }

  @Override
  public Void always() {
    enqueue(new SimpleRequest(method, path), createResponse(true, delay, delayUnit));
    return null;//Void
  }

  @Override
  public Void once() {
    enqueue(new SimpleRequest(method, path), createResponse(false, delay, delayUnit));
    return null;//Void
  }

  @Override
  public Void times(int times) {
    for (int i = 0; i < times; i++) {
      once();
    }
    return null;//Void
  }

  @Override
  public Pathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> delay(long delay, TimeUnit delayUnit) {
    return new MockServerExpectationImpl(context, method, path, statusCode, body, chunks, delay, delayUnit, times, responses);
  }

  @Override
  public Pathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> delay(long delayInMilliseconds) {
    return new MockServerExpectationImpl(context, method, path, statusCode, body, chunks, delayInMilliseconds, TimeUnit.MILLISECONDS, times, responses);
  }

  @Override
  public WebSocketSessionBuilder<TimesOrOnceable<Void>> andUpgradeToWebSocket() {
    return new InlineWebSocketSessionBuilder<>(context, new Function<WebSocketSession, TimesOrOnceable<Void>>() {
      @Override
      public TimesOrOnceable<Void> apply(final WebSocketSession webSocketSession) {
        return new TimesOrOnceable<Void>() {

          @Override
          public Void always() {
            enqueue(new SimpleRequest(method, path), new SimpleResponse(true, statusCode, null, webSocketSession));
            return null;//Void
          }

          @Override
          public Void once() {
            enqueue(new SimpleRequest(method, path), new SimpleResponse(false, statusCode, null, webSocketSession));
            return null;//Void
          }

          @Override
          public Void times(int times) {
            for (int i = 0; i < times; i++) {
              once();
            }
            return null;//Void
          }
        };
      }
    });
  }


  private void enqueue(ServerRequest req, ServerResponse resp) {
    Queue<ServerResponse> queuedResponses = responses.get(req);
    if (queuedResponses == null) {
      queuedResponses = new ArrayDeque<>();
      responses.put(req, queuedResponses);
    }
    queuedResponses.add(resp);
  }

  private ServerResponse createResponse(boolean repeatable, long delay, TimeUnit delayUnit) {
    if (chunks != null) {
      return new ChunkedResponse(repeatable, statusCode, delay, delayUnit, chunks);
    } else {
      return new SimpleResponse(repeatable, statusCode, body, null, delay, delayUnit);
    }
  }

  private String toString(Object object) {
    if (object instanceof String) {
      return (String) object;
    } else {
      try {
        return context.getMapper().writeValueAsString(object);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String[] toString(Object object[]) {
    String[] strings = new String[object.length];
    for (int i=0;i<object.length;i++) {
      strings[i] = toString(object[i]);
    }
    return strings;
  }
}
