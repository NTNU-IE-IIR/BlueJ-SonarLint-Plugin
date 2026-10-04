package no.ntnu.iir.bluej.extensions.linting.sonarlint.util;

import java.util.List;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleContextualSectionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleContextualSectionWithDefaultContextKeyDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleDescriptionTabDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleMonolithicDescriptionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleSplitDescriptionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;

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
   * @param name the name of the rule
   * @param key the key of the rule, e.g. "java:S100"
   * @param type the legacy type of the rule, e.g. "BUG"
   * @param severity the legacy severity of the rule, e.g. "MAJOR"
   * @param htmlDescription the description of the rule, as HTML
   * @return the formatted HtmlDescription as a String
   */
  public static String formatHtmlDescription(
      String name,
      String key,
      String type,
      String severity,
      String htmlDescription
  ) {
    String formatted = "";
    SonarLintIconMapper mapper = new SonarLintIconMapper();

    formatted += "<h1>" + name + "</h1>\n";
    formatted += "<div style=\"display: inline-flex; items-center\">";
    formatted += String.format(
      "<img style=\"padding: 0px 4px\" src=\"%s\" />",
      mapper.getIcon(type)
    );
    formatted += StringUtils.constantToReadable(type);
    formatted += String.format(
      "<img style=\"padding: 0px 4px\" src=\"%s\" />",
      mapper.getIcon(severity)
    );
    formatted += StringUtils.constantToReadable(severity);
    formatted += String.format(
      "<span style=\"padding: 0px 8px; color: #555\">(Key: %s)</span>",
      key.split(":")[1]
    );
    formatted += "</div>";
    formatted += htmlDescription;

    return formatted;
  }

  /**
   * Utility method to join a SonarLint rule description into a single HTML document.
   * Descriptions split into tabs (e.g. "Why is this an issue?", "How can I fix it?") are
   * rendered as consecutive sections. For tabs with context specific content (e.g. per framework),
   * the default context is shown.
   * 
   * @param description the rule description
   * @return the rule description as HTML
   */
  public static String descriptionToHtml(
      Either<RuleMonolithicDescriptionDto, RuleSplitDescriptionDto> description
  ) {
    if (description.isLeft()) {
      return description.getLeft().getHtmlContent();
    }

    RuleSplitDescriptionDto splitDescription = description.getRight();
    StringBuilder html = new StringBuilder();
    if (splitDescription.getIntroductionHtmlContent() != null) {
      html.append(splitDescription.getIntroductionHtmlContent());
    }
    for (RuleDescriptionTabDto tab : splitDescription.getTabs()) {
      html.append("<h2>").append(tab.getTitle()).append("</h2>\n");
      if (tab.getContent().isLeft()) {
        html.append(tab.getContent().getLeft().getHtmlContent());
      } else {
        html.append(defaultContextHtml(tab.getContent().getRight()));
      }
    }
    return html.toString();
  }

  private static String defaultContextHtml(RuleContextualSectionWithDefaultContextKeyDto content) {
    List<RuleContextualSectionDto> sections = content.getContextualSections();
    return sections.stream()
      .filter(section -> section.getContextKey().equals(content.getDefaultContextKey()))
      .findFirst()
      .or(() -> sections.stream().findFirst())
      .map(RuleContextualSectionDto::getHtmlContent)
      .orElse("");
  }
}
