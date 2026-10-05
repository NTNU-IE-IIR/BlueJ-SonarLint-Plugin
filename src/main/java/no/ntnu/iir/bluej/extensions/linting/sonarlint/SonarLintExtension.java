package no.ntnu.iir.bluej.extensions.linting.sonarlint;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import java.net.URL;

/**
 * The entry point of the extension, loaded by BlueJ.
 * BlueJ ships older versions of some libraries SonarLint depends on, and its extension class
 * loader prefers those over the ones bundled in the extension jar. To use the bundled versions,
 * the extension itself ({@link SonarLintRuntime}) is loaded through a {@link ChildFirstClassLoader},
 * and is only accessed by reflection from this class.
 */
public class SonarLintExtension extends Extension {
  private static final String RUNTIME_CLASS =
      "no.ntnu.iir.bluej.extensions.linting.sonarlint.SonarLintRuntime";

  private ChildFirstClassLoader classLoader;
  private Object runtime;

  @Override
  public void startup(BlueJ blueJ) {
    System.out.println("SonarLintExtension.startup() called...");
    System.out.println("BlueJ Extension API Version: "
        + Extension.getExtensionsAPIVersionMajor()
        + "."
        + Extension.getExtensionsAPIVersionMinor());

    Thread thread = Thread.currentThread();
    ClassLoader previousContextClassLoader = thread.getContextClassLoader();
    try {
      URL extensionJar = this.getClass().getProtectionDomain().getCodeSource().getLocation();
      this.classLoader = new ChildFirstClassLoader(
          new URL[] { extensionJar },
          this.getClass().getClassLoader()
      );
      thread.setContextClassLoader(this.classLoader);

      Class<?> runtimeClass = this.classLoader.loadClass(RUNTIME_CLASS);
      this.runtime = runtimeClass.getConstructor().newInstance();
      runtimeClass
          .getMethod("startup", BlueJ.class, String.class, String.class)
          .invoke(this.runtime, blueJ, this.getName(), this.getVersion());
    } catch (Exception e) {
      System.err.println("SonarLintExtension: unable to start the extension");
      e.printStackTrace();
    } finally {
      thread.setContextClassLoader(previousContextClassLoader);
    }
    System.out.println("SonarLintExtension.startup() finished...");
  }

  @Override
  public void terminate() {
    if (this.runtime != null) {
      try {
        this.runtime.getClass().getMethod("terminate").invoke(this.runtime);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (this.classLoader != null) {
      try {
        this.classLoader.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public boolean isCompatible() {
    // The SonarLint extension requires BlueJ Extension API 3.2 or later
    int versionMajor = Extension.getExtensionsAPIVersionMajor();
    int versionMinor = Extension.getExtensionsAPIVersionMinor();
    return (versionMajor == 3 && versionMinor >= 2);
  }

  @Override
  public String getVersion() {
    return this.getClass().getPackage().getImplementationVersion();
  }

  @Override
  public URL getURL() {
    try {
      return new URL("https://github.com/NTNU-IE-IIR/BlueJ-SonarLint-Plugin/");
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public String getName() {
    return this.getClass().getPackage().getImplementationTitle();
  }

  @Override
  public String getDescription() {
    return String.join(
      "", // delimiter
      "SonarLint for BlueJ."
    );
  }

}
