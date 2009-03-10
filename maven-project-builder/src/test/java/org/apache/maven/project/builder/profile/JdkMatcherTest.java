package org.apache.maven.project.builder.profile;

import org.apache.maven.project.builder.profile.JdkMatcher;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

public class JdkMatcherTest {

    @org.junit.Test(expected=IllegalArgumentException.class)
    public void modelContainerIsNull()  {
        JdkMatcher matcher = new JdkMatcher();
        matcher.isMatch(null, new ArrayList<InterpolatorProperty>());
    }

    @org.junit.Test
    public void jdkVersionMatches()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , "1.5"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.5"));

        JdkMatcher matcher = new JdkMatcher();
        assertTrue(matcher.isMatch(modelContainer, props));
    }

    @org.junit.Test
    public void jdkVersionDoesNotMatchWithNotSymbol()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , "!1.5"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.5"));

        JdkMatcher matcher = new JdkMatcher();
        assertTrue(!matcher.isMatch(modelContainer, props));
    }

    @org.junit.Test
    public void jdkVersionDoesMatchWithNotSymbol()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , "!1.5"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.6"));

        JdkMatcher matcher = new JdkMatcher();
        assertTrue(matcher.isMatch(modelContainer, props));
    }


    @org.junit.Test
    public void jdkVersionNotMatches()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , "1.5"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.4"));

        JdkMatcher matcher = new JdkMatcher();
        assertFalse(matcher.isMatch(modelContainer, props));
    }
    
    @org.junit.Test
    public void jdkVersionRange_ClosedEdge()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , "[1.5,"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.5"));

        JdkMatcher matcher = new JdkMatcher();
        assertTrue(matcher.isMatch(modelContainer, props));
    } 
    
    @org.junit.Test
    public void jdkVersionRange_OpenEdge()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , "(1.5,"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.5"));

        JdkMatcher matcher = new JdkMatcher();
        assertFalse(matcher.isMatch(modelContainer, props));
    }  
    
    @org.junit.Test
    public void jdkVersionRange_OpenEdge2()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , "(1.4,"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.5"));

        JdkMatcher matcher = new JdkMatcher();
        assertTrue(matcher.isMatch(modelContainer, props));
    }   
    
    @org.junit.Test
    public void jdkVersionRange_OpenEdgeWithPadding()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , "(1.5.0,"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.5"));

        JdkMatcher matcher = new JdkMatcher();
        assertFalse(matcher.isMatch(modelContainer, props));
    }  

    @org.junit.Test
    public void jdkVersionRange_OpenRightEdge()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , ", 1.6)"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.5"));

        JdkMatcher matcher = new JdkMatcher();
        assertTrue(matcher.isMatch(modelContainer, props));
    }
    
    @org.junit.Test
    public void jdkVersionRange_OpenRightEdge2()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , ",1.5)"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.5"));

        JdkMatcher matcher = new JdkMatcher();
        assertFalse(matcher.isMatch(modelContainer, props));
    }  
    
    @org.junit.Test
    public void jdkVersionRange_OpenRightEdgeWithWhiteSpace()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , ", 1.5)"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${java.specification.version}" , "1.5"));

        JdkMatcher matcher = new JdkMatcher();
        assertFalse(matcher.isMatch(modelContainer, props));
    } 

    @org.junit.Test
    public void jdkVersionNotFound()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.jdk , "1.5"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        JdkMatcher matcher = new JdkMatcher();
        assertFalse(matcher.isMatch(modelContainer, new ArrayList<InterpolatorProperty>()));
    }
}
