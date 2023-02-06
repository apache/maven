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
package org.apache.maven.artifact.repository;

/**
 * Authentication
 */
public class Authentication {

    private String privateKey;

    private String passphrase;

    public Authentication(String userName, String password) {
        this.username = userName;
        this.password = password;
    }

    /**
     * Username used to login to the host
     */
    private String username;

    /**
     * Password associated with the login
     */
    private String password;

    /**
     * Get the user's password which is used when connecting to the repository.
     *
     * @return password of user
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the user's password which is used when connecting to the repository.
     *
     * @param password password of the user
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get the username used to access the repository.
     *
     * @return username at repository
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set username used to access the repository.
     *
     * @param userName the username used to access repository
     */
    public void setUsername(final String userName) {
        this.username = userName;
    }

    /**
     * Get the passphrase of the private key file. The passphrase is used only when host/protocol supports
     * authentication via exchange of private/public keys and private key was used for authentication.
     *
     * @return passphrase of the private key file
     */
    public String getPassphrase() {
        return passphrase;
    }

    /**
     * Set the passphrase of the private key file.
     *
     * @param passphrase passphrase of the private key file
     */
    public void setPassphrase(final String passphrase) {
        this.passphrase = passphrase;
    }

    /**
     * Get the absolute path to the private key file.
     *
     * @return absolute path to private key
     */
    public String getPrivateKey() {
        return privateKey;
    }

    /**
     * Set the absolute path to private key file.
     *
     * @param privateKey path to private key in local file system
     */
    public void setPrivateKey(final String privateKey) {
        this.privateKey = privateKey;
    }
}
