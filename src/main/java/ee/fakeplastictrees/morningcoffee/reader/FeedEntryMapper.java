package ee.fakeplastictrees.morningcoffee.reader;

import com.rometools.rome.feed.synd.SyndEntry;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import java.time.Instant;
import java.util.UUID;

class FeedEntryMapper {
  public static FeedEntry syndEntryToFeedEntry(SyndEntry input) throws IllegalArgumentException {
    // The UI layer (webserver package) is supposed to validate stored data before rendering it,
    // but filtering out blatantly invalid feed entries here does no harm because there is no need
    // to store them in the database.

    var title = input.getTitle();
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title can't be empty");
    }

    var link = input.getLink();
    if (link == null || link.isBlank()) {
      throw new IllegalArgumentException("link can't be empty");
    }

    // todo: replace these values with real data once Repository is fully implemented
    var id = UUID.randomUUID();
    var publishedAt = Instant.now();
    var externalId = UUID.randomUUID().toString();
    var feedId = UUID.randomUUID();

    return new FeedEntry(id, publishedAt, externalId, feedId, title, link);
  }
}
