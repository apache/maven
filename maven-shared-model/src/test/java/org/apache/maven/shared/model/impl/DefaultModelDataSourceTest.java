package org.apache.maven.shared.model.impl;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelContainerAction;
import org.apache.maven.shared.model.ModelContainerFactory;
import org.apache.maven.shared.model.ModelDataSource;
import org.apache.maven.shared.model.ModelProperty;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DefaultModelDataSourceTest
{

    private static List<ModelContainerFactory> factories = new ArrayList<ModelContainerFactory>();

    static
    {
        factories.add( new DummyModelContainerFactory() );
    }

    @Test
    public void mergeModelContainersSetWithAppendChild() {
        ModelProperty dup0 = new ModelProperty( "http://apache.org/maven/project", null );
        ModelProperty dup1 = new ModelProperty( "http://apache.org/maven/project/build", null );
        ModelProperty dup2 =
            new ModelProperty( "http://apache.org/maven/project/build/plugins#collection", null );
        ModelProperty dup3 = new ModelProperty(
            "http://apache.org/maven/project/build/plugins#collection/plugin", null );
        ModelProperty dup4 = new ModelProperty(
            "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#set", null );
          ModelProperty dup5 = new ModelProperty(
            "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#set/myList", null );
        ModelProperty dup6 = new ModelProperty( "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#set/myList#property/combine.children", "append" );
        ModelProperty dup6a = new ModelProperty( "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#set/myList#property/combine.children/a", "x" );
        ModelProperty dup7 = new ModelProperty(
            "http://apache.org/maven/project/build/plugins#collection/plugin/version", "1.1" );
        ModelProperty dup8 = new ModelProperty(
            "http://apache.org/maven/project/build/plugins#collection/plugin", null );
        ModelProperty dup9 = new ModelProperty(
            "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#set", null );
        ModelProperty dup10 = new ModelProperty(
            "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#set/myList", null );
        ModelProperty dup11 = new ModelProperty( "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#set/myList#property/combine.children", "append" );
        ModelProperty dup11a = new ModelProperty( "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#set/myList#property/combine.children/b", "y" );
        ModelProperty dup12 = new ModelProperty(
            "http://apache.org/maven/project/build/plugins#collection/plugin/version", "1.1" );

        List<ModelProperty> modelProperties = Arrays.asList( dup0, dup1, dup2, dup3, dup4, dup5, dup6, dup6a, dup7, dup8,
                                                             dup9, dup10, dup11, dup11a, dup12 );
        DummyModelContainerFactory factory = new DummyModelContainerFactory();

        DefaultModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        List<ModelProperty> mps = datasource.mergeModelContainers(
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 3, 9 ) ) ),
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 9, 14 ) ) ) );
        for(ModelProperty mp : mps) {
            System.out.println(mp);
        }
        assertTrue(mps.contains(dup6a));
        assertTrue(mps.contains(dup11a));
    }


    @Test
    public void mergeModelContainersSet() {
        ModelProperty dup0 = new ModelProperty( "http://apache.org/maven/project", null );
        ModelProperty dup1 = new ModelProperty( "http://apache.org/maven/project/build", null );
        ModelProperty dup2 = new ModelProperty( "http://apache.org/maven/project/build/pluginManagement", null );
        ModelProperty dup3 =
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection", null );
        ModelProperty dup4 = new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin", null );
        ModelProperty dup5 = new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/configuration#set", null );
        ModelProperty dup6 = new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/configuration#set/jdk",
            "1.5" );
        ModelProperty dup7 = new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version", "1.1" );
        ModelProperty dup8 = new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin", null );
        ModelProperty dup9 = new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/configuration#set", null );
        ModelProperty dup10 = new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/configuration#set/jdk",
            "1.4" );
        ModelProperty dup11 = new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version", "1.1" );

        List<ModelProperty> modelProperties = Arrays.asList( dup0, dup1, dup2, dup3, dup4, dup5, dup6, dup7, dup8,
                                                             dup9, dup10, dup11 );
        DummyModelContainerFactory factory = new DummyModelContainerFactory();

        DefaultModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        List<ModelProperty> mps = datasource.mergeModelContainers(
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 4, 8 ) ) ),
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 8, 11 ) ) ) );
                for(ModelProperty mp : mps) {
            System.out.println(mp);
        }
        assertFalse(mps.contains(dup10));
    }

    @Test
    public void mergeModelContainersCollectionsOfCollections()
        throws IOException
    {
        List<ModelProperty> modelProperties = Arrays.asList(
            new ModelProperty( "http://apache.org/maven/project", null ),
            new ModelProperty( "http://apache.org/maven/project/build", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version", "2.0.2" ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/artifactId", "maven-compiler-plugin" ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/groupId", "org.apache.maven.plugins" ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency", null ),
            new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/groupId",
            "gid1" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/artifactId",
            "art1" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/version",
            "2.0" ),
                      new ModelProperty(
                          "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin", null ),
                      new ModelProperty(
                          "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version",
                          "2.0.2" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/artifactId",
            "maven-compiler-plugin" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/groupId",
            "org.apache.maven.plugins" ),

                                       new ModelProperty(
                                           "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection",
                                           null ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency",
            null ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/groupId",
            "gid1" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/artifactId",
            "art1" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/version",
            "1.0" ) );

        DummyModelContainerFactory factory = new DummyModelContainerFactory();

        DefaultModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        List<ModelProperty> mps = datasource.mergeModelContainers(
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 4, 13 ) ) ),
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 13, 21 ) ) ) );
        assertTrue( mps.containsAll( new ArrayList<ModelProperty>( modelProperties.subList( 4, 8 ) ) ) );
    }

    @Test
    public void mergeModelContainers()
        throws IOException
    {
        List<ModelProperty> modelProperties = Arrays.asList(
            new ModelProperty( "http://apache.org/maven/project", null ),
            new ModelProperty( "http://apache.org/maven/project/build", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection", null ),

            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin",
                               null ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version", "2.0.2" ),
                                       new ModelProperty(
                                           "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/artifactId",
                                           "maven-compiler-plugin" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/groupId",
            "org.apache.maven.plugins" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin", null ),
                                          new ModelProperty(
                                              "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version",
                                              "2.0.2" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/artifactId",
            "maven-compiler-plugin" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/groupId",
            "org.apache.maven.plugins" ) );

        DummyModelContainerFactory factory = new DummyModelContainerFactory();

        DefaultModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        List<ModelProperty> mps = datasource.mergeModelContainers(
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 4, 8 ) ) ),
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 8, 12 ) ) ) );
        assertTrue( mps.containsAll( new ArrayList<ModelProperty>( modelProperties.subList( 4, 8 ) ) ) );
    }

    @Test
    public void join1()
        throws DataSourceException, IOException
    {
        List<ModelProperty> modelProperties = Arrays.asList(
            new ModelProperty( "http://apache.org/maven/project", null ),
            new ModelProperty( "http://apache.org/maven/project/build", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection", null ),
            new ModelProperty( "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin",
                               null ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version", "2.0.2" ),
                                       new ModelProperty(
                                           "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/artifactId",
                                           "maven-compiler-plugin" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/groupId",
            "org.apache.maven.plugins" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin", null ),
                                          new ModelProperty(
                                              "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version",
                                              "2.0.2" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/artifactId",
            "maven-compiler-plugin" ), new ModelProperty(
            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/groupId",
            "org.apache.maven.plugins" ), new ModelProperty( "http://apache.org/maven/project/version",
                                                             "2.0.10-SNAPSHOT" ),
                                          new ModelProperty( "http://apache.org/maven/project/artifactId", "maven" ),
                                          new ModelProperty( "http://apache.org/maven/project/groupId",
                                                             "org.apache.maven" ), new ModelProperty(
            "http://apache.org/maven/project/modelVersion", "4.0.0" ) );

        DummyModelContainerFactory factory = new DummyModelContainerFactory();

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );
        ModelContainer joinedModelContainer = datasource.join(
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 4, 8 ) ) ),
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 8, 12 ) ) ) );

        for ( ModelProperty mp : joinedModelContainer.getProperties() )
        {
            System.out.println( "-" + mp );
        }

        if ( !datasource.getModelProperties().containsAll( joinedModelContainer.getProperties() ) )
        {
            throw new IOException();
        }

        for ( ModelProperty mp : datasource.getModelProperties() )
        {
            System.out.println( "+" + mp );
        }

    }


    @Test
    public void query()
        throws DataSourceException
    {
        ModelProperty mpA = new ModelProperty( "container-marker/a", null );
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( "container-marker", null ) );
        modelProperties.add( mpA );

        modelProperties.add( new ModelProperty( "container-marker", null ) );
        modelProperties.add( new ModelProperty( "container-marker/b", null ) );

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        List<ModelContainer> containers = datasource.queryFor( "container-marker" );
        assertEquals( "Number of containers: ", 2, containers.size() );
        assertEquals( "Properties for container 'a':", 2, containers.get( 0 ).getProperties().size() );
        assertEquals( mpA, containers.get( 0 ).getProperties().get( 1 ) );
    }

    @Test
    public void queryWithOneContainerMarker()
        throws DataSourceException
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( "container-marker", null ) );

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        List<ModelContainer> containers = datasource.queryFor( "container-marker" );
        assertEquals( "Number of containers: ", 1, containers.size() );
        assertEquals( "Properties for container 'a':", 1, containers.get( 0 ).getProperties().size() );
    }

    @Test
    public void queryWithMultipleContainerMarkers()
        throws DataSourceException
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( "container-marker", null ) );
        modelProperties.add( new ModelProperty( "container-marker", null ) );
        modelProperties.add( new ModelProperty( "acontainer-marker-1", null ) );
        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        List<ModelContainer> containers = datasource.queryFor( "container-marker" );
        assertEquals( "Number of containers: ", 2, containers.size() );
        assertEquals( "Properties for container 'a':", 1, containers.get( 0 ).getProperties().size() );
    }

    @Test(expected = DataSourceException.class)
    public void queryWithUriNotInContainerFactory()
        throws DataSourceException
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( "a", null ) );

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        datasource.queryFor( "bogus" );
    }

    @Test
    public void joinEmptyContainer()
        throws DataSourceException
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( "a", null ) );

        DummyModelContainerFactory factory = new DummyModelContainerFactory();

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        ModelContainer modelContainerA = factory.create( new ArrayList<ModelProperty>( modelProperties ) );
        ModelContainer modelContainer =
            datasource.join( modelContainerA, factory.create( new ArrayList<ModelProperty>() ) );
        assertEquals( modelContainer, modelContainerA );
    }

    /*
    @Test(expected = DataSourceException.class)
    public void joinContainerWithElementsNotInDataSource() throws DataSourceException {
        ModelProperty mpA = new ModelProperty("a", null);
        ModelProperty mpB = new ModelProperty("b", null);
        ModelProperty mpC = new ModelProperty("c", null);

        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(mpA);
        modelProperties.add(mpB);

        DummyModelContainerFactory factory = new DummyModelContainerFactory();

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init(modelProperties, factories);
        modelProperties.add(mpC);
        datasource.join(
                factory.create(new ArrayList<ModelProperty>(modelProperties.subList(0, 3))),
                factory.create(new ArrayList<ModelProperty>(modelProperties.subList(1, 2))));
    }
    */

    @Test
    public void cannotModifyDataSourceFromInitializedList()
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( "a", null ) );
        modelProperties.add( new ModelProperty( "b", null ) );

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        modelProperties.remove( 0 );

        assertEquals( 2, datasource.getModelProperties().size() );
    }

    @Test
    public void cannotModifyDataSourceFromReturnedList()
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( "a", null ) );
        modelProperties.add( new ModelProperty( "b", null ) );

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );

        datasource.getModelProperties().remove( 0 );

        assertEquals( 2, datasource.getModelProperties().size() );
    }

    @Test
    public void join()
        throws DataSourceException
    {
        ModelProperty mpA = new ModelProperty( "maven/a", null );
        ModelProperty mpB = new ModelProperty( "maven/b", null );
        ModelProperty mpC = new ModelProperty( "maven/a", null );

        List<ModelProperty> modelProperties = Arrays.asList( mpA, mpB, mpC );

        DummyModelContainerFactory factory = new DummyModelContainerFactory();

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );
        ModelContainer joinedModelContainer = datasource.join(
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 0, 1 ) ) ),
            factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 1, 3 ) ) ) );

        assertEquals( 2, joinedModelContainer.getProperties().size() );
        assertFalse( joinedModelContainer.getProperties().contains( mpC ) );
    }

    @Test
    public void delete()
    {
        ModelProperty mpA = new ModelProperty( "a", null );
        ModelProperty mpB = new ModelProperty( "b", null );
        ModelProperty mpC = new ModelProperty( "a", null );

        List<ModelProperty> modelProperties = Arrays.asList( mpA, mpB, mpC );
        DummyModelContainerFactory factory = new DummyModelContainerFactory();

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );
        datasource.delete( factory.create( new ArrayList<ModelProperty>( modelProperties.subList( 0, 1 ) ) ) );

        assertEquals( 2, datasource.getModelProperties().size() );
        assertFalse( datasource.getModelProperties().contains( mpA ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteNullModelContainer()
        throws IllegalArgumentException
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( "a", null ) );

        ModelDataSource datasource = new DefaultModelDataSource();
        datasource.init( modelProperties, factories );
        datasource.delete( null );
    }

    private static class DummyModelContainerFactory
        implements ModelContainerFactory
    {

        public Collection<String> getUris()
        {
            return Arrays.asList( "container-marker" );
        }

        public ModelContainer create( final List<ModelProperty> modelProperties )
        {
            return new DummyModelContainer( modelProperties );
        }

        private static class DummyModelContainer
            implements ModelContainer
        {

            private List<ModelProperty> modelProperties;

            private DummyModelContainer( List<ModelProperty> modelProperties )
            {
                this.modelProperties = new ArrayList<ModelProperty>( modelProperties );
            }

            public List<ModelProperty> getProperties()
            {
                return modelProperties;
            }

            public ModelContainerAction containerAction( ModelContainer modelContainer )
            {
                return ModelContainerAction.NOP;
            }

            public ModelContainer createNewInstance( List<ModelProperty> modelProperties )
            {
                return new DummyModelContainer( modelProperties );
            }

        }
    }
}
