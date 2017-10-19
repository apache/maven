package org.apache.maven.wrapper;

/**
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class Logger {
  private static final boolean VERBOSE = "true".equalsIgnoreCase(MavenWrapperMain.MVNW_VERBOSE);

  public static void info(String msg) {
    if (VERBOSE) {
      System.out.println(msg);
    }
  }

  public static void warn(String msg) {
    System.out.println(msg);
  }
}
