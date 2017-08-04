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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;
import io.fabric8.mockwebserver.dsl.DelayPathable;
import io.fabric8.mockwebserver.dsl.Function;
import io.fabric8.mockwebserver.dsl.HttpMethod;
import io.fabric8.mockwebserver.dsl.MockServerExpectation;
import io.fabric8.mockwebserver.dsl.Pathable;
import io.fabric8.mockwebserver.dsl.ReturnOrWebsocketable;
import io.fabric8.mockwebserver.dsl.TimesOnceableOrHttpHeaderable;
import io.fabric8.mockwebserver.dsl.TimesOrOnceable;
import io.fabric8.mockwebserver.dsl.WebSocketSessionBuilder;
import io.fabric8.mockwebserver.utils.BodyProvider;
import io.fabric8.mockwebserver.utils.ResponseProvider;
import io.fabric8.mockwebserver.utils.ResponseProviders;

import com.fasterxml.jackson.core.JsonProcessingException;

import okhttp3.Headers;
import okhttp3.mockwebserver.RecordedRequest;

public class MockServerExpectationImpl implements MockServerExpectation {

  private final Context context;
  private final HttpMethod method;
  private final String path;
  private final ResponseProvider<String> bodyProvider;
  private final ResponseProvider<List<String>> chunksProvider;
  private final long delay;
  private final TimeUnit delayUnit;
  private final int times;

  private final Map<ServerRequest, Queue<ServerResponse>> responses;

  public MockServerExpectationImpl(Map<ServerRequest, Queue<ServerResponse>> responses, Context context) {
    this(context, HttpMethod.ANY, null, 200, null, null, 0, TimeUnit.SECONDS, 1, responses);
  }

  public MockServerExpectationImpl(Context context, HttpMethod method, String path, int statusCode, String body, String[] chunks, long delay, TimeUnit delayUnit, int times, Map<ServerRequest, Queue<ServerResponse>> responses) {
    this(context, method, path, ResponseProviders.of(statusCode, body), ResponseProviders.ofAll(statusCode, chunks), delay, delayUnit, times, responses);
  }

