package org.apache.maven.tools.repoclean.rewrite;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * @author jdcasey
 */
public class V4PomRewriter
    implements ArtifactPomRewriter
{
    public void rewrite( Artifact artifact, File from, File to, Reporter reporter, boolean reportOnly )
        throws Exception
    {
        Model model = null;

        if ( from.exists() )
        {
            FileReader fromReader = null;
            try
            {
                fromReader = new FileReader( from );

                MavenXpp3Reader reader = new MavenXpp3Reader();

                try
                {
                    model = reader.read( fromReader );
                }
                catch ( Exception e )
                {
                    reporter.error( "Invalid v4 POM at \'" + from + "\'. Cannot read.", e );
                }
            }
            finally
            {
                IOUtil.close( fromReader );
            }
        }
        else
        {
            reporter.error( "POM for artifact[" + artifact.getId() + "] does not exist in source repository!" );
        }

        if ( model != null )
        {
            validateBasics( model, artifact, reporter );

            if ( !reportOnly )
            {
                File toParent = to.getParentFile();
                if ( !toParent.exists() )
                {
                    toParent.mkdirs();
                }

                FileWriter toWriter = null;
                try
                {
                    toWriter = new FileWriter( to );
                    MavenXpp3Writer writer = new MavenXpp3Writer();
                    writer.write( toWriter, model );
                }
                finally
                {
                    IOUtil.close( toWriter );
                }
            }
            else
            {
                reporter.info( "Skipping model write to target repository (we're in report-only mode)." );
            }
        }
    }

    private void validateBasics( Model model, Artifact artifact, Reporter reporter )
        throws Exception
    {
        if ( StringUtils.isEmpty( model.getModelVersion() ) )
        {
            reporter.info( "Setting modelVersion on v4 model to \'4.0.0\'" );
            model.setModelVersion( "4.0.0" );
        }

        if ( StringUtils.isEmpty( model.getGroupId() ) )
        {
            reporter.info( "Setting groupId on model using artifact information." );
            model.setGroupId( artifact.getGroupId() );
        }

        if ( StringUtils.isEmpty( model.getArtifactId() ) )
        {
            reporter.info( "Setting artifactId on model using artifact information." );
            model.setArtifactId( artifact.getArtifactId() );
        }

        if ( StringUtils.isEmpty( model.getVersion() ) )
        {
            reporter.info( "Setting version on model using artifact information." );
            model.setVersion( artifact.getVersion() );
        }

        if ( StringUtils.isEmpty( model.getPackaging() ) )
        {
            reporter.info( "Setting packaging on model using artifact type information." );
            model.setPackaging( artifact.getType() );
        }
    }

}