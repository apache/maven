package org.apache.maven.project.builder;

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

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.shared.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Provides services for building maven projects from models.
 */
public interface ProjectBuilder
{

    String ROLE = ProjectBuilder.class.getName();


    /**
     * Returns a maven project for the specified input stream.
     *
     * @param pom                         input stream of the model
     * @param inheritedModels             list of models containing additional parent models in order from most to least specialized
     * @param interpolatorProperties      properties used for interpolation of properties within the model
     * @param resolver                    artifact resolver used in resolving artifacts
     * @param baseDirectory               the base directory of the model
     * @param projectBuilderConfiguration
     * @return a maven project for the specified input stream
     * @throws IOException if there is a problem in the construction of the maven project
     */
    MavenProject buildFromLocalPath( InputStream pom, List<Model> inheritedModels, Collection<ImportModel> importModels,
                                     Collection<InterpolatorProperty> interpolatorProperties,
                                     PomArtifactResolver resolver, File baseDirectory,
                                     ProjectBuilderConfiguration projectBuilderConfiguration )
        throws IOException;


    public static class Interpolator {

        public static String interpolateXmlString( String xml, List<InterpolatorProperty> interpolatorProperties )
                throws IOException
        {
            List<ModelProperty> modelProperties =
                ModelMarshaller.marshallXmlToModelProperties( new ByteArrayInputStream(xml.getBytes()), ProjectUri.baseUri,
                        PomTransformer.URIS );

            Map<String, String> aliases = new HashMap<String, String>();
            aliases.put( "project.", "pom.");

            List<InterpolatorProperty> ips = new ArrayList<InterpolatorProperty>(interpolatorProperties);
            ips.addAll(ModelTransformerContext.createInterpolatorProperties(modelProperties, ProjectUri.baseUri, aliases,
                            PomInterpolatorTag.PROJECT_PROPERTIES.name(), false, false));

            for(ModelProperty mp : modelProperties)
            {
                if(mp.getUri().startsWith(ProjectUri.properties) && mp.getValue() != null )
                {
                    String uri = mp.getUri();
                    ips.add( new InterpolatorProperty( "${" + uri.substring( uri.lastIndexOf( "/" ) + 1,
                            uri.length() ) + "}", mp.getValue() ) );
                }
            }

            ModelTransformerContext.interpolateModelProperties( modelProperties, ips );
            return ModelMarshaller.unmarshalModelPropertiesToXml( modelProperties, ProjectUri.baseUri );
        }

        public static String interpolateModelAsString(Model model, List<InterpolatorProperty> interpolatorProperties, File projectDirectory)
                throws IOException
        {
            PomClassicDomainModel domainModel = new PomClassicDomainModel( model );
            domainModel.setProjectDirectory( projectDirectory );
            List<ModelProperty> modelProperties =
                    ModelMarshaller.marshallXmlToModelProperties( domainModel.getInputStream(), ProjectUri.baseUri, PomTransformer.URIS );
            interpolateModelProperties( modelProperties, interpolatorProperties, domainModel);

            return ModelMarshaller.unmarshalModelPropertiesToXml( modelProperties, ProjectUri.baseUri );
        }

        public static Model interpolateModel(Model model, List<InterpolatorProperty> interpolatorProperties, File projectDirectory)
            throws IOException
        {
            String pomXml = interpolateModelAsString( model, interpolatorProperties, projectDirectory );
            PomClassicDomainModel domainModel = new PomClassicDomainModel( new ByteArrayInputStream( pomXml.getBytes() ));
            return domainModel.getModel();
        }

        private static final Map<String, String> aliases = new HashMap<String, String>();

        private static void addProjectAlias( String element, boolean leaf )
        {
            String suffix = leaf ? "\\}" : "\\.";
            aliases.put( "\\$\\{project\\." + element + suffix, "\\$\\{" + element + suffix );
        }

        static
        {
            aliases.put( "\\$\\{project\\.", "\\$\\{pom\\.");
            addProjectAlias( "modelVersion", true );
            addProjectAlias( "groupId", true );
            addProjectAlias( "artifactId", true );
            addProjectAlias( "version", true );
            addProjectAlias( "packaging", true );
            addProjectAlias( "name", true );
            addProjectAlias( "description", true );
            addProjectAlias( "inceptionYear", true );
            addProjectAlias( "url", true );
            addProjectAlias( "parent", false );
            addProjectAlias( "prerequisites", false );
            addProjectAlias( "organization", false );
            addProjectAlias( "build", false );
            addProjectAlias( "reporting", false );
            addProjectAlias( "scm", false );
            addProjectAlias( "distributionManagement", false );
            addProjectAlias( "issueManagement", false );
            addProjectAlias( "ciManagement", false );
        }

