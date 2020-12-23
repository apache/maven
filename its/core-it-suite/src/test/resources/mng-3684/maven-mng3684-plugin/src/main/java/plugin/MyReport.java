package plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

/**
 * @goal check-report
 */
public class MyReport
    extends AbstractMavenReport
{
    /**
     * @parameter default-value="${project.build}"
     * @required
     * @readonly
     */
    private Build build;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private void runChecks()
        throws MavenReportException
    {
        Build projectBuild = project.getBuild();

        Map failedComparisons = new HashMap();

        check( "project.build.directory", projectBuild.getDirectory(), build.getDirectory(), failedComparisons );

        check( "project.build.outputDirectory", projectBuild.getOutputDirectory(), build.getOutputDirectory(),
               failedComparisons );

        check( "project.build.sourceDirectory", projectBuild.getSourceDirectory(), build.getSourceDirectory(),
               failedComparisons );

        check( "project.build.testSourceDirectory", projectBuild.getTestSourceDirectory(),
               build.getTestSourceDirectory(), failedComparisons );

        check( "project.build.scriptSourceDirectory", projectBuild.getScriptSourceDirectory(),
               build.getScriptSourceDirectory(), failedComparisons );

        List projectResources = projectBuild.getResources();
        List buildResources = build.getResources();

        if ( projectResources != null )
        {
            for ( int i = 0; i < projectResources.size(); i++ )
            {
                Resource projectRes = (Resource) projectResources.get( i );
                Resource buildRes = (Resource) buildResources.get( i );

                check( "project.build.resources[" + i + "].directory", projectRes.getDirectory(),
                       buildRes.getDirectory(), failedComparisons );

                check( "project.build.resources[" + i + "].targetPath", projectRes.getTargetPath(),
                       buildRes.getTargetPath(), failedComparisons );
            }
        }

        if ( !failedComparisons.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append( "One or more build-section values were not interpolated correctly"
                + "\nbefore the build instance was injected as a plugin parameter:\n" );

            for ( Iterator it = failedComparisons.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                String[] value = (String[]) entry.getValue();

                buffer.append( "\n- " ).append( key );
                buffer.append( "\n\tShould be: \'" ).append( value[0] );
                buffer.append( "\'\n\t Was: \'" ).append( value[1] ).append( "\'\n" );
            }

            throw new MavenReportException( buffer.toString() );
        }
    }

    private void check( String description, String projectValue, String buildValue, Map failedComparisons )
    {
        if ( projectValue == null && buildValue != null )
        {
            failedComparisons.put( description, new String[] { projectValue, buildValue } );
        }
        else if ( projectValue != null && !projectValue.equals( buildValue ) )
        {
            failedComparisons.put( description, new String[] { projectValue, buildValue } );
        }
    }

    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        runChecks();
    }

    protected String getOutputDirectory()
    {
        return project.getReporting().getOutputDirectory();
    }

    protected MavenProject getProject()
    {
        return project;
    }

    protected Renderer getSiteRenderer()
    {
        return null;
    }

    public String getDescription( Locale locale )
    {
        return "test";
    }

    public String getName( Locale locale )
    {
        return "test";
    }

    public String getOutputName()
    {
        return "test";
    }
}
