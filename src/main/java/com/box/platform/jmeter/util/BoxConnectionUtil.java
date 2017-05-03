package com.box.platform.jmeter.util;

import com.box.sdk.*;

/**
 * Utility class to get a Box connection
 */
public class BoxConnectionUtil {

    private BoxDeveloperEditionAPIConnection api = null;

    private String publicKeyId = null;
    private String privateKey = null;
    private String keyPass = null;
    private String enterpriseId = null;
    private String clientId = null;
    private String clientSecret = null;
    private int maxCacheEntries;

    public BoxConnectionUtil(String publicKeyId, String privateKey, String keyPass, String enterpriseId, String clientId, String clientSecret, int maxCacheEntries){
        this.publicKeyId = publicKeyId;
        this.privateKey = privateKey;
        this.keyPass = keyPass;
        this.enterpriseId = enterpriseId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.maxCacheEntries = maxCacheEntries;
    }

    /**
     * Get an app user connection
     */
    public BoxDeveloperEditionAPIConnection getAppUserConnection(String userLogin){
        try{
            // Create JWT Encryption preferences
            JWTEncryptionPreferences encryptionPref = new JWTEncryptionPreferences();
            encryptionPref.setPublicKeyID(publicKeyId);
            encryptionPref.setPrivateKey(privateKey);
            encryptionPref.setPrivateKeyPassword(keyPass);
            encryptionPref.setEncryptionAlgorithm(EncryptionAlgorithm.RSA_SHA_256);
            IAccessTokenCache accessTokenCache = new InMemoryLRUAccessTokenCache(maxCacheEntries);

            // Instantiate Box Connection
            this.api = BoxDeveloperEditionAPIConnection.getAppUserConnection(
                    userLogin, clientId, clientSecret, encryptionPref, accessTokenCache);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return api;
    }

    /**
     * Get an enterprise connection
     */
    public BoxDeveloperEditionAPIConnection getEnterpriseConnection(){
        try{
            // Create JWT Encryption preferences
            JWTEncryptionPreferences encryptionPref = new JWTEncryptionPreferences();
            encryptionPref.setPublicKeyID(publicKeyId);
            encryptionPref.setPrivateKey(privateKey);

            encryptionPref.setPrivateKeyPassword(keyPass);
            encryptionPref.setEncryptionAlgorithm(EncryptionAlgorithm.RSA_SHA_256);
            IAccessTokenCache accessTokenCache = new InMemoryLRUAccessTokenCache(maxCacheEntries);

            // Instantiate Box Connection
            api = BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(
                    enterpriseId, clientId, clientSecret, encryptionPref, accessTokenCache);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return api;
    }
}
