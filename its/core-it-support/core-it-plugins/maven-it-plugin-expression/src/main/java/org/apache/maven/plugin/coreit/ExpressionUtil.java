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
package org.apache.maven.plugin.coreit;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assists in evaluating expressions.
 *
 * @author Benjamin Bentmann
 *
 */
class ExpressionUtil {

    private static final Object[] NO_ARGS = {};

    private static final Class[] NO_PARAMS = {};

    private static final Class[] OBJECT_PARAM = {Object.class};

    private static final Class[] STRING_PARAM = {String.class};

    /**
     * Evaluates the specified expression. Expressions are composed of segments which are separated by a forward slash
     * ('/'). Each segment specifies a (public) bean property of the current object and drives the evaluation further
     * down the object graph. For lists, arrays and maps segments can additionally specify the index/key of an element.
     * The initial segment denotes the root object and the parameter <code>contexts</code> is used to specify which root
     * objects are available. For instance, if <code>contexts</code> maps the token "project" to a Maven project
     * instance, the expression "project/build/resources/0/directory" specifies the first resource directory of the
     * project.
     *
     * @param expression The expression to evaluate, may be <code>null</code>.
     * @param context    The object to start expression evaluation at, must not be <code>null</code>.
     * @return The values of the evaluation, indexed by expression, or an empty map if the segments could not be
     * evaluated.
     */
    public static Map evaluate(String expression, Object context) {
        Map values = Collections.EMPTY_MAP;

        if (expression != null && expression.length() > 0) {
            List segments = Arrays.asList(expression.split("/", 0));
            values = evaluate("", segments, context);
        }

        return values;
    }

    /**
     * Evaluates the given expression segments against the specified object.
     *
     * @param prefix   The expression prefix that led to the current context, must not be <code>null</code>.
     * @param segments The expression segments to evaluate, must not be <code>null</code>.
     * @param context  The object to evaluate the segments against, may be <code>null</code>.
     * @return The values of the evaluation, indexed by expression, or an empty map if the segments could not be
     * evaluated.
     */
    private static Map evaluate(String prefix, List segments, Object context) {
        Map values = Collections.EMPTY_MAP;

        if (segments.isEmpty()) {
            values = Collections.singletonMap(prefix, context);
        } else if (context != null) {
            Map targets = Collections.EMPTY_MAP;
            String segment = (String) segments.get(0);
            if (context.getClass().isArray() && Character.isDigit(segment.charAt(0))) {
                try {
                    int index = Integer.parseInt(segment);
                    targets = Collections.singletonMap(segment, Array.get(context, index));
                } catch (RuntimeException e) {
                    // invalid index, just ignore
                }
            } else if ((context instanceof List) && Character.isDigit(segment.charAt(0))) {
                try {
                    int index = Integer.parseInt(segment);
                    targets = Collections.singletonMap(segment, ((List) context).get(index));
                } catch (RuntimeException e) {
                    // invalid index, just ignore
                }
            } else if ((context instanceof Collection) && "*".equals(segment)) {
                targets = new LinkedHashMap();
                int index = 0;
                for (Iterator it = ((Collection) context).iterator(); it.hasNext(); index++) {
                    targets.put(Integer.toString(index), it.next());
                }
            } else if (context.getClass().isArray() && "*".equals(segment)) {
                targets = new LinkedHashMap();
                for (int index = 0, n = Array.getLength(context); index < n; index++) {
                    targets.put(Integer.toString(index), Array.get(context, index));
                }
            } else {
                targets = Collections.singletonMap(segment, getProperty(context, segment));
            }

            values = new LinkedHashMap();
            for (Object key : targets.keySet()) {
                Object target = targets.get(key);
                values.putAll(
                        evaluate(concat(prefix, String.valueOf(key)), segments.subList(1, segments.size()), target));
            }
        }

        return values;
    }

    private static String concat(String prefix, String segment) {
        return (prefix == null || prefix.length() <= 0) ? segment : (prefix + '/' + segment);
    }

    /**
     * Gets the value of a (public) bean property from the specified object.
     *
     * @param context  The object whose bean property should be retrieved, must not be <code>null</code>.
     * @param property The name of the bean property, must not be <code>null</code>.
     * @return The value of the bean property or <code>null</code> if the property does not exist.
     */
    static Object getProperty(Object context, String property) {
        Object value;

        Class type = context.getClass();
        if (context instanceof Collection) {
            type = Collection.class;
        } else if (context instanceof Map) {
            type = Map.class;
        }

        try {
            try {
                Method method = type.getMethod(property, NO_PARAMS);
                method.setAccessible(true);
                value = method.invoke(context, NO_ARGS);
            } catch (NoSuchMethodException e) {
                try {
                    String name = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
                    Method method = type.getMethod(name, NO_PARAMS);
                    method.setAccessible(true);
                    value = method.invoke(context, NO_ARGS);
                } catch (NoSuchMethodException e1) {
                    try {
                        String name = "is" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
                        Method method = type.getMethod(name, NO_PARAMS);
                        method.setAccessible(true);
                        value = method.invoke(context, NO_ARGS);
                    } catch (NoSuchMethodException e2) {
                        try {
                            Method method;
                            try {
                                method = type.getMethod("get", STRING_PARAM);
                            } catch (NoSuchMethodException e3) {
                                method = type.getMethod("get", OBJECT_PARAM);
                            }
                            method.setAccessible(true);
                            value = method.invoke(context, new Object[] {property});
                        } catch (NoSuchMethodException e3) {
                            try {
                                Field field = type.getField(property);
                                field.setAccessible(true);
                                value = field.get(context);
                            } catch (NoSuchFieldException e4) {
                                if ("length".equals(property) && type.isArray()) {
                                    value = Array.getLength(context);
                                } else {
                                    throw e4;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            value = null;
        }
        return value;
    }
}
