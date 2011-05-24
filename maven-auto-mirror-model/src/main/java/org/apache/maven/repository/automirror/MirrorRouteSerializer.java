package org.apache.maven.repository.automirror;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MirrorRouteSerializer
{

    // NOTE: Gson is supposed to be threadsafe, so all this static stuff should be fine.
    private static Gson gson;

    public static void serializeList( final List<MirrorRoute> mirrors, final Writer writer )
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

    public static String serializeListToString( final List<MirrorRoute> mirrors )
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

    public static List<MirrorRoute> deserializeList( final Reader reader )
        throws MirrorRouterModelException
    {
        try
        {
            return getGson().fromJson( reader, RepositoryMirrorListCreator.getType() );
        }
        catch ( final JsonParseException e )
        {
            throw new MirrorRouterModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static List<MirrorRoute> deserializeList( final String source )
        throws MirrorRouterModelException
    {
        try
        {
            return getGson().fromJson( source, RepositoryMirrorListCreator.getType() );
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

}
