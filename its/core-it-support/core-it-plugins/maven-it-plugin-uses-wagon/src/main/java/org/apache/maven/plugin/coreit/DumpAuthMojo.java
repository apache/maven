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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

/**
 * Dumps the authentication info registered with the wagon manager for a server to a properties file.
 *
 * @author Benjamin Bentmann
 */
@Mojo(name = "dump-auth", defaultPhase = LifecyclePhase.VALIDATE)
public class DumpAuthMojo extends AbstractMojo {

    /**
     * Project base directory used for manual path alignment.
     */
    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File basedir;

    /**
     * The Wagon manager used to retrieve authentication infos.
     */
    @Component
    private WagonManager wagonManager;

    /**
     * The path to the properties file used to dump the auth infos.
     */
    @Parameter(property = "wagon.propertiesFile")
    private File propertiesFile;

    /**
     * The set of server identifiers whose auth infos should be dumped.
     */
    @Parameter
    private String[] serverIds;

    /**
     * Runs this mojo.
     *
     * @throws MojoFailureException If the output file could not be created.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        Properties authProperties = new Properties();

        for (String serverId : serverIds) {
            getLog().info("[MAVEN-CORE-IT-LOG] Getting authentication info for server " + serverId);

            AuthenticationInfo authInfo = wagonManager.getAuthenticationInfo(serverId);
            if (authInfo != null) {
                if (authInfo.getUserName() != null) {
                    authProperties.setProperty(serverId + ".username", authInfo.getUserName());
                }
                if (authInfo.getPassword() != null) {
                    authProperties.setProperty(serverId + ".password", authInfo.getPassword());
                }
                if (authInfo.getPrivateKey() != null) {
                    authProperties.setProperty(serverId + ".privateKey", authInfo.getPrivateKey());
                }
                if (authInfo.getPassphrase() != null) {
                    authProperties.setProperty(serverId + ".passphrase", authInfo.getPassphrase());
                }

                getLog().info("[MAVEN-CORE-IT-LOG]   username = " + authInfo.getUserName());
                getLog().info("[MAVEN-CORE-IT-LOG]   password = " + authInfo.getPassword());
                getLog().info("[MAVEN-CORE-IT-LOG]   private key = " + authInfo.getPrivateKey());
                getLog().info("[MAVEN-CORE-IT-LOG]   passphrase = " + authInfo.getPassphrase());
            } else {
                getLog().info("[MAVEN-CORE-IT-LOG]   (no authentication info available)");
            }
        }

        if (!propertiesFile.isAbsolute()) {
            propertiesFile = new File(basedir, propertiesFile.getPath());
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Creating output file " + propertiesFile);

        OutputStream out = null;
        try {
            propertiesFile.getParentFile().mkdirs();
            out = new FileOutputStream(propertiesFile);
            authProperties.store(out, "MAVEN-CORE-IT-LOG");
        } catch (IOException e) {
            throw new MojoExecutionException("Output file could not be created: " + propertiesFile, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // just ignore
                }
            }
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Created output file " + propertiesFile);
    }
}
