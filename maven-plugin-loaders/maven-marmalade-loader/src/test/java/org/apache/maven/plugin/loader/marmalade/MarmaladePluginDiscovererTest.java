/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugin.MavenMojoDescriptor;
import org.apache.maven.plugin.MavenPluginDescriptor;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DefaultClassRealm;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class MarmaladePluginDiscovererTest extends TestCase {
    
    private static final String TEST_MOJO_SCRIPT = "<?xml version=\"1.0\"?>" +
        "<mojo xmlns=\"marmalade:mojo\">" +
            "<descriptor id=\"test\" goal=\"myTest\" instantiation-strategy=\"singleton\" execution-strategy=\"always\">" +
                "<description>This is a test mojo script.</description>" +
                "<prereqs><prereq name=\"test-prereq\"/></prereqs>" +
                "<parameters>" + 
                    "<parameter name=\"param\" type=\"String\" required=\"true\" validator=\"something\" expression=\"false\">" +
                        "<description>Parameter to test</description>" +
                    "</parameter>" +
                "</parameters>" +
                "<dependencies>" +
                    "<dependency groupId=\"marmalade\" artifactId=\"marmalade-core\" version=\"0.2\"/>" +
                "</dependencies>" +
            "</descriptor>" +
            "<c:set xmlns:c=\"marmalade:jstl-core\" var=\"testVar\" value=\"testValue\"/>" +
        "</mojo>";
    
    public void testShouldFindPluginWithOneMojoFromClassDirectory() throws IOException {
        File directory = new File("test-dir");
        File scriptFile = new File(directory, "test.mmld");
        
        try {
            if (!directory.exists()) {
                directory.mkdirs();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    scriptFile));
            writer.write(TEST_MOJO_SCRIPT);
            writer.flush();
            writer.close();

            ClassRealm realm = new DefaultClassRealm(new ClassWorld(), "root");
            realm.addConstituent(directory.toURL());

            MarmaladePluginDiscoverer discoverer = new MarmaladePluginDiscoverer();
            List componentSets = discoverer.findComponents(realm);

            verifyComponentSets(componentSets);
        } 
        finally {
            if(scriptFile.exists()) {
                scriptFile.delete();
            }
            
            if(directory.exists()) {
                directory.delete();
            }
        }
    }

    public void testShouldFindPluginWithOneMojoFromJar() throws IOException {
        File jar = new File("test-plugin.jar");
        
        try {
            JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(jar));
            jarOut.putNextEntry(new JarEntry("test.mmld"));
            jarOut.write(TEST_MOJO_SCRIPT.getBytes());
            jarOut.flush();
            jarOut.closeEntry();
            jarOut.close();

            ClassRealm realm = new DefaultClassRealm(new ClassWorld(), "root");
            realm.addConstituent(new URL("jar:" + jar.toURL().toExternalForm() + "!/"));

            MarmaladePluginDiscoverer discoverer = new MarmaladePluginDiscoverer();
            List componentSets = discoverer.findComponents(realm);

            verifyComponentSets(componentSets);
        } 
        finally {
            if(jar.exists()) {
                jar.delete();
            }
        }
    }
    private void verifyComponentSets(List componentSets) {
        assertNotNull(componentSets);
        assertEquals(1, componentSets.size());

        ComponentSetDescriptor setDescriptor = (ComponentSetDescriptor) componentSets
                .get(0);
        List components = setDescriptor.getComponents();

        assertNotNull(components);
        assertEquals(1, components.size());

        ComponentDescriptor descriptor = (ComponentDescriptor) components
                .get(0);
        
        assertEquals("marmalade", descriptor.getComponentFactory());

        MavenMojoDescriptor mojoDesc = (MavenMojoDescriptor) descriptor;

        MojoDescriptor mojo = mojoDesc.getMojoDescriptor();
        assertEquals("test", mojo.getId());
        assertEquals("myTest", mojo.getGoal());
        assertEquals("singleton", mojo.getInstantiationStrategy());
        assertEquals("always", mojo.getExecutionStrategy());
        
        List prereqs = mojoDesc.getPrereqs();
        
        assertEquals(1, prereqs.size());
        assertEquals("test-prereq", prereqs.get(0));
        
        List dependencies = setDescriptor.getDependencies();
        
        assertEquals(1, dependencies.size());
        
        ComponentDependency dep = (ComponentDependency)dependencies.get(0);
        assertEquals("marmalade", dep.getGroupId());
        assertEquals("marmalade-core", dep.getArtifactId());
        assertEquals("0.2", dep.getVersion());
        
        List parameters = mojo.getParameters();
        assertEquals(1, parameters.size());
        
        Parameter param = (Parameter)parameters.get(0);
        assertEquals("param", param.getName());
        assertEquals("String", param.getType());
        assertEquals(true, param.isRequired());
        assertEquals("something", param.getValidator());
        assertEquals("false", param.getExpression());
    }

}
