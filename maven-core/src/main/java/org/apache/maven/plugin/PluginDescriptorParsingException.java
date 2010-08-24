package org.apache.maven.plugin;

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

import org.apache.maven.model.Plugin;

/**
 * @author Jason van Zyl
 */
public class PluginDescriptorParsingException
    extends Exception
{

    public PluginDescriptorParsingException( Plugin plugin, String descriptorLocation, Throwable e )
    {
        super( createMessage( plugin, descriptorLocation, e ), e );
    }

    private static String createMessage( Plugin plugin, String descriptorLocation, Throwable e )
    {
        String message = "Failed to parse plugin descriptor";

        if ( plugin != null )
        {
            message += " for " + plugin.getId();
        }

        if ( descriptorLocation != null )
        {
            message += " (" + descriptorLocation + ")";
        }

        if ( e != null )
        {
            message += ": " + e.getMessage();
        }

        return message;
    }

}
