package ee.fakeplastictrees.morningcoffee.brief;

import java.util.List;

/// Provides brief generation and retrieval.
public interface BriefService {
  /// Requests a brief based on the supplied material.
  ///
  /// @param idempotencyKey identifier shared by retries of the same generation request
  /// @param sourceMaterial text on which to base the brief
  void generate(String idempotencyKey, String sourceMaterial) throws BriefServiceException;

  /// Retrieves the most recently published briefs.
  ///
  /// @param n maximum number of briefs to return
  /// @return up to `n` briefs, or an empty list if none are available
  List<BriefDto> latest(int n);
}