        public static void interpolateModelProperties(List<ModelProperty> modelProperties,
                                                       List<InterpolatorProperty> interpolatorProperties,
                                                       PomClassicDomainModel domainModel)
               throws IOException
        {
            if(!containsProjectVersion(interpolatorProperties))
            {
                aliases.put("\\$\\{project.version\\}", "\\$\\{version\\}");
            }

            List<ModelProperty> firstPassModelProperties = new ArrayList<ModelProperty>();
            List<ModelProperty> secondPassModelProperties = new ArrayList<ModelProperty>();

            ModelProperty buildProperty = new ModelProperty(ProjectUri.Build.xUri, null);
            for(ModelProperty mp : modelProperties)
            {
                if( mp.getValue() != null && !mp.getUri().contains( "#property" ) && !mp.getUri().contains( "#collection" ))
                {
                    if( (!buildProperty.isParentOf( mp ) && !mp.getUri().equals(ProjectUri.Reporting.outputDirectory)
                            || mp.getUri().equals(ProjectUri.Build.finalName ) ))
                    {
                        firstPassModelProperties.add(mp);
                    }
                    else
                    {
                        secondPassModelProperties.add(mp);
                    }
                }
            }


            List<InterpolatorProperty> standardInterpolatorProperties = new ArrayList<InterpolatorProperty>();
            if(domainModel.isPomInBuild())
            {
                String basedir = domainModel.getProjectDirectory().getAbsolutePath();
                standardInterpolatorProperties.add(new InterpolatorProperty("${project.basedir}", basedir,
                        PomInterpolatorTag.PROJECT_PROPERTIES.name() ));
                standardInterpolatorProperties.add(new InterpolatorProperty("${basedir}", basedir,
                        PomInterpolatorTag.PROJECT_PROPERTIES.name() ));
                standardInterpolatorProperties.add(new InterpolatorProperty("${pom.basedir}", basedir,
                        PomInterpolatorTag.PROJECT_PROPERTIES.name() ));

            }

            for(ModelProperty mp : modelProperties)
            {
                if(mp.getUri().startsWith(ProjectUri.properties) && mp.getValue() != null )
                {
                    String uri = mp.getUri();
                    standardInterpolatorProperties.add( new InterpolatorProperty( "${" + uri.substring( uri.lastIndexOf( "/" ) + 1,
                            uri.length() ) + "}", mp.getValue(), PomInterpolatorTag.PROJECT_PROPERTIES.name() ) );
                }
            }

            //FIRST PASS - Withhold using build directories as interpolator properties
            List<InterpolatorProperty> ips1 = new ArrayList<InterpolatorProperty>(interpolatorProperties);
            ips1.addAll(standardInterpolatorProperties);
            ips1.addAll(ModelTransformerContext.createInterpolatorProperties(firstPassModelProperties, ProjectUri.baseUri, aliases,
                            PomInterpolatorTag.PROJECT_PROPERTIES.name(), false, false));
            Collections.sort(ips1, new Comparator<InterpolatorProperty>()
            {
                public int compare(InterpolatorProperty o, InterpolatorProperty o1) {
                    return PomInterpolatorTag.valueOf(o.getTag()).compareTo(PomInterpolatorTag.valueOf(o1.getTag()));
                }
            });

            ModelTransformerContext.interpolateModelProperties( modelProperties, ips1 );

            //SECOND PASS - Set absolute paths on build directories
            if( domainModel.isPomInBuild() )
            {   String basedir = domainModel.getProjectDirectory().getAbsolutePath();
                Map<ModelProperty, ModelProperty> buildDirectories = new HashMap<ModelProperty, ModelProperty>();
                for(ModelProperty mp : secondPassModelProperties)
                {
                    if(mp.getUri().startsWith( ProjectUri.Build.xUri ) || mp.getUri().equals( ProjectUri.Reporting.outputDirectory ))
                    {
                        File file = new File(mp.getResolvedValue());
                        if( !file.isAbsolute() )
                        {
                            buildDirectories.put(mp, new ModelProperty(mp.getUri(), new File(basedir, file.getPath()).getAbsolutePath()));
                        }
                    }
                }

                for ( Map.Entry<ModelProperty, ModelProperty> e : buildDirectories.entrySet() )
                {
                    secondPassModelProperties.remove( e.getKey() );
                    secondPassModelProperties.add(e.getValue() );
                }
            }

            //THIRD PASS - Use build directories as interpolator properties
            List<InterpolatorProperty> ips2 = new ArrayList<InterpolatorProperty>(interpolatorProperties);
            ips2.addAll(standardInterpolatorProperties);
            ips2.addAll(ModelTransformerContext.createInterpolatorProperties(secondPassModelProperties, ProjectUri.baseUri, aliases,
                            PomInterpolatorTag.PROJECT_PROPERTIES.name(), false, false));
            ips2.addAll(interpolatorProperties);
            Collections.sort(ips2, new Comparator<InterpolatorProperty>()
            {
                public int compare(InterpolatorProperty o, InterpolatorProperty o1) {
                    return PomInterpolatorTag.valueOf(o.getTag()).compareTo(PomInterpolatorTag.valueOf(o1.getTag()));
                }
            });

            ModelTransformerContext.interpolateModelProperties( modelProperties, ips2 );
        }

        private static boolean containsProjectVersion( List<InterpolatorProperty> interpolatorProperties )
        {
            InterpolatorProperty versionInterpolatorProperty =
                    new ModelProperty( ProjectUri.version, "").asInterpolatorProperty( ProjectUri.baseUri);
            for( InterpolatorProperty ip : interpolatorProperties)
            {
                if ( ip.equals( versionInterpolatorProperty ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

}
