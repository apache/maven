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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.plugin.descriptor.Resolution;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.PathScopeRegistry;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.execution.scope.internal.MojoExecutionScopeModule;
import org.apache.maven.internal.impl.DefaultLog;
import org.apache.maven.internal.impl.DefaultMojoExecution;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.DebugConfigurationListener;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MavenPluginPrerequisitesChecker;
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
import org.apache.maven.plugin.PluginParameterExpressionEvaluatorV4;
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
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.session.scope.internal.SessionScopeModule;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.composition.CycleDetectedInComponentGraphException;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides basic services to manage Maven plugins and their mojos. This component is kept general in its design such
 * that the plugins/mojos can be used in arbitrary contexts. In particular, the mojos can be used for ordinary build
 * plugins as well as special purpose plugins like reports.
 *
 * @since 3.0
 */
@Named
@Singleton
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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PlexusContainer container;
    private final ClassRealmManager classRealmManager;
    private final PluginDescriptorCache pluginDescriptorCache;
    private final PluginRealmCache pluginRealmCache;
    private final PluginDependenciesResolver pluginDependenciesResolver;
    private final ExtensionRealmCache extensionRealmCache;
    private final PluginVersionResolver pluginVersionResolver;
    private final PluginArtifactsCache pluginArtifactsCache;
    private final MavenPluginValidator pluginValidator;
    private final List<MavenPluginConfigurationValidator> configurationValidators;
    private final PluginValidationManager pluginValidationManager;
    private final List<MavenPluginPrerequisitesChecker> prerequisitesCheckers;
    private final ExtensionDescriptorBuilder extensionDescriptorBuilder = new ExtensionDescriptorBuilder();
    private final PluginDescriptorBuilder builder = new PluginDescriptorBuilder();

    @Inject
    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultMavenPluginManager(
            PlexusContainer container,
            ClassRealmManager classRealmManager,
            PluginDescriptorCache pluginDescriptorCache,
            PluginRealmCache pluginRealmCache,
            PluginDependenciesResolver pluginDependenciesResolver,
            ExtensionRealmCache extensionRealmCache,
            PluginVersionResolver pluginVersionResolver,
            PluginArtifactsCache pluginArtifactsCache,
            MavenPluginValidator pluginValidator,
            List<MavenPluginConfigurationValidator> configurationValidators,
            PluginValidationManager pluginValidationManager,
            List<MavenPluginPrerequisitesChecker> prerequisitesCheckers) {
        this.container = container;
        this.classRealmManager = classRealmManager;
        this.pluginDescriptorCache = pluginDescriptorCache;
        this.pluginRealmCache = pluginRealmCache;
        this.pluginDependenciesResolver = pluginDependenciesResolver;
        this.extensionRealmCache = extensionRealmCache;
        this.pluginVersionResolver = pluginVersionResolver;
        this.pluginArtifactsCache = pluginArtifactsCache;
        this.pluginValidator = pluginValidator;
        this.configurationValidators = configurationValidators;
        this.pluginValidationManager = pluginValidationManager;
        this.prerequisitesCheckers = prerequisitesCheckers;
    }

    public PluginDescriptor getPluginDescriptor(
            Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException {
        PluginDescriptorCache.Key cacheKey = pluginDescriptorCache.createKey(plugin, repositories, session);

        PluginDescriptor pluginDescriptor = pluginDescriptorCache.get(cacheKey, () -> {
            org.eclipse.aether.artifact.Artifact artifact =
                    pluginDependenciesResolver.resolve(plugin, repositories, session);

            Artifact pluginArtifact = RepositoryUtils.toArtifact(artifact);

            PluginDescriptor descriptor = extractPluginDescriptor(pluginArtifact, plugin);

            boolean isBlankVersion = descriptor.getRequiredMavenVersion() == null
                    || descriptor.getRequiredMavenVersion().trim().isEmpty();

            if (isBlankVersion) {
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
                        pluginDescriptor = parsePluginDescriptor(
                                () -> pluginJar.getInputStream(pluginDescriptorEntry),
                                plugin,
                                pluginFile.getAbsolutePath());
                    }
                }
            } else {
                File pluginXml = new File(pluginFile, getPluginDescriptorLocation());

                if (pluginXml.isFile()) {
                    pluginDescriptor = parsePluginDescriptor(
                            () -> Files.newInputStream(pluginXml.toPath()), plugin, pluginXml.getAbsolutePath());
                }
            }

            if (pluginDescriptor == null) {
                throw new IOException("No plugin descriptor found at " + getPluginDescriptorLocation());
            }
        } catch (IOException e) {
            throw new PluginDescriptorParsingException(plugin, pluginFile.getAbsolutePath(), e);
        }

        List<String> errors = new ArrayList<>();
        pluginValidator.validate(pluginArtifact, pluginDescriptor, errors);

        if (!errors.isEmpty()) {
            throw new InvalidPluginDescriptorException(
                    "Invalid plugin descriptor for " + plugin.getId() + " (" + pluginFile + ")", errors);
        }

        pluginDescriptor.setPluginArtifact(pluginArtifact);

        return pluginDescriptor;
    }

    private String getPluginDescriptorLocation() {
        return "META-INF/maven/plugin.xml";
    }

    private PluginDescriptor parsePluginDescriptor(
            PluginDescriptorBuilder.StreamSupplier is, Plugin plugin, String descriptorLocation)
            throws PluginDescriptorParsingException {
        try {
            return builder.build(is, descriptorLocation);
        } catch (PlexusConfigurationException e) {
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

    @Override
    public void checkPrerequisites(PluginDescriptor pluginDescriptor) throws PluginIncompatibleException {
        List<IllegalStateException> prerequisiteExceptions = new ArrayList<>();
        prerequisitesCheckers.forEach(c -> {
            try {
                c.accept(pluginDescriptor);
            } catch (IllegalStateException e) {
                prerequisiteExceptions.add(e);
            }
        });
        // aggregate all exceptions
        if (!prerequisiteExceptions.isEmpty()) {
            String messages = prerequisiteExceptions.stream()
                    .map(IllegalStateException::getMessage)
                    .collect(Collectors.joining(", "));
            PluginIncompatibleException pie = new PluginIncompatibleException(
                    pluginDescriptor.getPlugin(),
                    "The plugin " + pluginDescriptor.getId() + " has unmet prerequisites: " + messages,
                    prerequisiteExceptions.get(0));
            // the first exception is added as cause, all other ones as suppressed exceptions
            prerequisiteExceptions.stream().skip(1).forEach(pie::addSuppressed);
            throw pie;
        }
    }

    @Override
    @Deprecated
    public void checkRequiredMavenVersion(PluginDescriptor pluginDescriptor) throws PluginIncompatibleException {
        checkPrerequisites(pluginDescriptor);
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
            boolean v4api = pluginDescriptor.getMojos().stream().anyMatch(MojoDescriptor::isV4Api);
            Map<String, ClassLoader> foreignImports = calcImports(project, parent, imports, v4api);

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

        DependencyResult result = pluginDependenciesResolver.resolvePlugin(
                plugin,
                RepositoryUtils.toArtifact(pluginArtifact),
                dependencyFilter,
                project.getRemotePluginRepositories(),
                repositorySession);

        pluginArtifacts = toMavenArtifacts(result);

        pluginRealm = classRealmManager.createPluginRealm(
                plugin, parent, null, foreignImports, toAetherArtifacts(pluginArtifacts));

        discoverPluginComponents(pluginRealm, plugin, pluginDescriptor);

        pluginDescriptor.setDependencyNode(result.getRoot());
        pluginDescriptor.setClassRealm(pluginRealm);
        pluginDescriptor.setArtifacts(pluginArtifacts);
    }

    private void discoverPluginComponents(
            final ClassRealm pluginRealm, Plugin plugin, PluginDescriptor pluginDescriptor)
            throws PluginContainerException {
        try {
            if (pluginDescriptor != null) {
                for (MojoDescriptor mojo : pluginDescriptor.getMojos()) {
                    if (!mojo.isV4Api()) {
                        mojo.setRealm(pluginRealm);
                        container.addComponentDescriptor(mojo);
                    }
                }
            }

            ((DefaultPlexusContainer) container)
                    .discoverComponents(
                            pluginRealm,
                            new SessionScopeModule(container.lookup(SessionScope.class)),
                            new MojoExecutionScopeModule(container.lookup(MojoExecutionScope.class)),
                            new PluginConfigurationModule(plugin.getDelegate()));
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

    private List<Artifact> toMavenArtifacts(DependencyResult dependencyResult) {
        List<Artifact> artifacts =
                new ArrayList<>(dependencyResult.getArtifactResults().size());
        dependencyResult.getArtifactResults().stream()
                .filter(ArtifactResult::isResolved)
                .forEach(a -> artifacts.add(RepositoryUtils.toArtifact(a.getArtifact())));
        return Collections.unmodifiableList(artifacts);
    }

    private Map<String, ClassLoader> calcImports(
            MavenProject project, ClassLoader parent, List<String> imports, boolean v4api) {
        Map<String, ClassLoader> foreignImports = new HashMap<>();

        ClassLoader projectRealm = project.getClassRealm();
        if (projectRealm != null) {
            foreignImports.put("", projectRealm);
        } else {
            foreignImports.put(
                    "", v4api ? classRealmManager.getMaven4ApiRealm() : classRealmManager.getMavenApiRealm());
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
            if (mojoDescriptor.isV4Api()) {
                return loadV4Mojo(mojoInterface, session, mojoExecution, mojoDescriptor, pluginDescriptor, pluginRealm);
            } else {
                return loadV3Mojo(mojoInterface, session, mojoExecution, mojoDescriptor, pluginDescriptor, pluginRealm);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
            container.setLookupRealm(oldLookupRealm);
        }
    }

    private <T> T loadV4Mojo(
            Class<T> mojoInterface,
            MavenSession session,
            MojoExecution mojoExecution,
            MojoDescriptor mojoDescriptor,
            PluginDescriptor pluginDescriptor,
            ClassRealm pluginRealm)
            throws PluginContainerException, PluginConfigurationException {
        T mojo;

        InternalMavenSession sessionV4 = InternalMavenSession.from(session.getSession());
        Project project = sessionV4.getProject(session.getCurrentProject());

        org.apache.maven.api.MojoExecution execution = new DefaultMojoExecution(sessionV4, mojoExecution);
        org.apache.maven.api.plugin.Log log = new DefaultLog(
                LoggerFactory.getLogger(mojoExecution.getMojoDescriptor().getFullGoalName()));
        try {
            Injector injector = Injector.create();
            injector.discover(pluginRealm);
            // Add known classes
            // TODO: get those from the existing plexus scopes ?
            injector.bindInstance(Session.class, sessionV4);
            injector.bindInstance(Project.class, project);
            injector.bindInstance(org.apache.maven.api.MojoExecution.class, execution);
            injector.bindInstance(org.apache.maven.api.plugin.Log.class, log);
            mojo = mojoInterface.cast(injector.getInstance(
                    Key.of(mojoDescriptor.getImplementationClass(), mojoDescriptor.getRoleHint())));

        } catch (Exception e) {
            throw new PluginContainerException(mojoDescriptor, pluginRealm, "Unable to lookup Mojo", e);
        }

        XmlNode dom = mojoExecution.getConfiguration() != null
                ? mojoExecution.getConfiguration().getDom()
                : null;

        PlexusConfiguration pomConfiguration;

        if (dom == null) {
            pomConfiguration = new DefaultPlexusConfiguration("configuration");
        } else {
            pomConfiguration = XmlPlexusConfiguration.toPlexusConfiguration(dom);
        }

        ExpressionEvaluator expressionEvaluator =
                new PluginParameterExpressionEvaluatorV4(sessionV4, project, execution);

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

        for (Resolution resolution : mojoDescriptor.getMojoDescriptorV4().getResolutions()) {
            Field field = null;
            for (Class<?> clazz = mojo.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    field = clazz.getDeclaredField(resolution.getField());
                    break;
                } catch (NoSuchFieldException e) {
                    // continue
                }
            }
            if (field == null) {
                throw new PluginConfigurationException(
                        pluginDescriptor,
                        "Unable to find field '" + resolution.getField() + "' annotated with @Resolution");
            }
            field.setAccessible(true);
            String pathScope = resolution.getPathScope();
            Object result = null;
            if (pathScope != null && !pathScope.isEmpty()) {
                // resolution
                PathScope ps = sessionV4.getService(PathScopeRegistry.class).require(pathScope);
                DependencyResolverResult res =
                        sessionV4.getService(DependencyResolver.class).resolve(sessionV4, project, ps);
                if (field.getType() == DependencyResolverResult.class) {
                    result = res;
                } else if (field.getType() == Node.class) {
                    result = res.getRoot();
                } else if (field.getType() == List.class && field.getGenericType() instanceof ParameterizedType pt) {
                    Type t = pt.getActualTypeArguments()[0];
                    if (t == Node.class) {
                        result = res.getNodes();
                    } else if (t == Path.class) {
                        result = res.getPaths();
                    }
                } else if (field.getType() == Map.class && field.getGenericType() instanceof ParameterizedType pt) {
                    Type k = pt.getActualTypeArguments()[0];
                    Type v = pt.getActualTypeArguments()[1];
                    if (k == PathType.class
                            && v instanceof ParameterizedType ptv
                            && ptv.getRawType() == List.class
                            && ptv.getActualTypeArguments()[0] == Path.class) {
                        result = res.getDispatchedPaths();
                    } else if (k == Dependency.class && v == Path.class) {
                        result = res.getDependencies();
                    }
                }
            } else {
                // collection
                DependencyResolverResult res =
                        sessionV4.getService(DependencyResolver.class).collect(sessionV4, project);
                if (field.getType() == DependencyResolverResult.class) {
                    result = res;
                } else if (field.getType() == Node.class) {
                    result = res.getRoot();
                }
            }
            if (result == null) {
                throw new PluginConfigurationException(
                        pluginDescriptor,
                        "Unable to inject field '" + resolution.getField()
                                + "' annotated with @Dependencies. Unsupported type " + field.getGenericType());
            }
            try {
                field.set(mojo, result);
            } catch (IllegalAccessException e) {
                throw new PluginConfigurationException(
                        pluginDescriptor,
                        "Unable to inject field '" + resolution.getField() + "' annotated with @Dependencies",
                        e);
            }
        }

        return mojo;
    }

    private <T> T loadV3Mojo(
            Class<T> mojoInterface,
            MavenSession session,
            MojoExecution mojoExecution,
            MojoDescriptor mojoDescriptor,
            PluginDescriptor pluginDescriptor,
            ClassRealm pluginRealm)
            throws PluginContainerException, PluginConfigurationException {
        T mojo;

        try {
            mojo = container.lookup(mojoInterface, mojoDescriptor.getRoleHint());
        } catch (ComponentLookupException e) {
            Throwable cause = e.getCause();
            while (cause != null && !(cause instanceof LinkageError) && !(cause instanceof ClassNotFoundException)) {
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
            Logger mojoLogger = LoggerFactory.getLogger(mojoDescriptor.getImplementation());
            ((Mojo) mojo).setLog(new MojoLogWrapper(mojoLogger));
        }

        if (mojo instanceof Contextualizable) {
            pluginValidationManager.reportPluginMojoValidationIssue(
                    PluginValidationManager.IssueLocality.EXTERNAL,
                    session,
                    mojoDescriptor,
                    mojo.getClass(),
                    "Mojo implements `Contextualizable` interface from Plexus Container, which is EOL.");
        }

        XmlNode dom = mojoExecution.getConfiguration() != null
                ? mojoExecution.getConfiguration().getDom()
                : null;

        PlexusConfiguration pomConfiguration;

        if (dom == null) {
            pomConfiguration = new DefaultPlexusConfiguration("configuration");
        } else {
            pomConfiguration = XmlPlexusConfiguration.toPlexusConfiguration(dom);
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

        if (configuratorId == null || configuratorId.isEmpty()) {
            configuratorId = mojoDescriptor.isV4Api() ? "enhanced" : "basic";
        }

        try {
            // TODO could the configuration be passed to lookup and the configurator known to plexus via the descriptor
            // so that this method could entirely be handled by a plexus lookup?
            configurator = container.lookup(ComponentConfigurator.class, configuratorId);

            ConfigurationListener listener = new DebugConfigurationListener(logger);

            ValidatingConfigurationListener validator =
                    new ValidatingConfigurationListener(mojo, mojoDescriptor, listener);

            if (logger.isDebugEnabled()) {
                logger.debug("Configuring mojo execution '" + mojoDescriptor.getId() + ':' + executionId + "' with "
                        + configuratorId + " configurator -->");
            }

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
                    logger.debug(
                            "Error releasing mojo for {} {execution: {}}",
                            goalExecId,
                            mojoExecution.getExecutionId(),
                            e);
                } else {
                    logger.debug("Error releasing mojo for {}", goalExecId, e);
                }
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

        // TODO: store plugin version

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
        DependencyResult root =
                pluginDependenciesResolver.resolvePlugin(extensionPlugin, null, null, repositories, session);
        return toMavenArtifacts(root);
    }

    static class NamedImpl implements Named {
        private final String value;

        NamedImpl(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }

        public int hashCode() {
            return 127 * "value".hashCode() ^ this.value.hashCode();
        }

        public boolean equals(Object o) {
            return o instanceof Named && this.value.equals(((Named) o).value());
        }

        public Class<? extends Annotation> annotationType() {
            return Named.class;
        }
    }
}
