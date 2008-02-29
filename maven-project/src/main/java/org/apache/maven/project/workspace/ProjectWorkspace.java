package org.apache.maven.project.workspace;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.build.model.ModelAndFile;

import java.io.File;

public interface ProjectWorkspace
{

    String PROJECT_INSTANCE_BYFILE_KEY = "maven:project:project:file";

    String MODEL_AND_FILE_BYFILE_KEY = "maven:project:modelAndFile:file";

    String PROJECT_INSTANCE_BYGAV_KEY = "maven:project:project:GAV";

    String MODEL_AND_FILE_BYGAV_KEY = "maven:project:modelAndFile:GAV";

    MavenProject getProject( File projectFile );

    MavenProject getProject( String groupId, String artifactId, String version );

    void storeProjectByFile( MavenProject project );

    void storeProjectByCoordinate( MavenProject project );

    ModelAndFile getModelAndFile( String groupId, String artifactId, String version );

    ModelAndFile getModelAndFile( File modelFile );

    void storeModelAndFile( ModelAndFile modelAndFile );
}
