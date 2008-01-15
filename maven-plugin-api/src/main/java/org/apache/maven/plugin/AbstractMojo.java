package org.apache.maven.plugin;

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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.util.Map;

/**
 * Abstract class to provide most of the infrastructure required to implement a <code>Mojo</code> except for
 * the execute method.
 * <br/>
 * The implementation should have a <code>goal</code> annotation in the class-level javadoc annotation:
 * <pre>
 * &#47;&#42;&#42;
 *  &#42; &#64;goal goalName
 *  &#42;&#47;
 * </pre>
 *
 * There are also a number of class-level javadoc annotations which can be used to control how and when the
 * <code>Mojo</code> is executed:
 * <br/>
 * <br/>
 *
 * <table border="1">
 *  <tr bgcolor="#CCCCCC">
 *      <th>Descriptor Element</th>
 *      <th>Annotation</th>
 *      <th>Required?</th>
 *      <th>Notes</th>
 *  </tr>
 *  <tr>
 *      <td>goal</td>
 *      <td>@goal &lt;goalName&gt;</td>
 *      <td>Yes</td>
 *      <td>The name for the Mojo that users will reference from the command line to execute the Mojo directly,
 *      or inside a POM in order to provide Mojo-specific configuration.</td>
 *  </tr>
 *  <tr>
 *      <td>implementation</td>
 *      <td>none (detected)</td>
 *      <td>Yes</td>
 *      <td>The Mojo's fully-qualified class name (or script path in the case of non-Java Mojos).</td>
 *  </tr>
 *  <tr>
 *      <td>language</td>
 *      <td>none (detected)</td>
 *      <td>No. Default: <code>java</code></td>
 *      <td>The implementation language for this Mojo (Java, beanshell, etc.).</td>
 *  </tr>
 *  <tr>
 *      <td>configurator</td>
 *      <td>@configurator &lt;roleHint&gt;</td>
 *      <td>No</td>
 *      <td>The configurator type to use when injecting parameter values into this Mojo. The value is normally
 *          deduced from the Mojo's implementation language, but can be specified to allow a custom
 *          ComponentConfigurator implementation to be used.
 *          <br/>
 *          <i>NOTE: This will only be used in very special cases, using a highly controlled vocabulary of possible
 *          values. (Elements like this are why it's a good idea to use the descriptor tools.)</i>
 *      </td>
 *   </tr>
 *   <tr>
 *      <td>phase</td>
 *      <td>@phase &lt;phaseName&gt;</td>
 *      <td>No</td>
 *      <td>Binds this Mojo to a particular phase of the standard build lifecycle, if specified.
 *          <br/>
 *          <i>NOTE: This is only required if this Mojo is to participate in the standard build process.</i>
 *      </td>
 *   </tr>
 *   <tr>
 *      <td>execute</td>
 *      <td>@execute [phase=&lt;phaseName&gt;|goal=&lt;goalName&gt;] [lifecycle=&lt;lifecycleId&gt;]</td>
 *      <td>No</td>
 *      <td>When this goal is invoked, it will first invoke a parallel lifecycle, ending at the given phase.
 *          If a goal is provided instead of a phase, that goal will be executed in isolation.
 *          The execution of either will not affect the current project, but instead make available the
 *          <code>${executedProject}</code> expression if required. An alternate lifecycle can also be provided:
 *          for more information see the documentation on the
 *          <a href="http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html" target="_blank">build lifecycle</a>.
 *      </td>
 *   </tr>
 *   <tr>
 *      <td>requiresDependencyResolution</td>
 *      <td>@requiresDependencyResolution &lt;requiredScope&gt;</td>
 *      <td>No</td>
 *      <td>Flags this Mojo as requiring the dependencies in the specified scope (or an implied scope) to be
 *          resolved before it can execute.
 *          <br/>
 *          <i>NOTE: Currently supports <b>compile</b>, <b>runtime</b>, and <b>test</b> scopes.</i>
 *      </td>
 *   </tr>
 *   <tr>
 *      <td>description</td>
 *      <td>none (detected)</td>
 *      <td>No</td>
 *      <td>The description of this Mojo's functionality. <i>Using the toolset, this will be the class-level
 *          Javadoc description provided.
 *          <br/>
 *          <i>NOTE: While this is not a required part of the Mojo specification, it <b>SHOULD</b> be provided to
 *          enable future tool support for browsing, etc. and for clarity.</i>
 *      </td>
 *   </tr>
 *   <tr>
 *      <td>parameters</td>
 *      <td>N/A</td>
 *      <td>No</td>
 *      <td>Specifications for the parameters which this Mojo uses will be provided in <b>parameter</b> sub-elements
 *          in this section.
 *          <br/>
 *          <i>NOTE: Parameters are discussed in more detail below.</i>
 *      </td>
 *   </tr>
 * </table>
 *
 * @see <a href="http://maven.apache.org/guides/plugin/guide-java-plugin-development.html" target="_blank">Guide to Developing Java Plugins</a>
 * @see <a href="http://maven.apache.org/guides/mini/guide-configuring-plugins.html" target="_blank">Guide to Configuring Plug-ins</a>
 * @see <a href="http://maven.apache.org/developers/mojo-api-specification.html" target="_blank">Mojo API Specification</a>
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author jdcasey
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public abstract class AbstractMojo
    implements Mojo, ContextEnabled
{
    /** Instance logger */
    private Log log;

    /** Plugin container context */
    private Map pluginContext;

    /**
     * @see org.apache.maven.plugin.Mojo#setLog(org.apache.maven.plugin.logging.Log)
     */
    public void setLog( Log log )
    {
        this.log = log;
    }

    /**
     * Returns the logger that has been injected into this mojo. If no logger has been setup yet, a <code>SystemStreamLog</code>
     * logger will be created and returned.
     * <br/><br/>
     * <strong>Note:</strong>
     * The logger returned by this method must not be cached in an instance field during the construction of the mojo.
     * This would cause the mojo to use a wrongly configured default logger when being run by Maven. The proper logger
     * gets injected by the Plexus container <em>after</em> the mojo has been constructed. Therefore, simply call this
     * method directly whenever you need the logger, it is fast enough and needs no caching.
     * 
     * @see org.apache.maven.plugin.Mojo#getLog()
     */
    public Log getLog()
    {
        if ( log == null )
        {
            log = new SystemStreamLog();
        }

        return log;
    }

    /**
     * @see org.apache.maven.plugin.ContextEnabled#getPluginContext()
     */
    public Map getPluginContext()
    {
        return pluginContext;
    }

    /**
     * @see org.apache.maven.plugin.ContextEnabled#setPluginContext(java.util.Map)
     */
    public void setPluginContext( Map pluginContext )
    {
        this.pluginContext = pluginContext;
    }
}
