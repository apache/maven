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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Guillaume Nodet
 */
public class DefaultModelBuilderTest
{

    private static final String BASE1_ID = "thegroup:base1:pom";

    private static final String BASE1 = "<project>\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <groupId>thegroup</groupId>\n" +
            "  <artifactId>base1</artifactId>\n" +
            "  <version>1</version>\n" +
            "  <packaging>pom</packaging>\n" +
            "  <dependencyManagement>\n" +
            "    <dependencies>\n" +
            "      <dependency>\n" +
            "        <groupId>thegroup</groupId>\n" +
            "        <artifactId>base2</artifactId>\n" +
            "        <version>1</version>\n" +
            "        <type>pom</type>\n" +
            "        <scope>import</scope>\n" +
            "      </dependency>\n" +
            "    </dependencies>\n" +
            "  </dependencyManagement>\n" +
            "</project>\n";

    private static final String BASE2_ID = "thegroup:base2:pom";

    private static final String BASE2 = "<project>\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <groupId>thegroup</groupId>\n" +
            "  <artifactId>base2</artifactId>\n" +
            "  <version>1</version>\n" +
            "  <packaging>pom</packaging>\n" +
            "  <dependencyManagement>\n" +
            "    <dependencies>\n" +
            "      <dependency>\n" +
            "        <groupId>thegroup</groupId>\n" +
            "        <artifactId>base1</artifactId>\n" +
            "        <version>1</version>\n" +
            "        <type>pom</type>\n" +
            "        <scope>import</scope>\n" +
            "      </dependency>\n" +
            "    </dependencies>\n" +
            "  </dependencyManagement>\n" +
            "</project>\n";

    @Test
    public void testCycleInImports()
            throws Exception
    {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull( builder );

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource( new StringModelSource( BASE1 ) );
        request.setModelResolver( new CycleInImportsResolver() );

        assertThrows( ModelBuildingException.class, () -> builder.build( request ) );
    }

    static class CycleInImportsResolver extends BaseModelResolver
    {
        @Override
        public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException
        {
            switch ( dependency.getManagementKey() )
            {
                case BASE1_ID: return new StringModelSource( BASE1 );
                case BASE2_ID: return new StringModelSource( BASE2 );
            }
            return null;
        }
    }

    static class BaseModelResolver implements ModelResolver
    {
        @Override
        public ModelSource resolveModel( String groupId, String artifactId, String version )
                throws UnresolvableModelException
        {
            return null;
        }

        @Override
        public ModelSource resolveModel( Parent parent ) throws UnresolvableModelException
        {
            return null;
        }

        @Override
        public ModelSource resolveModel( Dependency dependency ) throws UnresolvableModelException
        {
            return null;
        }

        @Override
        public void addRepository( Repository repository ) throws InvalidRepositoryException
        {
        }

        @Override
        public void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException
        {
        }

        @Override
        public ModelResolver newCopy()
        {
            return this;
        }
    }

}
