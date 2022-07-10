package org.apache.rocketmq.wanwanrocketmq;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class HttpUtil {
    public static String template = "{\n" +
            "    \"at\": {\n" +
            "        \"atMobiles\":[\n" +
            "            \"\"\n" +
            "        ],\n" +
            "        \"isAtAll\": false\n" +
            "    },\n" +
            "    \"text\": {\n" +
            "        \"content\":\"@CONTENT\"\n" +
            "    },\n" +
            "    \"msgtype\":\"text\"\n" +
            "}";

    public static String send(String url, String body) throws IOException {
        String content = template.replaceAll("@CONTENT", body);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse(APPLICATION_JSON_VALUE), content);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        return response.body().string();
    }
}
