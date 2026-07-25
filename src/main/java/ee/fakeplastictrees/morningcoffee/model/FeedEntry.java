package ee.fakeplastictrees.morningcoffee.model;

// todo: aligh with the db schema
// should probably add published_at to both for proper sorting
public record FeedEntry(String title, String link) {}
