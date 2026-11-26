package ee.fakeplastictrees.morningcoffee.feed;

import ee.fakeplastictrees.morningcoffee.common.ConnectionPool;
import ee.fakeplastictrees.morningcoffee.common.RepositoryException;
import java.util.List;
import java.util.UUID;

/// Provides access to stored feeds and their entries.
public final class FeedService {
  private final PostgresFeedRepository repository;

  public FeedService(ConnectionPool pool) {
    this.repository = new PostgresFeedRepository(pool);
  }

  /// Returns up to `n` entries from enabled feeds, newest entry ID first.
  public List<FeedEntryDto> getEntries(int n) throws RepositoryException {
    return repository.getEntries(n);
  }

  /// Returns entries and optional content from enabled feeds newer than `id`, newest entry ID
  /// first.
  public List<FeedEntryWithContentDto> getEntriesWithContentAfterId(UUID id)
      throws RepositoryException {
    return repository.getEntriesWithContentAfterId(id);
  }

  List<Feed> getFeeds() throws RepositoryException {
    return repository.getFeeds();
  }

  void saveFeedEntries(List<FeedEntry> entries) throws RepositoryException {
    repository.saveFeedEntries(entries);
  }
}
