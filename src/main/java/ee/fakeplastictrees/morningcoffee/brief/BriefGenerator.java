package ee.fakeplastictrees.morningcoffee.brief;

/// Generates the text of a brief.
interface BriefGenerator {
  /// Generates a brief body from the given prompt.
  ///
  /// @param prompt instructions and source material for the brief
  /// @return the generated brief body
  String generate(String prompt);
}
