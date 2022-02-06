package no.ntnu.iir.bluej.extensions.linting.sonarlint.util;

import java.net.URL;
import java.util.HashMap;
import no.ntnu.iir.bluej.extensions.linting.core.util.IconMapper;

/**
 * Represents a IconMapper implementation for SonarLint.
 * Responsible for mapping String values to an icon URL.
 */
public class SonarLintIconMapper implements IconMapper {
  private HashMap<String, URL> iconMap;

  /**
   * Instantiates the IconMapper, loading key-vallue pairs to the HashMap.
   */
  public SonarLintIconMapper() {
    this.iconMap = new HashMap<>();
    this.iconMap.put(
        "BLOCKER",
        this.getClass().getClassLoader().getResource("images/severity/blocker.png")
    );
    this.iconMap.put(
        "CRITICAL",
        this.getClass().getClassLoader().getResource("images/severity/critical.png")
    );
    this.iconMap.put(
        "INFO",
        this.getClass().getClassLoader().getResource("images/severity/info.png")
    );
    this.iconMap.put(
        "MAJOR",
        this.getClass().getClassLoader().getResource("images/severity/major.png")
    );
    this.iconMap.put(
        "MINOR",
        this.getClass().getClassLoader().getResource("images/severity/minor.png")
    );
    this.iconMap.put(
        "BUG",
        this.getClass().getClassLoader().getResource("images/type/bug.png")
    );
    this.iconMap.put(
        "CODE_SMELL",
        this.getClass().getClassLoader().getResource("images/type/code_smell.png")
    );
    this.iconMap.put(
        "VULNERABILITY",
        this.getClass().getClassLoader().getResource("images/type/vulnerability.png")
    );
  }

  /**
   * Returns the URL of the icon mapped to the input name.
   * 
   * @return the URL of the icon mapped to the input name
   */
  @Override
  public URL getIcon(String name) {
    return this.iconMap.get(name);
  }
}
