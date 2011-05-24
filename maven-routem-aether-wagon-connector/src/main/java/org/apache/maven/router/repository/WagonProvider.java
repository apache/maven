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
 * A component to acquire and release wagon instances for uploads/downloads.
 * 
 * @author Benjamin Bentmann
 */
public interface WagonProvider
{

    /**
     * Acquires a wagon instance that matches the specified role hint. The role hint is derived from the URI scheme,
     * e.g. "http" or "file".
     * 
     * @param roleHint The role hint to get a wagon for, must not be {@code null}.
     * @return The requested wagon instance, never {@code null}.
     * @throws Exception If no wagon could be retrieved for the specified role hint.
     */
    Wagon lookup( String roleHint )
        throws Exception;

    /**
     * Releases the specified wagon. A wagon provider may either free any resources allocated for the wagon instance or
     * return the instance back to a pool for future use.
     * 
     * @param wagon The wagon to release, may be {@code null}.
     */
    void release( Wagon wagon );

}
