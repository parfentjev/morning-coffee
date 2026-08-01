2. Consider propagating SQLException to ScheduledFeedReader and logging there.
3. Ensure that one invalid entry doesn't stop the processing of other entries in FeedParser.
4. Add tests.
5. Add database connection pool.
8. `ee.fakeplastictrees.morningcoffee.repository.Repository.saveFeedEntry` opens a new connection for each individual
   entry, which is silly. I can save multiple entries in one transaction. However, individual feeds normally don't
   produce more than one entry per a refresh cycle, so #5 is still a desirable solution.
