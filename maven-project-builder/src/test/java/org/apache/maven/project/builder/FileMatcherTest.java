package org.apache.maven.project.builder;

import org.apache.maven.project.builder.profile.FileMatcher;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileMatcherTest {

    private static String basedir = System.getProperty("basedir");

    @org.junit.Test(expected=IllegalArgumentException.class)
    public void modelContainerIsNull()  {
        FileMatcher matcher = new FileMatcher();
        matcher.isMatch(null, new ArrayList<InterpolatorProperty>());
    }

    @org.junit.Test
    public void fileExistActivationAndExists()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.File.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.File.exists ,
                new File(basedir, "src/test/resources/test.txt").getAbsolutePath()));
        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        FileMatcher matcher = new FileMatcher();
        assertTrue(matcher.isMatch(modelContainer, new ArrayList<InterpolatorProperty>()));
    }

    @org.junit.Test
    public void fileExistActivationButDoesNotExist()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.File.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.File.exists ,
                new File(basedir, "src/test/resources/bogus.txt").getAbsolutePath()));
        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        FileMatcher matcher = new FileMatcher();
        assertFalse(matcher.isMatch(modelContainer, new ArrayList<InterpolatorProperty>()));
    }

    @org.junit.Test
    public void fileMissingActivationButExists()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.File.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.File.missing ,
                new File(basedir, "src/test/resources/test.txt").getAbsolutePath()));
        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        FileMatcher matcher = new FileMatcher();
        assertFalse(matcher.isMatch(modelContainer, new ArrayList<InterpolatorProperty>()));
    }

    @org.junit.Test
    public void fileMissingActivationAndDoesNotExist()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.File.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.File.missing ,
                new File(basedir, "src/test/resources/bogus.txt").getAbsolutePath()));
        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        FileMatcher matcher = new FileMatcher();
        assertTrue(matcher.isMatch(modelContainer, new ArrayList<InterpolatorProperty>()));
    }

}
