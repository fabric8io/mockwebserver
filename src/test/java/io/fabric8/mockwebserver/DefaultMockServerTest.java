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

package io.fabric8.mockwebserver;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.junit.Test;

import java.io.IOException;
import static org.junit.Assert.*;

public class DefaultMockServerTest {

    @Test
    public void testSimpleRequest() throws IOException {
        DefaultMockServer defaultMockServer = new DefaultMockServer();
        defaultMockServer.expect().get().withPath("/").andReturn(200, "line").once();
        defaultMockServer.start();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(defaultMockServer.url("/")).get().build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
    }

    @Test
    public void testChunked() throws IOException {
        DefaultMockServer defaultMockServer = new DefaultMockServer();
        defaultMockServer.expect().get().withPath("/").andReturnChucked(200, "line1", "line2", "line3").once();
        defaultMockServer.start();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(defaultMockServer.url("/")).get().build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
    }

}