package org.apache.maven.project.interpolation;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;

import java.util.HashMap;
import java.util.Properties;

import junit.framework.TestCase;

public class ModelInterpolationEdgeCases
    extends TestCase
{

    public void testMultiExpressionRecursion()
        throws Throwable
    {
        Test test = new Test()
        {

            void run()
            {
                Model model = new Model();
                Build build = new Build();

                model.setBuild( build );

                build.setDirectory( "${project.build.sourceDirectory}" );
                build.setSourceDirectory( "${project.build.testSourceDirectory}/../main" );
                build.setTestSourceDirectory( "${project.build.directory}/test" );

                try
                {
                    interpolate( model );

                    System.out.println( "Should fail." );

                    failureMessage = "should fail with expression cycle in build paths.";
//                    fail("should fail with expression cycle in build paths.");
                }
                catch ( ModelInterpolationException e )
                {
                    // expected
                }
                catch ( Throwable e )
                {
                    error = e;
//                    throw e;
                }
            }
        };


        run( test );
    }

    public void testMultiPrefixRecursion_Synonyms()
        throws Throwable
    {
        Test test = new Test()
        {

            void run()
            {
                Model model = new Model();
                Build build = new Build();

                model.setBuild( build );

                build.setDirectory( "${project.build.directory}" );
                build.setSourceDirectory( "${pom.build.sourceDirectory}" );
                build.setTestSourceDirectory( "${build.testSourceDirectory}" );

                try
                {
                    interpolate( model );

                    System.out.println( "Should fail." );

                    failureMessage = "should fail with expression synonym recursion.";
                }
                catch ( ModelInterpolationException e )
                {
                    // expected
                }
                catch ( Throwable e )
                {
                    error = e;
                }
            }
        };

        run( test );
    }

    public void testMultiPrefixSeparation_EnvarPropertyDifference()
        throws Throwable
    {
        Test test = new Test()
        {

            void run()
            {
                Model model = new Model();

                Properties properties = new Properties();

                properties.setProperty( "ENVAR", "pomValue" );
                properties.setProperty( "prop", "${env.ENVAR}" );
                properties.setProperty( "prop2", "${pom.ENVAR}" );

                model.setProperties( properties );

                try
                {
                    Model result = interpolate( model );

                    Properties resultProps = result.getProperties();
                    assertFalse( "Values should not match!", resultProps.getProperty( "prop" ).equals( resultProps.getProperty( "prop2" ) ) );
                }
                catch ( ModelInterpolationException e )
                {
                    error = e;
                }
                catch ( Throwable e )
                {
                    error = e;
                }
            }
        };

        run( test );
    }

    private void run( Test test )
        throws Throwable
    {
        Thread t = new Thread( new TestRunnable( test ) );
        t.setDaemon( true );

        t.start();

        try
        {
            t.join( 5000 );
        }
        catch ( InterruptedException e )
        {
        }

        if ( t.isAlive() )
        {
            StackTraceElement element = new Throwable().getStackTrace()[1];

            fail( "Test: "
                  + element.getMethodName()
                  + " failed to execute in 5 seconds. Looks like it's caught in an infinite loop of some kind." );
        }
        else
        {
            test.checkFailure();
        }
    }

    private Model interpolate( Model model )
        throws Throwable
    {
        ModelInterpolator interpolator = new RegexBasedModelInterpolator();

        return interpolator.interpolate( model, new HashMap(), true );
    }

    private static final class TestRunnable
        implements Runnable
    {
        private Test test;

        TestRunnable( Test test )
        {
            this.test = test;
        }

        public void run()
        {
            test.run();
        }

    }

    private static abstract class Test
    {
        String failureMessage;

        Throwable error;

        abstract void run();

        void checkFailure()
            throws Throwable
        {
            if ( error != null )
            {
                throw error;
            }
            else if ( failureMessage != null )
            {
                fail( failureMessage );
            }
        }

    }

}
