package org.apache.maven.repository;

public class Authentication
{
    /**
     * Username used to login to the host
     */
    private String userName;

    /**
     * Password associated with the login
     */
    private String password;

    /**
     * Passphrase of the user's private key file
     */
    private String passphrase;

    /**
     * The absolute path to private key file
     */
    private String privateKey;

    /**
     * Get the passphrase of the private key file. The passphrase is used only when host/protocol
     * supports authentication via exchange of private/public keys and private key was used for
     * authentication.
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
    public void setPassphrase( String passphrase )
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
    public void setPrivateKey( String privateKey )
    {
        this.privateKey = privateKey;
    }

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
    public String getUserName()
    {
        return userName;
    }

    /**
     * Set username used to access the repository.
     * 
     * @param userName the username used to access repository
     */
    public void setUserName( final String userName )
    {
        this.userName = userName;
    }
}
