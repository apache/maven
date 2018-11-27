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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.CacheUtils;
import org.junit.Assert;
import org.junit.Test;

public class CacheUtilsTest {

    @Test
    public void eqInputNegativeNegativeOutputFalse() {

        // Arrange
        final Object s1 = -268_435_457;
        final Object s2 = -242_043_397;

        // Act
        final boolean retval = CacheUtils.eq(s1, s2);

        // Assert result
        Assert.assertEquals(false, retval);
    }

    @Test
    public void eqInputNullNegativeOutputFalse() {

        // Arrange
        final Object s1 = null;
        final Object s2 = -2_147_483_647;

        // Act
        final boolean retval = CacheUtils.eq(s1, s2);

        // Assert result
        Assert.assertEquals(false, retval);
    }

    @Test
    public void eqInputNullNullOutputTrue() {

        // Arrange
        final Object s1 = null;
        final Object s2 = null;

        // Act
        final boolean retval = CacheUtils.eq(s1, s2);

        // Assert result
        Assert.assertEquals(true, retval);
    }

}
