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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Interface as a test template for all model tests.
 *
 * @author Karl Heinz Marbaise
 */
@DisplayNameGeneration( ModelTestInterface.NameGenerator.class )
interface ModelTestInterface< T >
{

    Class<T> createValue();

    @Test
    @DisplayName( "hashCode should not fail with null." )
    default void hashCodeNullSafe()
    {
        assertThatCode( () -> createValue().hashCode() ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName( "equals should not fail with null." )
    default void equalsNullSafe() throws ReflectiveOperationException
    {
        T newInstance = createValue().getDeclaredConstructor().newInstance();
        assertThat( newInstance.equals( null ) ).isFalse();
    }

    @Test
    @DisplayName( "equals should result in false for two different instances." )
    default void equalsSameToBeFalse() throws ReflectiveOperationException
    {
        T firstInstance = createValue().getDeclaredConstructor().newInstance();
        T secondInstance = createValue().getDeclaredConstructor().newInstance();
        assertThat(firstInstance).isNotSameAs(secondInstance);
    }

    @Test
    @DisplayName( "toString should not be null." )
    default void toStringNullSafe() throws ReflectiveOperationException
    {
        assertThat( createValue().getDeclaredConstructor().newInstance().toString() ).isNotNull();
    }

    /**
     * The @DisplayName will be the test class name without the trailing "Test".
     */
    class NameGenerator extends DisplayNameGenerator.Standard
    {
        public String generateDisplayNameForClass( Class<?> testClass )
        {
            String name = testClass.getSimpleName();
            return name.substring(0, name.length() - 4);
        }
    }
}
