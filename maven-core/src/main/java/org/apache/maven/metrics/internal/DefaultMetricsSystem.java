package org.apache.maven.metrics.internal;

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

import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.metrics.MetricsContext;
import org.apache.maven.metrics.MetricsProvider;
import org.apache.maven.metrics.MetricsSystem;
import org.apache.maven.metrics.impl.NullMetricsProvider;

/**
 * Default Implementation of Metrics System Runtime.
 * Implementations are supposed to be configured as Maven Extensions.
 * @author Enrico Olivelli
 */
@Singleton
@Named( MetricsSystem.HINT )
public class DefaultMetricsSystem extends MetricsSystem
{

    @Override
    public MetricsContext getMetricsContext()
    {
        return NullMetricsProvider.INSTANCE.getRootContext();
    }

    @Override
    public MetricsProvider getMetricsProvider()
    {
        return NullMetricsProvider.INSTANCE;
    }

}
