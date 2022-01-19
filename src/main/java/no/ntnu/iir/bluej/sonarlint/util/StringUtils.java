package no.ntnu.iir.bluej.sonarlint.util;

import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;

/**
 * Simple utility class for working with and parsing Strings to a given format.
 */
public class StringUtils {
  protected StringUtils() {}

  /**
   * Converts a String formatted like a constant to a eye-friendly String.
   * <p>
   * Example: THIS_IS_A_CONSTANT => This is a constant
   * </p>
   * 
   * @param inputString the String to convert
   * @return the converted String
   */
  public static String constantToReadable(String inputString) {
    StringBuilder formatted = new StringBuilder(inputString.length());
    boolean nextUppercase = true;
    
    for (char character : inputString.toCharArray()) {
      if (character == '_') {
        character = ' ';
      } else if (nextUppercase) {
        character = Character.toUpperCase(character);
        nextUppercase = false;
      } else {
        character = Character.toLowerCase(character);
      }

      formatted.append(character);
    }
    
    return formatted.toString();
  }


  /**
   * Utility method to format a SonarLint rules HtmlDescription.
   * 
   * @param ruleDetails the StandaloneRuleDetails to format the HtmlDescription for
   * @return the formatted HtmlDescription as a String
   */
  public static String formatHtmlDescription(StandaloneRuleDetails ruleDetails) {
    String formatted = "";
    SonarLintIconMapper mapper = new SonarLintIconMapper();

    formatted += "<h1>" + ruleDetails.getName() + "</h1>\n";
    formatted += "<div style=\"display: inline-flex; items-center\">";
    formatted += String.format(
      "<img style=\"padding: 0px 4px\" src=\"%s\" />",
      mapper.getIcon(ruleDetails.getType())
    );
    formatted += StringUtils.constantToReadable(ruleDetails.getType());
    formatted += String.format(
      "<img style=\"padding: 0px 4px\" src=\"%s\" />",
      mapper.getIcon(ruleDetails.getSeverity())
    );
    formatted += StringUtils.constantToReadable(ruleDetails.getSeverity());
    formatted += String.format(
      "<span style=\"padding: 0px 8px; color: #555\">(Key: %s)</span>",
      ruleDetails.getKey().split(":")[1]
    );
    formatted += "</div>";
    formatted += ruleDetails.getHtmlDescription();

    return formatted;
  }
}
