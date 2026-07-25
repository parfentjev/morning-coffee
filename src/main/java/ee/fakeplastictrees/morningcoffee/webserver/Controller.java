package ee.fakeplastictrees.morningcoffee.webserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class Controller {
  private final Repository repository;

  Controller(Repository repository) {
    this.repository = repository;
  }

  IndexHandler indexHandler() {
    return new IndexHandler();
  }

  private class IndexHandler implements HttpHandler {
    private final String template;

    private IndexHandler() {
      try {
        var templateFile = "templates/index.html";
        var resource = getClass().getClassLoader().getResourceAsStream(templateFile);
        this.template = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        // todo: add error handling
        System.out.println("x");
        throw new RuntimeException(e);
      }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      var sb = new StringBuilder();
      repository
          .getEntries(100)
          .forEach(
              entry ->
                  sb.append("<li><a href=\"")
                      .append(entry.link())
                      .append("\" target=\"_blank\">")
                      .append(entry.title())
                      .append("</a></li>"));

      try {
        var entriesHtml = sb.toString();
        var body = template.replaceFirst("% entries %", entriesHtml).getBytes();

        exchange.sendResponseHeaders(200, body.length);
        try (var outputStream = exchange.getResponseBody()) {
          outputStream.write(body);
        }
      } catch (Exception e) {
        // todo: add some error handling for handlers
        e.printStackTrace();
      }
    }
  }
}
