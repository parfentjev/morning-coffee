package ee.fakeplastictrees.morningcoffee.brief;

import ee.fakeplastictrees.morningcoffee.common.repository.RepositoryException;
import java.util.List;

/// Stores and retrieves published briefs.
interface BriefRepository {
  /// Stores a brief with its publication time.
  ///
  /// @param brief the brief to store
  /// @throws RepositoryException if the write fails
  void save(Brief brief) throws RepositoryException;

  /// Retrieves stored briefs in descending order of publication time.
  ///
  /// @param n maximum number of briefs to return
  /// @return up to `n` briefs, or an empty list if no briefs are stored or `n` is zero
  /// @throws RepositoryException if the read fails
  List<Brief> latest(int n) throws RepositoryException;
}
