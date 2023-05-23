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
package org.apache.maven.internal.build.impl.maven.digest;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.build.Incremental;
import org.apache.maven.model.v4.MavenStaxWriter;

class Digesters {

    private static final Map<Class<?>, Digester<?>> DIGESTERS;

    static {
        Map<Class<?>, Digester<?>> digesters = new LinkedHashMap<>();
        // common Maven objects
        digesters.put(RemoteRepository.class, (Digester<RemoteRepository>) Digesters::digestRemoteRepository);
        // digesters.put(Artifact.class, (Digester<Artifact>) Digesters::digestArtifact);
        digesters.put(Project.class, (Digester<Project>) Digesters::digestProject);
        digesters.put(Session.class, (Digester<Session>) Digesters::digestSession);
        //
        digesters.put(Collection.class, (Digester<Collection<?>>) Digesters::digestCollection);
        //
        digesters.put(Serializable.class, (Digester<Serializable>) (member, value) -> value);
        DIGESTERS = Collections.unmodifiableMap(digesters);
    }

    public static Serializable digest(Member member, Object value) {
        // TODO: check on mojo as a default
        Incremental configuration = getConfiguration(member);
        if (configuration != null && !configuration.consider()) {
            return null; // no digest, ignore
        }

        return rawtypesDigest(member, value);
    }

    static Incremental getConfiguration(Member member) {
        if (member instanceof AnnotatedElement) {
            return ((AnnotatedElement) member).getAnnotation(Incremental.class);
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Serializable rawtypesDigest(Member member, Object value) {
        return ((Digester) getDigester(value)).digest(member, value);
    }

    private static Digester<?> getDigester(Object value) {
        Digester<?> digester = null;
        for (Map.Entry<Class<?>, Digester<?>> entry : DIGESTERS.entrySet()) {
            if (entry.getKey().isInstance(value)) {
                digester = entry.getValue();
                break;
            }
        }
        if (digester == null) {
            throw new UnsupportedParameterTypeException(value.getClass());
        }
        return digester;
    }

    private static Serializable digestProject(Member member, Project value) {
        if (getConfiguration(member) == null) {
            throw new IllegalArgumentException("Explicit @Incremental required: " + member);
        }

        final MessageDigest digester = SHA1Digester.newInstance();

        // effective pom.xml defines project configuration, rebuild whenever project configuration
        // changes we can't be more specific here because mojo can access entire project model, not
        // just its own configuration
        try {
            new MavenStaxWriter()
                    .write(
                            new OutputStream() {
                                @Override
                                public void write(int b) throws IOException {
                                    digester.update((byte) b);
                                }
                            },
                            value.getModel());
        } catch (IOException | XMLStreamException e) {
            // can't happen
        }

        return new BytesHash(digester.digest());
    }

    private static Serializable digestSession(Member member, Session session) {
        if (getConfiguration(member) == null) {
            throw new IllegalArgumentException("Explicit @Incremental required: " + member);
        }

        // execution properties define build parameters passed in from command line and jvm used
        SortedMap<String, String> executionProperties = new TreeMap<>();
        Properties props = new Properties();
        props.putAll(session.getSystemProperties());
        props.putAll(session.getUserProperties());
        for (Map.Entry<Object, Object> property : props.entrySet()) {
            // TODO unit test non-string keys do not cause problems at runtime
            // TODO test if non-string values can or cannot be used
            Object key = property.getKey();
            Object value = property.getValue();
            if (key instanceof String && value instanceof String) {
                executionProperties.put(key.toString(), value.toString());
            }
        }

        // m2e workspace launch
        executionProperties.remove("classworlds.conf");

        // Environment has PID of java process (env.JAVA_MAIN_CLASS_<PID>), SSH_AGENT_PID,
        // unique TMPDIR (on OSX) and other volatile variables.
        executionProperties.entrySet().removeIf(property -> property.getKey().startsWith("env."));

        MessageDigest digester = SHA1Digester.newInstance();

        for (Map.Entry<String, String> property : executionProperties.entrySet()) {
            digester.update(property.getKey().getBytes(StandardCharsets.UTF_8));
            digester.update(property.getValue().getBytes(StandardCharsets.UTF_8));
        }

        return new BytesHash(digester.digest());
    }

    //    private static Serializable digestArtifact(Member member, Artifact value) {
    //        return value.getPath().get().toFile();
    //    }

    private static Serializable digestRemoteRepository(Member member, RemoteRepository value) {
        return value.getUrl();
    }

    private static Serializable digestCollection(Member member, Collection<?> collection) {
        // TODO consider collapsing to single SHA1 hash
        ArrayList<Serializable> digest = new ArrayList<>();
        for (Object element : collection) {
            Serializable elementDigest = rawtypesDigest(member, element);
            if (elementDigest != null) {
                digest.add(elementDigest);
            }
        }
        return digest;
    }

    interface Digester<T> {
        Serializable digest(Member member, T value);
    }

    public static class UnsupportedParameterTypeException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        final Class<?> type;

        UnsupportedParameterTypeException(Class<?> type) {
            this.type = type;
        }
    }
}
