package org.apache.maven.usability;

import org.apache.maven.plugin.MojoExecutionException;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MojoExecutionExceptionDiagnoserTest
    extends TestCase
{

    public void testDiag()
    {
        MojoExecutionExceptionDiagnoser diag = new MojoExecutionExceptionDiagnoser();
        
        Exception e = new NullPointerException("Test");
        
        assertFalse( diag.canDiagnose( e ) );
        
        MojoExecutionException me = new MojoExecutionException(null);
        
        assertTrue( diag.canDiagnose( me ) );
        
        diag.diagnose( me );   
        
    }
}
