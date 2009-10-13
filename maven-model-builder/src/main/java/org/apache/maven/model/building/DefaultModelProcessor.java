package org.apache.maven.model.building;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Component(role = ModelProcessor.class)
public class DefaultModelProcessor
    implements ModelProcessor
{
    public File locatePom( File projectDirectory )
    {
        return new File( projectDirectory, "pom.xml" );
    }

    public Model read( File input, Map<String, ?> options )
        throws IOException
    {
        if ( input == null )
        {
            throw new IllegalArgumentException( "input file missing" );
        }

        Model model = read( ReaderFactory.newXmlReader( input ), options );

        model.setPomFile( input );

        return model;
    }

    public Model read( Reader input, Map<String, ?> options )
        throws IOException
    {
        if ( input == null )
        {
            throw new IllegalArgumentException( "input reader missing" );
        }

        try
        {
            MavenXpp3Reader r = new MavenXpp3Reader();
            return r.read( input, isStrict( options ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new ModelParseException( e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e );
        }
        finally
        {
            IOUtil.close( input );
        }
    }

    public Model read( InputStream input, Map<String, ?> options )
        throws IOException
    {
        if ( input == null )
        {
            throw new IllegalArgumentException( "input stream missing" );
        }

        try
        {
            MavenXpp3Reader r = new MavenXpp3Reader();
            return r.read( input, isStrict( options ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new ModelParseException( e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e );
        }
        finally
        {
            IOUtil.close( input );
        }
    }

    private boolean isStrict( Map<String, ?> options )
    {
        Object value = ( options != null ) ? options.get( IS_STRICT ) : null;
        return value == null || Boolean.parseBoolean( value.toString() );
    }
}
