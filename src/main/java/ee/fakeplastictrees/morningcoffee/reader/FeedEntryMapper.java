package ee.fakeplastictrees.morningcoffee.reader;

import com.rometools.rome.feed.synd.SyndEntry;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;

class FeedEntryMapper {
  public static FeedEntry syndEntryToFeedEntry(SyndEntry input) throws IllegalArgumentException {
    var title = input.getTitle();
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title can't be empty");
    }

    var link = input.getLink();
    if (link == null || link.isBlank()) {
      throw new IllegalArgumentException("link can't be empty");
    }

    if (link.startsWith("http://") || link.startsWith("https://")) {
      return new FeedEntry(title, link);
    }

    throw new IllegalArgumentException("link must start with http or https");
  }
}
