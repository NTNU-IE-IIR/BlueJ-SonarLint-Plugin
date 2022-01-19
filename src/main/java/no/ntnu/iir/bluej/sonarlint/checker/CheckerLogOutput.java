package no.ntnu.iir.bluej.sonarlint.checker;

import java.util.logging.Logger;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;

public class CheckerLogOutput implements LogOutput { 
  private static final Logger LOGGER = Logger.getLogger(CheckerLogOutput.class.getName());

  @Override
  public void log(String formattedMessage, Level logLevel) {
    if (logLevel.equals(Level.WARN)) {
      LOGGER.warning(formattedMessage);
    } else if (logLevel.equals(Level.ERROR)) {
      LOGGER.severe(formattedMessage);
    } else if (logLevel.equals(Level.TRACE)) {
      LOGGER.warning(formattedMessage);
    }
  }
}
