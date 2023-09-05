package nextstep.jwp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.model.User;
import org.apache.coyote.http11.cookie.HttpCookie;
import org.apache.coyote.http11.request.HttpMethod;
import org.apache.coyote.http11.request.HttpRequest;
import org.apache.coyote.http11.request.RequestBody;
import org.apache.coyote.http11.request.RequestURI;
import org.apache.coyote.http11.response.HttpResponse;
import org.apache.coyote.http11.response.HttpStatus;
import org.apache.coyote.http11.session.Session;
import org.apache.coyote.http11.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Handler {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    private static final SessionManager sessionManager = new SessionManager();

    private Handler() {
    }

    public static HttpResponse run(HttpRequest httpRequest) throws IOException {
        RequestURI requestURI = httpRequest.getRequestUrl();
        if (requestURI.isLoginPage() && httpRequest.getHttpMethod().isEqualTo(HttpMethod.GET)) {
            return HttpResponse.builder()
                    .httpStatus(HttpStatus.OK)
                    .responseBody(parseResponseBody(requestURI.getResourcePath()))
                    .contentType(httpRequest.contentType())
                    .build();
        }
        if (requestURI.isLoginPage() && httpRequest.getHttpMethod().isEqualTo(HttpMethod.POST)) {
            return doLogin(httpRequest);
        }
        if (requestURI.isHome()) {
            return HttpResponse.builder()
                    .httpStatus(HttpStatus.OK)
                    .responseBody("Hello world!")
                    .contentType(httpRequest.contentType())
                    .build();
        }
        if (requestURI.isRegister()) {
            return doRegister(httpRequest);
        }
        return HttpResponse.builder()
                .httpStatus(HttpStatus.OK)
                .responseBody(parseResponseBody(requestURI.getResourcePath()))
                .contentType(httpRequest.contentType())
                .build();
    }

    private static HttpResponse doRegister(HttpRequest httpRequest) throws IOException {
        if (httpRequest.isRequestBodyEmpty()) {
            return HttpResponse.builder()
                    .httpStatus(HttpStatus.OK)
                    .responseBody(parseResponseBody("static/register.html"))
                    .contentType(httpRequest.contentType())
                    .redirectPage("register.html")
                    .build();
        }

        RequestBody requestBody = httpRequest.getRequestBody();
        String account = requestBody.getValueOf("account");

        Optional<User> user = InMemoryUserRepository.findByAccount(account);
        if (user.isPresent()) {
            return HttpResponse.builder()
                    .httpStatus(HttpStatus.FOUND)
                    .responseBody(parseResponseBody("static/register.html"))
                    .contentType(httpRequest.contentType())
                    .redirectPage("register.html")
                    .build();
        }
        String password = requestBody.getValueOf("password");
        String email = requestBody.getValueOf("email");
        InMemoryUserRepository.save(new User(account, password, email));
        return HttpResponse.builder()
                .httpStatus(HttpStatus.CREATED)
                .responseBody(parseResponseBody("static/login.html"))
                .contentType(httpRequest.contentType())
                .redirectPage("login.html")
                .build();
    }

    private static HttpResponse doLogin(HttpRequest httpRequest) throws IOException {
        RequestBody requestBody = httpRequest.getRequestBody();
        String account = requestBody.getValueOf("account");
        User user = InMemoryUserRepository.findByAccount(account)
                .orElseThrow(IllegalArgumentException::new);
        log.info(user.toString());
        String password = requestBody.getValueOf("password");
        if (!user.checkPassword(password)) {
            return HttpResponse.builder()
                    .httpStatus(HttpStatus.UNAUTHORIZED)
                    .responseBody(parseResponseBody("static/401.html"))
                    .contentType(httpRequest.contentType())
                    .redirectPage("401.html")
                    .build();
        }
        HttpCookie cookie = HttpCookie.from(httpRequest.getHeaderValue("Cookie"));
        Session foundSession = sessionManager.findSession(cookie.getValue("JSESSIONID"));
        if (foundSession != null) {
            return HttpResponse.builder()
                    .httpStatus(HttpStatus.FOUND)
                    .responseBody(parseResponseBody("static/index.html"))
                    .contentType(httpRequest.contentType())
                    .redirectPage("index.html")
                    .httpCookie(HttpCookie.jSessionId(foundSession.getId()))
                    .build();
        }
        String uuid = UUID.randomUUID().toString();
        Session session = new Session(uuid);
        session.setAttribute("user", user);
        sessionManager.add(session);
        return HttpResponse.builder()
                .httpStatus(HttpStatus.FOUND)
                .responseBody(parseResponseBody("static/index.html"))
                .contentType(httpRequest.contentType())
                .redirectPage("index.html")
                .httpCookie(HttpCookie.jSessionId(uuid))
                .build();
    }

    private static String parseResponseBody(String resourcePath) throws IOException {
        Path path = new File(Objects.requireNonNull(
                Handler.class.getClassLoader().getResource(resourcePath)).getFile()
        ).toPath();
        return new String(Files.readAllBytes(path));
    }

}
