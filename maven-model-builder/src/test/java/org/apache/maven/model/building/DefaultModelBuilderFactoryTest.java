package org.apache.maven.model.building;

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

import java.io.File;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import junit.framework.TestCase;

/**
 * @author Benjamin Bentmann
 */
public class DefaultModelBuilderFactoryTest
    extends TestCase
{

    private File getPom( String name )
    {
        return new File( "src/test/resources/poms/factory/" + name + ".xml" ).getAbsoluteFile();
    }

    public void testCompleteWiring()
        throws Exception
    {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull( builder );

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setProcessPlugins( true );
        request.setPomFile( getPom( "simple" ) );

        ModelBuildingResult result = builder.build( request );
        assertNotNull( result );
        assertNotNull( result.getEffectiveModel() );
        assertEquals( "activated", result.getEffectiveModel().getProperties().get( "profile.file" ) );
        Xpp3Dom conf = (Xpp3Dom) result.getEffectiveModel().getBuild().getPlugins().get( 0 ).getConfiguration();
        assertEquals( "1.5", conf.getChild( "source" ).getValue() );
        assertEquals( "  1.5  ", conf.getChild( "target" ).getValue() );
    }

}
