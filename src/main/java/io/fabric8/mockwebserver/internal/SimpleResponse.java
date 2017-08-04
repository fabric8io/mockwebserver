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

import java.util.concurrent.TimeUnit;

import io.fabric8.mockwebserver.ServerResponse;
import io.fabric8.mockwebserver.utils.ResponseProvider;
import io.fabric8.mockwebserver.utils.ResponseProviders;

import okhttp3.Headers;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

public class SimpleResponse implements ServerResponse {

  private final ResponseProvider<String> bodyProvider;

  private final WebSocketSession webSocketSession;
  private final boolean repeatable;
  private final long responseDelay;
  private final TimeUnit responseDelayUnit;

  public SimpleResponse(boolean repeatable, int statusCode, String body, WebSocketSession webSocketSession) {
    this(repeatable, ResponseProviders.of(statusCode, body), webSocketSession);
  }

  public SimpleResponse(boolean repeatable, ResponseProvider<String> bodyProvider, WebSocketSession webSocketSession) {
    this(repeatable, bodyProvider, webSocketSession, 0, TimeUnit.MILLISECONDS);
  }

  public SimpleResponse(boolean repeatable, int statusCode, String body, WebSocketSession webSocketSession, long responseDelay, TimeUnit responseDelayUnit) {
    this(repeatable, ResponseProviders.of(statusCode, body), webSocketSession, responseDelay, responseDelayUnit);
  }

  public SimpleResponse(boolean repeatable, ResponseProvider<String> bodyProvider, WebSocketSession webSocketSession, long responseDelay, TimeUnit responseDelayUnit) {
    this.bodyProvider = bodyProvider;
    this.webSocketSession = webSocketSession;
    this.repeatable = repeatable;
    this.responseDelay = responseDelay;
    this.responseDelayUnit = responseDelayUnit;
  }

  public ResponseProvider<String> getBodyProvider() {
    return bodyProvider;
  }

  @Deprecated
  public MockResponse toMockResponse() {
    return toMockResponse(null);
  }

  public MockResponse toMockResponse(RecordedRequest request) {
    MockResponse mockResponse = new MockResponse();
    if (webSocketSession != null) {
      mockResponse.withWebSocketUpgrade(webSocketSession);
    } else {
      mockResponse.setHeaders(bodyProvider.getHeaders());
      mockResponse.setBody(bodyProvider.getBody(request));
      mockResponse.setResponseCode(bodyProvider.getStatusCode());
    }

    if (responseDelay > 0) {
      mockResponse.setBodyDelay(responseDelay, responseDelayUnit);
    }

    return mockResponse;
  }

  public WebSocketSession getWebSocketSession() {
    return webSocketSession;
  }

  public boolean isRepeatable() {
    return repeatable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SimpleResponse that = (SimpleResponse) o;

    if (repeatable != that.repeatable) return false;
    if (bodyProvider != null ? !bodyProvider.equals(that.bodyProvider) : that.bodyProvider != null) return false;
    return webSocketSession != null ? webSocketSession.equals(that.webSocketSession) : that.webSocketSession == null;

  }

  @Override
  public int hashCode() {
    int result = bodyProvider != null ? bodyProvider.hashCode() : 0;
    result = 31 * result + (webSocketSession != null ? webSocketSession.hashCode() : 0);
    result = 31 * result + (repeatable ? 1 : 0);
    return result;
  }

}
