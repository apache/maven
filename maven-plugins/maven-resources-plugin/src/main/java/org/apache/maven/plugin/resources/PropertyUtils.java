package org.apache.maven.plugin.resources;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;


/**
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @version $Id$
 */
public final class PropertyUtils
{
    private PropertyUtils()
    {
        // prevent instantiation
    }

    /**
     * Reads a property file, resolving all internal variables.
     *
     * @param propfile The property file to load
     * @param fail wheter to throw an exception when the file cannot be loaded or to return null
     * @param useSystemProps wheter to incorporate System.getProperties settings into the returned Properties object.
     * @return the loaded and fully resolved Properties object
     */
    public static Properties loadPropertyFile( File propfile, boolean fail, boolean useSystemProps )
        throws IOException
    {
        Properties props = new Properties();

        if ( useSystemProps )
        {
            props = new Properties( System.getProperties() );
        }

        if ( propfile.exists() )
        {
            FileInputStream inStream = new FileInputStream( propfile );
            try
            {
                props.load( inStream );
            }
            finally
            {
                IOUtil.close( inStream );
            }
        }
        else if ( fail )
        {
            throw new FileNotFoundException( propfile.toString() );
        }

        for ( Enumeration n = props.propertyNames(); n.hasMoreElements(); )
        {
            String k = (String) n.nextElement();
            props.setProperty( k, getPropertyValue( k, props ) );
        }

        return props;
    }


    /**
     * Retrieves a property value, replacing values like ${token}
     * using the Properties to look them up.
     *
     * It will leave unresolved properties alone, trying for System
     * properties, and implements reparsing (in the case that
     * the value of a property contains a key), and will
     * not loop endlessly on a pair like
     * test = ${test}.
     */
    private static String getPropertyValue( String k, Properties p )
    {
        // This can also be done using InterpolationFilterReader,
        // but it requires reparsing the file over and over until
        // it doesn't change.

        String v = p.getProperty( k );
        String ret = "";
        int idx, idx2;

        while ( ( idx = v.indexOf( "${" ) ) >= 0 )
        {
            // append prefix to result
            ret += v.substring( 0, idx );

            // strip prefix from original
            v = v.substring( idx + 2 );

            // if no matching } then bail
            if ( ( idx2 = v.indexOf( '}' ) ) < 0 )
            {
                break;
            }

            // strip out the key and resolve it
            // resolve the key/value for the ${statement}
            String nk = v.substring( 0, idx2 );
            v = v.substring( idx2 + 1 );
            String nv = p.getProperty( nk );

            // try global environment..
            if ( nv == null )
            {
                nv = System.getProperty( nk );
            }

            // if the key cannot be resolved,
            // leave it alone ( and don't parse again )
            // else prefix the original string with the
            // resolved property ( so it can be parsed further )
            // taking recursion into account.
            if ( nv == null || nv.equals( k ) )
            {
                ret += "${" + nk + "}";
            }
            else
            {
                v = nv + v;
            }
        }
        return ret + v;
    }
}
