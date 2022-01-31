package org.apache.maven.internal.aether;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.building.DefaultBuildPomXMLFilterFactory;
import org.apache.maven.model.building.TransformerContext;
import org.apache.maven.model.transform.RawToConsumerPomXMLFilterFactory;
import org.apache.maven.model.transform.pull.XmlUtils;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.EntityReplacementMap;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

class ConsumerModelSourceTransformer
{
    public InputStream transform( Path pomFile, TransformerContext context )
            throws IOException, XmlPullParserException
    {
        XmlStreamReader reader = ReaderFactory.newXmlReader( Files.newInputStream( pomFile ) );
        XmlPullParser parser = new MXParser( EntityReplacementMap.defaultEntityReplacementMap );
        parser.setInput( reader );
        parser = new RawToConsumerPomXMLFilterFactory( new DefaultBuildPomXMLFilterFactory( context, true ) )
                .get( parser, pomFile );

        return XmlUtils.writeDocument( reader, parser );
    }

}
