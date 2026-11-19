package com.memory.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * APP 内的 AI 请求转发：WebView 发起跨域请求会失败（同浏览器），
 * 这里用原生 HttpURLConnection 发出去，把状态码和响应体原样返回给 JS。
 * chat 为一次性返回（兼容旧版），chatStream 逐块推送，首字到达即可显示。
 */
@CapacitorPlugin(name = "AiProxy")
public class AiProxyPlugin extends Plugin {

    @PluginMethod
    public void chat(PluginCall call) {
        final String endpoint = call.getString("endpoint", "");
        final String apiKey = call.getString("apiKey", "");
        final String body = call.getString("body", "{}");

        if (endpoint == null || endpoint.isEmpty()) {
            call.reject("缺少接口地址");
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(120000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");
                if (apiKey != null && !apiKey.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                }

                byte[] data = body.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(data.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data);
                }

                int status = conn.getResponseCode();
                InputStream is = (status >= 200 && status < 400)
                        ? conn.getInputStream()
                        : conn.getErrorStream();
                String text = readAll(is);

                JSObject ret = new JSObject();
                ret.put("status", status);
                ret.put("body", text);
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("请求失败：" + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    /**
     * 流式转发：每读到一段响应就用 aiChunk 事件推给 JS，全部读完再 resolve。
     * JS 侧需要先 addListener('aiChunk') 再调用本方法。
     */
    @PluginMethod
    public void chatStream(PluginCall call) {
        final String endpoint = call.getString("endpoint", "");
        final String apiKey = call.getString("apiKey", "");
        final String body = call.getString("body", "{}");

        if (endpoint == null || endpoint.isEmpty()) {
            call.reject("缺少接口地址");
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            BufferedReader br = null;
            try {
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(120000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "text/event-stream");
                if (apiKey != null && !apiKey.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                }

                byte[] data = body.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(data.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data);
                }

                int status = conn.getResponseCode();
                if (status < 200 || status >= 400) {
                    String err = readAll(conn.getErrorStream());
                    JSObject ret = new JSObject();
                    ret.put("status", status);
                    ret.put("body", err);
                    call.resolve(ret);
                    return;
                }

                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                char[] buf = new char[2048];
                int n;
                while ((n = br.read(buf)) > 0) {
                    JSObject chunk = new JSObject();
                    chunk.put("data", new String(buf, 0, n));
                    notifyListeners("aiChunk", chunk);
                }

                JSObject ret = new JSObject();
                ret.put("status", status);
                ret.put("body", "");
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("请求失败：" + e.getMessage());
            } finally {
                try {
                    if (br != null) br.close();
                } catch (Exception ignored) {
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private String readAll(InputStream is) throws Exception {
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
        }
        return sb.toString();
    }
}
