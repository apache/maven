package org.apache.maven.plugin.descriptor;

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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.alias.DefaultClassMapper;
import com.thoughtworks.xstream.alias.DefaultNameMapper;
import com.thoughtworks.xstream.objecttree.reflection.JavaReflectionObjectFactory;
import com.thoughtworks.xstream.xml.xpp3.Xpp3DomBuilder;
import com.thoughtworks.xstream.xml.xpp3.Xpp3DomXMLReader;
import com.thoughtworks.xstream.xml.xpp3.Xpp3DomXMLReaderDriver;

import java.io.Reader;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginDescriptorBuilder
{
    private XStream xstream;

    public PluginDescriptorBuilder()
    {
        xstream = new XStream( new JavaReflectionObjectFactory(),
                               new DefaultClassMapper( new DefaultNameMapper() ),
                               new Xpp3DomXMLReaderDriver(),
                               "implementation" );

        xstream.alias( "plugin", PluginDescriptor.class );

        xstream.alias( "mojo", MojoDescriptor.class );

        xstream.alias( "prereq", String.class );

        xstream.alias( "parameter", Parameter.class );

        xstream.alias( "dependency", Dependency.class );
    }

    public PluginDescriptor build( Reader reader )
        throws Exception
    {
        return (PluginDescriptor) xstream.fromXML( new Xpp3DomXMLReader( Xpp3DomBuilder.build( reader ) ) );
    }
}
