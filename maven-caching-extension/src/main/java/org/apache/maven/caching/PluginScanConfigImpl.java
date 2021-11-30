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
package org.apache.maven.caching;

import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.caching.xml.config.DirScanConfig;
import org.apache.maven.caching.xml.config.TagExclude;
import org.apache.maven.caching.xml.config.TagScanConfig;

/**
 * PluginScanConfigImpl
 */
public class PluginScanConfigImpl implements PluginScanConfig
{

    private final DirScanConfig dto;

    public PluginScanConfigImpl( DirScanConfig scanConfig )
    {
        this.dto = scanConfig;
    }

    @Override
    public boolean isSkip()
    {
        return StringUtils.equals( dto.getMode(), "skip" );
    }

    @Override
    public boolean accept( String tagName )
    {
        // include or exclude is a choice element, could be only obe property set

        //noinspection ConstantConditions
        final List<TagScanConfig> includes = dto.getIncludes();
        if ( !includes.isEmpty() )
        {
            return findTagScanProperties( tagName ) != null;
        }

        return !contains( dto.getExcludes(), tagName );
    }

    private boolean contains( List<TagExclude> excludes, String tagName )
    {
        for ( TagExclude exclude : excludes )
        {
            if ( StringUtils.equals( exclude.getTagName(), tagName ) )
            {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public PluginScanConfig mergeWith( final PluginScanConfig overrideConfig )
    {
        if ( dto == null )
        {
            return overrideConfig;
        }

        final DirScanConfig override = overrideConfig.dto();
        if ( override == null )
        {
            return this;
        }

        if ( override.isIgnoreParent() )
        {
            return overrideConfig;
        }

        DirScanConfig merged = new DirScanConfig();
        if ( override.getMode() != null )
        {
            merged.setMode( override.getMode() );
        }
        else
        {
            merged.setMode( dto.getMode() );
        }

        merged.getExcludes().addAll( dto.getExcludes() );
        merged.getExcludes().addAll( override.getExcludes() );

        merged.getIncludes().addAll( dto.getIncludes() );
        merged.getIncludes().addAll( override.getIncludes() );

        return new PluginScanConfigImpl( merged );
    }

    @Nonnull
    public ScanConfigProperties getTagScanProperties( String tagName )
    {
        ScanConfigProperties scanProperties = findTagScanProperties( tagName );
        return scanProperties != null ? scanProperties : defaultScanConfig();
    }

    @Override
    public DirScanConfig dto()
    {
        return dto;
    }

    private ScanConfigProperties findTagScanProperties( String tagName )
    {
        ScanConfigProperties scanConfigProperties = findConfigByName( tagName, dto.getIncludes() );
        if ( scanConfigProperties == null )
        {
            scanConfigProperties = findConfigByName( tagName, dto.getTagScanConfigs() );
        }
        return scanConfigProperties;
    }

    private ScanConfigProperties findConfigByName( String tagName, List<TagScanConfig> configs )
    {
        if ( configs == null )
        {
            return null;
        }

        for ( TagScanConfig config : configs )
        {
            if ( StringUtils.equals( tagName, config.getTagName() ) )
            {
                return new ScanConfigProperties( config.isRecursive(), config.getGlob() );
            }
        }
        return null;
    }

    private static ScanConfigProperties defaultScanConfig()
    {
        return new ScanConfigProperties( true, null );
    }
}
