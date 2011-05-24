package org.apache.maven.router.repository;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * The Apache License v2.0 is available at
 *   http://www.apache.org/licenses/LICENSE-2.0.html
 * You may elect to redistribute this code under either of these licenses.
 *******************************************************************************/

import org.apache.maven.wagon.Wagon;

/**
 * A component to configure wagon instances with provider-specific parameters.
 * 
 * @author Benjamin Bentmann
 */
public interface WagonConfigurator
{

    /**
     * Configures the specified wagon instance with the given configuration.
     * 
     * @param wagon The wagon instance to configure, must not be {@code null}.
     * @param configuration The configuration to apply to the wagon instance, must not be {@code null}.
     * @throws Exception If the configuration could not be applied to the wagon.
     */
    void configure( Wagon wagon, Object configuration )
        throws Exception;

}
