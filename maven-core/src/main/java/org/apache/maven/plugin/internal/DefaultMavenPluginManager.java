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
package org.apache.maven.plugin.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.internal.MojoExecutionScopeModule;
import org.apache.maven.model.Plugin;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.DebugConfigurationListener;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MavenPluginValidator;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginArtifactsCache;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginDescriptorCache;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginIncompatibleException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginParameterException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.PluginRealmCache;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.project.ExtensionDescriptor;
import org.apache.maven.project.ExtensionDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.session.scope.internal.SessionScopeModule;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.composition.CycleDetectedInComponentGraphException;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

/**
 * Provides basic services to manage Maven plugins and their mojos. This component is kept general in its design such
 * that the plugins/mojos can be used in arbitrary contexts. In particular, the mojos can be used for ordinary build
 * plugins as well as special purpose plugins like reports.
 *
 * @author Benjamin Bentmann
 * @since 3.0
 */
@Component(role = MavenPluginManager.class)
public class DefaultMavenPluginManager implements MavenPluginManager {

    /**
     * <p>
     * PluginId =&gt; ExtensionRealmCache.CacheRecord map MavenProject context value key. The map is used to ensure the
     * same class realm is used to load build extensions and load mojos for extensions=true plugins.
     * </p>
     * <strong>Note:</strong> This is part of internal implementation and may be changed or removed without notice
     *
     * @since 3.3.0
     */
    public static final String KEY_EXTENSIONS_REALMS = DefaultMavenPluginManager.class.getName() + "/extensionsRealms";

    @Requirement
    private Logger logger;

    @Requirement
    private LoggerManager loggerManager;

    @Requirement
    private PlexusContainer container;

    @Requirement
    private ClassRealmManager classRealmManager;

    @Requirement
    private PluginDescriptorCache pluginDescriptorCache;

    @Requirement
    private PluginRealmCache pluginRealmCache;

    @Requirement
    private PluginDependenciesResolver pluginDependenciesResolver;

    @Requirement
    private RuntimeInformation runtimeInformation;

    @Requirement
    private ExtensionRealmCache extensionRealmCache;

    @Requirement
    private PluginVersionResolver pluginVersionResolver;

    @Requirement
    private PluginArtifactsCache pluginArtifactsCache;

    @Requirement
    private List<MavenPluginConfigurationValidator> configurationValidators;

    @Requirement
    private PluginValidationManager pluginValidationManager;

    private ExtensionDescriptorBuilder extensionDescriptorBuilder = new ExtensionDescriptorBuilder();

    private PluginDescriptorBuilder builder = new PluginDescriptorBuilder();

