package org.apache.maven.model;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@code ActivationFile}.
 *
 * @author Benjamin Bentmann
 */
public class ActivationFileTest
{

    @Test
    public void testHashCodeNullSafe()
    {
        new ActivationFile().hashCode();
    }

    @Test
    public void testEqualsNullSafe()
    {
        assertFalse( new ActivationFile().equals( null ) );

        new ActivationFile().equals( new ActivationFile() );
    }

    @Test
    public void testEqualsIdentity()
    {
        ActivationFile thing = new ActivationFile();
        assertTrue( thing.equals( thing ) );
    }

    @Test
    public void testToStringNullSafe()
    {
        assertNotNull( new ActivationFile().toString() );
    }

}
