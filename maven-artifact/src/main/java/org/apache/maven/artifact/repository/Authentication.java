package org.apache.maven.artifact.repository;

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

/**
 * Authentication
 * <br>May 2020, MNG-5583 per endpoint PKI authentication
 */
public class Authentication
{

    private String privateKey;

    private String passphrase;

    public Authentication( String userName, String password )
    {
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
    public String getPassword()
    {
        return password;
    }

    /**
     * Set the user's password which is used when connecting to the repository.
     *
     * @param password password of the user
     */
    public void setPassword( String password )
    {
        this.password = password;
    }

    /**
     * Get the username used to access the repository.
     *
     * @return username at repository
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * Set username used to access the repository.
     *
     * @param userName the username used to access repository
     */
    public void setUsername( final String userName )
    {
        this.username = userName;
    }

    /**
     * Get the passphrase of the private key file. The passphrase is used only when host/protocol supports
     * authentication via exchange of private/public keys and private key was used for authentication.
     *
     * @return passphrase of the private key file
     */
    public String getPassphrase()
    {
        return passphrase;
    }

    /**
     * Set the passphrase of the private key file.
     *
     * @param passphrase passphrase of the private key file
     */
    public void setPassphrase( final String passphrase )
    {
        this.passphrase = passphrase;
    }

    /**
     * Get the absolute path to the private key file.
     *
     * @return absolute path to private key
     */
    public String getPrivateKey()
    {
        return privateKey;
    }

    /**
     * Set the absolute path to private key file.
     *
     * @param privateKey path to private key in local file system
     */
    public void setPrivateKey( final String privateKey )
    {
        this.privateKey = privateKey;
    }
    
    
     /**
     *
     *
     * The path to the trust store. If not defined, the JRE's cacert store is
     * used.
     *
     *
     */
    private String trustStore;

    /**
     *
     *
     * The password to the trust store.
     *
     *
     */
    private String trustStorePassword;

    /**
     *
     *
     * The type of trust store, default is JKS
     *
     * .
     */
    private String trustStoreType;

    /**
     *
     *
     * The path to the keystore used for authentication purposes, or null
     *
     * .
     */
    private String keyStore;

    /**
     *
     *
     * Keystore password, can be null
     *
     * .
     */
    private String keyStorePassword;

    /**
     *
     *
     * Keystore if the key store has multiple key pairs, this can be used to
     * explicitly select a specific certificate via it's alias. If null, the
     * most appropriate certificate is automatically selected by the SSL Factory
     *
     * .
     */
    private String keyAlias;

    /**
     *
     *
     * The password to unlock the key, can be null
     *
     * .
     */
    private String keyPassword;

    /**
     *
     *
     * The key store type, defaults to JKS
     *
     * .
     */
    private String keyStoreType;

    /**
     *
     *
     * The path to the trust store. If not defined, the JRE's cacert store is
     * used.
     *
     *
     * @return path, name or null
     */
    public String getTrustStore()
    {
        return trustStore;
    }

    /**
     *
     *
     * The path to the trust store. If not defined, the JRE's cacert store is
     * used.
     *
     *
     * @param trustStore path name or null
     */
    public void setTrustStore( String trustStore )
    {
        this.trustStore = trustStore;
    }

    /**
     *
     *
     * The password to the trust store.
     *
     *
     * @return password or null
     */
    public String getTrustStorePassword()
    {
        return trustStorePassword;
    }

    /**
     *
     *
     * The password to the trust store.
     *
     *
     * @param trustStorePassword password or null
     */
    public void setTrustStorePassword( String trustStorePassword ) 
    {
        this.trustStorePassword = trustStorePassword;
    }

    /**
     *
     *
     * The type of trust store, default is JKS
     *
     * .
     *
     * @return type
     */
    public String getTrustStoreType()
    {
        return trustStoreType;
    }

    /**
     *
     *
     * The type of trust store, default is JKS
     *
     * .
     *
     * @param trustStoreType key store type
     */
    public void setTrustStoreType( String trustStoreType )
    {
        this.trustStoreType = trustStoreType;
    }

    /**
     *
     *
     * The path to the keystore used for authentication purposes, or null
     *
     * .
     *
     * @return path, named keystore (such as MY) or null
     */
    public String getKeyStore()
    {
        return keyStore;
    }

    /**
     *
     *
     * The path to the keystore used for authentication purposes, or null
     *
     * .
     *
     * @param keyStore keystore path, name or null
     */
    public void setKeyStore( String keyStore )
    {
        this.keyStore = keyStore;
    }

    /**
     *
     *
     * Keystore password, can be null
     *
     * .
     *
     * @return password or null
     */
    public String getKeyStorePassword()
    {
        return keyStorePassword;
    }

    /**
     *
     *
     * Keystore password, can be null
     *
     * .
     *
     * @param keyStorePassword password or null
     */
    public void setKeyStorePassword( String keyStorePassword )
    {
        this.keyStorePassword = keyStorePassword;
    }

    /**
     *
     *
     * Keystore if the key store has multiple key pairs, this can be used to
     * explicitly select a specific certificate via it's alias. If null, the
     * most appropriate certificate is automatically selected by the SSL Factory
     *
     * .
     *
     * @return the alias or null
     */
    public String getKeyAlias()
    {
        return keyAlias;
    }

    /**
     *
     *
     * Keystore if the key store has multiple key pairs, this can be used to
     * explicitly select a specific certificate via it's alias. If null, the
     * most appropriate certificate is automatically selected by the SSL Factory
     *
     * .
     *
     * @param keyAlias alias
     */
    public void setKeyAlias( String keyAlias )
    {
        this.keyAlias = keyAlias;
    }

    /**
     *
     *
     * The password to unlock the key, can be null
     *
     * .
     */
    public String getKeyPassword()
    {
        return keyPassword;
    }

    /**
     *
     *
     * The password to unlock the key, can be null
     *
     * .
     */
    public void setKeyPassword( String keyPassword )
    {
        this.keyPassword = keyPassword;
    }

    /**
     *
     *
     * The key store type, defaults to JKS
     *
     * .
     */
    public String getKeyStoreType()
    {
        return keyStoreType;
    }

    /**
     *
     *
     * The key store type, defaults to JKS
     *
     * .
     */
    public void setKeyStoreType( String keyStoreType )
    {
        this.keyStoreType = keyStoreType;
    }
    
    

}
