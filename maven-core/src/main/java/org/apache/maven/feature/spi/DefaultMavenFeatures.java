package org.apache.maven.feature.spi;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.feature.api.MavenFeatureContext;
import org.apache.maven.feature.api.MavenFeatures;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 * <p>
 * Feature flags for experiments. There is no partial opt-in, you get all the feature that are in turned on in the
 * current version of Maven if you activate the experiments extension and none of the features if you don't.
 *
 * <h2>How we use this</h2>
 * <ul>
 * <li>When starting work on a feature, add a constant string to this class to hold the feature name.</li>
 * <li>When ready to expose the experiment, add the feature name to {@code META-INF/plexus/components.xml}.</li>
 * <li>When the experiment has been concluded, remove the feature name and collapse whatever branch logic was based
 *  * on the feature flag.</li>
 * </ul>
 */
@Component( role = MavenFeatures.class, hint = "default" )
public class DefaultMavenFeatures
    implements MavenFeatures
{
    /**
     * The feature name of dynamic phases.
     */
    public static final String DYNAMIC_PHASES = "dynamic-phases";

    @Requirement
    private Logger log;

    /**
     * The contexts that are enabled.
     */
    private final Map<MavenFeatureContext, Boolean> enabled = new WeakHashMap<>();

    /**
     * The current experimental features being exposed to opt-in builds.
     */
    private Set<String> features;

    public DefaultMavenFeatures()
    {
        this.features = Collections.<String>emptySet();
    }

    public List<String> getFeatures()
    {
        return features == null ? Collections.<String>emptyList() : new ArrayList<String>( features );
    }

    public void setFeatures( List<String> features )
    {
        this.features = features == null ? Collections.<String>emptySet() : new HashSet<String>( features );
    }

    /**
     * Enabled the feature context. This method is only to be invoked by {@code MavenExperimentEnabler}.
     *
     * @param context the context to enable.
     * @throws MavenExecutionException if we detect illegal usage.
     *                                 {@code MavenExperimentEnabler}.
     */
    public void enable( MavenFeatureContext context )
        throws MavenExecutionException
    {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for ( StackTraceElement element : Thread.currentThread().getStackTrace() )
        {
            if ( "org.apache.maven.feature.check.MavenExperimentEnabler".equals( element.getClassName() ) )
            {
                enabled.put( context, Boolean.TRUE );
                log.info( "Experimental features enabled:" );
                for ( String feature: new TreeSet<>( features ) )
                {
                    log.info( "  * " + feature );
                }
                return;
            }
        }
        throw new MavenExecutionException( "Detected illegal attempt to bypass experimental feature activation",
                                           (File) null );
    }

    @Override
    public boolean enabled( MavenFeatureContext context, String featureName )
    {
        if ( Boolean.TRUE.equals( enabled.get( context ) ) )
        {
            return features != null && features.contains( featureName );
        }
        else
        {
            return false;
        }
    }
}
