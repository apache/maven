package org.apache.maven.util.introspection;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;

import java.lang.reflect.Method;

/**
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:bob@werken.com">Bob McWhirter</a>
 * @author <a href="mailto:Christoph.Reck@dlr.de">Christoph Reck</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @version $Id$
 */
public class MethodMap
{
    private static final int MORE_SPECIFIC = 0;
    private static final int LESS_SPECIFIC = 1;
    private static final int INCOMPARABLE = 2;

    /**
     * Keep track of all methods with the same name.
     */
    Map methodByNameMap = new Hashtable();

    /**
     * Add a method to a list of methods by name.
     * For a particular class we are keeping track
     * of all the methods with the same name.
     */
    public void add(Method method)
    {
        String methodName = method.getName();

        List l = get( methodName );

        if ( l == null)
        {
            l = new ArrayList();
            methodByNameMap.put(methodName, l);
        }

        l.add(method);

        return;
    }

    /**
     * Return a list of methods with the same name.
     *
     * @param String key
     * @return List list of methods
     */
    public List get(String key)
    {
        return (List) methodByNameMap.get(key);
    }

    /**
     *  <p>
     *  Find a method.  Attempts to find the
     *  most specific applicable method using the
     *  algorithm described in the JLS section
     *  15.12.2 (with the exception that it can't
     *  distinguish a primitive type argument from
     *  an object type argument, since in reflection
     *  primitive type arguments are represented by
     *  their object counterparts, so for an argument of
     *  type (say) java.lang.Integer, it will not be able
     *  to decide between a method that takes int and a
     *  method that takes java.lang.Integer as a parameter.
     *  </p>
     *
     *  <p>
     *  This turns out to be a relatively rare case
     *  where this is needed - however, functionality
     *  like this is needed.
     *  </p>
     *
     *  @param methodName name of method
     *  @param args the actual arguments with which the method is called
     *  @return the most specific applicable method, or null if no
     *  method is applicable.
     *  @throws AmbiguousException if there is more than one maximally
     *  specific applicable method
     */
    public Method find(String methodName, Object[] args)
        throws AmbiguousException
    {
        List methodList = get(methodName);

        if (methodList == null)
        {
            return null;
        }

        int l = args.length;
        Class[] classes = new Class[l];

        for(int i = 0; i < l; ++i)
        {
            Object arg = args[i];

            /*
             * if we are careful down below, a null argument goes in there
             * so we can know that the null was passed to the method
             */
            classes[i] =
                    arg == null ? null : arg.getClass();
        }

        return getMostSpecific(methodList, classes);
    }

    /**
     *  simple distinguishable exception, used when
     *  we run across ambiguous overloading
     */
    public static class AmbiguousException extends Exception
    {
    }


    private static Method getMostSpecific(List methods, Class[] classes)
        throws AmbiguousException
    {
        LinkedList applicables = getApplicables(methods, classes);

        if(applicables.isEmpty())
        {
            return null;
        }

        if(applicables.size() == 1)
        {
            return (Method)applicables.getFirst();
        }

        /*
         * This list will contain the maximally specific methods. Hopefully at
         * the end of the below loop, the list will contain exactly one method,
         * (the most specific method) otherwise we have ambiguity.
         */

        LinkedList maximals = new LinkedList();

        for (Iterator applicable = applicables.iterator();
             applicable.hasNext();)
        {
            Method app = (Method) applicable.next();
            Class[] appArgs = app.getParameterTypes();
            boolean lessSpecific = false;

            for (Iterator maximal = maximals.iterator();
                 !lessSpecific && maximal.hasNext();)
            {
                Method max = (Method) maximal.next();

                switch(moreSpecific(appArgs, max.getParameterTypes()))
                {
                    case MORE_SPECIFIC:
                    {
                        /*
                         * This method is more specific than the previously
                         * known maximally specific, so remove the old maximum.
                         */

                        maximal.remove();
                        break;
                    }

                    case LESS_SPECIFIC:
                    {
                        /*
                         * This method is less specific than some of the
                         * currently known maximally specific methods, so we
                         * won't add it into the set of maximally specific
                         * methods
                         */

                        lessSpecific = true;
                        break;
                    }
                }
            }

            if(!lessSpecific)
            {
                maximals.addLast(app);
            }
        }

        if(maximals.size() > 1)
        {
            // We have more than one maximally specific method
            throw new AmbiguousException();
        }

        return (Method)maximals.getFirst();
    }

    /**
     * Determines which method signature (represented by a class array) is more
     * specific. This defines a partial ordering on the method signatures.
     * @param c1 first signature to compare
     * @param c2 second signature to compare
     * @return MORE_SPECIFIC if c1 is more specific than c2, LESS_SPECIFIC if
     * c1 is less specific than c2, INCOMPARABLE if they are incomparable.
     */
    private static int moreSpecific(Class[] c1, Class[] c2)
    {
        boolean c1MoreSpecific = false;
        boolean c2MoreSpecific = false;

        for(int i = 0; i < c1.length; ++i)
        {
            if(c1[i] != c2[i])
            {
                c1MoreSpecific =
                    c1MoreSpecific ||
                    isStrictMethodInvocationConvertible(c2[i], c1[i]);
                c2MoreSpecific =
                    c2MoreSpecific ||
                    isStrictMethodInvocationConvertible(c1[i], c2[i]);
            }
        }

        if(c1MoreSpecific)
        {
            if(c2MoreSpecific)
            {
                /*
                 *  Incomparable due to cross-assignable arguments (i.e.
                 * foo(String, Object) vs. foo(Object, String))
                 */

                return INCOMPARABLE;
            }

            return MORE_SPECIFIC;
        }

        if(c2MoreSpecific)
        {
            return LESS_SPECIFIC;
        }

        /*
         * Incomparable due to non-related arguments (i.e.
         * foo(Runnable) vs. foo(Serializable))
         */

        return INCOMPARABLE;
    }

