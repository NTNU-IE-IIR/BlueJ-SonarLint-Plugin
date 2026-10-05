package no.ntnu.iir.bluej.extensions.linting.sonarlint;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Represents a class loader that prefers the classes in the extension jar over the parent's.
 * BlueJ loads extensions with a parent-first class loader, and BlueJ itself ships older versions
 * of libraries the SonarLint backend depends on (e.g. slf4j 1.7, Guava, JGit). Loading the
 * extension through this class loader makes sure the bundled versions are used. Only the JDK,
 * BlueJ and JavaFX are shared with BlueJ, as the extension talks to BlueJ through them.
 */
public class ChildFirstClassLoader extends URLClassLoader {
  private static final String[] PARENT_FIRST_PREFIXES = {
    "java.", "javax.", "jdk.", "sun.", "com.sun.", "org.w3c.", "org.xml.",
    "bluej.", "javafx."
  };

  static {
    registerAsParallelCapable();
  }

  public ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (isParentFirst(name)) {
      return super.loadClass(name, resolve);
    }
    synchronized (getClassLoadingLock(name)) {
      Class<?> loadedClass = findLoadedClass(name);
      if (loadedClass == null) {
        try {
          loadedClass = findClass(name);
        } catch (ClassNotFoundException e) {
          loadedClass = super.loadClass(name, false);
        }
      }
      if (resolve) {
        resolveClass(loadedClass);
      }
      return loadedClass;
    }
  }

  @Override
  public URL getResource(String name) {
    URL url = findResource(name);
    return url != null ? url : super.getResource(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    List<URL> urls = new ArrayList<>(Collections.list(findResources(name)));
    ClassLoader parent = getParent();
    if (parent != null) {
      urls.addAll(Collections.list(parent.getResources(name)));
    }
    return Collections.enumeration(urls);
  }

  private static boolean isParentFirst(String className) {
    for (String prefix : PARENT_FIRST_PREFIXES) {
      if (className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
