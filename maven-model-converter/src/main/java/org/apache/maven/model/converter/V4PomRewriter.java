package org.apache.maven.model.converter;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * @author jdcasey
 * @plexus.component role="org.apache.maven.model.converter.ArtifactPomRewriter" role-hint="v4"
 */
public class V4PomRewriter
    implements ArtifactPomRewriter
{
    /**
     * @plexus.requirement
     */
    private ModelConverter translator;

    public void rewrite( Reader from, Writer to, boolean reportOnly, String groupId, String artifactId, String version,
                         String packaging )
        throws Exception
    {
        Model model = null;

        if ( from != null )
        {
            MavenXpp3Reader reader = new MavenXpp3Reader();

            model = reader.read( from );
        }
        else
        {
            model = new Model();
        }

        if ( model != null )
        {
            translator.validateV4Basics( model, groupId, artifactId, version, packaging );

            if ( !reportOnly )
            {
                MavenXpp3Writer writer = new MavenXpp3Writer();
                writer.write( to, model );
            }
        }
    }

    public List getWarnings()
    {
        return translator.getWarnings();
    }
}