package org.apache.maven.project.interpolation;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.maven.model.Model;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.logging.Logger;

import sun.security.action.GetIntegerAction;

public class StringSearchModelInterpolator
    extends AbstractStringBasedModelInterpolator
{

    private static final Map fieldsByClass = new WeakHashMap();
    private static final Map fieldIsPrimitiveByClass = new WeakHashMap();

    public StringSearchModelInterpolator()
    {
    }

    public StringSearchModelInterpolator( PathTranslator pathTranslator )
    {
        super( pathTranslator );
    }

    public Model interpolate( Model model, File projectDir, ProjectBuilderConfiguration config, boolean debugEnabled )
        throws ModelInterpolationException
    {
        interpolateObject( model, model, projectDir, config, debugEnabled );
        
        return model;
    }
    
    protected void interpolateObject( Object obj, Model model, File projectDir, ProjectBuilderConfiguration config,
                                      boolean debugEnabled )
        throws ModelInterpolationException
    {
        try
        {
            List valueSources = createValueSources( model, projectDir, config );
            List postProcessors = createPostProcessors( model, projectDir, config );
            
            InterpolateObjectAction action =
                new InterpolateObjectAction( obj, valueSources, postProcessors, debugEnabled,
                                             this, getLogger() );
            
            ModelInterpolationException error =
                (ModelInterpolationException) AccessController.doPrivileged( action );
            
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
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers( true );
        
        return interpolator;
    }
    
    private static final class InterpolateObjectAction implements PrivilegedAction
    {

        private final boolean debugEnabled;
        private final LinkedList interpolationTargets;
        private final StringSearchModelInterpolator modelInterpolator;
        private final Logger logger;
        private final List valueSources;
        private final List postProcessors;
        
        public InterpolateObjectAction( Object target, List valueSources, List postProcessors,
                                        boolean debugEnabled,
                                        StringSearchModelInterpolator modelInterpolator, Logger logger )
        {
            this.valueSources = valueSources;
            this.postProcessors = postProcessors;
            this.debugEnabled = debugEnabled;
            
            this.interpolationTargets = new LinkedList();
            interpolationTargets.add( target );
            
            this.modelInterpolator = modelInterpolator;
            this.logger = logger;
        }

        public Object run()
        {
            while( !interpolationTargets.isEmpty() )
            {
                Object obj = interpolationTargets.removeFirst();
                
                try
                {
                    traverseObjectWithParents( obj.getClass(), obj );
                }
                catch ( ModelInterpolationException e )
                {
                    return e;
                }
            }
            
            return null;
        }

        private void traverseObjectWithParents( Class cls, Object target )
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
                Field[] fields = (Field[]) fieldsByClass.get( cls );
                if ( fields == null )
                {
                    fields = cls.getDeclaredFields();
                    fieldsByClass.put( cls, fields );
                }
                
                for ( int i = 0; i < fields.length; i++ )
                {
                    Class type = fields[i].getType();
                    if ( isQualifiedForInterpolation( fields[i], type ) )
                    {
                        boolean isAccessible = fields[i].isAccessible();
                        fields[i].setAccessible( true );
                        try
                        {
                            try
                            {
                                if ( String.class == type )
                                {
                                    String value = (String) fields[i].get( target );
                                    if ( value != null )
                                    {
                                        String interpolated = modelInterpolator.interpolateInternal( value, valueSources, postProcessors, debugEnabled );
                                        
                                        if ( !interpolated.equals( value ) )
                                        {
                                            fields[i].set( target, interpolated );
                                        }
                                    }
                                }
                                else if ( Collection.class.isAssignableFrom( type ) )
                                {
                                    Collection c = (Collection) fields[i].get( target );
                                    if ( c != null && !c.isEmpty() )
                                    {
                                        List originalValues = new ArrayList( c );
                                        try
                                        {
                                            c.clear();
                                        }
                                        catch( UnsupportedOperationException e )
                                        {
                                            if ( debugEnabled && logger != null )
                                            {
                                                logger.debug( "Skipping interpolation of field: " + fields[i] + " in: " + cls.getName() + "; it is an unmodifiable collection." );
                                            }
                                            continue;
                                        }
                                        
                                        for ( Iterator it = originalValues.iterator(); it.hasNext(); )
                                        {
                                            Object value = it.next();
                                            if ( value != null )
                                            {
                                                if( String.class == value.getClass() )
                                                {
                                                    String interpolated = modelInterpolator.interpolateInternal( (String) value, valueSources, postProcessors, debugEnabled );
                                                    
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
                                    Map m = (Map) fields[i].get( target );
                                    if ( m != null && !m.isEmpty() )
                                    {
                                        for ( Iterator it = m.entrySet().iterator(); it.hasNext(); )
                                        {
                                            Map.Entry entry = (Map.Entry) it.next();
                                            
                                            Object value = entry.getValue();
                                            
                                            if ( value != null )
                                            {
                                                if( String.class == value.getClass() )
                                                {
                                                    String interpolated = modelInterpolator.interpolateInternal( (String) value, valueSources, postProcessors, debugEnabled );
                                                    
                                                    if ( !interpolated.equals( value ) )
                                                    {
                                                        try
                                                        {
                                                            entry.setValue( interpolated );
                                                        }
                                                        catch( UnsupportedOperationException e )
                                                        {
                                                            if ( debugEnabled && logger != null )
                                                            {
                                                                logger.debug( "Skipping interpolation of field: " + fields[i] + " (key: " + entry.getKey() + ") in: " + cls.getName() + "; it is an unmodifiable collection." );
                                                            }
                                                            continue;
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
                                    Object value = fields[i].get( target );
                                    if ( value != null )
                                    {
                                        if ( fields[i].getType().isArray() )
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
                            catch ( IllegalArgumentException e )
                            {
                                throw new ModelInterpolationException( "Failed to interpolate field: " + fields[i] + " on class: " + cls.getName(), e );
                            }
                            catch ( IllegalAccessException e )
                            {
                                throw new ModelInterpolationException( "Failed to interpolate field: " + fields[i] + " on class: " + cls.getName(), e );
                            }
                        }
                        finally
                        {
                            fields[i].setAccessible( isAccessible );
                        }
                    }
                }
                
                traverseObjectWithParents( cls.getSuperclass(), target );
            }
        }

        private boolean isQualifiedForInterpolation( Class cls )
        {
            return !cls.getPackage().getName().startsWith( "java" );
        }

        private boolean isQualifiedForInterpolation( Field field, Class fieldType )
        {
            if ( !fieldIsPrimitiveByClass.containsKey( fieldType ) )
            {
                fieldIsPrimitiveByClass.put( fieldType, Boolean.valueOf( fieldType.isPrimitive() ) );
            }
            
            if ( ((Boolean) fieldIsPrimitiveByClass.get( fieldType )).booleanValue() )
            {
                return false;
            }
            
//            if ( fieldType.isPrimitive() )
//            {
//                return false;
//            }
            
            if ( "parent".equals( field.getName() ) )
            {
                return false;
            }
            
            return true;
        }

        private void evaluateArray( Object target )
            throws ModelInterpolationException
        {
            int len = Array.getLength( target );
            for( int i = 0; i < len; i++ )
            {
                Object value = Array.get( target, i );
                if ( value != null )
                {
                    if ( String.class == value.getClass() )
                    {
                        String interpolated = modelInterpolator.interpolateInternal( (String) value, valueSources, postProcessors, debugEnabled );
                        
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
