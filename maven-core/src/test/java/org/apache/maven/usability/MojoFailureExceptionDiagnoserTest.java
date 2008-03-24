package org.apache.maven.usability;

import junit.framework.TestCase;

import org.apache.maven.plugin.MojoFailureException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MojoFailureExceptionDiagnoserTest
    extends TestCase
{

    public void testDiag()
    {
        MojoFailureExceptionDiagnoser diag = new MojoFailureExceptionDiagnoser();
        
        Exception e = new NullPointerException("Test");
        
        assertFalse( diag.canDiagnose( e ) );
        
        MojoFailureException me = new MojoFailureException(null);
        
        assertTrue( diag.canDiagnose( me ) );
        
        diag.diagnose( me );   
        
    }
}
