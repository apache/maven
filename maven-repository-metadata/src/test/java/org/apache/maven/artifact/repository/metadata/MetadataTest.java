package org.apache.maven.artifact.repository.metadata;

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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetadataTest {
	
	public static final String DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT = "yyyyMMdd.HHmmss";

    public static final TimeZone DEFAULT_SNAPSHOT_TIME_ZONE = TimeZone.getTimeZone( "Etc/UTC" );

    static final String SNAPSHOT = "SNAPSHOT";

    @Test
    void testMultipleSnapshotVersions() {
    	Artifact snapshotArtifact = new DefaultArtifact( "groupId:artifactId:jar:1.0.0-SNAPSHOT" );
    	Metadata metadata = createMetadataFromArtifact( snapshotArtifact );
    
    	Metadata newMetadata = metadata.clone();
    	Versioning versioning = new Versioning();
    	addSnapshotVersion( versioning, new Date(), snapshotArtifact );
    	metadata.setVersioning(versioning);

    	versioning = new Versioning();
    	addSnapshotVersion( versioning, new Date(), snapshotArtifact );
    	newMetadata.setVersioning( versioning );
    	
        Assertions.assertTrue( metadata.merge( newMetadata ) );
        Assertions.assertEquals( 2, metadata.getVersioning().getSnapshotVersions().size() );
    }
    
    private static Metadata createMetadataFromArtifact( Artifact artifact ) {
    	Metadata metadata = new Metadata();
    	metadata.setArtifactId( artifact.getArtifactId() );
    	metadata.setGroupId( artifact.getGroupId() );
    	metadata.setVersion( artifact.getVersion() );
    	return metadata;
    }
    
    private static void addSnapshotVersion( Versioning versioning, Date timestamp, Artifact artifact ) {
    	// https://github.com/apache/maven/blob/03df5f7c639db744a3597c7175c92c8e2a27767b/maven-resolver-provider/src/main/java/org/apache/maven/repository/internal/RemoteSnapshotMetadata.java#L79
    	DateFormat utcDateFormatter = new SimpleDateFormat( DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT );
        utcDateFormatter.setCalendar( new GregorianCalendar() );
        utcDateFormatter.setTimeZone( DEFAULT_SNAPSHOT_TIME_ZONE );

        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber( 1 );
        snapshot.setTimestamp( utcDateFormatter.format( timestamp ) );

        String version = artifact.getVersion();
        String qualifier = snapshot.getTimestamp() + '-' + snapshot.getBuildNumber();
        version = version.substring( 0, version.length() - SNAPSHOT.length() ) + qualifier;

        SnapshotVersion sv = new SnapshotVersion();
        sv.setExtension( artifact.getExtension() );
        sv.setVersion( version );
        sv.setUpdated( snapshot.getTimestamp() );
        versioning.addSnapshotVersion( sv );
        
        
        // make the new snapshot the current one
        versioning.setSnapshot( snapshot );
        versioning.setLastUpdatedTimestamp( timestamp );
    }
}
