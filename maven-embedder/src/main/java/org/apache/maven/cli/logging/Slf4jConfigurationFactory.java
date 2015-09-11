package org.apache.maven.cli.logging;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.cli.logging.impl.UnsupportedSlf4jBindingConfiguration;
import org.codehaus.plexus.util.PropertyUtils;
import org.slf4j.ILoggerFactory;

/**
 * Slf4jConfiguration factory, loading implementations from <code>META-INF/maven/slf4j-configuration.properties</code>
 * configuration files in class loader: key is the class name of the ILoggerFactory, value is the class name of
 * the corresponding Slf4jConfiguration.
 *
 * @author Hervé Boutemy
 * @since 3.1.0
 */
public class Slf4jConfigurationFactory
{
    public static final String RESOURCE = "META-INF/maven/slf4j-configuration.properties";

    public static Slf4jConfiguration getConfiguration( ILoggerFactory loggerFactory )
    {
        Map<URL, Set<Object>> supported = new LinkedHashMap<>();

        String slf4jBinding = loggerFactory.getClass().getCanonicalName();

        try
        {
            Enumeration<URL> resources = Slf4jConfigurationFactory.class.getClassLoader().getResources( RESOURCE );

            while ( resources.hasMoreElements() )
            {
                URL resource = resources.nextElement();

                Properties conf = PropertyUtils.loadProperties( resource.openStream() );

                String impl = conf.getProperty( slf4jBinding );

                if ( impl != null )
                {
                    return (Slf4jConfiguration) Class.forName( impl ).newInstance();
                }

                supported.put( resource, conf.keySet() );
            }
        }
        catch ( IOException | ClassNotFoundException | IllegalAccessException | InstantiationException e )
        {
            e.printStackTrace();
        }

        return new UnsupportedSlf4jBindingConfiguration( slf4jBinding, supported );
    }
}
