package org.apache.maven.acm.util;

import java.util.Map;
import java.io.IOException;
import java.io.Writer;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class MapInterpolationHandler
    implements InterpolationHandler
{
    private Map map;

    public MapInterpolationHandler( Map map )
    {
        this.map = map;
    }

    public void interpolate( String key, Writer out )
        throws IOException
    {
        Object o = map.get( key );

        if ( o == null )
        {
            out.write( key );
        }
        else
        {
            out.write( o.toString() );
        }
    }
}