    /**
     * Returns all methods that are applicable to actual argument types.
     * @param methods list of all candidate methods
     * @param classes the actual types of the arguments
     * @return a list that contains only applicable methods (number of
     * formal and actual arguments matches, and argument types are assignable
     * to formal types through a method invocation conversion).
     */
    private static LinkedList getApplicables(List methods, Class[] classes)
    {
        LinkedList list = new LinkedList();

        for (Iterator imethod = methods.iterator(); imethod.hasNext();)
        {
            Method method = (Method) imethod.next();

            if(isApplicable(method, classes))
            {
                list.add(method);
            }

        }
        return list;
    }

    /**
     * Returns true if the supplied method is applicable to actual
     * argument types.
     */
    private static boolean isApplicable(Method method, Class[] classes)
    {
        Class[] methodArgs = method.getParameterTypes();

        if(methodArgs.length != classes.length)
        {
            return false;
        }

        for(int i = 0; i < classes.length; ++i)
        {
            if(!isMethodInvocationConvertible(methodArgs[i], classes[i]))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines whether a type represented by a class object is
     * convertible to another type represented by a class object using a
     * method invocation conversion, treating object types of primitive
     * types as if they were primitive types (that is, a Boolean actual
     * parameter type matches boolean primitive formal type). This behavior
     * is because this method is used to determine applicable methods for
     * an actual parameter list, and primitive types are represented by
     * their object duals in reflective method calls.
     *
     * @param formal the formal parameter type to which the actual
     * parameter type should be convertible
     * @param actual the actual parameter type.
     * @return true if either formal type is assignable from actual type,
     * or formal is a primitive type and actual is its corresponding object
     * type or an object type of a primitive type that can be converted to
     * the formal type.
     */
    private static boolean isMethodInvocationConvertible(Class formal,
                                                         Class actual)
    {
        /*
         * if it's a null, it means the arg was null
         */
        if (actual == null && !formal.isPrimitive())
        {
            return true;
        }

        /*
         *  Check for identity or widening reference conversion
         */

        if (actual != null && formal.isAssignableFrom(actual))
        {
            return true;
        }

        /*
         * Check for boxing with widening primitive conversion. Note that
         * actual parameters are never primitives.
         */

        if (formal.isPrimitive())
        {
            if(formal == Boolean.TYPE && actual == Boolean.class)
                return true;
            if(formal == Character.TYPE && actual == Character.class)
                return true;
            if(formal == Byte.TYPE && actual == Byte.class)
                return true;
            if(formal == Short.TYPE &&
               (actual == Short.class || actual == Byte.class))
                return true;
            if(formal == Integer.TYPE &&
               (actual == Integer.class || actual == Short.class ||
                actual == Byte.class))
                return true;
            if(formal == Long.TYPE &&
               (actual == Long.class || actual == Integer.class ||
                actual == Short.class || actual == Byte.class))
                return true;
            if(formal == Float.TYPE &&
               (actual == Float.class || actual == Long.class ||
                actual == Integer.class || actual == Short.class ||
                actual == Byte.class))
                return true;
            if(formal == Double.TYPE &&
               (actual == Double.class || actual == Float.class ||
                actual == Long.class || actual == Integer.class ||
                actual == Short.class || actual == Byte.class))
                return true;
        }

        return false;
    }

    /**
     * Determines whether a type represented by a class object is
     * convertible to another type represented by a class object using a
     * method invocation conversion, without matching object and primitive
     * types. This method is used to determine the more specific type when
     * comparing signatures of methods.
     *
     * @param formal the formal parameter type to which the actual
     * parameter type should be convertible
     * @param actual the actual parameter type.
     * @return true if either formal type is assignable from actual type,
     * or formal and actual are both primitive types and actual can be
     * subject to widening conversion to formal.
     */
    private static boolean isStrictMethodInvocationConvertible(Class formal,
                                                               Class actual)
    {
        /*
         * we shouldn't get a null into, but if so
         */
        if (actual == null && !formal.isPrimitive())
        {
            return true;
        }

        /*
         *  Check for identity or widening reference conversion
         */

        if(formal.isAssignableFrom(actual))
        {
            return true;
        }

        /*
         *  Check for widening primitive conversion.
         */

        if(formal.isPrimitive())
        {
            if(formal == Short.TYPE && (actual == Byte.TYPE))
                return true;
            if(formal == Integer.TYPE &&
               (actual == Short.TYPE || actual == Byte.TYPE))
                return true;
            if(formal == Long.TYPE &&
               (actual == Integer.TYPE || actual == Short.TYPE ||
                actual == Byte.TYPE))
                return true;
            if(formal == Float.TYPE &&
               (actual == Long.TYPE || actual == Integer.TYPE ||
                actual == Short.TYPE || actual == Byte.TYPE))
                return true;
            if(formal == Double.TYPE &&
               (actual == Float.TYPE || actual == Long.TYPE ||
                actual == Integer.TYPE || actual == Short.TYPE ||
                actual == Byte.TYPE))
                return true;
        }
        return false;
    }
}
