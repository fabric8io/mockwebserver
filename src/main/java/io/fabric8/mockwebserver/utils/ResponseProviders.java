/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.mockwebserver.utils;

import okhttp3.Headers;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Useful methods for creating basic response providers.
 */
public class ResponseProviders {


    public static <R> ResponseProvider<R> of(int statusCode, R element) {
        if (element != null) {
            return new FixedResponseProvider<>(statusCode, element);
        }
        return null;
    }

    public static <R> ResponseProvider<List<R>> ofAll(int statusCode, R... elements) {
        if (elements != null) {
            return new FixedResponseProvider<>(statusCode, Arrays.asList(elements));
        }
        return null;
    }

    public static <R> ResponseProvider<R> of(final int statusCode, final BodyProvider<R> bodyProvider) {
        if (bodyProvider != null) {
            return new ResponseProvider<R>() {
                private Headers headers = new Headers.Builder().build();

                @Override
                public int getStatusCode() {
                    return statusCode;
                }

                @Override
                public R getBody(RecordedRequest request) {
                    return bodyProvider.getBody(request);
                }

                @Override
                public Headers getHeaders() {
                    return headers;
                }

                @Override
                public void setHeaders(Headers headers) {
                    this.headers = headers;
                }
            };
        }
        return null;
    }

    private static class FixedResponseProvider<T> implements ResponseProvider<T> {

        private int statusCode;

        private T element;

        private Headers headers = new Headers.Builder().build();

        public FixedResponseProvider(int statusCode, T element) {
            this.statusCode = statusCode;
            this.element = element;
        }

        @Override
        public T getBody(RecordedRequest request) {
            return element;
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public Headers getHeaders() {
            return headers;
        }

        @Override
        public void setHeaders(Headers headers) {
            this.headers = headers;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FixedResponseProvider<?> that = (FixedResponseProvider<?>) o;

            return element != null ? element.equals(that.element) : that.element == null;

        }

        @Override
        public int hashCode() {
            return element != null ? element.hashCode() : 0;
        }
    }

}
