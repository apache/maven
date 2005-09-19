package org.apache.maven.acm;

import java.util.List;
import java.util.Properties;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class PropertiesComparator
{
    public List compare( Properties source, Properties target )
    {
        List propertiesMissingFromTarget = new ArrayList();

        for ( Iterator i = source.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            if ( target.get( key ) == null )
            {
                propertiesMissingFromTarget.add( key );
            }
        }

        return propertiesMissingFromTarget;
    }

    public void compareReport( File sourceFile, File targetFile )
        throws Exception
    {
        Properties source = new Properties();

        source.load( new FileInputStream( sourceFile ) );

        Properties target = new Properties();

        target.load( new FileInputStream( targetFile ) );

        List diff = compare( source, target );        
    }
}
