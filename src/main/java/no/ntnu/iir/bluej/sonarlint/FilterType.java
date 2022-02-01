package no.ntnu.iir.bluej.sonarlint;

/**
 * Represents filter types for filtering rules.
 * Utility used to provide an easy interface instead of constants.
 */
public enum FilterType {
  ALL("All rules"),
  ENABLED("Enabled rules"),
  DISABLED("Disabled rules");
  
  private final String text;

  private FilterType(final String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return this.text;
  }
}
