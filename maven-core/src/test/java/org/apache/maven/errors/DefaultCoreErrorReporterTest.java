package org.apache.maven.errors;

import org.apache.maven.InvalidTaskException;
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.plugin.InvalidPluginException;

import junit.framework.TestCase;

public class DefaultCoreErrorReporterTest
    extends TestCase
{

    public void testReportInvalidPluginForDirectInvocation()
    {
        CoreErrorReporter reporter = new DefaultCoreErrorReporter();

        InvalidPluginException err = new InvalidPluginException( "Test message" );
        reporter.reportInvalidPluginForDirectInvocation( "test", null, null, err );

        TaskValidationResult tvr = new TaskValidationResult( "test", "This is a test invalid task.", err );
        InvalidTaskException exception = tvr.generateInvalidTaskException();

        Throwable realCause = reporter.findReportedException( exception );

        assertSame( err, realCause );

        String message = reporter.getFormattedMessage( realCause );

        System.out.println( message );

        assertNotNull( message );
        assertTrue( message.length() > 0 );
    }

}
