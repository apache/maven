package org.apache.maven.toolchain.java;

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

import java.io.File;
import java.util.Map.Entry;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.RequirementMatcher;
import org.apache.maven.toolchain.RequirementMatcherFactory;
import org.apache.maven.toolchain.ToolchainFactory;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * JDK toolchain factory.
 * This is a <code>ToolchainFactory</code> Plexus component registered with
 * <code>jdk</code> hint.
 *
 * @author mkleint
 * @since 2.0.9, renamed from <code>DefaultJavaToolchainFactory</code> in 3.2.4
 */
@Named( "jdk" )
@Singleton
public class JavaToolchainFactory
    implements ToolchainFactory
{

    @Inject
    private Logger logger;

    public ToolchainPrivate createToolchain( ToolchainModel model )
        throws MisconfiguredToolchainException
    {
        if ( model == null )
        {
            return null;
        }

        // use DefaultJavaToolChain for compatibility with maven 3.2.3 and earlier

        @SuppressWarnings( "deprecation" )
        JavaToolchainImpl jtc = new DefaultJavaToolChain( model, logger );

        // populate the provides section
        Properties provides = model.getProvides();
        for ( Entry<Object, Object> provide : provides.entrySet() )
        {
            String key = (String) provide.getKey();
            String value = (String) provide.getValue();

            if ( value == null )
            {
                throw new MisconfiguredToolchainException(
                    "Provides token '" + key + "' doesn't have any value configured." );
            }

            RequirementMatcher matcher;
            if ( "version".equals( key ) )
            {
                matcher = RequirementMatcherFactory.createVersionMatcher( value );
            }
            else
            {
                matcher = RequirementMatcherFactory.createExactMatcher( value );
            }

            jtc.addProvideToken( key, matcher );
        }

        // populate the configuration section
        Xpp3Dom dom = (Xpp3Dom) model.getConfiguration();
        Xpp3Dom javahome = dom.getChild( JavaToolchainImpl.KEY_JAVAHOME );
        if ( javahome == null )
        {
            throw new MisconfiguredToolchainException( "Java toolchain without the "
                + JavaToolchainImpl.KEY_JAVAHOME + " configuration element." );
        }
        File normal = new File( FileUtils.normalize( javahome.getValue() ) );
        if ( normal.exists() )
        {
            jtc.setJavaHome( FileUtils.normalize( javahome.getValue() ) );
        }
        else
        {
            throw new MisconfiguredToolchainException( "Non-existing JDK home configuration at "
                + normal.getAbsolutePath() );
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

}