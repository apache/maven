package org.apache.maven.model.building;

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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.merge.ModelMerger;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

public class FileToRawModelMergerTest
{

    /**
     * Ensures that all list-merge methods are overridden
     */
    @Test
    public void testOverriddenMergeMethods()
    {
        List<String> methodNames =
            Stream.of( ModelMerger.class.getDeclaredMethods() )
                .filter( m -> m.getName().startsWith( "merge" ) )
                .filter( m ->
                    {
                        String baseName = m.getName().substring( 5 /* merge */ );
                        String entity = baseName.substring( baseName.indexOf( '_' ) + 1 );
                        try
                        {
                            Type returnType = m.getParameterTypes()[0].getMethod( "get" + entity ).getGenericReturnType();
                            if ( returnType instanceof ParameterizedType )
                            {
                                return !( (ParameterizedType) returnType ).getActualTypeArguments()[0].equals( String.class );
                            }
                            else
                            {
                                return false;
                            }
                        }
                        catch ( ReflectiveOperationException | SecurityException e )
                        {
                            return false;
                        }
                    } )
                .map( Method::getName )
                .collect( Collectors.toList() );

        List<String> overriddenMethods =
            Stream.of( FileToRawModelMerger.class.getDeclaredMethods() )
                .map( Method::getName )
                .filter( m -> m.startsWith( "merge" ) )
                .collect( Collectors.toList() );

        assertThat( overriddenMethods, hasItems( methodNames.toArray( new String[0] ) ) );
    }


}
