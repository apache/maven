package org.apache.maven.project;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * @author jdcasey Created on Feb 1, 2005
 */
public class DefaultProjectDefaultsInjector
implements ProjectDefaultsInjector
{

    public void injectDefaults( MavenProject project )
    {
        injectDependencyDefaults( project.getDependencies(), project.getDependencyManagement() );
    }

    /**
     * Added: Feb 1, 2005 by jdcasey
     */
    private void injectDependencyDefaults( List dependencies, DependencyManagement dependencyManagement )
    {
        if ( dependencyManagement != null )
        {
            // a given project's dependencies should be smaller than the
            // group-defined defaults set...
            // in other words, the project's deps will probably be a subset of
            // those specified in defaults.
            Map depsMap = new TreeMap();
            for ( Iterator it = dependencies.iterator(); it.hasNext(); )
            {
                Dependency dep = (Dependency) it.next();
                depsMap.put( dep.getManagementKey(), dep );
            }

            List dependencyDefaults = dependencyManagement.getDependencies();

            for ( Iterator it = dependencyDefaults.iterator(); it.hasNext(); )
            {
                Dependency def = (Dependency) it.next();
                String key = def.getManagementKey();

                Dependency dep = (Dependency) depsMap.get( key );
                if ( dep != null )
                {
                    mergeWithDefaults( dep, def );
                }
            }
        }
    }

    /**
     * Added: Feb 1, 2005 by jdcasey
     */
    private void mergeWithDefaults( Dependency dep, Dependency def )
    {
        if ( dep.getVersion() == null && def.getVersion() != null )
        {
            dep.setVersion( def.getVersion() );
        }

        Properties props = new Properties( def.getProperties() );
        props.putAll( dep.getProperties() );
        dep.setProperties( props );
    }

}