package org.apache.maven.legacy;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import com.werken.forehead.Forehead;
import com.werken.forehead.ForeheadClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import java.lang.reflect.*;
import java.net.*;

import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultMaven1xIntegration
    extends AbstractLogEnabled
    implements Maven1xIntegration
{
    private String mavenHome;
    private String mavenHomeLocal;


    // ----------------------------------------------------------------------
    // Execution
    // ----------------------------------------------------------------------

    // TODO: may want an executionresponse returned? If so, that may need to be part of another component
    public void execute( File project, List goals )
    {
        // TODO: need real integration
        try
        {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            URL foreheadUrl = new File( mavenHome, "lib/forehead-1.0-beta-5.jar" ).toURL();
            File foreheadConf = new File( mavenHome, "bin/forehead.conf" );
            ClassLoader cl = URLClassLoader.newInstance( new URL[] { foreheadUrl } );
            Class c = Class.forName( "com.werken.forehead.Forehead", true, cl );
            Method m = c.getMethod( "getInstance", new Class[] {} );
            Object forehead = m.invoke( null, new Object[] {} );
            System.setProperty( "maven.home", mavenHome );
            System.setProperty( "tools.jar", "file:" + System.getProperty( "java.home" ) + "/lib/tools.jar" );
            m = c.getMethod( "config", new Class[] { java.io.InputStream.class } );
            m.invoke( forehead, new Object[] { new FileInputStream( foreheadConf ) } );

            System.setProperty( "user.dir", project.getParentFile().getAbsolutePath() );
            /*m = c.getMethod( "getClassLoader", new Class[] { String.class } );
            cl = ( ClassLoader ) m.invoke( forehead, new Object[] { "root.maven" } );
            Thread.currentThread().setContextClassLoader( cl );
            c = Class.forName( "org.apache.maven.cli.App", true, cl );
            Object app = c.newInstance();
            m = c.getMethod( "initialize", new Class[] { String[].class } );
            m.invoke( app, new Object[] { (String[]) goals.toArray( new String[0] ) } );
            Object session = c.getDeclaredField( "mavenSession" ).get( app );
            m = session.getClass().getMethod( "initialize", new Class[0] );
            m.invoke( session, new Object[0] );
            m = session.getClass().getMethod( "getRootProject", new Class[0] );
            Object p = m.invoke( session, new Object[0] );
            m = session.getClass().getMethod( "attainGoals", new Class[] { p.getClass(), List.class } );
            m.invoke( session, new Object[] { p, goals } ); */

            // TODO: this currently system.exit's
            m = c.getMethod( "run", new Class[] { String[].class } );
            m.invoke( forehead, new Object[] { (String[]) goals.toArray( new String[0] ) } );
            Thread.currentThread().setContextClassLoader( oldClassLoader );
        }
        catch ( Exception e )
        {
            // TODO: handle this
            e.printStackTrace();
        }
    }

    public void setMavenHome( String mavenHome )
    {
        this.mavenHome = mavenHome;
    }

    public String getMavenHome()
    {
        return mavenHome;
    }

    public void setMavenHomeLocal( String mavenHomeLocal )
    {
        this.mavenHomeLocal = mavenHomeLocal;
    }

    public String getMavenHomeLocal()
    {
        return mavenHomeLocal;
    }

}

