package no.ntnu.iir.bluej.sonarlint;

import no.ntnu.iir.bluej.sonarlint.util.StringUtils;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;

/**
 * Represents an adapter for SonarLint rule details.
 */
public class SonarLintRuleDetails {
  private StandaloneRuleDetails details; 
  private boolean enabled;

  public SonarLintRuleDetails(
      StandaloneRuleDetails details,
      boolean enabled
  ) {
    this.details = details;
    this.enabled = enabled;
  }

  public String getKey() {
    return this.details.getKey();
  }

  public String getName() {
    return this.details.getName();
  }

  public String getType() {
    return this.details.getType();
  }

  public String getSeverity() {
    return this.details.getSeverity();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getHtmlDescription() {
    return StringUtils.formatHtmlDescription(this.details);
  }

  /**
   * Builds a String that is designed to be used for Regex matching across all fields of the type.
   * 
   * @return a String containing all fields joined together represented as a String
   */
  public String getSearchableString() {
    String[] formatElements = {
      StringUtils.constantToReadable(this.getType()),
      StringUtils.constantToReadable(this.getSeverity()),
      this.getKey(),
      this.isEnabled() ? "Yes" : "No",
      this.getName()
    };

    return String.join(" ", formatElements);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
