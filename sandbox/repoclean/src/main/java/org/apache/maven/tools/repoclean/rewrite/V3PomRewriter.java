package org.apache.maven.tools.repoclean.rewrite;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.apache.maven.tools.repoclean.translate.PomV3ToV4Translator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

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

/**
 * @author jdcasey
 */
public class V3PomRewriter
    implements ArtifactPomRewriter
{
    private PomV3ToV4Translator translator;

    public void rewrite( Artifact artifact, File from, File to, Reporter reporter, boolean reportOnly )
        throws Exception
    {
        Model v4Model = null;

        if ( from.exists() )
        {
            FileReader fromReader = null;
            try
            {
                org.apache.maven.model.v3_0_0.Model v3Model = null;
                try
                {
                    fromReader = new FileReader( from );

                    MavenXpp3Reader v3Reader = new MavenXpp3Reader();

                    v3Model = v3Reader.read( fromReader );
                }
                catch ( Exception e )
                {
                    reporter.error( "Invalid v3 POM at: \'" + from + "\'. Cannot read.", e );
                }

                if(v3Model != null)
                {
                    v4Model = translator.translate( v3Model, reporter );
                }
            }
            finally
            {
                IOUtil.close( fromReader );
            }
        }
        else
        {
            reporter.info( "POM for artifact[" + artifact.getId()
                + "] does not exist in source repository. We will create a skeletal one here." );
            
            v4Model = new Model();
        }

        if(v4Model != null)
        {
            validateV4Basics( v4Model, artifact, reporter );

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
                    MavenXpp3Writer v4Writer = new MavenXpp3Writer();
                    v4Writer.write( toWriter, v4Model );
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

    private void validateV4Basics( Model model, Artifact artifact, Reporter reporter )
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