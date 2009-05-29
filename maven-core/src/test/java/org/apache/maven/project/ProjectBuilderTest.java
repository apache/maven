package org.apache.maven.project;

import org.apache.maven.AbstractCoreMavenComponentTestCase;

public class ProjectBuilderTest
    extends AbstractCoreMavenComponentTestCase
{
    protected String getProjectsDirectory()
    {
        return "src/test/projects/project-builder";
    }

    public void testProjectBuilderWhereOutputDirectoryIsOverridden()
        throws Exception
    {
    }
}
