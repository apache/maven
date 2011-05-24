package org.apache.maven.artifact.router.io;

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

import org.apache.maven.artifact.router.ArtifactRouter;
import org.apache.maven.artifact.router.GroupPattern;
import org.apache.maven.artifact.router.GroupRoute;
import org.apache.maven.artifact.router.MirrorRoute;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArtifactRouteSerializer
{

    // NOTE: Gson is supposed to be threadsafe, so all this static stuff should be fine.
    private static Gson gson;

    public static void serializeMirrors( final Set<MirrorRoute> mirrors, final Writer writer )
        throws ArtifactRouterModelException
    {
        try
        {
            getGson().toJson( mirrors, writer );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot write mirrors to router format.", e );
        }
    }

    public static String serializeMirrorsToString( final Set<MirrorRoute> mirrors )
        throws ArtifactRouterModelException
    {
        try
        {
            return getGson().toJson( mirrors );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot write mirrors to router format.", e );
        }
    }

    public static Set<MirrorRoute> deserializeMirrors( final Reader reader )
        throws ArtifactRouterModelException
    {
        try
        {
            return getGson().fromJson( reader, RepositoryMirrorSetCreator.getType() );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot read mirrors from router format.", e );
        }
    }

    public static Set<MirrorRoute> deserializeMirrors( final String source )
        throws ArtifactRouterModelException
    {
        try
        {
            return getGson().fromJson( source, RepositoryMirrorSetCreator.getType() );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot read mirrors from router format.", e );
        }
    }

    public static void serializeGroups( final Set<GroupRoute> groups, final Writer writer )
        throws ArtifactRouterModelException
    {
        try
        {
            getGson().toJson( groups, writer );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot write groups to router format.", e );
        }
    }

    public static String serializeGroupsToString( final Set<GroupRoute> groups )
        throws ArtifactRouterModelException
    {
        try
        {
            return getGson().toJson( groups );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot write groups to router format.", e );
        }
    }

    public static Set<GroupRoute> deserializeGroups( final Reader reader )
        throws ArtifactRouterModelException
    {
        try
        {
            return getGson().fromJson( reader, GroupRouteSetCreator.getType() );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot read groups from router format.", e );
        }
    }

    public static Set<GroupRoute> deserializeGroups( final String source )
        throws ArtifactRouterModelException
    {
        try
        {
            return getGson().fromJson( source, GroupRouteSetCreator.getType() );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot read groups from router format.", e );
        }
    }

    public static void serialize( final ArtifactRouter tables, final Writer writer )
        throws ArtifactRouterModelException
    {
        try
        {
            getGson().toJson( tables, writer );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot write routing tables.", e );
        }
    }

    public static String serializeToString( final ArtifactRouter tables )
        throws ArtifactRouterModelException
    {
        try
        {
            return getGson().toJson( tables );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot write routing tables.", e );
        }
    }

    public static ArtifactRouter deserialize( final Reader reader )
        throws ArtifactRouterModelException
    {
        try
        {
            return getGson().fromJson( reader, ArtifactRouter.class );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot read routing tables.", e );
        }
    }

    public static ArtifactRouter deserialize( final String source )
        throws ArtifactRouterModelException
    {
        try
        {
            return getGson().fromJson( source, ArtifactRouter.class );
        }
        catch ( final JsonParseException e )
        {
            throw new ArtifactRouterModelException( "Cannot read routing tables.", e );
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
            builder.registerTypeAdapter( GroupRouteMapCreator.getType(), new GroupRouteMapCreator() );
            builder.registerTypeAdapter( GroupRouteSetCreator.getType(), new GroupRouteSetCreator() );
            builder.registerTypeAdapter( GroupPattern.class, new GroupPatternSerializer() );

            gson = builder.create();
        }

        return gson;
    }

    public static final class GroupRouteMapCreator
        implements InstanceCreator<Map<GroupPattern, GroupRoute>>
    {

        public Map<GroupPattern, GroupRoute> createInstance( Type type )
        {
            return new HashMap<GroupPattern, GroupRoute>();
        }

        public static Type getType()
        {
            return new TypeToken<Map<GroupPattern, GroupRoute>>()
            {
            }.getType();
        }

    }

    public static final class GroupRouteSetCreator
        implements InstanceCreator<Set<GroupRoute>>
    {

        public Set<GroupRoute> createInstance( Type type )
        {
            return new HashSet<GroupRoute>();
        }

        public static Type getType()
        {
            return new TypeToken<Set<GroupRoute>>()
            {
            }.getType();
        }

    }

    public static final class GroupPatternListCreator
        implements InstanceCreator<List<GroupPattern>>
    {

        public List<GroupPattern> createInstance( Type type )
        {
            return new ArrayList<GroupPattern>();
        }

        public static Type getType()
        {
            return new TypeToken<List<GroupPattern>>()
            {
            }.getType();
        }

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

    public static final class GroupPatternSerializer
        implements JsonSerializer<GroupPattern>, JsonDeserializer<GroupPattern>
    {

        public GroupPattern deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context )
            throws JsonParseException
        {
            String pattern = json.getAsString();
            return new GroupPattern( pattern );
        }

        public JsonElement serialize( GroupPattern src, Type typeOfSrc, JsonSerializationContext context )
        {
            return new JsonPrimitive( src.getPattern() );
        }

    }

}
