package org.apache.maven.project.interpolation;

import org.codehaus.plexus.interpolation.ValueSource;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BuildTimestampValueSource
    implements ValueSource
{

    private String formattedDate;

    public BuildTimestampValueSource( Date startTime, String format )
    {
        if ( startTime != null )
        {
            formattedDate = new SimpleDateFormat( format ).format( startTime );
        }
    }

    public Object getValue( String expression )
    {
        if ( "build.timestamp".equals( expression ) || "maven.build.timestamp".equals( expression ) )
        {
            return formattedDate;
        }

        return null;
    }

}
