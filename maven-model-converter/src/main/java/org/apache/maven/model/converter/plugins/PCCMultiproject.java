package org.apache.maven.model.converter.plugins;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.model.Model;
import org.apache.maven.model.converter.ProjectConverterException;

import java.util.Properties;

/**
 * @plexus.component role="org.apache.maven.model.converter.plugins.PluginConfigurationConverter" role-hint="multiproject"
 *
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class PCCMultiproject
    implements PluginConfigurationConverter
{

    /**
     * @see org.apache.maven.model.converter.plugins.PluginConfigurationConverter#convertConfiguration(org.apache.maven.model.Model, org.apache.maven.model.v3_0_0.Model, java.util.Properties)
     */
    public void convertConfiguration( Model v4Model, org.apache.maven.model.v3_0_0.Model v3Model,
                                      Properties projectProperties )
        throws ProjectConverterException
    {
        String projectType = projectProperties.getProperty( "maven.multiproject.type" );

        if ( projectType != null )
        {
            v4Model.setPackaging( projectType );
        }
    }

}
