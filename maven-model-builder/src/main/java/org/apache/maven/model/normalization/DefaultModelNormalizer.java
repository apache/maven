package org.apache.maven.model.normalization;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBuildingRequest;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.merge.MavenModelMerger;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Handles normalization of a model.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = ModelNormalizer.class )
public class DefaultModelNormalizer
    implements ModelNormalizer
{

    private DuplicateMerger merger = new DuplicateMerger();

    public void mergeDuplicates( Model model, ModelBuildingRequest request )
    {
        Build build = model.getBuild();
        if ( build != null )
        {
            List<Plugin> original = build.getPlugins();
            Map<Object, Plugin> normalized = new LinkedHashMap<Object, Plugin>();

            for ( Plugin plugin : original )
            {
                Object key = plugin.getKey();
                Plugin first = normalized.get( key );
                if ( first != null )
                {
                    merger.mergePlugin( plugin, first );
                }
                normalized.put( key, plugin );
            }

            build.setPlugins( new ArrayList<Plugin>( normalized.values() ) );
        }
    }

    private static class DuplicateMerger
        extends MavenModelMerger
    {

        public void mergePlugin( Plugin target, Plugin source )
        {
            super.mergePlugin( target, source, false, Collections.emptyMap() );
        }

    }

}
