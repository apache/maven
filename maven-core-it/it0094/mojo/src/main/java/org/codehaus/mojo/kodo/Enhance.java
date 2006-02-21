/*
 *  Copyright 2005-2006 Brian Fox (brianefox@gmail.com)
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

package org.codehaus.mojo.kodo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.codehaus.classworlds.ClassRealm;

import javax.xml.parsers.SAXParserFactory;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Goal that enhances persistant classes
 *
 * @requiresDependancyResolution test
 * @goal enhance
 * @phase compile
 */
public class Enhance
    extends AbstractMojo

{
    public Enhance()
    {
        super();
    }

    public void execute()
        throws MojoExecutionException
    {
        printClassPath();

        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        System.out.println( originalLoader.getClass() );

        setupClassloader();
        originalLoader = Thread.currentThread().getContextClassLoader();
        System.out.println( originalLoader.getClass() );

        SAXParserFactoryImpl spi = new SAXParserFactoryImpl();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        this.getLog().info( spf.toString() );
        String t = "org/apache/xerces/jaxp/SAXParserFactoryImpl.class";
        this.getLog().info( t );
        URL url = originalLoader.getResource( t );
        //URL url = spf.getClass().getClassLoader().getResource("javax/xml/parsers/SAXParserFactory.class");
        this.getLog().info( "Loaded from: " + url.toString() );

    }

    /**
     * Adds nessessary items to the classloader.
     *
     * @return ClassLoader original Classloader.
     * @throws MojoExecutionException
     */
    public ClassLoader setupClassloader()
        throws MojoExecutionException
    {

        URLClassLoader loader = null;
        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        this.getLog().info( "orig classloader:" );
        printURLClassPath( Thread.currentThread().getContextClassLoader(), "" );
        URL[] urls = new URL[0];
        loader = new URLClassLoader( urls, originalLoader );

        Thread.currentThread().setContextClassLoader( loader );
        this.getLog().info( "new classloader:" );
        printURLClassPath( Thread.currentThread().getContextClassLoader(), "" );
        return originalLoader;

    }

    public void printURLClassPath( ClassLoader sysClassLoader, String s )
        throws MojoExecutionException
    {
        //Get the Classloader
        //Get the URLs
        URL[] urls;
        if ( sysClassLoader instanceof URLClassLoader )
        {
            urls = ( (URLClassLoader) sysClassLoader ).getURLs();
        }
        else
        {
            try
            {
                Field f = sysClassLoader.getClass().getDeclaredField( "realm" );
                f.setAccessible( true );
                ClassRealm r = (ClassRealm) f.get( sysClassLoader );
                urls = r.getConstituents();
            }
            catch ( NoSuchFieldException e )
            {
                throw new MojoExecutionException( "mee ", e );
            }
            catch ( IllegalAccessException e )
            {
                throw new MojoExecutionException( "mee ", e );
            }
        }
        for ( int i = 0; i < urls.length; i++ )
        {
            this.getLog().info( s + urls[i].getFile() );
        }

        if ( sysClassLoader.getParent() != null )
        {
            printURLClassPath( sysClassLoader.getParent(), s + "  " );
        }
    }

    public void printClassPath()
    {
        ClassLoader sysClassLoader = Thread.currentThread().getContextClassLoader();
        URL[] urls = null;
        Field field;
        try
        {

            field = sysClassLoader.getClass().getDeclaredField( "realm" );
            field.setAccessible( true );
            ClassRealm realm = (ClassRealm) field.get( sysClassLoader );

            urls = realm.getConstituents();
        }
        catch ( SecurityException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( NoSuchFieldException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IllegalArgumentException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IllegalAccessException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //URL[] urls = ( (URLClassLoader) sysClassLoader ).getURLs();
        this.getLog().info( "Initial Classpath:" );
        for ( int i = 0; i < urls.length; i++ )
        {
            this.getLog().info( urls[i].getFile() );
        }
    }
}
