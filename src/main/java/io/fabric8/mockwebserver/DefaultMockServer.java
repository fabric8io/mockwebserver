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

package io.fabric8.mockwebserver;

import okhttp3.mockwebserver.MockWebServer;
import io.fabric8.mockwebserver.dsl.MockServerExpectation;
import io.fabric8.mockwebserver.internal.MockDispatcher;
import io.fabric8.mockwebserver.internal.MockSSLContextFactory;
import io.fabric8.mockwebserver.internal.MockServerExpectationImpl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultMockServer {

  private final Context context;
  private final boolean useHttps;
  private final MockWebServer server;
  private final Map<ServerRequest, Queue<ServerResponse>> responses;

  private final AtomicBoolean initialized = new AtomicBoolean();
  private final AtomicBoolean shutdown = new AtomicBoolean();

  public DefaultMockServer() {
    this(new Context(), new MockWebServer(), new HashMap<ServerRequest, Queue<ServerResponse>>(), false);
  }

  public DefaultMockServer(boolean useHttps) {
    this(new Context(), new MockWebServer(), new HashMap<ServerRequest, Queue<ServerResponse>>(), useHttps);
  }

  public DefaultMockServer(MockWebServer server, Map<ServerRequest, Queue<ServerResponse>> responses, boolean useHttps) {
    this(new Context(), server, responses, useHttps);
  }

  public DefaultMockServer(Context context, MockWebServer server, Map<ServerRequest, Queue<ServerResponse>> responses, boolean useHttps) {
    this.context = context;
    this.useHttps = useHttps;
    this.server = server;
    this.responses = responses;
    this.server.setDispatcher(new MockDispatcher(responses));
  }

  /**
   * This method is called right before start. Override it to add extra initialization.
   */
  public void onStart() {
  }

  /**
   * This method is called right after shutdown. Override it to add extra cleanup.
   */
  public void onShutdown() {

  }


  private void startInternal() {
    if (initialized.compareAndSet(false, true)) {
      if (useHttps) {
        server.useHttps(MockSSLContextFactory.create().getSocketFactory(), false);
      }
      onStart();
    }
  }

  private void shutdownInternal() {
    if (shutdown.compareAndSet(false, true)) {
      onShutdown();
    }
  }

  public void start()  {
    try {
      startInternal();
      server.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void start(int port)  {
    try {
      startInternal();
      server.start(port);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void start(InetAddress inetAddress, int port) {
    try {
      startInternal();
      server.start(inetAddress, port);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void shutdown() {
    try {
      server.shutdown();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      shutdownInternal();
    }
  }

  public String url(String path) {
    return server.url(path).toString();
  }

  public int getPort() {
    return server.getPort();
  }

  public String getHostName() {
    return server.getHostName();
  }

  public Proxy toProxyAddress() {
    return server.toProxyAddress();
  }

  public int getRequestCount() {
    return server.getRequestCount();
  }


  /**
   * This method was only intended to be used just for getting the host and port, which are now exposed directly from this class.
   * @return
     */
  @Deprecated
  public MockWebServer getServer() {
    return server;
  }

  public MockServerExpectation expect() {
    return new MockServerExpectationImpl(responses, context);
  }
}
