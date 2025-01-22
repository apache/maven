package org.apache.maven.plugins;


import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

@MojoTest
public class HelloMojoTest {


    @Test
    @InjectMojo(goal="hello")
    @MojoParameter(name="name", value = "World")
    public void testHello(HelloMojo mojoUnderTest) {
        mojoUnderTest.execute();
    }

}
