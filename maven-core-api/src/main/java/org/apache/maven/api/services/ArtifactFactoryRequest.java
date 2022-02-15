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

public interface ArtifactFactoryRequest
{

    @Nonnull
    Session getSession();

    String getGroupId();

    String getArtifactId();

    String getVersion();

    String getType();

    String getExtension();

    String getClassifier();

    static ArtifactFactoryRequest build( Session session, String groupId, String artifactId,
                                         String version, String type )
    {
        return ArtifactFactoryRequest.builder()
                .session( session )
                .groupId( groupId )
                .artifactId( artifactId )
                .version( version )
                .type( type )
                .build();
    }

    static ArtifactFactoryRequest build( Session session, String groupId, String artifactId,
                                         String classifier, String version, String type )
    {
        return ArtifactFactoryRequest.builder()
                .session( session )
                .groupId( groupId )
                .artifactId( artifactId )
                .classifier( classifier )
                .version( version )
                .type( type )
                .build();
    }

    static ArtifactFactoryRequestBuilder builder()
    {
        return new ArtifactFactoryRequestBuilder();
    }

    class ArtifactFactoryRequestBuilder
    {
        private Session session;
        private String groupId;
        private String artifactId;
        private String version;
        private String type;
        private String extension;
        private String classifier;

        public ArtifactFactoryRequestBuilder session( Session session )
        {
            this.session = session;
            return this;
        }

        public ArtifactFactoryRequestBuilder groupId( String groupId )
        {
            this.groupId = groupId;
            return this;
        }

        public ArtifactFactoryRequestBuilder artifactId( String artifactId )
        {
            this.artifactId = artifactId;
            return this;
        }

        public ArtifactFactoryRequestBuilder version( String version )
        {
            this.version = version;
            return this;
        }

        public ArtifactFactoryRequestBuilder type( String type )
        {
            this.type = type;
            return this;
        }

        public ArtifactFactoryRequestBuilder extension( String extension )
        {
            this.extension = extension;
            return this;
        }

        public ArtifactFactoryRequestBuilder classifier( String classifier )
        {
            this.classifier = classifier;
            return this;
        }

        public ArtifactFactoryRequest build()
        {
            return new DefaultArtifactFactoryRequest( session, groupId, artifactId,
                                                      version, type, extension, classifier );
        }

        private static class DefaultArtifactFactoryRequest extends BaseRequest implements ArtifactFactoryRequest
        {
            private final String groupId;
            private final String artifactId;
            private final String version;
            private final String type;
            private final String extension;
            private final String classifier;

            DefaultArtifactFactoryRequest( @Nonnull Session session,
                                           String groupId,
                                           String artifactId,
                                           String version,
                                           String type,
                                           String extension,
                                           String classifier )
            {
                super( session );
                this.groupId = groupId;
                this.artifactId = artifactId;
                this.version = version;
                this.type = type;
                this.extension = extension;
                this.classifier = classifier;
            }

            @Override
            public String getGroupId()
            {
                return groupId;
            }

            @Override
            public String getArtifactId()
            {
                return artifactId;
            }

            @Override
            public String getVersion()
            {
                return version;
            }

            @Override
            public String getType()
            {
                return type;
            }

            @Override
            public String getExtension()
            {
                return extension;
            }

            @Override
            public String getClassifier()
            {
                return classifier;
            }
        }
    }

}
