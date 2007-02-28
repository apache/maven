package org.apache.maven.usability;

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

import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginParameterException;
import org.apache.maven.usability.diagnostics.DiagnosisUtils;
import org.apache.maven.usability.diagnostics.ErrorDiagnoser;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;

public class PluginConfigurationDiagnoser
    implements ErrorDiagnoser
{
    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, PluginConfigurationException.class );
    }

    public String diagnose( Throwable error )
    {
        PluginConfigurationException pce =
            (PluginConfigurationException) DiagnosisUtils.getFromCausality( error, PluginConfigurationException.class );

        if ( pce instanceof PluginParameterException )
        {
            PluginParameterException exception = (PluginParameterException) pce;

            return exception.buildDiagnosticMessage();
        }
        else if ( DiagnosisUtils.containsInCausality( pce, ComponentConfigurationException.class ) )
        {
            ComponentConfigurationException cce = (ComponentConfigurationException) DiagnosisUtils.getFromCausality(
                pce, ComponentConfigurationException.class );

            return pce.buildConfigurationDiagnosticMessage( cce );
        }
        else
        {
            return pce.getMessage();
        }
    }
}
