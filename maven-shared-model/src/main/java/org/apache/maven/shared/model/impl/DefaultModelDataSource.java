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
import org.apache.maven.shared.model.ModelContainerFactory;
import org.apache.maven.shared.model.ModelDataSource;
import org.apache.maven.shared.model.ModelProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of the ModelDataSource.
 */
public final class DefaultModelDataSource
    implements ModelDataSource
{

    /**
     * List of current model properties underlying the data source
     */
    private List<ModelProperty> modelProperties;

    /**
     * Copy of the model properties used during initialization of the data source
     */
    private List<ModelProperty> originalModelProperties;

    /**
     * History of joins and deletes
     */
    private List<DataEvent> dataEvents;

    /**
     * Map of model container factories used in creation of model containers
     */
    private Map<String, ModelContainerFactory> modelContainerFactoryMap;

    /**
     * Default constructor
     */
    public DefaultModelDataSource()
    {
    }

    /**
     * @see ModelDataSource#join(org.apache.maven.shared.model.ModelContainer, org.apache.maven.shared.model.ModelContainer)
     */
    public ModelContainer join( ModelContainer a, ModelContainer b )
        throws DataSourceException
    {
        if ( a == null || a.getProperties() == null || a.getProperties().size() == 0 )
        {
            throw new IllegalArgumentException( "a or a.properties: empty" );
        }
        if ( b == null || b.getProperties() == null )
        {
            throw new IllegalArgumentException( "b: null or b.properties: empty" );
        }
        /*
        if ( !modelProperties.containsAll( a.getProperties() ) )
        {
            List<ModelProperty> unknownProperties = new ArrayList<ModelProperty>();
            for ( ModelProperty mp : a.getProperties() )
            {
                if ( !modelProperties.contains( mp ) )
                {
                    unknownProperties.add( mp );
                }
            }

            List<DataEvent> des = new ArrayList<DataEvent>();
            for ( DataEvent de : dataEvents )
            {
                if ( aContainsAnyOfB( de.getRemovedModelProperties(), unknownProperties ) )
                {
                    des.add( de );
                }
            }
            //output
            StringBuffer sb = new StringBuffer();
            sb.append( "Found unknown properties in container 'a': Name = " ).append( a.getClass().getName() ).append(
                "\r\n" );
            for ( ModelProperty mp : unknownProperties )
            {
                sb.append( mp ).append( "\r\n" );
            }

            System.out.println( sb );
            throw new DataSourceException( "ModelContainer 'a' contains elements not within datasource" );
        }
        */
        if ( a.equals( b ) || b.getProperties().size() == 0 )
        {
            return a;
        }

        int startIndex = modelProperties.indexOf( b.getProperties().get( 0 ) );
        if(startIndex == -1)
        {
            startIndex = modelProperties.indexOf( a.getProperties().get( 0 ) );
            if(startIndex == -1) {
                return null;
            }
        }
        delete( a );
        delete( b );

        List<ModelProperty> joinedProperties = mergeModelContainers( a, b );
        if ( modelProperties.size() == 0 )
        {
            startIndex = 0;
        }
        joinedProperties = sort(joinedProperties, findBaseUriFrom(joinedProperties));

        modelProperties.addAll( startIndex, joinedProperties );
        /*
        List<ModelProperty> deletedProperties = new ArrayList<ModelProperty>();
        deletedProperties.addAll( a.getProperties() );
        deletedProperties.addAll( b.getProperties() );
        deletedProperties.removeAll( joinedProperties );
        if ( deletedProperties.size() > 0 )
        {
            dataEvents.add( new DataEvent( a, b, deletedProperties, "join" ) );
        }
        */
        return a.createNewInstance( joinedProperties );
    }

    /**
     * @see ModelDataSource#delete(org.apache.maven.shared.model.ModelContainer)
     */
    public void delete( ModelContainer modelContainer )
    {
        if ( modelContainer == null )
        {
            throw new IllegalArgumentException( "modelContainer: null" );
        }
        if ( modelContainer.getProperties() == null )
        {
            throw new IllegalArgumentException( "modelContainer.properties: null" );
        }
        modelProperties.removeAll( modelContainer.getProperties() );
        //dataEvents.add( new DataEvent( modelContainer, null, modelContainer.getProperties(), "delete" ) );
    }

    /**
     * @see org.apache.maven.shared.model.ModelDataSource#getModelProperties()
     */
    public List<ModelProperty> getModelProperties()
    {
        return new ArrayList<ModelProperty>( modelProperties );
    }

    /**
     * @see ModelDataSource#queryFor(String)
     */
    public List<ModelContainer> queryFor( String uri )
        throws DataSourceException
    {
        if ( uri == null )
        {
            throw new IllegalArgumentException( "uri" );
        }

        if ( modelProperties.isEmpty() )
        {
            return Collections.emptyList();
        }

        ModelContainerFactory factory = modelContainerFactoryMap.get( uri );
        if ( factory == null )
        {
            throw new DataSourceException( "Unable to find factory for uri: URI = " + uri );
        }

        List<ModelContainer> modelContainers = new LinkedList<ModelContainer>();

        final int NO_TAG = 0;
        final int START_TAG = 1;
        final int END_START_TAG = 2;
        final int END_TAG = 3;
        int state = NO_TAG;

        List<ModelProperty> tmp = new ArrayList<ModelProperty>();

        for ( Iterator<ModelProperty> i = modelProperties.iterator(); i.hasNext(); )
        {
            ModelProperty mp = i.next();
            if ( state == START_TAG && ( !i.hasNext() || !mp.getUri().startsWith( uri ) ) )
            {
                state = END_TAG;
            }
            else if ( state == START_TAG && mp.getUri().equals( uri ) )
            {
                state = END_START_TAG;
            }
            else if ( mp.getUri().startsWith( uri ) )
            {
                state = START_TAG;
            }
            else
            {
                state = NO_TAG;
            }
            switch ( state )
            {
                case START_TAG:
                {
                    tmp.add( mp );
                    if ( !i.hasNext() )
                    {
                        modelContainers.add( factory.create( tmp ) );
                    }
                    break;
                }
                case END_START_TAG:
                {
                    modelContainers.add( factory.create( tmp ) );
                    tmp.clear();
                    tmp.add( mp );
                    state = START_TAG;
                    break;
                }
                case END_TAG:
                {
                    if ( !i.hasNext() && mp.getUri().startsWith(uri))
                    {
                        tmp.add( mp );
                    }
                    modelContainers.add( factory.create( tmp ) );
                    tmp.clear();
                    state = NO_TAG;
                }
            }
        }

        //verify data source integrity
        List<ModelProperty> unknownProperties = findUnknownModelPropertiesFrom( modelContainers );
        if ( !unknownProperties.isEmpty() )
        {
            for ( ModelProperty mp : unknownProperties )
            {
                System.out.println( "Missing property from ModelContainer: " + mp );
            }
            throw new DataSourceException(
                "Unable to query datasource. ModelContainer contains elements not within datasource" );
        }

        return modelContainers;
    }

    /**
     * @see ModelDataSource#init(java.util.List, java.util.Collection)
     */
    public void init( List<ModelProperty> modelProperties, Collection<ModelContainerFactory> modelContainerFactories )
    {
        if ( modelProperties == null )
        {
            throw new IllegalArgumentException( "modelProperties: null" );
        }
        if ( modelContainerFactories == null )
        {
            throw new IllegalArgumentException( "modeContainerFactories: null" );
        }
        this.modelProperties = new LinkedList<ModelProperty>( modelProperties );
        this.modelContainerFactoryMap = new HashMap<String, ModelContainerFactory>();
        this.dataEvents = new ArrayList<DataEvent>();
        this.originalModelProperties = new ArrayList<ModelProperty>( modelProperties );

        for ( ModelContainerFactory factory : modelContainerFactories )
        {
            Collection<String> uris = factory.getUris();
            if ( uris == null )
            {
                throw new IllegalArgumentException( "factory.uris: null" );
            }

            for ( String uri : uris )
            {
                modelContainerFactoryMap.put( uri, factory );
            }
        }
    }

    /**
     * @see org.apache.maven.shared.model.ModelDataSource#getEventHistory()
     */
    public String getEventHistory()
    {
        StringBuffer sb = new StringBuffer();
        /*
        sb.append( "Original Model Properties\r\n" );
        for ( ModelProperty mp : originalModelProperties )
        {
            sb.append( mp ).append( "\r\n" );
        }

        for ( DataEvent de : dataEvents )
        {
            sb.append( de.toString() );
        }

        sb.append( "Processed Model Properties\r\n" );
        for ( ModelProperty mp : modelProperties )
        {
            sb.append( mp ).append( "\r\n" );
        }
        */
        return sb.toString();
    }

    /**
     * Removes duplicate model properties from the containers and return list.
     *
     * @param a container A
     * @param b container B
     * @return list of merged properties
     */
    protected static List<ModelProperty> mergeModelContainers( ModelContainer a, ModelContainer b )
    {
        List<ModelProperty> m = new ArrayList<ModelProperty>();
        m.addAll( a.getProperties() );
        m.addAll( b.getProperties() );

        List<String> combineChildrenUris = new ArrayList<String>();
        for ( ModelProperty mp : m )
        {
            String x = mp.getUri();
            if ( x.endsWith( "#property/combine.children" ) && mp.getResolvedValue().equals( "append" ) )
            {
                combineChildrenUris.add( x.substring( 0, x.length() - 26 ) );
            }
        }

        LinkedList<ModelProperty> processedProperties = new LinkedList<ModelProperty>();
        List<String> uris = new ArrayList<String>();
        String baseUri = a.getProperties().get( 0 ).getUri();
        for ( ModelProperty p : m )
        {
            int modelPropertyLength = p.getUri().length();
            if ( baseUri.length() > modelPropertyLength )
            {
                throw new IllegalArgumentException(
                    "Base URI is longer than model property uri: Base URI = " + baseUri + ", ModelProperty = " + p );
            }

           String subUri = p.getUri().substring( baseUri.length(), modelPropertyLength );
            if ( !uris.contains( p.getUri() ) || ( (subUri.contains( "#collection" ) || subUri.contains("#set")) &&
                (!subUri.endsWith( "#collection" ) && !subUri.endsWith("#set")) && !isParentASet(subUri) && combineChildrenRule(p, combineChildrenUris) )
               )
            {
                    processedProperties.add( findLastIndexOfParent( p, processedProperties ) + 1, p );
                    uris.add( p.getUri() );
            }
            //if parentUri ends in set and uri is contained don't include it
        }
        return processedProperties;
    }

    private static boolean combineChildrenRule(ModelProperty mp,  List<String> combineChildrenUris) {
        return  !combineChildrenUris.contains( mp.getUri() ) || mp.getUri().endsWith( "#property/combine.children" ) ;
    }

    private static boolean isParentASet(String uri)
    {
        String x = uri.replaceAll("#property", "").replaceAll("/combine.children", "");
        String parentUri = (x.lastIndexOf( "/" ) > 0)
                ? x.substring( 0, x.lastIndexOf( "/" ) ) : "";
        return parentUri.endsWith("#set");
    }

    /**
     * Returns list of model properties (from the specified list of model containers) that are not contained in the data
     * source
     *
     * @param modelContainers the model containers (containing model properties) to check for unknown model properties
     * @return list of model properties (from the specified list of model containers) that are not contained in the data
     *         source
     */
    private List<ModelProperty> findUnknownModelPropertiesFrom( List<ModelContainer> modelContainers )
    {
        List<ModelProperty> unknownProperties = new ArrayList<ModelProperty>();
        for ( ModelContainer mc : modelContainers )
        {
            if ( !modelProperties.containsAll( mc.getProperties() ) )
            {
                for ( ModelProperty mp : mc.getProperties() )
                {
                    if ( !modelProperties.contains( mp ) )
                    {
                        unknownProperties.add( mp );
                    }
                }
            }
        }
        return unknownProperties;
    }

    /**
     * Returns the last position of the uri of the specified model property (ModelProperty.getUri) from within the specified
     * list of model properties.
     *
     * @param modelProperty   the model property
     * @param modelProperties the list of model properties used in determining the returned index
     * @return the last position of the uri of the specified model property (ModelProperty.getUri) from within the specified
     *         list of model properties.
     */
    private static int findLastIndexOfParent( ModelProperty modelProperty, List<ModelProperty> modelProperties )
    {
        for ( int i = modelProperties.size() - 1; i >= 0; i-- )
        {
            if ( modelProperties.get( i ).getUri().equals( modelProperty.getUri() ) )
            {
                for ( int j = i; j < modelProperties.size(); j++ )
                {
                    if ( !modelProperties.get( j ).getUri().startsWith( modelProperty.getUri() ) )
                    {
                        return j - 1;
                    }
                }
                return modelProperties.size() - 1;
            }
            else if ( modelProperties.get( i ).isParentOf( modelProperty ) )
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns true if specified list 'a' contains one or more elements of specified list 'b', otherwise returns false.
     *
     * @param a list of model containers
     * @param b list of model containers
     * @return true if specified list 'a' contains one or more elements of specified list 'b', otherwise returns false.
     */
    private static boolean aContainsAnyOfB( List<ModelProperty> a, List<ModelProperty> b )
    {
        for ( ModelProperty mp : b )
        {
            if ( a.contains( mp ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Join or delete event
     */
    private static class DataEvent
    {

        private List<ModelProperty> removedModelProperties;

        private ModelContainer mcA;

        private ModelContainer mcB;

        private String methodName;

        DataEvent( ModelContainer mcA, ModelContainer mcB, List<ModelProperty> removedModelProperties,
                   String methodName )
        {
            this.mcA = mcA;
            this.mcB = mcB;
            this.removedModelProperties = removedModelProperties;
            this.methodName = methodName;
        }

        public ModelContainer getMcA()
        {
            return mcA;
        }

        public ModelContainer getMcB()
        {
            return mcB;
        }

        public List<ModelProperty> getRemovedModelProperties()
        {
            return removedModelProperties;
        }

        public String getMethodName()
        {
            return methodName;
        }

        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append( "Delete Event: " ).append( methodName ).append( "\r\n" );
            sb.append( "Model Container A:\r\n" );
            for ( ModelProperty mp : mcA.getProperties() )
            {
                sb.append( mp ).append( "\r\n" );
            }
            if ( mcB != null )
            {
                sb.append( "Model Container B:\r\n" );
                for ( ModelProperty mp : mcB.getProperties() )
                {
                    sb.append( mp ).append( "\r\n" );
                }
            }

            sb.append( "Removed Properties:\r\n" );
            for ( ModelProperty mp : removedModelProperties )
            {
                sb.append( mp ).append( "\r\n" );
            }
            return sb.toString();
        }
    }

    protected static List<ModelProperty> sort( List<ModelProperty> properties, String baseUri )
    {
        if ( properties == null )
        {
            throw new IllegalArgumentException( "properties" );
        }
        LinkedList<ModelProperty> processedProperties = new LinkedList<ModelProperty>();
        List<String> position = new ArrayList<String>();
        boolean projectIsContained = false;

        for ( ModelProperty p : properties )
        {
            String uri = p.getUri();
            String parentUri = uri.substring( 0, uri.lastIndexOf( "/" ) );

            if ( !projectIsContained && uri.equals( baseUri ) )
            {
                projectIsContained = true;
                processedProperties.add( p );
                position.add( 0, uri );
            }
            else if ( !position.contains( uri ) || parentUri.contains( "#collection" ) || parentUri.contains( "#set" ) )
            {
                int pst = (parentUri.endsWith("#property"))
                        ? (position.indexOf( parentUri.replaceAll("#property", "") ) + 1) : (position.indexOf( parentUri ) + 1);
                processedProperties.add( pst, p );
                position.add( pst, uri );
            }
        }
        return processedProperties;
    }

        private static String findBaseUriFrom(List<ModelProperty> modelProperties)
        {
            String baseUri = null;
            for(ModelProperty mp : modelProperties)
            {
                if(baseUri == null || mp.getUri().length() < baseUri.length())
                {
                    baseUri = mp.getUri();
                }
            }
            return baseUri;
        }
}