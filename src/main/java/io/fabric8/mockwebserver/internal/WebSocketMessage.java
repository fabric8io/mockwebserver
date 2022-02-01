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

import java.nio.charset.StandardCharsets;

public class WebSocketMessage {

    private final Long delay;
    private final byte[] body;
    private final boolean toBeRemoved;
    private final boolean binary;
    private final Integer closingReason;

    public WebSocketMessage(String body) {
        this(0L, body, true);
    }

    public WebSocketMessage(byte[] body) {
        this(0L, body, true, true, null);
    }

    public WebSocketMessage(String body, boolean toBeRemoved) {
        this(0L, body.getBytes(StandardCharsets.UTF_8), toBeRemoved, false, null);
    }

    public WebSocketMessage(byte[] body, boolean toBeRemoved) {
        this(0L, body, toBeRemoved, true, null);
    }

    public WebSocketMessage(Long delay, String body, boolean toBeRemoved) {
        this(delay, body.getBytes(StandardCharsets.UTF_8), toBeRemoved, false, null);
    }

    public WebSocketMessage(Long delay, byte[] body, boolean toBeRemoved) {
        this(delay, body, toBeRemoved, true, null);
    }
    
    public WebSocketMessage(Long delay, String body, boolean toBeRemoved, Integer closingReason) {
	      this(delay, body.getBytes(StandardCharsets.UTF_8), toBeRemoved, false, closingReason);
	  }

	  public WebSocketMessage(Long delay, byte[] body, boolean toBeRemoved, Integer closingReason) {
	      this(delay, body, toBeRemoved, true, closingReason);
	  }

    public WebSocketMessage(Long delay, String body, boolean toBeRemoved, boolean binary, Integer closingReason) {
        this(delay, body.getBytes(StandardCharsets.UTF_8), toBeRemoved, binary, closingReason);
    }

    public WebSocketMessage(Long delay, byte[] body, boolean toBeRemoved, boolean binary, Integer closingReason) {
        this.delay = delay;
        this.body = body;
        this.toBeRemoved = toBeRemoved;
        this.binary = binary;
        this.closingReason = closingReason;
    }

    public Long getDelay() {
        return delay;
    }

    public String getBody() {
        return new String(body);
    }

    public boolean isToBeRemoved() {
        return toBeRemoved;
    }

    public byte[] getBytes() {
        return body;
    }

    public boolean isBinary() {
        return binary;
    }
    
    public Integer getClosingReason() {
      return closingReason;
  }
}
