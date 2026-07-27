package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class AbstractHttpHandler implements HttpHandler {
  private final Logger logger = LogManager.getLogger();

  private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain; charset=utf-8";
  private static final String CONTENT_TYPE_TEXT_HTML = "text/html; charset=utf-8";

  @Override
  public final void handle(HttpExchange exchange) {
    String contentType;
    byte[] body;
    int statusCode;

    try {
      var response = response();
      statusCode = response.getStatusCode();
      body = response.getBytes();
      contentType = CONTENT_TYPE_TEXT_HTML;
    } catch (Exception e) {
      logger.error("http handler error", e);
      statusCode = 500;
      body = "Internal Server Error".getBytes();
      contentType = CONTENT_TYPE_TEXT_PLAIN;
    }

    write(exchange, statusCode, body, contentType);
  }

  private void write(HttpExchange exchange, int statusCode, byte[] body, String contentType) {
    try {
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
    } finally {
      exchange.close();
    }
  }

  protected abstract Response response() throws Exception;

  protected static class Response {
    private String body;
    private int statusCode;

    public String getBody() {
      return body;
    }

    public void setBody(String body) {
      this.body = body;
    }

    public int getStatusCode() {
      return statusCode;
    }

    public void setStatusCode(int statusCode) {
      this.statusCode = statusCode;
    }

    public byte[] getBytes() {
      return body.getBytes(StandardCharsets.UTF_8);
    }

    static Response of(String body, int statusCode) {
      var response = new Response();
      response.setBody(body);
      response.setStatusCode(statusCode);

      return response;
    }
  }
}
