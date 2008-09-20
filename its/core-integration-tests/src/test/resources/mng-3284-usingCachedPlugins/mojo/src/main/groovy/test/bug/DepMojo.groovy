package test.bug

import org.codehaus.mojo.groovy.GroovyMojoSupport


/**
 * Show bug?
 *
 * @goal bug
 * @phase package
 */
class DepMojo
    extends GroovyMojoSupport
{
    
    
    void execute() {
     println "USING VERSION 1"; 
    }
    
    
  
}



