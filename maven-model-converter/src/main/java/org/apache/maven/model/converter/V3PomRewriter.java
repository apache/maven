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
import org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * @author jdcasey
 * @plexus.component role="org.apache.maven.model.converter.ArtifactPomRewriter" role-hint="v3"
 */
public class V3PomRewriter
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
        Model v4Model;

        if ( from != null )
        {
            MavenXpp3Reader v3Reader = new MavenXpp3Reader();

            StringWriter w = new StringWriter();
            IOUtil.copy( from, w );
            String content = StringUtils.replace( w.toString(), "${pom.currentVersion}", "${project.version}" );

            org.apache.maven.model.v3_0_0.Model v3Model = v3Reader.read( new StringReader( content ) );
            v4Model = translator.translate( v3Model );
        }
        else
        {
            v4Model = new Model();
        }

        if ( v4Model != null )
        {
            translator.validateV4Basics( v4Model, groupId, artifactId, version, packaging );

            if ( !reportOnly )
            {
                MavenXpp3Writer v4Writer = new MavenXpp3Writer();
                v4Writer.write( to, v4Model );
            }
        }
    }

    public List getWarnings()
    {
        return translator.getWarnings();
    }

}