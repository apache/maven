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
package org.apache.maven.api;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.annotations.Nonnull;

/**
 * A root directory of source files.
 * The sources may be Java main classes, test classes, resources or anything else identified by the scope.
 *
 * <h2>Default values</h2>
 * The properties in this interface are defined in the {@code <Source>} element of the
 * {@linkplain org.apache.maven.api.model.Model Maven project descriptor}.
 * For each property, the default value is either empty or documented in the project descriptor.
 */
public interface SourceRoot {
    /**
     * {@return the root directory where the sources are stored}
     * The path is relative to the <abbr>POM</abbr> file.
     *
     * <h4>Default implementation</h4>
     * The default value depends on whether a {@linkplain #module() module name} is specified in this source root:
     * <ul>
     *   <li>
     *     If no module name, then the default directory is
     *     <code>src/{@linkplain #scope() scope}/{@linkplain #language() language}</code>.
     *   </li><li>
     *     If a module name is present, then the default directory is
     *     <code>src/{@linkplain #module() module}/{@linkplain #scope() scope}/{@linkplain #language() language}</code>.
     *   </li>
     * </ul>
     *
     * The default value is relative.
     * Implementation may override with absolute path instead.
     */
    default Path directory() {
        Path src = Path.of("src");
        return module().map(src::resolve)
                .orElse(src)
                .resolve(scope().id())
                .resolve(language().id());
    }

    /**
     * {@return the list of patterns for the files to include}
     * The path separator is {@code /} on all platforms, including Windows.
     * The prefix before the {@code :} character, if present and longer than 1 character, is the syntax.
     * If no syntax is specified, or if its length is 1 character (interpreted as a Windows drive),
     * the default is a Maven-specific variation of the {@code "glob"} pattern.
     *
     * <p>The default implementation returns an empty list, which means to apply a language-dependent pattern.
     * For example, for the Java language, the pattern includes all files with the {@code .java} suffix.</p>
     *
     * @see java.nio.file.FileSystem#getPathMatcher(String)
     */
    default List<String> includes() {
        return List.of();
    }

    /**
     * {@return the list of patterns for the files to exclude}
     * The exclusions are applied after the inclusions.
     * The default implementation returns an empty list.
     */
    default List<String> excludes() {
        return List.of();
    }

    /**
     * {@return a matcher combining the include and exclude patterns}
     * If the user did not specify any includes, the given {@code defaultIncludes} are used.
     * These defaults depend on the plugin.
     * For example, the default include of the Java compiler plugin is <code>"**&sol;*.java"</code>.
     *
     * <p>If the user did not specify any excludes, the default is often files generated
     * by Source Code Management (<abbr>SCM</abbr>) software or by the operating system.
     * Examples: <code>"**&sol;.gitignore"</code>, <code>"**&sol;.DS_Store"</code>.</p>
     *
     * @param defaultIncludes the default includes if unspecified by the user
     * @param useDefaultExcludes whether to add the default set of patterns to exclude,
     *        mostly Source Code Management (<abbr>SCM</abbr>) files
     */
    PathMatcher matcher(Collection<String> defaultIncludes, boolean useDefaultExcludes);

    /**
     * {@return in which context the source files will be used}
     * Not to be confused with dependency scope.
     * The default value is {@code "main"}.
     *
     * @see ProjectScope#MAIN
     */
    default ProjectScope scope() {
        return ProjectScope.MAIN;
    }

    /**
     * {@return the language of the source files}
     * The default value is {@code "java"}.
     *
     * @see Language#JAVA_FAMILY
     */
    default Language language() {
        return Language.JAVA_FAMILY;
    }

    /**
     * {@return the name of the Java module (or other language-specific module) which is built by the sources}
     * The default value is empty.
     */
    default Optional<String> module() {
        return Optional.empty();
    }

    /**
     * {@return the version of the platform where the code will be executed}
     * In a Java environment, this is the value of the {@code --release} compiler option.
     * The default value is empty.
     */
    default Optional<Version> targetVersion() {
        return Optional.empty();
    }

