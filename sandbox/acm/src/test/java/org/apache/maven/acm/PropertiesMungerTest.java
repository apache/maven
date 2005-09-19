/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.acm;

import junit.framework.TestCase;
import org.apache.maven.acm.convert.PropertiesMunger;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class PropertiesMungerTest
    extends TestCase
{


    public void testPropertiesMunger()
        throws Exception
    {
        String basedir = System.getProperty( "basedir" );

        PropertiesMunger munger = new PropertiesMunger();

        File appSource = new File( basedir, "t3/app" );

        File envSource = new File( basedir, "t3/env" );

        File output = new File( basedir, "target/config" );

        munger.munge( appSource, envSource, output );
    }
}
