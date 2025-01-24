package org.apache.maven.plugins;


import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@MojoTest
public class HelloMojoTest {

    @Inject
    private MavenDIComponent componentMock;

    @Test
    @InjectMojo(goal="hello")
    @MojoParameter(name="name", value = "World")
    public void testHello(HelloMojo mojoUnderTest) {

        mojoUnderTest.execute();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(componentMock).hello(captor.capture());
        assertThat(captor.getValue()).isEqualTo("World");
    }

    @Singleton
    @Provides
    private MavenDIComponent createMavenDIComponent() {
        return mock(MavenDIComponent.class);
    }

}
