package ee.fakeplastictrees.morningcoffee.reader;

import static java.util.Optional.ofNullable;

import com.rometools.rome.feed.synd.SyndEntry;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;

class FeedEntryMapper {
  public static FeedEntry syndEntryToFeedEntry(SyndEntry input) throws IllegalArgumentException {
    // The UI layer (webserver package) is supposed to validate stored data before rendering it,
    // but filtering out blatantly invalid feed entries here does no harm because there is no need
    // to store them in the database.
    var entry = new FeedEntry();

    var title = input.getTitle();
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title can't be empty");
    } else {
      entry.setTitle(title);
    }

    var link = input.getLink();
    if (link == null || link.isBlank()) {
      throw new IllegalArgumentException("link can't be empty");
    } else {
      entry.setLink(link);
    }

    // Link is validated above, so it can be safely used as a fallback.
    var externalId = ofNullable(input.getUri()).orElseGet(() -> input.getLink());
    entry.setExtrnalId(externalId);

    if (input.getPublishedDate() == null) {
      throw new IllegalArgumentException("published date is null");
    } else {
      entry.setPublishedAt(input.getPublishedDate().toInstant());
    }

    return entry;
  }
}
