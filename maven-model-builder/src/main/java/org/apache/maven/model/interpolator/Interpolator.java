package org.apache.maven.model.interpolator;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.model.Model;

public interface Interpolator 
{
	Model interpolateModel( Model model, Properties properties, File projectDirectory )
        throws IOException;
}
