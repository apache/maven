/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.buildcache.its.junit;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.maven.buildcache.CacheUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock( Resources.SYSTEM_PROPERTIES )
public class IntegrationTestExtension implements BeforeAllCallback, TestTemplateInvocationContextProvider
{

    private static boolean initialized;
    private static Path maven3;
    private static Path maven4;

    @Override
    public void beforeAll( ExtensionContext context ) throws Exception
    {
        buildMaven();
    }

    @Override
    public boolean supportsTestTemplate( ExtensionContext context )
    {
        return context.getTestMethod()
                .filter( m -> m.isAnnotationPresent( Test.class ) )
                .isPresent();
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext )
    {
        Method m = extensionContext.getRequiredTestMethod();
        return Stream.of( maven3, maven4 ).map( p -> new MavenTemplate( m, p ) );
    }

    private static void buildMaven() throws Exception
    {
        if ( initialized )
        {
            return;
        }
        String root = Objects.requireNonNull( System.getProperty( "maven.multiModuleProjectDirectory" ),
                "The 'maven.multiModuleProjectDirectory' system property need to be set" );

        // maven3
        Path maven3Zip = Paths.get( root, "maven/maven3/apache-maven/target/apache-maven-bin.zip" );
        if ( !Files.exists( maven3Zip ) )
        {
            throw new IllegalStateException( "Unable to find " + maven3Zip + "\n"
                    + "Please build the maven3 and maven4 distributions using the build-maven.sh script" );
        }
        Path outMaven3 = Paths.get( "target/maven3" );
        deleteDir( outMaven3 );
        Files.createDirectories( outMaven3 );
        CacheUtils.unzip( maven3Zip, outMaven3 );
        maven3 = outMaven3.resolve( "apache-maven" ).toAbsolutePath();
        maven3.resolve( "bin/mvn" ).toFile().setExecutable( true );

        // maven4
        Path maven4Zip = Paths.get( root, "maven/maven4/apache-maven/target/apache-maven-bin.zip" );
        if ( !Files.exists( maven4Zip ) )
        {
            throw new IllegalStateException( "Unable to find " + maven4Zip + "\n"
                    + "Please build the maven3 and maven4 distributions using the build-maven.sh script" );
        }
        Path outMaven4 = Paths.get( "target/maven4" );
        deleteDir( outMaven4 );
        Files.createDirectories( outMaven4 );
        CacheUtils.unzip( maven4Zip, outMaven4 );
        maven4 = outMaven4.resolve( "apache-maven" ).toAbsolutePath();
        maven4.resolve( "bin/mvn" ).toFile().setExecutable( true );

        initialized = true;
    }

    public static Path deleteDir( Path dir )
    {
        return deleteDir( dir, true );
    }

    public static Path deleteDir( Path dir, boolean failOnError )
    {
        if ( Files.exists( dir ) )
        {
            try ( Stream<Path> files = Files.walk( dir ) )
            {
                files.sorted( Comparator.reverseOrder() ).forEach( f -> deleteFile( f, failOnError ) );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( "Could not walk " + dir, e );
            }
        }
        return dir;
    }

    private static void deleteFile( Path f, boolean failOnError )
    {
        try
        {
            Files.delete( f );
        }
        catch ( Exception e )
        {
            if ( failOnError )
            {
                throw new RuntimeException( "Could not delete " + f, e );
            }
            else
            {
                System.err.println( "Error deleting " + f + ": " + e );
            }
        }
    }

    public static class MavenTemplate implements TestTemplateInvocationContext
    {

        private final Method method;
        private final Path mavenPath;

        public MavenTemplate( Method method, Path mavenPath )
        {
            this.method = method;
            this.mavenPath = mavenPath;
        }

        @Override
        public String getDisplayName( int invocationIndex )
        {
            return mavenPath == maven3 ? "[maven3]" : "[maven4]";
        }

        @Override
        public List<Extension> getAdditionalExtensions()
        {
            return Arrays.asList( ( TestInstancePostProcessor ) this::postProcessTestInstance,
                    new VerifierParameterResolver() );
        }