    public PluginDescriptor getPluginDescriptor(
            Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException {
        PluginDescriptorCache.Key cacheKey = pluginDescriptorCache.createKey(plugin, repositories, session);

        PluginDescriptor pluginDescriptor = pluginDescriptorCache.get(cacheKey, () -> {
            org.eclipse.aether.artifact.Artifact artifact =
                    pluginDependenciesResolver.resolve(plugin, repositories, session);

            Artifact pluginArtifact = RepositoryUtils.toArtifact(artifact);

            PluginDescriptor descriptor = extractPluginDescriptor(pluginArtifact, plugin);

            if (StringUtils.isBlank(descriptor.getRequiredMavenVersion())) {
                // only take value from underlying POM if plugin descriptor has no explicit Maven requirement
                descriptor.setRequiredMavenVersion(artifact.getProperty("requiredMavenVersion", null));
            }

            return descriptor;
        });

        pluginDescriptor.setPlugin(plugin);

        return pluginDescriptor;
    }

    private PluginDescriptor extractPluginDescriptor(Artifact pluginArtifact, Plugin plugin)
            throws PluginDescriptorParsingException, InvalidPluginDescriptorException {
        PluginDescriptor pluginDescriptor = null;

        File pluginFile = pluginArtifact.getFile();

        try {
            if (pluginFile.isFile()) {
                try (JarFile pluginJar = new JarFile(pluginFile, false)) {
                    ZipEntry pluginDescriptorEntry = pluginJar.getEntry(getPluginDescriptorLocation());

                    if (pluginDescriptorEntry != null) {
                        InputStream is = pluginJar.getInputStream(pluginDescriptorEntry);

                        pluginDescriptor = parsePluginDescriptor(is, plugin, pluginFile.getAbsolutePath());
                    }
                }
            } else {
                File pluginXml = new File(pluginFile, getPluginDescriptorLocation());

                if (pluginXml.isFile()) {
                    try (InputStream is = new BufferedInputStream(new FileInputStream(pluginXml))) {
                        pluginDescriptor = parsePluginDescriptor(is, plugin, pluginXml.getAbsolutePath());
                    }
                }
            }

            if (pluginDescriptor == null) {
                throw new IOException("No plugin descriptor found at " + getPluginDescriptorLocation());
            }
        } catch (IOException e) {
            throw new PluginDescriptorParsingException(plugin, pluginFile.getAbsolutePath(), e);
        }

        MavenPluginValidator validator = new MavenPluginValidator(pluginArtifact);

        validator.validate(pluginDescriptor);

        if (validator.hasErrors()) {
            throw new InvalidPluginDescriptorException(
                    "Invalid plugin descriptor for " + plugin.getId() + " (" + pluginFile + ")", validator.getErrors());
        }

        pluginDescriptor.setPluginArtifact(pluginArtifact);

        return pluginDescriptor;
    }

    private String getPluginDescriptorLocation() {
        return "META-INF/maven/plugin.xml";
    }

    private PluginDescriptor parsePluginDescriptor(InputStream is, Plugin plugin, String descriptorLocation)
            throws PluginDescriptorParsingException {
        try {
            Reader reader = ReaderFactory.newXmlReader(is);

            PluginDescriptor pluginDescriptor = builder.build(reader, descriptorLocation);

            return pluginDescriptor;
        } catch (IOException | PlexusConfigurationException e) {
            throw new PluginDescriptorParsingException(plugin, descriptorLocation, e);
        }
    }

    public MojoDescriptor getMojoDescriptor(
            Plugin plugin, String goal, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws MojoNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    InvalidPluginDescriptorException {
        PluginDescriptor pluginDescriptor = getPluginDescriptor(plugin, repositories, session);

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo(goal);

        if (mojoDescriptor == null) {
            throw new MojoNotFoundException(goal, pluginDescriptor);
        }

        return mojoDescriptor;
    }

    public void checkRequiredMavenVersion(PluginDescriptor pluginDescriptor) throws PluginIncompatibleException {
        String requiredMavenVersion = pluginDescriptor.getRequiredMavenVersion();
        if (StringUtils.isNotBlank(requiredMavenVersion)) {
            try {
                if (!runtimeInformation.isMavenVersion(requiredMavenVersion)) {
                    throw new PluginIncompatibleException(
                            pluginDescriptor.getPlugin(),
                            "The plugin " + pluginDescriptor.getId() + " requires Maven version "
                                    + requiredMavenVersion);
                }
            } catch (RuntimeException e) {
                logger.warn("Could not verify plugin's Maven prerequisite: " + e.getMessage());
            }
        }
    }

    public void setupPluginRealm(
            PluginDescriptor pluginDescriptor,
            MavenSession session,
            ClassLoader parent,
            List<String> imports,
            DependencyFilter filter)
            throws PluginResolutionException, PluginContainerException {
        Plugin plugin = pluginDescriptor.getPlugin();
        MavenProject project = session.getCurrentProject();

        if (plugin.isExtensions()) {
            ExtensionRealmCache.CacheRecord extensionRecord;
            try {
                RepositorySystemSession repositorySession = session.getRepositorySession();
                extensionRecord = setupExtensionsRealm(project, plugin, repositorySession);
            } catch (PluginManagerException e) {
                // extensions realm is expected to be fully setup at this point
                // any exception means a problem in maven code, not a user error
                throw new IllegalStateException(e);
            }

            ClassRealm pluginRealm = extensionRecord.getRealm();
            List<Artifact> pluginArtifacts = extensionRecord.getArtifacts();

            for (ComponentDescriptor<?> componentDescriptor : pluginDescriptor.getComponents()) {
                componentDescriptor.setRealm(pluginRealm);
            }

            pluginDescriptor.setClassRealm(pluginRealm);
            pluginDescriptor.setArtifacts(pluginArtifacts);
        } else {
            Map<String, ClassLoader> foreignImports = calcImports(project, parent, imports);

            PluginRealmCache.Key cacheKey = pluginRealmCache.createKey(
                    plugin,
                    parent,
                    foreignImports,
                    filter,
                    project.getRemotePluginRepositories(),
                    session.getRepositorySession());

            PluginRealmCache.CacheRecord cacheRecord = pluginRealmCache.get(cacheKey, () -> {
                createPluginRealm(pluginDescriptor, session, parent, foreignImports, filter);

                return new PluginRealmCache.CacheRecord(
                        pluginDescriptor.getClassRealm(), pluginDescriptor.getArtifacts());
            });

            pluginDescriptor.setClassRealm(cacheRecord.getRealm());
            pluginDescriptor.setArtifacts(new ArrayList<>(cacheRecord.getArtifacts()));
            for (ComponentDescriptor<?> componentDescriptor : pluginDescriptor.getComponents()) {
                componentDescriptor.setRealm(cacheRecord.getRealm());
            }

            pluginRealmCache.register(project, cacheKey, cacheRecord);
        }
    }

    private void createPluginRealm(
            PluginDescriptor pluginDescriptor,
            MavenSession session,
            ClassLoader parent,
            Map<String, ClassLoader> foreignImports,
            DependencyFilter filter)
            throws PluginResolutionException, PluginContainerException {
        Plugin plugin = Objects.requireNonNull(pluginDescriptor.getPlugin(), "pluginDescriptor.plugin cannot be null");

        Artifact pluginArtifact = Objects.requireNonNull(
                pluginDescriptor.getPluginArtifact(), "pluginDescriptor.pluginArtifact cannot be null");

        MavenProject project = session.getCurrentProject();

        final ClassRealm pluginRealm;
        final List<Artifact> pluginArtifacts;

        RepositorySystemSession repositorySession = session.getRepositorySession();
        DependencyFilter dependencyFilter = project.getExtensionDependencyFilter();
        dependencyFilter = AndDependencyFilter.newInstance(dependencyFilter, filter);

        DependencyNode root = pluginDependenciesResolver.resolve(
                plugin,
                RepositoryUtils.toArtifact(pluginArtifact),
                dependencyFilter,
                project.getRemotePluginRepositories(),
                repositorySession);

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        root.accept(nlg);

        pluginArtifacts = toMavenArtifacts(root, nlg);

        pluginRealm = classRealmManager.createPluginRealm(
                plugin, parent, null, foreignImports, toAetherArtifacts(pluginArtifacts));

        discoverPluginComponents(pluginRealm, plugin, pluginDescriptor);

        pluginDescriptor.setClassRealm(pluginRealm);
        pluginDescriptor.setArtifacts(pluginArtifacts);
    }

    private void discoverPluginComponents(
            final ClassRealm pluginRealm, Plugin plugin, PluginDescriptor pluginDescriptor)
            throws PluginContainerException {
        try {
            if (pluginDescriptor != null) {
                for (ComponentDescriptor<?> componentDescriptor : pluginDescriptor.getComponents()) {
                    componentDescriptor.setRealm(pluginRealm);
                    container.addComponentDescriptor(componentDescriptor);
                }
            }

            ((DefaultPlexusContainer) container)
                    .discoverComponents(
                            pluginRealm, new SessionScopeModule(container), new MojoExecutionScopeModule(container));
        } catch (ComponentLookupException | CycleDetectedInComponentGraphException e) {
            throw new PluginContainerException(
                    plugin,
                    pluginRealm,
                    "Error in component graph of plugin " + plugin.getId() + ": " + e.getMessage(),
                    e);
        }
    }

    private List<org.eclipse.aether.artifact.Artifact> toAetherArtifacts(final List<Artifact> pluginArtifacts) {
        return new ArrayList<>(RepositoryUtils.toArtifacts(pluginArtifacts));
    }

    private List<Artifact> toMavenArtifacts(DependencyNode root, PreorderNodeListGenerator nlg) {
        List<Artifact> artifacts = new ArrayList<>(nlg.getNodes().size());
        RepositoryUtils.toArtifacts(artifacts, Collections.singleton(root), Collections.<String>emptyList(), null);
        for (Iterator<Artifact> it = artifacts.iterator(); it.hasNext(); ) {
            Artifact artifact = it.next();
            if (artifact.getFile() == null) {
                it.remove();
            }
        }
        return Collections.unmodifiableList(artifacts);
    }

    private Map<String, ClassLoader> calcImports(MavenProject project, ClassLoader parent, List<String> imports) {
        Map<String, ClassLoader> foreignImports = new HashMap<>();

        ClassLoader projectRealm = project.getClassRealm();
        if (projectRealm != null) {
            foreignImports.put("", projectRealm);
        } else {
            foreignImports.put("", classRealmManager.getMavenApiRealm());
        }

        if (parent != null && imports != null) {
            for (String parentImport : imports) {
                foreignImports.put(parentImport, parent);
            }
        }

        return foreignImports;
    }

    public <T> T getConfiguredMojo(Class<T> mojoInterface, MavenSession session, MojoExecution mojoExecution)
            throws PluginConfigurationException, PluginContainerException {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        ClassRealm pluginRealm = pluginDescriptor.getClassRealm();

        if (pluginRealm == null) {
            try {
                setupPluginRealm(pluginDescriptor, session, null, null, null);
            } catch (PluginResolutionException e) {
                String msg = "Cannot setup plugin realm [mojoDescriptor=" + mojoDescriptor.getId()
                        + ", pluginDescriptor=" + pluginDescriptor.getId() + "]";
                throw new PluginConfigurationException(pluginDescriptor, msg, e);
            }
            pluginRealm = pluginDescriptor.getClassRealm();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Loading mojo " + mojoDescriptor.getId() + " from plugin realm " + pluginRealm);
        }

        // We are forcing the use of the plugin realm for all lookups that might occur during
        // the lifecycle that is part of the lookup. Here we are specifically trying to keep
        // lookups that occur in contextualize calls in line with the right realm.
        ClassRealm oldLookupRealm = container.setLookupRealm(pluginRealm);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(pluginRealm);

        try {
            T mojo;

            try {
                mojo = container.lookup(mojoInterface, mojoDescriptor.getRoleHint());
            } catch (ComponentLookupException e) {
                Throwable cause = e.getCause();
                while (cause != null
                        && !(cause instanceof LinkageError)
                        && !(cause instanceof ClassNotFoundException)) {
                    cause = cause.getCause();
                }

                if ((cause instanceof NoClassDefFoundError) || (cause instanceof ClassNotFoundException)) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
                    PrintStream ps = new PrintStream(os);
                    ps.println("Unable to load the mojo '" + mojoDescriptor.getGoal() + "' in the plugin '"
                            + pluginDescriptor.getId() + "'. A required class is missing: "
                            + cause.getMessage());
                    pluginRealm.display(ps);

                    throw new PluginContainerException(mojoDescriptor, pluginRealm, os.toString(), cause);
                } else if (cause instanceof LinkageError) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
                    PrintStream ps = new PrintStream(os);
                    ps.println("Unable to load the mojo '" + mojoDescriptor.getGoal() + "' in the plugin '"
                            + pluginDescriptor.getId() + "' due to an API incompatibility: "
                            + e.getClass().getName() + ": " + cause.getMessage());
                    pluginRealm.display(ps);

                    throw new PluginContainerException(mojoDescriptor, pluginRealm, os.toString(), cause);
                }

                throw new PluginContainerException(
                        mojoDescriptor,
                        pluginRealm,
                        "Unable to load the mojo '" + mojoDescriptor.getGoal()
                                + "' (or one of its required components) from the plugin '"
                                + pluginDescriptor.getId() + "'",
                        e);
            }

            if (mojo instanceof ContextEnabled) {
                MavenProject project = session.getCurrentProject();

                Map<String, Object> pluginContext = session.getPluginContext(pluginDescriptor, project);

                if (pluginContext != null) {
                    pluginContext.put("project", project);

                    pluginContext.put("pluginDescriptor", pluginDescriptor);

                    ((ContextEnabled) mojo).setPluginContext(pluginContext);
                }
            }

            if (mojo instanceof Mojo) {
                Logger mojoLogger = loggerManager.getLoggerForComponent(mojoDescriptor.getImplementation());
                ((Mojo) mojo).setLog(new DefaultLog(mojoLogger));
            }

            if (mojo instanceof Contextualizable) {
                pluginValidationManager.reportPluginMojoValidationIssue(
                        PluginValidationManager.IssueLocality.EXTERNAL,
                        session,
                        mojoDescriptor,
                        mojo.getClass(),
                        "Mojo implements `Contextualizable` interface from Plexus Container, which is EOL.");
            }

            Xpp3Dom dom = mojoExecution.getConfiguration();

            PlexusConfiguration pomConfiguration;

            if (dom == null) {
                pomConfiguration = new XmlPlexusConfiguration("configuration");
            } else {
                pomConfiguration = new XmlPlexusConfiguration(dom);
            }

            ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);

            for (MavenPluginConfigurationValidator validator : configurationValidators) {
                validator.validate(session, mojoDescriptor, mojo.getClass(), pomConfiguration, expressionEvaluator);
            }

            populateMojoExecutionFields(
                    mojo,
                    mojoExecution.getExecutionId(),
                    mojoDescriptor,
                    pluginRealm,
                    pomConfiguration,
                    expressionEvaluator);

            return mojo;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            container.setLookupRealm(oldLookupRealm);
        }
    }

    private void populateMojoExecutionFields(
            Object mojo,
            String executionId,
            MojoDescriptor mojoDescriptor,
            ClassRealm pluginRealm,
            PlexusConfiguration configuration,
            ExpressionEvaluator expressionEvaluator)
            throws PluginConfigurationException {
        ComponentConfigurator configurator = null;

        String configuratorId = mojoDescriptor.getComponentConfigurator();

        if (StringUtils.isEmpty(configuratorId)) {
            configuratorId = "basic";
        }

        try {
            // TODO could the configuration be passed to lookup and the configurator known to plexus via the descriptor
            // so that this method could entirely be handled by a plexus lookup?
            configurator = container.lookup(ComponentConfigurator.class, configuratorId);

            ConfigurationListener listener = new DebugConfigurationListener(logger);

            ValidatingConfigurationListener validator =
                    new ValidatingConfigurationListener(mojo, mojoDescriptor, listener);

            logger.debug("Configuring mojo execution '" + mojoDescriptor.getId() + ':' + executionId + "' with "
                    + configuratorId + " configurator -->");

            configurator.configureComponent(mojo, configuration, expressionEvaluator, pluginRealm, validator);

            logger.debug("-- end configuration --");

            Collection<Parameter> missingParameters = validator.getMissingParameters();
            if (!missingParameters.isEmpty()) {
                if ("basic".equals(configuratorId)) {
                    throw new PluginParameterException(mojoDescriptor, new ArrayList<>(missingParameters));
                } else {
                    /*
                     * NOTE: Other configurators like the map-oriented one don't call into the listener, so do it the
                     * hard way.
                     */
                    validateParameters(mojoDescriptor, configuration, expressionEvaluator);
                }
            }
        } catch (ComponentConfigurationException e) {
            String message = "Unable to parse configuration of mojo " + mojoDescriptor.getId();
            if (e.getFailedConfiguration() != null) {
                message += " for parameter " + e.getFailedConfiguration().getName();
            }
            message += ": " + e.getMessage();

            throw new PluginConfigurationException(mojoDescriptor.getPluginDescriptor(), message, e);
        } catch (ComponentLookupException e) {
            throw new PluginConfigurationException(
                    mojoDescriptor.getPluginDescriptor(),
                    "Unable to retrieve component configurator " + configuratorId + " for configuration of mojo "
                            + mojoDescriptor.getId(),
                    e);
        } catch (NoClassDefFoundError e) {
            ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
            PrintStream ps = new PrintStream(os);
            ps.println("A required class was missing during configuration of mojo " + mojoDescriptor.getId() + ": "
                    + e.getMessage());
            pluginRealm.display(ps);

            throw new PluginConfigurationException(mojoDescriptor.getPluginDescriptor(), os.toString(), e);
        } catch (LinkageError e) {
            ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
            PrintStream ps = new PrintStream(os);
            ps.println("An API incompatibility was encountered during configuration of mojo " + mojoDescriptor.getId()
                    + ": " + e.getClass().getName() + ": " + e.getMessage());
            pluginRealm.display(ps);

            throw new PluginConfigurationException(mojoDescriptor.getPluginDescriptor(), os.toString(), e);
        } finally {
            if (configurator != null) {
                try {
                    container.release(configurator);
                } catch (ComponentLifecycleException e) {
                    logger.debug("Failed to release mojo configurator - ignoring.");
                }
            }
        }
    }

    private void validateParameters(
            MojoDescriptor mojoDescriptor, PlexusConfiguration configuration, ExpressionEvaluator expressionEvaluator)
            throws ComponentConfigurationException, PluginParameterException {
        if (mojoDescriptor.getParameters() == null) {
            return;
        }

        List<Parameter> invalidParameters = new ArrayList<>();

        for (Parameter parameter : mojoDescriptor.getParameters()) {
            if (!parameter.isRequired()) {
                continue;
            }

            Object value = null;

            PlexusConfiguration config = configuration.getChild(parameter.getName(), false);
            if (config != null) {
                String expression = config.getValue(null);

                try {
                    value = expressionEvaluator.evaluate(expression);

                    if (value == null) {
                        value = config.getAttribute("default-value", null);
                    }
                } catch (ExpressionEvaluationException e) {
                    String msg = "Error evaluating the expression '" + expression + "' for configuration value '"
                            + configuration.getName() + "'";
                    throw new ComponentConfigurationException(configuration, msg, e);
                }
            }

            if (value == null && (config == null || config.getChildCount() <= 0)) {
                invalidParameters.add(parameter);
            }
        }

        if (!invalidParameters.isEmpty()) {
            throw new PluginParameterException(mojoDescriptor, invalidParameters);
        }
    }

    public void releaseMojo(Object mojo, MojoExecution mojoExecution) {
        if (mojo != null) {
            try {
                container.release(mojo);
            } catch (ComponentLifecycleException e) {
                String goalExecId = mojoExecution.getGoal();

                if (mojoExecution.getExecutionId() != null) {
                    goalExecId += " {execution: " + mojoExecution.getExecutionId() + "}";
                }

                logger.debug("Error releasing mojo for " + goalExecId, e);
            }
        }
    }

    public ExtensionRealmCache.CacheRecord setupExtensionsRealm(
            MavenProject project, Plugin plugin, RepositorySystemSession session) throws PluginManagerException {
        @SuppressWarnings("unchecked")
        Map<String, ExtensionRealmCache.CacheRecord> pluginRealms =
                (Map<String, ExtensionRealmCache.CacheRecord>) project.getContextValue(KEY_EXTENSIONS_REALMS);
        if (pluginRealms == null) {
            pluginRealms = new HashMap<>();
            project.setContextValue(KEY_EXTENSIONS_REALMS, pluginRealms);
        }

        final String pluginKey = plugin.getId();

        ExtensionRealmCache.CacheRecord extensionRecord = pluginRealms.get(pluginKey);
        if (extensionRecord != null) {
            return extensionRecord;
        }

        final List<RemoteRepository> repositories = project.getRemotePluginRepositories();

        // resolve plugin version as necessary
        if (plugin.getVersion() == null) {
            PluginVersionRequest versionRequest = new DefaultPluginVersionRequest(plugin, session, repositories);
            try {
                plugin.setVersion(pluginVersionResolver.resolve(versionRequest).getVersion());
            } catch (PluginVersionResolutionException e) {
                throw new PluginManagerException(plugin, e.getMessage(), e);
            }
        }

        // resolve plugin artifacts
        List<Artifact> artifacts;
        PluginArtifactsCache.Key cacheKey = pluginArtifactsCache.createKey(plugin, null, repositories, session);
        PluginArtifactsCache.CacheRecord recordArtifacts;
        try {
            recordArtifacts = pluginArtifactsCache.get(cacheKey);
        } catch (PluginResolutionException e) {
            throw new PluginManagerException(plugin, e.getMessage(), e);
        }
        if (recordArtifacts != null) {
            artifacts = recordArtifacts.getArtifacts();
        } else {
            try {
                artifacts = resolveExtensionArtifacts(plugin, repositories, session);
                recordArtifacts = pluginArtifactsCache.put(cacheKey, artifacts);
            } catch (PluginResolutionException e) {
                pluginArtifactsCache.put(cacheKey, e);
                pluginArtifactsCache.register(project, cacheKey, recordArtifacts);
                throw new PluginManagerException(plugin, e.getMessage(), e);
            }
        }
        pluginArtifactsCache.register(project, cacheKey, recordArtifacts);

        // create and cache extensions realms
        final ExtensionRealmCache.Key extensionKey = extensionRealmCache.createKey(artifacts);
        extensionRecord = extensionRealmCache.get(extensionKey);
        if (extensionRecord == null) {
            ClassRealm extensionRealm = classRealmManager.createExtensionRealm(plugin, toAetherArtifacts(artifacts));

            // TODO figure out how to use the same PluginDescriptor when running mojos

            PluginDescriptor pluginDescriptor = null;
            if (plugin.isExtensions() && !artifacts.isEmpty()) {
                // ignore plugin descriptor parsing errors at this point
                // these errors will reported during calculation of project build execution plan
                try {
                    pluginDescriptor = extractPluginDescriptor(artifacts.get(0), plugin);
                } catch (PluginDescriptorParsingException | InvalidPluginDescriptorException e) {
                    // ignore, see above
                }
            }

            discoverPluginComponents(extensionRealm, plugin, pluginDescriptor);

            ExtensionDescriptor extensionDescriptor = null;
            Artifact extensionArtifact = artifacts.get(0);
            try {
                extensionDescriptor = extensionDescriptorBuilder.build(extensionArtifact.getFile());
            } catch (IOException e) {
                String message = "Invalid extension descriptor for " + plugin.getId() + ": " + e.getMessage();
                if (logger.isDebugEnabled()) {
                    logger.error(message, e);
                } else {
                    logger.error(message);
                }
            }
            extensionRecord = extensionRealmCache.put(extensionKey, extensionRealm, extensionDescriptor, artifacts);
        }
        extensionRealmCache.register(project, extensionKey, extensionRecord);
        pluginRealms.put(pluginKey, extensionRecord);

        return extensionRecord;
    }

    private List<Artifact> resolveExtensionArtifacts(
            Plugin extensionPlugin, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginResolutionException {
        DependencyNode root = pluginDependenciesResolver.resolve(extensionPlugin, null, null, repositories, session);
        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        root.accept(nlg);
        return toMavenArtifacts(root, nlg);
    }
}
