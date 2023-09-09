package org.apache.coyote.http11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import nextstep.jwp.controller.FrontController;
import nextstep.jwp.exception.UncheckedServletException;
import org.apache.coyote.Processor;
import org.apache.catalina.controller.Controller;
import org.apache.coyote.http11.request.HttpRequest;
import org.apache.coyote.http11.request.RequestBody;
import org.apache.coyote.http11.response.HttpResponse;
import org.apache.coyote.http11.response.ResponseViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http11Processor implements Runnable, Processor {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);

    private final Socket connection;

    public Http11Processor(final Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.info("connect host: {}, port: {}", connection.getInetAddress(), connection.getPort());
        process(connection);
    }

    @Override
    public void process(final Socket connection) {
        try (final var inputStream = connection.getInputStream();
             final var outputStream = connection.getOutputStream();
             final var bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            List<String> requestHeader = readRequestHeader(bufferedReader);
            RequestBody requestBody = readRequestBody(requestHeader, bufferedReader);

            HttpRequest httpRequest = HttpRequest.of(requestHeader, requestBody);

            Controller frontController = new FrontController();
            HttpResponse httpResponse = new HttpResponse();
            frontController.service(httpRequest, httpResponse);
            ResponseViewer from = ResponseViewer.from(httpResponse);

            outputStream.write(from.getView().getBytes());
            outputStream.flush();
        } catch (IOException | UncheckedServletException e) {
            log.error(e.getMessage(), e);
        }
    }

    private List<String> readRequestHeader(BufferedReader bufferedReader) throws IOException {
        List<String> request = new ArrayList<>();
        String line;
        while (!(line = bufferedReader.readLine()).isBlank()) {
            request.add(line);
        }
        return request;
    }

    private RequestBody readRequestBody(List<String> headers, BufferedReader bufferedReader) throws IOException {
        int contentLength = 0;
        for (String header: headers) {
            if (header.startsWith("Content-Length")) {
                contentLength = Integer.parseInt(header.split(" ")[1]);
            }
        }
        char[] buffer = new char[contentLength];
        bufferedReader.read(buffer, 0, contentLength);
        return RequestBody.from(new String(buffer));
    }

}
