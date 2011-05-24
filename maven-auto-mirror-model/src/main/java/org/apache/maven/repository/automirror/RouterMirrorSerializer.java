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

public class RouterMirrorSerializer
{

    // NOTE: Gson is supposed to be threadsafe, so all this static stuff should be fine.
    private static Gson gson;

    public static void serialize( final RouterMirrors mirrors, final Writer writer )
        throws RouterMirrorModelException
    {
        try
        {
            getGson().toJson( mirrors, writer );
        }
        catch ( final JsonParseException e )
        {
            throw new RouterMirrorModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static String serializeToString( final RouterMirrors mirrors )
        throws RouterMirrorModelException
    {
        try
        {
            return getGson().toJson( mirrors );
        }
        catch ( final JsonParseException e )
        {
            throw new RouterMirrorModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static RouterMirrors deserialize( final Reader reader )
        throws RouterMirrorModelException
    {
        try
        {
            return getGson().fromJson( reader, RouterMirrors.class );
        }
        catch ( final JsonParseException e )
        {
            throw new RouterMirrorModelException( "Cannot read router-mirrors.", e );
        }
    }

    public static RouterMirrors deserialize( final String source )
        throws RouterMirrorModelException
    {
        try
        {
            return getGson().fromJson( source, RouterMirrors.class );
        }
        catch ( final JsonParseException e )
        {
            throw new RouterMirrorModelException( "Cannot read router-mirrors.", e );
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
        implements InstanceCreator<List<RouterMirror>>
    {

        public List<RouterMirror> createInstance( final Type type )
        {
            return new ArrayList<RouterMirror>();
        }

        public static Type getType()
        {
            return new TypeToken<List<RouterMirror>>()
            {
            }.getType();
        }

    }

}
