package no.ntnu.iir.bluej.extensions.linting.sonarlint.checker;

import java.util.logging.Logger;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;

public class CheckerLogOutput implements LogOutput { 
  private static final Logger LOGGER = Logger.getLogger(CheckerLogOutput.class.getName());

  @Override
  public void log(String formattedMessage, Level logLevel) {
    switch (logLevel) {
      case WARN, TRACE -> LOGGER.warning(formattedMessage);
      case ERROR -> LOGGER.severe(formattedMessage);
      default -> {
        // do not log anything for other levels
      }
    }
  }
}
