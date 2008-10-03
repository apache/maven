package org.apache.maven.its;

import junit.framework.Test;
import junit.framework.TestCase;
import org.apache.maven.it.IntegrationTestSuite;
import org.apache.maven.it.VerificationException;

public class Suite
    extends TestCase
{
    public static Test suite()
        throws VerificationException
    {
        return IntegrationTestSuite.suite();
    }
}
