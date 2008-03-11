package org.apache.maven.project.build.model;

import org.apache.maven.model.Model;

import java.io.File;

public class ModelAndFile
{

    private final Model model;

    private final File file;

    private final boolean validProfilesXmlLocation;

    public ModelAndFile( Model model,
                  File file,
                  boolean validProfilesXmlLocation )
    {
        this.model = model;
        this.file = file;
        this.validProfilesXmlLocation = validProfilesXmlLocation;
    }

    public Model getModel()
    {
        return model;
    }

    public File getFile()
    {
        return file;
    }

    public boolean isValidProfilesXmlLocation()
    {
        return validProfilesXmlLocation;
    }

    public String toString()
    {
        return model.getId() + "@" + file;
    }

}
