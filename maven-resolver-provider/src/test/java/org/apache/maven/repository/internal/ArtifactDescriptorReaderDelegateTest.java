package org.apache.maven.repository.internal;

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
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

public class ArtifactDescriptorReaderDelegateTest extends AbstractRepositoryTestCase
{
    public void testMNG7509()
            throws Exception
    {

        ArtifactDescriptorReaderDelegate delegate1 = new ArtifactDescriptorReaderDelegate();
        ArtifactDescriptorResult result1 = mockResult( "org.apache.maven.its", "dep-mng-1", "0.0.1-SNAPSHOT" );
        Model model1 = mockModelWithDepMgmt( "org.apache.maven.its", "depMgmt-1", "0.0.1-SNAPSHOT" );
        delegate1.populateResult( session, result1, model1 );

        ArtifactDescriptorReaderDelegate delegate2 = new ArtifactDescriptorReaderDelegate();
        ArtifactDescriptorResult result2 = mockResult( "org.apache.maven.its", "dep-mng-2", "0.0.2-SNAPSHOT" );
        Model model2 = mockModelWithDepMgmt( "org.apache.maven.its", "depMgmt-1", "0.0.1-SNAPSHOT" );
        delegate2.populateResult( session, result2, model2 );

        ArtifactDescriptorReaderDelegate delegate3 = new ArtifactDescriptorReaderDelegate();
        ArtifactDescriptorResult result3 = mockResult( "org.apache.maven.its", "dep-mng-3", "0.0.2-SNAPSHOT" );
        Model model3 = mockModelWithDepMgmt( "org.apache.maven.its", "depMgmt-2", "0.0.2-SNAPSHOT" );
        delegate3.populateResult( session, result3, model3 );

        // When dependencyManagement in different Model include the same dependency,
        // they should refer to the same dependency instance
        assertNotSame( model1.getDependencyManagement().getDependencies(),
                model2.getDependencyManagement().getDependencies().get( 0 ) );
        assertSame( result1.getManagedDependencies().get( 0 ), result2.getManagedDependencies().get( 0 ) );

        assertNotSame( model2.getDependencyManagement().getDependencies(),
                model3.getDependencyManagement().getDependencies().get( 0 ) );
        assertNotSame( result2.getManagedDependencies().get( 0 ), result3.getManagedDependencies().get( 0 ) );

    }

    private Model mockModelWithDepMgmt( String groupId, String artifactId, String version )
    {
        Model model = new Model();
        model.setDependencyManagement( new DependencyManagement() );
        Dependency dep = new Dependency();
        dep.setGroupId( groupId );
        dep.setArtifactId( artifactId );
        dep.setVersion( version );
        model.getDependencyManagement().addDependency( dep );

        return model;
    }

    private ArtifactDescriptorResult mockResult( String groupId, String artifactId, String version ) throws Exception
    {
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.addRepository( newTestRepository() );
        request.setArtifact( new DefaultArtifact( groupId, artifactId, "jar", version ) );
        return new ArtifactDescriptorResult( request );
    }


}
