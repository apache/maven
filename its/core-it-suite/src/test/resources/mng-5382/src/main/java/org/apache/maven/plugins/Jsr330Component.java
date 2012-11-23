package org.apache.maven.plugins;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class Jsr330Component {

  public void hello() {
    System.out.println();
    System.out.println();
    System.out.println("Hello! I am a component that is being used via constructor injection! That's right, I'm a JSR330 badass.");
    System.out.println();
    System.out.println();
  }
}
