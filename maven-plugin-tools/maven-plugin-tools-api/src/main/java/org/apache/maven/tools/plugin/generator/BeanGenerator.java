package org.apache.maven.tools.plugin.generator;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.MavenMojoDescriptor;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.modello.generator.java.javasource.JClass;
import org.codehaus.modello.generator.java.javasource.JConstructor;
import org.codehaus.modello.generator.java.javasource.JMethod;
import org.codehaus.modello.generator.java.javasource.JParameter;
import org.codehaus.modello.generator.java.javasource.JSourceWriter;
import org.codehaus.modello.generator.java.javasource.JType;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @todo use the descriptions in the descriptor for the javadoc pushed into the
 *       source code.
 */
public class BeanGenerator
    implements Generator
{
    public void execute( String destinationDirectory, Set mavenMojoDescriptors, MavenProject project ) throws Exception
    {
        for ( Iterator it = mavenMojoDescriptors.iterator(); it.hasNext(); )
        {
            MavenMojoDescriptor descriptor = (MavenMojoDescriptor) it.next();
            processPluginDescriptor( descriptor, destinationDirectory );
        }
    }

    protected void processPluginDescriptor( MavenMojoDescriptor descriptor, String destinationDirectory )
        throws Exception
    {
        String implementation = descriptor.getImplementation();

        String className = implementation.substring( implementation.lastIndexOf( "." ) + 1 ) + "Bean";

        JClass jClass = new JClass( className );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        jClass.setSuperClass( "org.apache.maven.plugin.BeanPluginAdapter" );

        jClass.addImport( "java.util.*" );

        // ----------------------------------------------------------------------
        // Use the same package as the plugin we are wrapping.
        // ----------------------------------------------------------------------

        String packageName = implementation.substring( 0, implementation.lastIndexOf( "." ) );

        jClass.setPackageName( packageName );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        JConstructor constructor = new JConstructor( jClass );

        constructor.getSourceCode().add( "super( new " + implementation + "() );" );

        jClass.addConstructor( constructor );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        MojoDescriptor mojoDescriptor = descriptor.getMojoDescriptor();
        List parameters = mojoDescriptor.getParameters();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

            jClass.addMethod( createSetter( parameter, jClass ) );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String packageDirectory = replace( packageName, ".", "/", -1 );

        File destination = new File( destinationDirectory, packageDirectory + "/" + className + ".java" );

        if ( !destination.getParentFile().exists() )
        {
            destination.getParentFile().mkdirs();
        }

        FileWriter writer = new FileWriter( destination );

        JSourceWriter sourceWriter = new JSourceWriter( writer );

        jClass.print( sourceWriter );

        writer.flush();

        writer.close();
    }

    private JMethod createSetter( Parameter parameter, JClass jClass )
    {
        String propertyName = capitalise( parameter.getName() );

        JMethod setter = new JMethod( null, "set" + propertyName );

        String type = parameter.getType();

        JType parameterType;

        int arrayLocation = type.indexOf( "[]" );

        if ( arrayLocation > 0 )
        {
            type = type.substring( 0, arrayLocation );

            parameterType = new JClass( type ).createArray();
        }
        else
        {
            parameterType = new JClass( type );
        }

        setter.addParameter( new JParameter( parameterType, parameter.getName() ) );

        setter.getSourceCode().add(
            "addParameter( " + "\"" + parameter.getName() + "\", " + parameter.getName() + " );" );

        return setter;
    }

    protected String capitalise( String str )
    {
        if ( str == null || str.length() == 0 )
        {
            return str;
        }

        return new StringBuffer( str.length() ).append( Character.toTitleCase( str.charAt( 0 ) ) ).append(
            str.substring( 1 ) ).toString();
    }

    protected String replace( String text, String repl, String with, int max )
    {
        if ( text == null || repl == null || with == null || repl.length() == 0 )
        {
            return text;
        }

        StringBuffer buf = new StringBuffer( text.length() );
        int start = 0, end = 0;
        while ( (end = text.indexOf( repl, start )) != -1 )
        {
            buf.append( text.substring( start, end ) ).append( with );
            start = end + repl.length();

            if ( --max == 0 )
            {
                break;
            }
        }
        buf.append( text.substring( start ) );
        return buf.toString();
    }
}