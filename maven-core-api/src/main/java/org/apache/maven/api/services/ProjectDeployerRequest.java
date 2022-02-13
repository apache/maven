package org.apache.maven.api.services;

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

import javax.annotation.Nonnull;

import org.apache.maven.api.Session;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;

public interface ProjectDeployerRequest
{
    @Nonnull
    Session getSession();

    @Nonnull
    Project getProject();

    @Nonnull
    RemoteRepository getRepository();

    int getRetryFailedDeploymentCount();

//    @Nonnull
//    Optional<String> getAltDeploymentRepository();

//    @Nonnull
//    Optional<String> getAltSnapshotDeploymentRepository();

//    @Nonnull
//    Optional<String> getAltReleaseDeploymentRepository();

    static ProjectDeployerRequest build( Session session, Project project )
    {
        return builder()
                .session( session )
                .project( project )
                .build();
    }

    static ProjectDeployerRequestBuilder builder()
    {
        return new ProjectDeployerRequestBuilder();
    }

    class ProjectDeployerRequestBuilder
    {
        Session session;
        Project project;
        RemoteRepository repository;
        int retryFailedDeploymentCount;

        @Nonnull
        public ProjectDeployerRequestBuilder session( @Nonnull Session session )
        {
            this.session = session;
            return this;
        }

        @Nonnull
        public ProjectDeployerRequestBuilder project( @Nonnull Project project )
        {
            this.project = project;
            return this;
        }

        @Nonnull
        public ProjectDeployerRequestBuilder repository( @Nonnull RemoteRepository repository )
        {
            this.repository = repository;
            return this;
        }

        @Nonnull
        public ProjectDeployerRequestBuilder retryFailedDeploymentCount( int retryFailedDeploymentCount )
        {
            this.retryFailedDeploymentCount = retryFailedDeploymentCount;
            return this;
        }

        public ProjectDeployerRequest build()
        {
            return new DefaultProjectDeployerRequest( session, project, repository, retryFailedDeploymentCount );
        }

        private static class DefaultProjectDeployerRequest extends BaseRequest
                implements ProjectDeployerRequest
        {
            private final Project project;
            private final RemoteRepository repository;
            private final int retryFailedDeploymentCount;

            DefaultProjectDeployerRequest( @Nonnull Session session,
                                           @Nonnull Project project,
                                           @Nonnull RemoteRepository repository,
                                           int retryFailedDeploymentCount )
            {
                super( session );
                this.project = requireNonNull( project, "project" );
                this.repository = requireNonNull( repository, "repository" );
                this.retryFailedDeploymentCount = retryFailedDeploymentCount;
            }

            @Nonnull
            @Override
            public Project getProject()
            {
                return project;
            }

            @Nonnull
            @Override
            public RemoteRepository getRepository()
            {
                return repository;
            }

            @Override
            public int getRetryFailedDeploymentCount()
            {
                return retryFailedDeploymentCount;
            }
        }
    }
}