    /**
     * {@return an explicit target path, overriding the default value}
     * <p>
     * <strong>Important:</strong> This method returns the target path <em>as specified in the configuration</em>,
     * which may be relative or absolute. It does <strong>not</strong> perform any path resolution.
     * For the fully resolved absolute path, use {@link #targetPath(Project)} instead.
     * </p>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * </p>
     * <ul>
     *   <li><strong>Empty Optional</strong> - No explicit target path was specified. Files should be copied
     *       to the root of the output directory (see {@link Project#getOutputDirectory(ProjectScope)}).</li>
     *   <li><strong>Relative Path</strong> (e.g., {@code Path.of("META-INF/resources")}) - The path is
     *       <em>intended to be resolved</em> relative to the output directory for this source root's {@link #scope()}.
     *       <ul>
     *         <li>For {@link ProjectScope#MAIN}: relative to {@code target/classes}</li>
     *         <li>For {@link ProjectScope#TEST}: relative to {@code target/test-classes}</li>
     *       </ul>
     *       The actual resolution is performed by {@link #targetPath(Project)}.</li>
     *   <li><strong>Absolute Path</strong> (e.g., {@code Path.of("/tmp/custom")}) - The path is used as-is
     *       without any resolution. Files will be copied to this exact location.</li>
     * </ul>
     * <p>
     * <strong>Maven 3 Compatibility:</strong> This behavior maintains compatibility with Maven 3.x,
     * where resource {@code targetPath} elements were always interpreted as relative to the output directory
     * ({@code project.build.outputDirectory} or {@code project.build.testOutputDirectory}),
     * not the project base directory. Maven 3 plugins (like maven-resources-plugin) expect to receive
     * the relative path and perform the resolution themselves.
     * </p>
     * <p>
     * <strong>Effect on Module and Target Version:</strong>
     * When a target path is explicitly specified, the values of {@link #module()} and {@link #targetVersion()}
     * are not used for inferring the output path (they are still used as compiler options however).
     * This means that for scripts and resources, the files below the path specified by {@link #directory()}
     * are copied to the path specified by {@code targetPath()} with the exact same directory structure.
     * </p>
     * <p>
     * <strong>Usage Guidance:</strong>
     * </p>
     * <ul>
     *   <li><strong>For Maven 4 API consumers:</strong> Use {@link #targetPath(Project)} to get the
     *       fully resolved absolute path where files should be copied.</li>
     *   <li><strong>For Maven 3 compatibility layer:</strong> Use this method to get the path as specified
     *       in the configuration, which can then be passed to legacy plugins that expect to perform
     *       their own resolution.</li>
     *   <li><strong>For implementers:</strong> Store the path exactly as provided in the configuration.
     *       Do not resolve relative paths at storage time.</li>
     * </ul>
     *
     * @see #targetPath(Project)
     * @see Project#getOutputDirectory(ProjectScope)
     */
    default Optional<Path> targetPath() {
        return Optional.empty();
    }

