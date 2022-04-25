package http;

import java.util.*;

/**
 * @description:
 * @author: luojx
 * @create 2022-04-21  17:00
 */
public class HttpParser {

    private String method;

    private String uri;

    private String httpVersion;

    private Map<String, String> headers;

    public HttpParser(String method, String uri, String httpVersion, Map<String, String> headers) {
        this.method = method;
        this.uri = uri;
        this.httpVersion = httpVersion;
        this.headers = headers;
    }

    public static void main(String[] args) {
        StringBuilder builder = new StringBuilder();
        builder.append("GET " + "/get" + " HTTP/1.1\r\n");
        builder.append("Host: httpbin.org:80\r\n");
        builder.append("User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727; TheWorld)\r\n");
        builder.append("Accept: text/html,application/xhtml+xml,application/xml.application/json;q=0.9,*/*;q=0.8\r\n");
        builder.append("Accept-Language: en-US;q=0.7,en;q=0.3\r\n");
        builder.append("Connection: close\r\n\r\n");
        HttpParser parse = HttpParser.parse(builder.toString());
        String s = parse.generateRequestHeader();
        System.out.println(s);
    }

    public String generateRequestHeader(){
        StringBuilder builder = new StringBuilder();
        builder.append(method + " " + uri + " " + httpVersion + "\r\n");
        headers.forEach((k, v) -> {
            builder.append(k + ": " + v + "\r\n");
        });
        builder.append("\r\n");
        return builder.toString();
    }

    public static HttpParser parse(String rawRequest){
        String[] split = rawRequest.split("\\r\\n");
        List<String> lines = Arrays.asList(split);
        //parse GET /search?hl=zh-CN&source=hp&q=domety&aq=f&oq= HTTP/1.1
        String requestLine = lines.get(0);
        String method;
        String uri;
        String httpVersion;
        String[] requestLineArr = requestLine.split(" ");
        if(requestLineArr.length != 3){
            method = "GET";
            uri = "/";
            httpVersion = "HTTP/1.1";
        }else {
            //TODO: method not allowed
            method = requestLineArr[0];
            uri = requestLineArr[1];
            httpVersion = requestLineArr[2];
        }
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String item = lines.get(i);
            if(item.equals(""))
                continue;
            String[] arr = item.split(": ");
            headers.put(arr[0], arr[1]);
        }
        if(!method.equals("CONNECT"))
            fillHeaders(headers);
        return new HttpParser(method, uri, httpVersion, headers);
    }

    private static void fillHeaders(Map<String, String> headers){
            headers.putIfAbsent("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36");
            headers.putIfAbsent("Accept", "*/*");
            headers.putIfAbsent("Accept-Language", "en-US;q=0.7,en;q=0.3");
            //设置为keep-alive用不了? :(
            headers.putIfAbsent("Connection", "close");
    }

    public String[] getAddrPort(){
        String host = this.headers.get("Host");
        if(host == null || host.equals(""))
            return null;
        String[] arr = host.split(":");
        String addr = arr[0];
        String port = "80";
        if(arr.length > 1)
            port = arr[1];
        return new String[]{addr, port};
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }
}
