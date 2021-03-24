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
import java.io.FileInputStream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Benjamin Bentmann
 */
public class DefaultModelBuilderFactoryTest
{

    private File getPom( String name )
    {
        return new File( "src/test/resources/poms/factory/" + name + ".xml" ).getAbsoluteFile();
    }

    @Test
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

    public void test_pom_changes() throws Exception
    {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull( builder );
        File pom = getPom( "simple" );

        String originalExists = readPom( pom ).getProfiles().get( 1 ).getActivation().getFile().getExists();

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setProcessPlugins( true );
        request.setPomFile( pom );
        ModelBuildingResult result = builder.build( request );
        String resultedExists = result.getRawModel().getProfiles().get( 1 ).getActivation().getFile().getExists();

        assertEquals( originalExists, resultedExists );
        assertTrue( result.getEffectiveModel().getProfiles().get( 1 ).getActivation().getFile().getExists()
                .contains( "src/test/resources/poms/factory/" ) );
    }

    private static Model readPom( File file ) throws Exception
    {
        MavenXpp3Reader reader = new MavenXpp3Reader();

        return reader.read( new FileInputStream( file ) );
    }
}
