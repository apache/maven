package org.apache.maven.project.interpolation;

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

import org.apache.maven.model.Model;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * StringSearchModelInterpolator
 */
@Deprecated
@Component( role = ModelInterpolator.class )
public class StringSearchModelInterpolator
    extends AbstractStringBasedModelInterpolator
{

    private static final Map<Class<?>, Field[]> FIELDS_BY_CLASS = new WeakHashMap<>();
    private static final Map<Class<?>, Boolean> PRIMITIVE_BY_CLASS = new WeakHashMap<>();

    public StringSearchModelInterpolator()
    {
    }

    public StringSearchModelInterpolator( final PathTranslator pathTranslator )
    {
        super( pathTranslator );
    }

    public Model interpolate( final Model model, final File projectDir, final ProjectBuilderConfiguration config, final boolean debugEnabled )
        throws ModelInterpolationException
    {
        interpolateObject( model, model, projectDir, config, debugEnabled );

        return model;
    }

    protected void interpolateObject( final Object obj, final Model model, final File projectDir, final ProjectBuilderConfiguration config,
                                      final boolean debugEnabled )
        throws ModelInterpolationException
    {
        try
        {
            final List<ValueSource> valueSources = createValueSources( model, projectDir, config );
            final List<InterpolationPostProcessor> postProcessors = createPostProcessors( model, projectDir, config );

            final InterpolateObjectAction action =
                new InterpolateObjectAction( obj, valueSources, postProcessors, debugEnabled,
                                             this, getLogger() );

            final ModelInterpolationException error = AccessController.doPrivileged( action );

            if ( error != null )
            {
                throw error;
            }
        }
        finally
        {
            getInterpolator().clearAnswers();
        }
    }

    protected Interpolator createInterpolator()
    {
        final StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers( true );

        return interpolator;
    }

    private static final class InterpolateObjectAction implements PrivilegedAction<ModelInterpolationException>
    {

        private final boolean debugEnabled;
        private final LinkedList<Object> interpolationTargets;
        private final StringSearchModelInterpolator modelInterpolator;
        private final Logger logger;
        private final List<ValueSource> valueSources;
        private final List<InterpolationPostProcessor> postProcessors;

        InterpolateObjectAction( final Object target, final List<ValueSource> valueSources,
                                 final List<InterpolationPostProcessor> postProcessors, final boolean debugEnabled,
                                 final StringSearchModelInterpolator modelInterpolator, final Logger logger )
        {
            this.valueSources = valueSources;
            this.postProcessors = postProcessors;
            this.debugEnabled = debugEnabled;

            this.interpolationTargets = new LinkedList<>();
            interpolationTargets.add( target );

            this.modelInterpolator = modelInterpolator;
            this.logger = logger;
        }

        public ModelInterpolationException run()
        {
            while ( !interpolationTargets.isEmpty() )
            {
                final Object obj = interpolationTargets.removeFirst();

                try
                {
                    traverseObjectWithParents( obj.getClass(), obj );
                }
                catch ( final ModelInterpolationException e )
                {
                    return e;
                }
            }

            return null;
        }

        @SuppressWarnings( { "unchecked", "checkstyle:methodlength" } )
        private void traverseObjectWithParents( final Class<?> cls, final Object target )
            throws ModelInterpolationException
        {
            if ( cls == null )
            {
                return;
            }


            if ( cls.isArray() )
            {
                evaluateArray( target );
            }
            else if ( isQualifiedForInterpolation( cls ) )
            {
                final Field[] fields = FIELDS_BY_CLASS.computeIfAbsent( cls, k -> cls.getDeclaredFields() );

                for ( final Field field : fields )
                {
                    final Class<?> type = field.getType();
                    if ( isQualifiedForInterpolation( field, type ) )
                    {
                        final boolean isAccessible = field.isAccessible();
                        field.setAccessible( true );
                        try
                        {
                            try
                            {
                                if ( String.class == type )
                                {
                                    final String value = (String) field.get( target );
                                    if ( value != null )
                                    {
                                        final String interpolated =
                                            modelInterpolator.interpolateInternal( value, valueSources, postProcessors,
                                                                                   debugEnabled );

                                        if ( !interpolated.equals( value ) )
                                        {
                                            field.set( target, interpolated );
                                        }
                                    }
                                }
                                else if ( Collection.class.isAssignableFrom( type ) )
                                {
                                    final Collection<Object> c = (Collection<Object>) field.get( target );
                                    if ( c != null && !c.isEmpty() )
                                    {
                                        final List<Object> originalValues = new ArrayList<>( c );
                                        try
                                        {
                                            c.clear();
                                        }
                                        catch ( final UnsupportedOperationException e )
                                        {
                                            if ( debugEnabled && logger != null )
                                            {
                                                logger.debug( "Skipping interpolation of field: " + field + " in: "
                                                                  + cls.getName()
                                                                  + "; it is an unmodifiable collection." );
                                            }
                                            continue;
                                        }

                                        for ( final Object value : originalValues )
                                        {
                                            if ( value != null )
                                            {
                                                if ( String.class == value.getClass() )
                                                {
                                                    final String interpolated =
                                                        modelInterpolator.interpolateInternal( (String) value,
                                                                                               valueSources,
                                                                                               postProcessors,
                                                                                               debugEnabled );

                                                    if ( !interpolated.equals( value ) )
                                                    {
                                                        c.add( interpolated );
                                                    }
                                                    else
                                                    {
                                                        c.add( value );
                                                    }
                                                }
                                                else
                                                {
                                                    c.add( value );
                                                    if ( value.getClass().isArray() )
                                                    {
                                                        evaluateArray( value );
                                                    }
                                                    else
                                                    {
                                                        interpolationTargets.add( value );
                                                    }
                                                }
                                            }
                                            else
                                            {
                                                // add the null back in...not sure what else to do...
                                                c.add( value );
                                            }
                                        }
                                    }
                                }
                                else if ( Map.class.isAssignableFrom( type ) )
                                {
                                    final Map<Object, Object> m = (Map<Object, Object>) field.get( target );
                                    if ( m != null && !m.isEmpty() )
                                    {
                                        for ( final Map.Entry<Object, Object> entry : m.entrySet() )
                                        {
                                            final Object value = entry.getValue();

                                            if ( value != null )
                                            {
                                                if ( String.class == value.getClass() )
                                                {
                                                    final String interpolated =
                                                        modelInterpolator.interpolateInternal( (String) value,
                                                                                               valueSources,
                                                                                               postProcessors,
                                                                                               debugEnabled );

                                                    if ( !interpolated.equals( value ) )
                                                    {
                                                        try
                                                        {
                                                            entry.setValue( interpolated );
                                                        }
                                                        catch ( final UnsupportedOperationException e )
                                                        {
                                                            if ( debugEnabled && logger != null )
                                                            {
                                                                logger.debug(
                                                                    "Skipping interpolation of field: " + field
                                                                        + " (key: " + entry.getKey() + ") in: "
                                                                        + cls.getName()
                                                                        + "; it is an unmodifiable collection." );
                                                            }
                                                        }
                                                    }
                                                }
                                                else
                                                {
                                                    if ( value.getClass().isArray() )
                                                    {
                                                        evaluateArray( value );
                                                    }
                                                    else
                                                    {
                                                        interpolationTargets.add( value );
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    final Object value = field.get( target );
                                    if ( value != null )
                                    {
                                        if ( field.getType().isArray() )
                                        {
                                            evaluateArray( value );
                                        }
                                        else
                                        {
                                            interpolationTargets.add( value );
                                        }
                                    }
                                }
                            }
                            catch ( final IllegalArgumentException | IllegalAccessException e )
                            {
                                throw new ModelInterpolationException(
                                    "Failed to interpolate field: " + field + " on class: " + cls.getName(), e );
                            }
                        }
                        finally
                        {
                            field.setAccessible( isAccessible );
                        }
                    }
                }

                traverseObjectWithParents( cls.getSuperclass(), target );
            }
        }

        private boolean isQualifiedForInterpolation( final Class<?> cls )
        {
            return !cls.getPackage().getName().startsWith( "java" );
        }

        private boolean isQualifiedForInterpolation( final Field field, final Class<?> fieldType )
        {
            if ( !PRIMITIVE_BY_CLASS.containsKey( fieldType ) )
            {
                PRIMITIVE_BY_CLASS.put( fieldType, fieldType.isPrimitive() );
            }

            if ( PRIMITIVE_BY_CLASS.get( fieldType ) )
            {
                return false;
            }

//            if ( fieldType.isPrimitive() )
//            {
//                return false;
//            }

            return !"parent".equals( field.getName() );
        }

        private void evaluateArray( final Object target )
            throws ModelInterpolationException
        {
            final int len = Array.getLength( target );
            for ( int i = 0; i < len; i++ )
            {
                final Object value = Array.get( target, i );
                if ( value != null )
                {
                    if ( String.class == value.getClass() )
                    {
                        final String interpolated =
                            modelInterpolator.interpolateInternal( (String) value, valueSources, postProcessors,
                                                                   debugEnabled );

                        if ( !interpolated.equals( value ) )
                        {
                            Array.set( target, i, interpolated );
                        }
                    }
                    else
                    {
                        interpolationTargets.add( value );
                    }
                }
            }
        }
    }

}
