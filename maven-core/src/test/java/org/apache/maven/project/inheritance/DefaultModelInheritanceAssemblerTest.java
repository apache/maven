/* Created on Aug 23, 2004 */
package org.apache.maven.project.inheritance;


import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.UnitTest;
import org.apache.maven.project.MavenProject;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class DefaultModelInheritanceAssemblerTest extends TestCase {
    
    public void testShouldOverrideUnitTestExcludesOnly() {
        Model parent = new Model();
        parent.setGroupId("test");
        parent.setArtifactId("test");
        parent.setVersion("0.0");
        
        Build parentBuild = new Build();
        parentBuild.setSourceDirectory("src/main/java");
        parentBuild.setAspectSourceDirectory("src/main/aspects");
        parentBuild.setUnitTestSourceDirectory("src/test/java");
        
        UnitTest parentUT = new UnitTest();
        parentUT.setIncludes(Arrays.asList(new String[] {"**/*Test.java"}));
        parentUT.setExcludes(Arrays.asList(new String[] {"**/*Abstract*.java"}));
        
        parentBuild.setUnitTest(parentUT);
        parent.setBuild(parentBuild);
        
        Model child = new Model();
        
        Build childBuild = new Build();
        
        UnitTest childUT = new UnitTest();
        parentUT.setExcludes(Arrays.asList(new String[] {"**/*Abstract*.java", "**/*AspectTest.java"}));
        
        childBuild.setUnitTest(childUT);
        child.setBuild(childBuild);
        
        ModelInheritanceAssembler assembler = new DefaultModelInheritanceAssembler();
        assembler.assembleModelInheritance(child, parent);
        
        List childExcludesTest = child.getBuild().getUnitTest().getExcludes();
        assertEquals("source directory should be from parent", "src/main/java", child.getBuild().getSourceDirectory());
        assertEquals("unit test source directory should be from parent", "src/test/java", child.getBuild().getUnitTestSourceDirectory());
        assertEquals("aspect source directory should be from parent", "src/main/aspects", child.getBuild().getAspectSourceDirectory());
        assertTrue("unit test excludes should have **/*AspectTest.java", childExcludesTest.contains("**/*AspectTest.java"));
        assertTrue("unit test excludes should have **/*Abstract*.java", childExcludesTest.contains("**/*Abstract*.java"));
    }

}
