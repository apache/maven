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

package org.apache.maven.toolchain.java;

import java.io.File;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.RequirementMatcherFactory;
import org.apache.maven.toolchain.ToolchainFactory;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 *
 * @author mkleint
 */
public class DefaultJavaToolchainFactory
    implements ToolchainFactory, LogEnabled
{

    private Logger logger;

    public DefaultJavaToolchainFactory( )
    {
    }
    
    public ToolchainPrivate createToolchain( ToolchainModel model )
        throws MisconfiguredToolchainException
    {
        if (model == null) {
            return null;
        }
        DefaultJavaToolChain jtc = new DefaultJavaToolChain( model , logger);
        Xpp3Dom dom = (Xpp3Dom) model.getConfiguration();
        Xpp3Dom javahome = dom.getChild( DefaultJavaToolChain.KEY_JAVAHOME );
        if ( javahome == null )
        {
            throw new MisconfiguredToolchainException( "Java toolchain without the " + DefaultJavaToolChain.KEY_JAVAHOME + " configuration element." );
        }
        File normal = new File( FileUtils.normalize( javahome.getValue() ) );
        if ( normal.exists() )
        {
            jtc.setJavaHome( FileUtils.normalize( javahome.getValue() ) );
        }
        else
        {
            throw new MisconfiguredToolchainException( "Non-existing JDK home configuration at " + normal.getAbsolutePath(  ) );
        }

        //now populate the provides section.
        //TODO possibly move at least parts to a utility method or abstract implementation.
        dom = (Xpp3Dom) model.getProvides();
        Xpp3Dom[] provides = dom.getChildren();
        for ( int i = 0; i < provides.length; i++ )
        {
            String key = provides[i].getName();
            String value = provides[i].getValue();
            if ( value == null )
            {
                throw new MisconfiguredToolchainException( "Provides token '" + key + "' doesn't have any value configured." );
            }
            if ( "version".equals( key ) )
            {
                jtc.addProvideToken( key,
                    RequirementMatcherFactory.createVersionMatcher( value ) );
            }
            else
            {
                jtc.addProvideToken( key,
                    RequirementMatcherFactory.createExactMatcher( value ) );
            }
        }
        return jtc;
    }

    public ToolchainPrivate createDefaultToolchain()
    {
        //not sure it's necessary to provide a default toolchain here.
        //only version can be eventually supplied, and 
        return null;
    }
    
    protected Logger getLogger()
    {
        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }
    
}