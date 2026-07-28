package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

abstract class AbstractHttpHandler implements HttpHandler {
  private final Logger logger = LogManager.getLogger();

  private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain; charset=utf-8";
  private static final String CONTENT_TYPE_TEXT_HTML = "text/html; charset=utf-8";

  @Override
  public final void handle(HttpExchange exchange) {
    String contentType;
    byte[] body;
    int statusCode;

    logger.debug("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());

    var matchingMethod = exchange.getRequestMethod().equals(requestMethod());
    var matchingPath = exchange.getRequestURI().toString().equals(requestPath());
    if (matchingMethod && matchingPath) {
      try {
        var response = response();
        statusCode = response.getStatusCode();
        body = response.getBytes();
        contentType = CONTENT_TYPE_TEXT_HTML;
      } catch (Exception e) {
        logger.error("http handler error", e);
        statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
        body = "Internal Server Error".getBytes();
        contentType = CONTENT_TYPE_TEXT_PLAIN;
      }
    } else if (!matchingPath) {
      statusCode = HttpURLConnection.HTTP_NOT_FOUND;
      body = "This page does not exist.".getBytes();
      contentType = CONTENT_TYPE_TEXT_PLAIN;
    } else {
      statusCode = HttpURLConnection.HTTP_BAD_REQUEST;
      body = "Bad request.".getBytes();
      contentType = CONTENT_TYPE_TEXT_PLAIN;
    }

    write(exchange, statusCode, body, contentType);
  }

  protected abstract String requestMethod();

  protected abstract String requestPath();

  protected abstract Response response() throws Exception;

  private void write(HttpExchange exchange, int statusCode, byte[] body, String contentType) {
    try (exchange) {
      var length = body.length == 0 ? -1 : body.length;
      exchange.getResponseHeaders().set("Content-Type", contentType);
      exchange.sendResponseHeaders(statusCode, length);
      if (length > 0) {
        try (var outputStream = exchange.getResponseBody()) {
          outputStream.write(body);
        }
      }
    } catch (IOException e) {
      // IOException is somewhat normal, I don't need error-level logs for it.
      logger.debug("http exchange i/o error", e);
    } catch (Exception e) {
      logger.error("http exchange error", e);
    }
  }

  protected static class Response {
    private final String body;
    private final int statusCode;

    private Response(String body, int statusCode) {
      this.body = body;
      this.statusCode = statusCode;
    }

    protected int getStatusCode() {
      return statusCode;
    }

    protected byte[] getBytes() {
      return body.getBytes(StandardCharsets.UTF_8);
    }

    protected static Response of(String body, int statusCode) {
      return new Response(body, statusCode);
    }
  }
}
