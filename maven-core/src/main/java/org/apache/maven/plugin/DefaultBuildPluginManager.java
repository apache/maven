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
package org.apache.maven.plugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.internal.impl.DefaultLog;
import org.apache.maven.internal.impl.DefaultMojoExecution;
import org.apache.maven.internal.impl.DefaultSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.LoggerFactory;

// TODO the antrun plugin has its own configurator, the only plugin that does. might need to think about how that works
// TODO remove the coreArtifactFilterManager

/**
 * DefaultBuildPluginManager
 */
@Named
@Singleton
public class DefaultBuildPluginManager implements BuildPluginManager {

    private final MavenPluginManager mavenPluginManager;
    private final LegacySupport legacySupport;
    private final MojoExecutionScope scope;
    private final MojoExecutionListener mojoExecutionListener;

    @Inject
    public DefaultBuildPluginManager(
            MavenPluginManager mavenPluginManager,
            LegacySupport legacySupport,
            MojoExecutionScope scope,
            List<MojoExecutionListener> mojoExecutionListeners) {
        this.mavenPluginManager = mavenPluginManager;
        this.legacySupport = legacySupport;
        this.scope = scope;
        this.mojoExecutionListener = new CompoundMojoExecutionListener(mojoExecutionListeners);
    }

    /**
     * @param plugin
     * @param repositories
     * @param session
     * @return PluginDescriptor The component descriptor for the Maven plugin.
     * @throws PluginNotFoundException The plugin could not be found in any repositories.
     * @throws PluginResolutionException The plugin could be found but could not be resolved.
     * @throws InvalidPluginDescriptorException
     */
    public PluginDescriptor loadPlugin(
            Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    InvalidPluginDescriptorException {
        return mavenPluginManager.getPluginDescriptor(plugin, repositories, session);
    }

    // ----------------------------------------------------------------------
    // Mojo execution
    // ----------------------------------------------------------------------

    public void executeMojo(MavenSession session, MojoExecution mojoExecution)
            throws MojoFailureException, MojoExecutionException, PluginConfigurationException, PluginManagerException {
        MavenProject project = session.getCurrentProject();

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        Mojo mojo = null;

        ClassRealm pluginRealm;
        try {
            pluginRealm = getPluginRealm(session, mojoDescriptor.getPluginDescriptor());
        } catch (PluginResolutionException e) {
            throw new PluginExecutionException(mojoExecution, project, e);
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(pluginRealm);

        MavenSession oldSession = legacySupport.getSession();

        scope.enter();

        try {
            scope.seed(MavenProject.class, project);
            scope.seed(MojoExecution.class, mojoExecution);
            scope.seed(
                    org.apache.maven.api.plugin.Log.class,
                    new DefaultLog(LoggerFactory.getLogger(
                            mojoExecution.getMojoDescriptor().getFullGoalName())));
            scope.seed(Project.class, ((DefaultSession) session.getSession()).getProject(project));
            scope.seed(org.apache.maven.api.MojoExecution.class, new DefaultMojoExecution(mojoExecution));

            if (mojoDescriptor.isV4Api()) {
                org.apache.maven.api.plugin.Mojo mojoV4 = mavenPluginManager.getConfiguredMojo(
                        org.apache.maven.api.plugin.Mojo.class, session, mojoExecution);
                mojo = new MojoWrapper(mojoV4);
            } else {
                mojo = mavenPluginManager.getConfiguredMojo(Mojo.class, session, mojoExecution);
            }

            legacySupport.setSession(session);

            // NOTE: DuplicateArtifactAttachmentException is currently unchecked, so be careful removing this try/catch!
            // This is necessary to avoid creating compatibility problems for existing plugins that use
            // MavenProjectHelper.attachArtifact(..).
            try {
                MojoExecutionEvent mojoExecutionEvent = new MojoExecutionEvent(session, project, mojoExecution, mojo);
                mojoExecutionListener.beforeMojoExecution(mojoExecutionEvent);
                mojo.execute();
                mojoExecutionListener.afterMojoExecutionSuccess(mojoExecutionEvent);
            } catch (ClassCastException e) {
                // to be processed in the outer catch block
                throw e;
            } catch (RuntimeException e) {
                throw new PluginExecutionException(mojoExecution, project, e);
            }
        } catch (PluginContainerException e) {
            mojoExecutionListener.afterExecutionFailure(
                    new MojoExecutionEvent(session, project, mojoExecution, mojo, e));
            throw new PluginExecutionException(mojoExecution, project, e);
        } catch (NoClassDefFoundError e) {
            mojoExecutionListener.afterExecutionFailure(
                    new MojoExecutionEvent(session, project, mojoExecution, mojo, e));
            ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
            PrintStream ps = new PrintStream(os);
            ps.println(
                    "A required class was missing while executing " + mojoDescriptor.getId() + ": " + e.getMessage());
            pluginRealm.display(ps);
            Exception wrapper = new PluginContainerException(mojoDescriptor, pluginRealm, os.toString(), e);
            throw new PluginExecutionException(mojoExecution, project, wrapper);
        } catch (LinkageError e) {
            mojoExecutionListener.afterExecutionFailure(
                    new MojoExecutionEvent(session, project, mojoExecution, mojo, e));
            ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
            PrintStream ps = new PrintStream(os);
            ps.println("An API incompatibility was encountered while executing " + mojoDescriptor.getId() + ": "
                    + e.getClass().getName() + ": " + e.getMessage());
            pluginRealm.display(ps);
            Exception wrapper = new PluginContainerException(mojoDescriptor, pluginRealm, os.toString(), e);
            throw new PluginExecutionException(mojoExecution, project, wrapper);
        } catch (ClassCastException e) {
            mojoExecutionListener.afterExecutionFailure(
                    new MojoExecutionEvent(session, project, mojoExecution, mojo, e));
            ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
            PrintStream ps = new PrintStream(os);
            ps.println("A type incompatibility occurred while executing " + mojoDescriptor.getId() + ": "
                    + e.getMessage());
            pluginRealm.display(ps);
            throw new PluginExecutionException(mojoExecution, project, os.toString(), e);
        } catch (RuntimeException e) {
            mojoExecutionListener.afterExecutionFailure(
                    new MojoExecutionEvent(session, project, mojoExecution, mojo, e));
            throw e;
        } finally {
            mavenPluginManager.releaseMojo(mojo, mojoExecution);
            scope.exit();
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            legacySupport.setSession(oldSession);
        }
    }

    /**
     * TODO pluginDescriptor classRealm and artifacts are set as a side effect of this
     *      call, which is not nice.
     * @throws PluginResolutionException
     */
    public ClassRealm getPluginRealm(MavenSession session, PluginDescriptor pluginDescriptor)
            throws PluginResolutionException, PluginManagerException {
        ClassRealm pluginRealm = pluginDescriptor.getClassRealm();
        if (pluginRealm != null) {
            return pluginRealm;
        }

        mavenPluginManager.setupPluginRealm(pluginDescriptor, session, null, null, null);

        return pluginDescriptor.getClassRealm();
    }

    public MojoDescriptor getMojoDescriptor(
            Plugin plugin, String goal, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, InvalidPluginDescriptorException {
        return mavenPluginManager.getMojoDescriptor(plugin, goal, repositories, session);
    }

    private static class MojoWrapper implements Mojo {
        private final org.apache.maven.api.plugin.Mojo mojoV4;

        MojoWrapper(org.apache.maven.api.plugin.Mojo mojoV4) {
            this.mojoV4 = mojoV4;
        }

        @Override
        public void execute() throws MojoExecutionException, MojoFailureException {
            try {
                mojoV4.execute();
            } catch (MojoException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        @Override
        public void setLog(Log log) {}

        @Override
        public Log getLog() {
            return null;
        }
    }
}
