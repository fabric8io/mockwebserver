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
import io.fabric8.mockwebserver.dsl.Emitable;
import io.fabric8.mockwebserver.dsl.EventDoneable;
import io.fabric8.mockwebserver.dsl.Function;
import io.fabric8.mockwebserver.dsl.TimesOrOnceable;
import io.fabric8.mockwebserver.dsl.WebSocketSessionBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class InlineWebSocketSessionBuilder<T> implements WebSocketSessionBuilder<T>, EventDoneable<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    InlineWebSocketSessionBuilder(Function<WebSocketSession, T> function) {
        this.function = function;
    }

    private final Function<WebSocketSession, T> function;
    private WebSocketSession session;

    @Override
    public EventDoneable<T> open(Object... response) {
        this.session = new WebSocketSession(toWebSocketMessages(response), null, null);
        return this;
    }


    @Override
    public T failure(Object response, Exception e) {
        return function.apply(new WebSocketSession(Collections.<WebSocketMessage>emptyList(), toWebSocketMessage(response), e));
    }

    @Override
    public T done() {
        return function.apply(session);
    }

    @Override
    public Emitable<TimesOrOnceable<EventDoneable<T>>> expect(final Object in) {
        return new Emitable<TimesOrOnceable<EventDoneable<T>>>() {
            @Override
            public TimesOrOnceable<EventDoneable<T>> andEmit(final Object event) {
                return new TimesOrOnceable<EventDoneable<T>>() {
                    @Override
                    public EventDoneable<T> always() {
                        enqueue(in, toWebSocketMessage(event, false));
                        return InlineWebSocketSessionBuilder.this;
                    }

                    @Override
                    public EventDoneable<T> once() {
                        enqueue(in, toWebSocketMessage(event, true));
                        return InlineWebSocketSessionBuilder.this;
                    }

                    @Override
                    public EventDoneable<T> times(int times) {
                        for (int i = 0; i < times; i++) {
                            enqueue(in, toWebSocketMessage(event, true));
                        }
                        return InlineWebSocketSessionBuilder.this;
                    }
                };
            }
        };
    }

    @Override
    public Emitable<EventDoneable<T>> waitFor(final long millis) {
        // XXX: Why are millis not used here ? Should it be used in the 'addTimeEvent' below when creating
        // the WebSocketMessage (as first argument) ?
        return new Emitable<EventDoneable<T>>() {
            @Override
            public EventDoneable<T> andEmit(Object event) {
                session.addTimedEvent(toWebSocketMessage(event));
                return InlineWebSocketSessionBuilder.this;
            }
        };
    }

    private List<WebSocketMessage> toWebSocketMessages(Object... messages) {
        List<WebSocketMessage> response = new ArrayList<>();
        for (Object msg : messages) {
            response.add(toWebSocketMessage(msg));
        }
        return response;
    }

    private WebSocketMessage toWebSocketMessage(Object content) {
        return toWebSocketMessage(0L, content, true);
    }

    private WebSocketMessage toWebSocketMessage(Long delay, Object content) {
        return toWebSocketMessage(delay, content, true);
    }

    private WebSocketMessage toWebSocketMessage(Object content, Boolean toBeRemoved) {
        return toWebSocketMessage(0L, content, toBeRemoved);
    }

    private WebSocketMessage toWebSocketMessage(Long delay, Object content, Boolean toBeRemoved) {
        if (content instanceof String) {
            return new WebSocketMessage(delay, (String) content, toBeRemoved);
        } else if (content instanceof WebSocketMessage) {
            return (WebSocketMessage) content;
        } else {
            try {
                return toWebSocketMessage(delay, MAPPER.writeValueAsString(content), toBeRemoved);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void enqueue(Object req, WebSocketMessage resp) {
        session.registerRequestEvent(req, resp);
    }

}
