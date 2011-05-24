package org.apache.maven.repository.automirror;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MirrorRouteSerializer
{

    // NOTE: Gson is supposed to be threadsafe, so all this static stuff should be fine.
    private static Gson gson;

    public static void serializeLoose( final Set<MirrorRoute> mirrors, final Writer writer )
        throws MirrorRouterModelException
    {
        try
        {
            getGson().toJson( mirrors, writer );
        }
        catch ( final JsonParseException e )
        {
            throw new MirrorRouterModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static String serializeLooseToString( final Set<MirrorRoute> mirrors )
        throws MirrorRouterModelException
    {
        try
        {
            return getGson().toJson( mirrors );
        }
        catch ( final JsonParseException e )
        {
            throw new MirrorRouterModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static Set<MirrorRoute> deserializeLoose( final Reader reader )
        throws MirrorRouterModelException
    {
        try
        {
            return getGson().fromJson( reader, RepositoryMirrorSetCreator.getType() );
        }
        catch ( final JsonParseException e )
        {
            throw new MirrorRouterModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static Set<MirrorRoute> deserializeLoose( final String source )
        throws MirrorRouterModelException
    {
        try
        {
            return getGson().fromJson( source, RepositoryMirrorSetCreator.getType() );
        }
        catch ( final JsonParseException e )
        {
            throw new MirrorRouterModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static void serialize( final MirrorRoutingTable mirrors, final Writer writer )
        throws MirrorRouterModelException
    {
        try
        {
            getGson().toJson( mirrors, writer );
        }
        catch ( final JsonParseException e )
        {
            throw new MirrorRouterModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static String serializeToString( final MirrorRoutingTable mirrors )
        throws MirrorRouterModelException
    {
        try
        {
            return getGson().toJson( mirrors );
        }
        catch ( final JsonParseException e )
        {
            throw new MirrorRouterModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static MirrorRoutingTable deserialize( final Reader reader )
        throws MirrorRouterModelException
    {
        try
        {
            return getGson().fromJson( reader, MirrorRoutingTable.class );
        }
        catch ( final JsonParseException e )
        {
            throw new MirrorRouterModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static MirrorRoutingTable deserialize( final String source )
        throws MirrorRouterModelException
    {
        try
        {
            return getGson().fromJson( source, MirrorRoutingTable.class );
        }
        catch ( final JsonParseException e )
        {
            throw new MirrorRouterModelException( "Cannot read router-mirrors.", e );
        }
    }

    private static Gson getGson()
    {
        if ( gson == null )
        {
            final GsonBuilder builder = new GsonBuilder();
            builder.disableHtmlEscaping().disableInnerClassSerialization().setPrettyPrinting();
            builder.registerTypeAdapter( RepositoryMirrorSetCreator.getType(), new RepositoryMirrorSetCreator() );
            builder.registerTypeAdapter( RepositoryMirrorListCreator.getType(), new RepositoryMirrorListCreator() );

            gson = builder.create();
        }

        return gson;
    }

    public static final class RepositoryMirrorListCreator
        implements InstanceCreator<List<MirrorRoute>>
    {

        public List<MirrorRoute> createInstance( final Type type )
        {
            return new ArrayList<MirrorRoute>();
        }

        public static Type getType()
        {
            return new TypeToken<List<MirrorRoute>>()
            {
            }.getType();
        }

    }

    public static final class RepositoryMirrorSetCreator
        implements InstanceCreator<Set<MirrorRoute>>
    {

        public Set<MirrorRoute> createInstance( final Type type )
        {
            return new LinkedHashSet<MirrorRoute>();
        }

        public static Type getType()
        {
            return new TypeToken<Set<MirrorRoute>>()
            {
            }.getType();
        }

    }

}
