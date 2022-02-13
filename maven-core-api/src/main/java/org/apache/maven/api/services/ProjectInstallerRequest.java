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
 *  http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * @author Guillaume Nodet
 */

public interface ProjectInstallerRequest
{

    @Nonnull
    Session getSession();

    @Nonnull
    Project getProject();

    @Nonnull
    static ProjectInstallerRequest build( @Nonnull Session session, @Nonnull Project project )
    {
        return builder()
                .session( session )
                .project( project )
                .build();
    }

    @Nonnull
    static ProjectInstallerRequestBuilder builder()
    {
        return new ProjectInstallerRequestBuilder();
    }

    class ProjectInstallerRequestBuilder
    {
        Session session;
        Project project;

        @Nonnull
        public ProjectInstallerRequestBuilder session( Session session )
        {
            this.session = session;
            return this;
        }

        @Nonnull
        public ProjectInstallerRequestBuilder project( Project project )
        {
            this.project = project;
            return this;
        }

        @Nonnull
        public ProjectInstallerRequest build()
        {
            return new DefaultProjectInstallerRequest( session, project );
        }

        private static class DefaultProjectInstallerRequest extends BaseRequest
                implements ProjectInstallerRequest
        {

            final Project project;

            DefaultProjectInstallerRequest( @Nonnull Session session, @Nonnull Project project )
            {
                super( session );
                this.project = requireNonNull( project, "project" );
            }

            @Nonnull
            @Override
            public Project getProject()
            {
                return project;
            }
        }
    }

}