        protected void postProcessTestInstance( Object testInstance, ExtensionContext context ) throws Exception
        {
            if ( !context.getTestMethod().isPresent() )
            {
                return;
            }
            final Class<?> testClass = context.getRequiredTestClass();
            final IntegrationTest test = testClass.getAnnotation( IntegrationTest.class );
            final String rawProjectDir = test.value();
            final String className = context.getRequiredTestClass().getSimpleName();
            String methodName = context.getRequiredTestMethod().getName();
            if ( rawProjectDir == null )
            {
                throw new IllegalStateException( "@IntegrationTest must be set" );
            }
            final Path testDir = Paths.get( "target/maven-tests/" + className + "/" + methodName + "/"
                    + ( mavenPath == maven3 ? "maven3" : "maven4" ) ).toAbsolutePath();
            deleteDir( testDir );
            Files.createDirectories( testDir );
            final Path testExecutionDir;

            final Path testSrcDir = Paths.get( rawProjectDir ).toAbsolutePath().normalize();
            if ( !Files.exists( testSrcDir ) )
            {
                throw new IllegalStateException( "@IntegrationTest(\"" + testSrcDir
                        + "\") points at a path that does not exist: " + testSrcDir );
            }
            testExecutionDir = testDir.resolve( "project" );
            try ( Stream<Path> files = Files.walk( testSrcDir ) )
            {
                files.forEach( source ->
                {
                    final Path dest = testExecutionDir.resolve( testSrcDir.relativize( source ) );
                    try
                    {
                        if ( Files.isDirectory( source ) )
                        {
                            Files.createDirectories( dest );
                        }
                        else
                        {
                            Files.createDirectories( dest.getParent() );
                            Files.copy( source, dest );
                        }
                    }
                    catch ( IOException e )
                    {
                        throw new RuntimeException( e );
                    }
                } );
            }
        }

        private class VerifierParameterResolver implements ParameterResolver
        {

            @Override
            public boolean supportsParameter( ParameterContext parameterContext,
                    ExtensionContext extensionContext )
                    throws ParameterResolutionException
            {
                return parameterContext.getParameter().getType() == Verifier.class;
            }

            @Override
            public Object resolveParameter( ParameterContext parameterContext,
                    ExtensionContext context )
                    throws ParameterResolutionException
            {
                String prevMavenHome = System.getProperty( "maven.home" );
                try
                {
                    final IntegrationTest test = context.getRequiredTestClass().getAnnotation( IntegrationTest.class );
                    final String rawProjectDir = test.value();
                    if ( rawProjectDir == null )
                    {
                        throw new IllegalStateException( "value of @IntegrationTest must be set" );
                    }

                    final String className = context.getRequiredTestClass().getSimpleName();
                    String methodName = context.getRequiredTestMethod().getName();
                    final Path testDir = Paths.get( "target/mvnd-tests/" + className + "/" + methodName + "/"
                            + ( mavenPath == maven3 ? "maven3" : "maven4" ) ).toAbsolutePath();

                    deleteDir( testDir );
                    Files.createDirectories( testDir );

                    final Path testSrcDir = Paths.get( rawProjectDir ).toAbsolutePath().normalize();
                    if ( !Files.exists( testSrcDir ) )
                    {
                        throw new IllegalStateException( "@IntegrationTest(\"" + testSrcDir
                                + "\") points at a path that does not exist: " + testSrcDir );
                    }

                    final Path testExecutionDir = testDir.resolve( "project" );
                    try ( Stream<Path> files = Files.walk( testSrcDir ) )
                    {
                        files.forEach( source ->
                        {
                            final Path dest = testExecutionDir.resolve( testSrcDir.relativize( source ) );
                            try
                            {
                                if ( Files.isDirectory( source ) )
                                {
                                    Files.createDirectories( dest );
                                }
                                else
                                {
                                    Files.createDirectories( dest.getParent() );
                                    Files.copy( source, dest );
                                }
                            }
                            catch ( IOException e )
                            {
                                throw new RuntimeException( e );
                            }
                        } );
                    }

                    System.setProperty( "maven.home", mavenPath.toString() );
                    Verifier verifier = new Verifier( testExecutionDir.toString() );
                    verifier.setLogFileName( "../log.txt" );
                    return verifier;
                }
                catch ( VerificationException | IOException e )
                {
                    throw new ParameterResolutionException( "Unable to create Verifier", e );
                }
                finally
                {
                    if ( prevMavenHome != null )
                    {
                        System.setProperty( "maven.home", prevMavenHome );
                    }
                    else
                    {
                        System.clearProperty( "maven.home" );
                    }
                }
            }
        }
    }

}
