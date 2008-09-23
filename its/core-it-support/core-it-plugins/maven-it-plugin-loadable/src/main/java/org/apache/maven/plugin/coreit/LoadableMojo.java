package org.apache.maven.plugin.coreit;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal loadable
 * @requiresDependencyResolution test
 */
public class LoadableMojo
    extends AbstractMojo
{
    /**
     * @parameter
     * @required
     */
    private String className;

    public void execute() throws MojoFailureException
    {
        if ( !load( true ) || !load( false ) )
        {
            throw new MojoFailureException( this, "Class-loading test failed..", "Failed to load class: " + className + " using one or more methods." );
        }
    }
    
    private boolean load( boolean useContextClassloader ) throws MojoFailureException
    {
        getLog().info( "Executing in java version: " + System.getProperty( "java.version" ) );
        
        ClassLoader cl;
        if ( useContextClassloader )
        {
            cl = Thread.currentThread().getContextClassLoader();
        }
        else
        {
            cl = this.getClass().getClassLoader();
        }

        getLog().info( "Attepting to load: " + className + " from: " + cl + (useContextClassloader ? " (context classloader)" : "" ) );
        
        try
        {
            Class result = cl.loadClass( className );
            
            getLog().info( "Load succeeded." );
            
            return true;
        }
        catch ( ClassNotFoundException e )
        {
            getLog().info( "Failed to load class: " + className
                + (useContextClassloader ? " using context classloader" : "") );
            
            return false;
        }
    }
}
