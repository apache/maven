package org.apache.maven.its.it0121.D;

import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;

public class Foo
{
    public String getTimestamp( Locale aLocale )
    {
        DateFormat format = DateFormat.getDateInstance(
                DateFormat.LONG, aLocale );

        return format.format( new Date() );
    }
}
