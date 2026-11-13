package com.rss.support;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/** Scripted responses: unexpected requests fail instead of reaching the network. */
public class SimulatedHttp implements Interceptor {
    private final Queue<Reply> replies = new ConcurrentLinkedQueue<>();
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    public OkHttpClient client() {
        return new OkHttpClient.Builder().addInterceptor(this).build();
    }

    public void enqueue(int status, String body, String... headers) {
        replies.add(new Reply(status, body, headers));
    }

    public List<Request> requests() {
        return requests;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        requests.add(chain.request());
        Reply reply = replies.poll();
        if (reply == null) throw new IOException("Unexpected simulated request: " + chain.request().url());
        Response.Builder response = new Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(reply.status)
                .message("Simulated response")
                .body(ResponseBody.create(reply.body, MediaType.get("application/json")));
        for (int index = 0; index < reply.headers.length; index += 2) {
            response.header(reply.headers[index], reply.headers[index + 1]);
        }
        return response.build();
    }

    private static class Reply {
        private final int status;
        private final String body;
        private final String[] headers;

        private Reply(int status, String body, String[] headers) {
            this.status = status;
            this.body = body;
            this.headers = headers;
        }
    }
}
