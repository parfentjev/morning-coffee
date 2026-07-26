package ee.fakeplastictrees.morningcoffee.webserver;

import static java.util.stream.Collectors.joining;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.render.FeedEntryTemplate;
import ee.fakeplastictrees.morningcoffee.webserver.render.IndexTemplate;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateException;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateValue;
import java.nio.charset.StandardCharsets;
import org.owasp.encoder.Encode;

class Controller {
  private final Repository repository;

  Controller(Repository repository) {
    this.repository = repository;
  }

  IndexHandler indexHandler() {
    return new IndexHandler();
  }

  private void response(HttpExchange exchange, String body) {
    try {
      var response = body.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, response.length);
      try (var outputStream = exchange.getResponseBody()) {
        outputStream.write(response);
      }
    } catch (Exception e) {
      // todo: add some error handling for handlers
      e.printStackTrace();
    }
  }

  private class IndexHandler implements HttpHandler {
    private final IndexTemplate indexTemplate;
    private final FeedEntryTemplate feedEntryTemplate;

    private IndexHandler() {
      try {
        this.indexTemplate = new IndexTemplate();
        this.feedEntryTemplate = new FeedEntryTemplate();
      } catch (TemplateException e) {
        // todo: add proper exception
        throw new RuntimeException(e);
      }
    }

    @Override
    public void handle(HttpExchange exchange) {
      var entriesHtml =
          repository.getEntries(10).stream()
              .map(
                  entry -> {
                    var feedName =
                        new TemplateValue("feed.name", "Some Feed", Encode::forHtmlContent);

                    // todo: here, it's not obvious at all that ScheduledFeedReader
                    // validated the link, need to do it here
                    var entryLink =
                        new TemplateValue("entry.link", entry.link(), Encode::forHtmlAttribute);

                    var entryTitle =
                        new TemplateValue("entry.title", entry.title(), Encode::forHtmlContent);

                    return feedEntryTemplate.renderToHtml(feedName, entryLink, entryTitle);
                  })
              .collect(joining());

      var key = "entries";
      // todo: `s -> s` is just a workaround for an enforced encoder;
      // it's silly and als here I don't really know which encoder to use
      // without looking at the template;
      //
      // the renderer should probably take responsibility for encoding
      // also need to distinguish between safe html (like `entiresHtml`) that is already encoded
      // and new raw inputs - add some wrapper class? Rendered/Safe/EncodedHtml?
      var entries = new TemplateValue(key, entriesHtml, s -> s);

      response(exchange, indexTemplate.renderToHtml(entries));
    }
  }
}
