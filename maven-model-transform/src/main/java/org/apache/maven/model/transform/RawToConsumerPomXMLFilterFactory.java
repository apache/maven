package org.apache.maven.model.transform;

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

import java.nio.file.Path;

import org.codehaus.plexus.util.xml.pull.XmlPullParser;

/**
 * @author Guillaume Nodet
 * @author Robert Scholte
 * @since 4.0.0
 */
public class RawToConsumerPomXMLFilterFactory
{
    private BuildToRawPomXMLFilterFactory buildPomXMLFilterFactory;

    public RawToConsumerPomXMLFilterFactory( BuildToRawPomXMLFilterFactory buildPomXMLFilterFactory )
    {
        this.buildPomXMLFilterFactory = buildPomXMLFilterFactory;
    }

    public final XmlPullParser get( XmlPullParser orgParser, Path projectPath )
    {
        XmlPullParser parser = orgParser;

        parser = buildPomXMLFilterFactory.get( parser, projectPath );

        // Ensure that xs:any elements aren't touched by next filters
        parser = new FastForwardFilter( parser );

        // Strip modules
        parser = new ModulesXMLFilter( parser );
        // Adjust relativePath
        parser = new RelativePathXMLFilter( parser );

        return parser;
    }
}