  public MockServerExpectationImpl(Context context, HttpMethod method, String path, ResponseProvider<String> bodyProvider, ResponseProvider<List<String>> chunksProvider, long delay, TimeUnit delayUnit, int times, Map<ServerRequest, Queue<ServerResponse>> responses) {
    this.context = context;
    this.method = method;
    this.path = path;
    this.bodyProvider = bodyProvider;
    this.chunksProvider = chunksProvider;
    this.delay = delay;
    this.delayUnit = delayUnit;
    this.times = times;
    this.responses = responses;
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOnceableOrHttpHeaderable<Void>>> any() {
    return new MockServerExpectationImpl(context, HttpMethod.ANY, path, bodyProvider, chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOnceableOrHttpHeaderable<Void>>> post() {
    return new MockServerExpectationImpl(context, HttpMethod.POST, path, bodyProvider, chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOnceableOrHttpHeaderable<Void>>> get() {
    return new MockServerExpectationImpl(context, HttpMethod.GET, path, bodyProvider, chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOnceableOrHttpHeaderable<Void>>> put() {
    return new MockServerExpectationImpl(context, HttpMethod.PUT, path, bodyProvider, chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOnceableOrHttpHeaderable<Void>>> delete() {
    return new MockServerExpectationImpl(context, HttpMethod.DELETE, path, bodyProvider, chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  public DelayPathable<ReturnOrWebsocketable<TimesOnceableOrHttpHeaderable<Void>>> patch() {
    return new MockServerExpectationImpl(context, HttpMethod.PATCH, path, bodyProvider, chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  public ReturnOrWebsocketable<TimesOnceableOrHttpHeaderable<Void>> withPath(String path) {
    return new MockServerExpectationImpl(context, method, path, bodyProvider, chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  public TimesOnceableOrHttpHeaderable<Void> andReturn(int statusCode, Object content) {
    return new MockServerExpectationImpl(context, method, path, ResponseProviders.of(statusCode, toString(content)), chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  public TimesOnceableOrHttpHeaderable<Void> andReply(int statusCode, BodyProvider<Object> content) {
    return andReply(ResponseProviders.of(statusCode, content));
  }

  @Override
  public TimesOnceableOrHttpHeaderable<Void> andReply(ResponseProvider<Object> content) {
    return new MockServerExpectationImpl(context, method, path, toString(content), chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  @Deprecated
  public TimesOnceableOrHttpHeaderable<Void> andReturnChucked(int statusCode, Object... contents) {
    return andReturnChunked(statusCode, contents);
  }

  @Override
  public TimesOnceableOrHttpHeaderable<Void> andReturnChunked(int statusCode, Object... contents) {
    return new MockServerExpectationImpl(context, method, path, bodyProvider, ResponseProviders.of(statusCode, toString(contents)), delay, delayUnit, times, responses);
  }

  @Override
  public TimesOnceableOrHttpHeaderable<Void> andReplyChunked(int statusCode, BodyProvider<List<Object>> contents) {
    return andReplyChunked(ResponseProviders.of(statusCode, contents));
  }

  @Override
  public TimesOnceableOrHttpHeaderable<Void> andReplyChunked(ResponseProvider<List<Object>> contents) {
    return new MockServerExpectationImpl(context, method, path, bodyProvider, listToString(contents), delay, delayUnit, times, responses);
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
  public Pathable<ReturnOrWebsocketable<TimesOnceableOrHttpHeaderable<Void>>> delay(long delay, TimeUnit delayUnit) {
    return new MockServerExpectationImpl(context, method, path, bodyProvider, chunksProvider, delay, delayUnit, times, responses);
  }

  @Override
  public Pathable<ReturnOrWebsocketable<TimesOnceableOrHttpHeaderable<Void>>> delay(long delayInMilliseconds) {
    return new MockServerExpectationImpl(context, method, path, bodyProvider, chunksProvider, delayInMilliseconds, TimeUnit.MILLISECONDS, times, responses);
  }

  @Override
  public WebSocketSessionBuilder<TimesOnceableOrHttpHeaderable<Void>> andUpgradeToWebSocket() {
    return new InlineWebSocketSessionBuilder<>(context, new Function<WebSocketSession, TimesOnceableOrHttpHeaderable<Void>>() {
      @Override
      public TimesOnceableOrHttpHeaderable<Void> apply(final WebSocketSession webSocketSession) {
        return new TimesOnceableOrHttpHeaderable<Void>() {

          @Override
          public Void always() {
            enqueue(new SimpleRequest(method, path), new SimpleResponse(true, null, webSocketSession));
            return null;//Void
          }

          @Override
          public Void once() {
            enqueue(new SimpleRequest(method, path), new SimpleResponse(false, null, webSocketSession));
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
          public TimesOnceableOrHttpHeaderable<Void> withHeader(String header) {
            return null;//Void
          }

          @Override
          public TimesOnceableOrHttpHeaderable<Void> withHeader(String name, String value) {
            return null;//Void
          }
        };
      }
    });
  }

  @Override
  public TimesOnceableOrHttpHeaderable<Void> withHeader(String header) {
    bodyProvider.setHeaders(bodyProvider.getHeaders().newBuilder().add(header).build());
    return new MockServerExpectationImpl(context, method, path, bodyProvider, chunksProvider, delay, TimeUnit.MILLISECONDS, times, responses);
  }

  @Override
  public TimesOnceableOrHttpHeaderable<Void> withHeader(String name, String value) {
    bodyProvider.setHeaders(bodyProvider.getHeaders().newBuilder().add(name, value).build());
    return new MockServerExpectationImpl(context, method, path, bodyProvider, chunksProvider, delay, TimeUnit.MILLISECONDS, times, responses);
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
    if (chunksProvider != null) {
      return new ChunkedResponse(repeatable, delay, delayUnit, chunksProvider);
    } else {
      return new SimpleResponse(repeatable, bodyProvider, null, delay, delayUnit);
    }
  }

  private ResponseProvider<String> toString(final ResponseProvider<Object> provider) {
    return new ResponseProvider<String>() {
      @Override
      public String getBody(RecordedRequest request) {
        Object object = provider.getBody(request);
        return MockServerExpectationImpl.this.toString(object);
      }

      @Override
      public int getStatusCode() {
        return provider.getStatusCode();
      }

      @Override
      public Headers getHeaders() {
        return provider.getHeaders();
      }

      @Override
      public void setHeaders(Headers headers) {
        provider.setHeaders(headers);
      }
    };
  }

  private ResponseProvider<List<String>> listToString(final ResponseProvider<List<Object>> provider) {
    return new ResponseProvider<List<String>>() {
      @Override
      public List<String> getBody(RecordedRequest request) {
        List<Object> objects = provider.getBody(request);
        List<String> strings = new ArrayList<>(objects.size());
        for (Object o : objects) {
          strings.add(MockServerExpectationImpl.this.toString(o));
        }
        return strings;
      }

      @Override
      public int getStatusCode() {
        return provider.getStatusCode();
      }

      @Override
      public Headers getHeaders() {
        return provider.getHeaders();
      }

      @Override
      public void setHeaders(Headers headers) {
        provider.setHeaders(headers);
      }
    };
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

  private List<String> toString(Object object[]) {
    List<String> strings = new ArrayList<>(object.length);
    for (int i=0;i<object.length;i++) {
      strings.add(toString(object[i]));
    }
    return strings;
  }
}
