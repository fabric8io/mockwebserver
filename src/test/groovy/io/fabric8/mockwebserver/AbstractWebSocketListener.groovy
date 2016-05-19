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

import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.ws.WebSocket;
import com.squareup.okhttp.ws.WebSocketListener;
import okio.Buffer;

import java.io.IOException;

/**
 * Just for the shake of anonymous inner classes
 */
class AbstractWebSocketListener implements WebSocketListener {

    @Override
    public void onOpen(WebSocket webSocket, Response response) {

    }

    @Override
    public void onFailure(IOException e, Response response) {

    }

    @Override
    public void onMessage(ResponseBody message) throws IOException {

    }

    @Override
    public void onPong(Buffer payload) {

    }

    @Override
    public void onClose(int code, String reason) {

    }
}
