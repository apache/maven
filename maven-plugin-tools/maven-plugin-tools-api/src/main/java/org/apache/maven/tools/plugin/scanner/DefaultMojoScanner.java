package org.apache.maven.tools.plugin.scanner;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author jdcasey
 */
public class DefaultMojoScanner
    implements MojoScanner
{

    private Map mojoDescriptorExtractors;

    public DefaultMojoScanner( Map extractors )
    {
        this.mojoDescriptorExtractors = extractors;
    }

    public DefaultMojoScanner()
    {
    }

    public Set execute( MavenProject project )
        throws Exception
    {
        Set descriptors = new HashSet();

        System.out.println( "Using " + mojoDescriptorExtractors.size() + " extractors." );

        for ( Iterator it = mojoDescriptorExtractors.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            String language = (String) entry.getKey();
            MojoDescriptorExtractor extractor = (MojoDescriptorExtractor) entry.getValue();

            System.out.println( "Applying extractor for language: " + language );

            Set extractorDescriptors = extractor.execute( project );

            System.out.println( "Extractor for language: " + language + " found " + extractorDescriptors.size() +
                                " mojo descriptors." );

            descriptors.addAll( extractorDescriptors );
        }

        return descriptors;
    }

}