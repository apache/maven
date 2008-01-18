package org.apache.maven.reactor;


import java.io.File;

public class MissingModuleException
    extends MavenExecutionException
{

    private File moduleFile;
    private final String moduleName;

    public MissingModuleException( String moduleName, File moduleFile, File pomFile )
    {
        super( "The module: " + moduleName + " cannot be found in file: " + moduleFile, pomFile );
        this.moduleName = moduleName;
        this.moduleFile = moduleFile;
    }

    public File getModuleFile()
    {
        return moduleFile;
    }

    public String getModuleName()
    {
        return moduleName;
    }

}
