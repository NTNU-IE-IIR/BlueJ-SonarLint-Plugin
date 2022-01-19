package no.ntnu.iir.bluej.sonarlint;

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.MenuItem;
import no.ntnu.iir.bluej.extensions.linting.core.handlers.PackageEventHandler;

public class SonarLintMenuBuilder extends MenuGenerator {
  private PackageEventHandler packageEventHandler;

  public SonarLintMenuBuilder(PackageEventHandler packageEventHandler) {
    this.packageEventHandler = packageEventHandler;
  }

  @Override
  public MenuItem getToolsMenuItem(BPackage bluePackage) {
    MenuItem menuItem = new MenuItem("Show SonarLint overview");
    menuItem.setOnAction(event -> packageEventHandler.showProjectWindow(bluePackage));
    return menuItem;
  }
  
}