    /**
     * {@return the fully resolved absolute target path where files should be copied}
     * <p>
     * <strong>Purpose:</strong> This method performs the complete path resolution logic, converting
     * the potentially relative {@link #targetPath()} into an absolute filesystem path. This is the
     * method that Maven 4 API consumers should use when they need to know the actual destination
     * directory for copying files.
     * </p>
     * <p>
     * <strong>Resolution Algorithm:</strong>
     * </p>
     * <ol>
     *   <li>Obtain the {@linkplain #targetPath() configured target path} (which may be empty, relative, or absolute)</li>
     *   <li>If the configured target path is absolute (e.g., {@code /tmp/custom}):
     *       <ul><li>Return it unchanged (no resolution needed)</li></ul></li>
     *   <li>Otherwise, get the output directory for this source root's {@link #scope()} by calling
     *       {@code project.getOutputDirectory(scope())}:
     *       <ul>
     *         <li>For {@link ProjectScope#MAIN}: typically {@code /path/to/project/target/classes}</li>
     *         <li>For {@link ProjectScope#TEST}: typically {@code /path/to/project/target/test-classes}</li>
     *       </ul></li>
     *   <li>If the configured target path is empty:
     *       <ul><li>Return the output directory as-is</li></ul></li>
     *   <li>If the configured target path is relative (e.g., {@code META-INF/resources}):
     *       <ul><li>Resolve it against the output directory using {@code outputDirectory.resolve(targetPath)}</li></ul></li>
     * </ol>
     * <p>
     * <strong>Concrete Examples:</strong>
     * </p>
     * <p>
     * Given a project at {@code /home/user/myproject} with {@link ProjectScope#MAIN}:
     * </p>
     * <table class="striped">
     *   <caption>Target Path Resolution Examples</caption>
     *   <thead>
     *     <tr>
     *       <th>Configuration ({@code targetPath()})</th>
     *       <th>Output Directory</th>
     *       <th>Result ({@code targetPath(project)})</th>
     *       <th>Explanation</th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *       <td>{@code Optional.empty()}</td>
     *       <td>{@code /home/user/myproject/target/classes}</td>
     *       <td>{@code /home/user/myproject/target/classes}</td>
     *       <td>No explicit path → use output directory</td>
     *     </tr>
     *     <tr>
     *       <td>{@code Optional.of(Path.of("META-INF"))}</td>
     *       <td>{@code /home/user/myproject/target/classes}</td>
     *       <td>{@code /home/user/myproject/target/classes/META-INF}</td>
     *       <td>Relative path → resolve against output directory</td>
     *     </tr>
     *     <tr>
     *       <td>{@code Optional.of(Path.of("WEB-INF/classes"))}</td>
     *       <td>{@code /home/user/myproject/target/classes}</td>
     *       <td>{@code /home/user/myproject/target/classes/WEB-INF/classes}</td>
     *       <td>Relative path with subdirectories</td>
     *     </tr>
     *     <tr>
     *       <td>{@code Optional.of(Path.of("/tmp/custom"))}</td>
     *       <td>{@code /home/user/myproject/target/classes}</td>
     *       <td>{@code /tmp/custom}</td>
     *       <td>Absolute path → use as-is (no resolution)</td>
     *     </tr>
     *   </tbody>
     * </table>
     * <p>
     * <strong>Relationship to {@link #targetPath()}:</strong>
     * </p>
     * <p>
     * This method is the <em>resolution</em> counterpart to {@link #targetPath()}, which is the
     * <em>storage</em> method. While {@code targetPath()} returns the path as configured (potentially relative),
     * this method returns the absolute path where files will actually be written. The separation allows:
     * </p>
     * <ul>
     *   <li>Maven 4 API consumers to get absolute paths via this method</li>
     *   <li>Maven 3 compatibility layer to get relative paths via {@code targetPath()} for legacy plugins</li>
     *   <li>Implementations to store paths without premature resolution</li>
     * </ul>
     * <p>
     * <strong>Implementation Note:</strong> The default implementation is equivalent to:
     * </p>
     * <pre>{@code
     * Optional<Path> configured = targetPath();
     * if (configured.isPresent() && configured.get().isAbsolute()) {
     *     return configured.get();
     * }
     * Path outputDir = project.getOutputDirectory(scope());
     * return configured.map(outputDir::resolve).orElse(outputDir);
     * }</pre>
     *
     * @param project the project to use for obtaining the output directory
     * @return the absolute path where files from {@link #directory()} should be copied
     *
     * @see #targetPath()
     * @see Project#getOutputDirectory(ProjectScope)
     */
    @Nonnull
    default Path targetPath(@Nonnull Project project) {
        Optional<Path> targetPath = targetPath();
        // The test for `isAbsolute()` is a small optimization for avoiding the call to `getOutputDirectory(…)`.
        return targetPath.filter(Path::isAbsolute).orElseGet(() -> {
            Path base = project.getOutputDirectory(scope());
            return targetPath.map(base::resolve).orElse(base);
        });
    }

    /**
     * {@return whether resources are filtered to replace tokens with parameterized values}
     * The default value is {@code false}.
     */
    default boolean stringFiltering() {
        return false;
    }

    /**
     * {@return whether the directory described by this source element should be included in the build}
     * The default value is {@code true}.
     */
    default boolean enabled() {
        return true;
    }
}
