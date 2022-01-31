package org.apache.maven.session.scope.internal;

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

import javax.inject.Provider;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import org.apache.maven.model.building.DefaultModelSourceTransformer;
import org.apache.maven.model.building.ModelSourceTransformer;
import org.apache.maven.model.locator.DefaultModelLocator;
import org.apache.maven.model.locator.ModelLocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SessionScopeTest {

    @Test
    public void testScope() throws Exception
    {
        SessionScope scope = new SessionScope();

        assertThrows( OutOfScopeException.class, () -> scope.seed( ModelLocator.class, new DefaultModelLocator() ) );

        Provider<ModelLocator> pml = scope.scope( Key.get( ModelLocator.class), DefaultModelLocator::new );
        assertNotNull( pml );
        assertThrows( OutOfScopeException.class, pml::get );

        Provider<ModelSourceTransformer> pmst = scope.scope( Key.get( ModelSourceTransformer.class ), DefaultModelSourceTransformer::new );
        assertNotNull( pmst );

        scope.enter();

        final DefaultModelLocator dml1 = new DefaultModelLocator();
        scope.seed( ModelLocator.class, dml1 );

        assertSame( dml1, pml.get() );

        ModelSourceTransformer mst1 = pmst.get();
        assertSame( mst1, pmst.get() );
        Provider<ModelSourceTransformer> pmst1 = scope.scope( Key.get( ModelSourceTransformer.class ), DefaultModelSourceTransformer::new );
        assertNotNull( pmst1 );
        assertSame( mst1, pmst1.get() );

        scope.enter();

        pmst1 = scope.scope( Key.get( ModelSourceTransformer.class ), DefaultModelSourceTransformer::new );
        assertNotNull( pmst1 );
        assertNotSame( mst1, pmst1.get() );

        scope.exit();

        assertSame( mst1, pmst.get() );

        scope.exit();

        assertThrows( OutOfScopeException.class, pmst::get );
        assertThrows( OutOfScopeException.class, () -> scope.seed( ModelLocator.class, new DefaultModelLocator() ) );
    }
}
