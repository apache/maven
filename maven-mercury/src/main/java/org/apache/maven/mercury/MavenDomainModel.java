package org.apache.maven.mercury;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.PomClassicDomainModel;

public final class MavenDomainModel
    extends PomClassicDomainModel
{

    private ArtifactMetadata parentMetadata;

    /**
     * Constructor
     *
     * @throws IOException if there is a problem constructing the model
     */
    public MavenDomainModel( byte[] bytes )
        throws IOException
    {
        super( new ByteArrayInputStream( bytes ) );
    }


    public MavenDomainModel(PomClassicDomainModel model) 
    	throws IOException
    {
    	super(model.getModel());
    }    
    
    public MavenDomainModel(Model model) 
		throws IOException
	{
		super(model);
	}    
  
    public boolean hasParent()
    {
        return getParentMetadata() != null;
    }

    public List<ArtifactMetadata> getDependencyMetadata()
    {
        List<ArtifactMetadata> metadatas = new ArrayList<ArtifactMetadata>();

        for(Dependency d: model.getDependencies())
        {
        	ArtifactMetadata metadata = new ArtifactMetadata();
        	metadata.setArtifactId(d.getArtifactId());
        	metadata.setClassifier(d.getClassifier());
        	metadata.setGroupId(d.getGroupId());
        	metadata.setScope( (d.getScope() == null) ? "compile" : d.getScope());
        	metadata.setVersion(d.getVersion());
        	metadata.setOptional(d.isOptional());
        	
        	 if( "test-jar".equals( d.getType() ) )
             {
                 metadata.setType( "jar" );
                 metadata.setClassifier( "tests" );
             }
             else
             {
            	 metadata.setType( d.getType() );	 
             }
                      	 
            List<ArtifactMetadata> exclusions = new ArrayList<ArtifactMetadata>();
            for( Exclusion e : d.getExclusions() ) 
            {
            	ArtifactMetadata md = new ArtifactMetadata();
            	md.setArtifactId(e.getArtifactId());
            	md.setGroupId(e.getGroupId());
            	exclusions.add(md);
            }
            metadata.setExclusions(exclusions);
            metadatas.add(metadata);
        }
        
        return metadatas;
    }

    public ArtifactMetadata getParentMetadata()
    {
        if(parentMetadata == null)
        {
            Parent parent = model.getParent();
            if(parent != null)
            {
                parentMetadata = new ArtifactMetadata();
                parentMetadata.setArtifactId( parent.getArtifactId() );
                parentMetadata.setVersion( parent.getVersion() );
                parentMetadata.setGroupId( parent.getGroupId() );        	
            }       	
        }
        return (parentMetadata != null) ? copyArtifactBasicMetadata( parentMetadata ) : null;
    }

    private ArtifactMetadata copyArtifactBasicMetadata( ArtifactMetadata metadata )
    {
        ArtifactMetadata amd = new ArtifactMetadata();
        amd.setArtifactId( metadata.getArtifactId() );
        amd.setGroupId( metadata.getGroupId() );
        amd.setVersion( metadata.getVersion() );
        return amd;
    }
}
