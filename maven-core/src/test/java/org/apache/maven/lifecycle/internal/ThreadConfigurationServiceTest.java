package org.apache.maven.lifecycle.internal;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.maven.lifecycle.internal.stub.LoggerStub;

/**
 * @author Kristian Rosenvold
 */
public class ThreadConfigurationServiceTest
    extends TestCase
{
    public void testGetThreadCount()
        throws Exception
    {
        ThreadConfigurationService threadConfigurationService = new ThreadConfigurationService( new LoggerStub(), 3 );

        Assert.assertEquals( 5, threadConfigurationService.getThreadCount( "1.75", true, 6 ).intValue() );
        Assert.assertEquals( 6, threadConfigurationService.getThreadCount( "1.84", true, 6 ).intValue() );
    }
}
