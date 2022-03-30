package org.codehaus.modello.plugin.velocity;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.modello.maven.AbstractModelloGeneratorMojo;
import org.codehaus.modello.model.ModelField;

/**
 * Creates an XML schema from the model.
 *
 * @author <a href="mailto:brett@codehaus.org">Brett Porter</a>
 */
@Mojo( name = "velocity", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true )
public class ModelloVelocityMojo
        extends AbstractModelloGeneratorMojo
{
    /**
     * The output directory of the generated XML Schema.
     */
    @Parameter( defaultValue = "${project.build.directory}/generated-sources/modello", required = true )
    private File outputDirectory;

    @Parameter
    private List<String> templates;

    protected String getGeneratorType()
    {
        return "velocity";
    }

    protected void customizeParameters( Properties parameters )
    {
        try
        {
            Field field = ModelField.class.getDeclaredField( "PRIMITIVE_TYPES" );
            field.setAccessible( true );
            unsetFinal( field );
            field.set( null, new String[] { "boolean", "Boolean", "char", "Character", "byte",
                    "Byte", "short", "Short", "int", "Integer", "long", "Long", "float", "Float",
                    "double", "Double", "String", "Date", "DOM", "java.nio.file.Path" } );

            super.customizeParameters( parameters );
            parameters.put( "basedir", Objects.requireNonNull( getBasedir(), "basedir is null" ) );
            parameters.put( VelocityGenerator.VELOCITY_TEMPLATES, String.join( ",", templates ) );
        }
        catch ( Throwable e )
        {
            throw new RuntimeException( e );
        }
    }

    private void unsetFinal( Field field ) throws IllegalAccessException, NoSuchFieldException
    {
// TODO: on jdk >= 12, accessing Field.modifiers fail, need to check with VarHandle or adds an open module
//        try
//        {
            Field modifiersField = Field.class.getDeclaredField( "modifiers" );
            modifiersField.setAccessible( true );
            modifiersField.setInt( field, field.getModifiers() & ~Modifier.FINAL );
//        }
//        catch ( Throwable t )
//        {
//            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
//            VarHandle modifiers = lookup.findVarHandle( Field.class, "modifiers", int.class );
//            modifiers.set( field, field.getModifiers() & ~Modifier.FINAL );
//        }
    }

    protected boolean producesCompilableResult()
    {
        return true;
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }
}
