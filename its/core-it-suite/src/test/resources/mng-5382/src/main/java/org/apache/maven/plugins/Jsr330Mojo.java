package org.apache.maven.plugins;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "hello", defaultPhase = LifecyclePhase.VALIDATE, requiresProject = false)
public class Jsr330Mojo extends AbstractMojo {

  private Jsr330Component component;
  
  @Inject
  public Jsr330Mojo(Jsr330Component component) {
    this.component = component;    
  }
  
  public void execute() throws MojoExecutionException {    
    //
    // Say hello to the world, my little constructor injected component!
    //
    component.hello();
  }
}
