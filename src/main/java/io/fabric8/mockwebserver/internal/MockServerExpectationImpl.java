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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.mockwebserver.dsl.HttpMethod;
import io.fabric8.mockwebserver.dsl.Function;
import io.fabric8.mockwebserver.dsl.MockServerExpectation;
import io.fabric8.mockwebserver.dsl.Pathable;
import io.fabric8.mockwebserver.dsl.ReturnOrWebsocketable;
import io.fabric8.mockwebserver.dsl.TimesOrOnceable;
import io.fabric8.mockwebserver.dsl.Timesable;
import io.fabric8.mockwebserver.dsl.WebSocketSessionBuilder;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class MockServerExpectationImpl implements MockServerExpectation {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final HttpMethod method;
  private final String path;
  private final int statusCode;
  private final String body;
  private final long initialDelay;
  private final long period;
  private final TimeUnit timeUnit;
  private final int times;

  private final Map<ServerRequest, Queue<ServerResponse>> responses;

  public MockServerExpectationImpl(Map<ServerRequest, Queue<ServerResponse>> responses) {
    this(HttpMethod.ANY, null, 200, null, 0, 0, TimeUnit.SECONDS, 1, responses);
  }
  public MockServerExpectationImpl(HttpMethod method, String path, int statusCode, String body, long initialDelay, long period, TimeUnit timeUnit, int times, Map<ServerRequest, Queue<ServerResponse>> responses) {
    this.method = method;
    this.path = path;
    this.statusCode = statusCode;
    this.body = body;
    this.initialDelay = initialDelay;
    this.period = period;
    this.timeUnit = timeUnit;
    this.times = times;
    this.responses = responses;
  }

  @Override
  public Pathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> any() {
    return new MockServerExpectationImpl(HttpMethod.ANY, path, statusCode, body, initialDelay, period, timeUnit, times, responses);
  }

  @Override
  public Pathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> post() {
    return new MockServerExpectationImpl(HttpMethod.POST, path, statusCode, body, initialDelay, period, timeUnit, times, responses);
  }

  @Override
  public Pathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> get() {
    return new MockServerExpectationImpl(HttpMethod.GET, path, statusCode, body, initialDelay, period, timeUnit, times, responses);
  }

  @Override
  public Pathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> put() {
    return new MockServerExpectationImpl(HttpMethod.PUT, path, statusCode, body, initialDelay, period, timeUnit, times, responses);
  }

  @Override
  public Pathable<ReturnOrWebsocketable<TimesOrOnceable<Void>>> delete() {
    return new MockServerExpectationImpl(HttpMethod.DELETE, path, statusCode, body, initialDelay, period, timeUnit, times, responses);
  }

  @Override
  public ReturnOrWebsocketable<TimesOrOnceable<Void>> withPath(String path) {
    return new MockServerExpectationImpl(method, path, statusCode, body, initialDelay, period, timeUnit, times, responses);
  }

  @Override
  public TimesOrOnceable<Void> andReturn(int statusCode, Object content) {
    if (content instanceof String) {
      return new MockServerExpectationImpl(method, path, statusCode, (String) content, initialDelay, period, timeUnit, times, responses);
    } else {
      try {
        return new MockServerExpectationImpl(method, path, statusCode, MAPPER.writeValueAsString(content), initialDelay, period, timeUnit, times, responses);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public Void always() {
    enqueue(new ServerRequest(method, path), new ServerResponse(statusCode, body, null, true));
    return null;//Void
  }

  @Override
  public Void once() {
    enqueue(new ServerRequest(method, path), new ServerResponse(statusCode, body, null, false));
    return null;//Void
  }

  @Override
  public Void times(int times) {
    for (int i = 0; i < times; i++) {
      once();
    }
    return null;//Void
  }

  private void enqueue(ServerRequest req, ServerResponse resp) {
    Queue<ServerResponse> queuedResponses = responses.get(req);
    if (queuedResponses == null) {
      queuedResponses = new ArrayDeque<>();
      responses.put(req, queuedResponses);
    }
    queuedResponses.add(resp);
  }


  @Override
  public WebSocketSessionBuilder<TimesOrOnceable<Void>> andUpgradeToWebSocket() {
    return new InlineWebSocketSessionBuilder<>(new Function<WebSocketSession, TimesOrOnceable<Void>>() {
      @Override
      public TimesOrOnceable<Void> apply(final WebSocketSession webSocketSession) {
        return new TimesOrOnceable<Void>() {

          @Override
          public Void always() {
            enqueue(new ServerRequest(method, path), new ServerResponse(statusCode, null, webSocketSession, true));
            return null;//Void
          }

          @Override
          public Void once() {
            enqueue(new ServerRequest(method, path), new ServerResponse(statusCode, null, webSocketSession, false));
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
}
