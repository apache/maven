/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.acm.convert;

import org.apache.maven.acm.model.Model;
import org.apache.maven.acm.model.Environment;

import java.util.Iterator;
import java.util.Properties;
import java.io.FileOutputStream;
import java.io.File;

/**
 * Take an application configuration management model and general
 * properties files, one for each environment.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class PropertiesGenerator
{
    public void generate( Model model, String outputDirectory, String prefix )
        throws Exception
    {
        for ( Iterator i = model.getEnvironments().values().iterator(); i.hasNext(); )
        {
            Environment e = (Environment) i.next();

            Properties p = e.getProperties();

            String filename;

            if ( prefix != null )
            {
                filename = prefix + e.getId() + ".properties"; 
            }
            else
            {
                filename = e.getId() + ".properties";
            }

            File outputFile = new File( outputDirectory, filename );

            FileOutputStream os = new FileOutputStream( outputFile );

            p.store( os, "Properties for the " + e.getId() + " environment." );
        }
    }
}
