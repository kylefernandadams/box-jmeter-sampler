package com.box.platform.jmeter.sampler;

import com.box.sdk.*;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * JMeter sampler client which creates a Box connection.
 */
public class GetBoxConnection implements JavaSamplerClient{
    private static final String CLIENT_ID = "client.id";
    private static final String CLIENT_SECRET = "client.secret";
    private static final String ENTERPRISE_ID = "enterprise.id";
    private static final String PUBLIC_KEY_ID = "public.key.id";
    private static final String PRIVATE_KEY_PATH = "private.key.path";
    private static final String KEY_PASS = "key.pass";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";

    private String clientId = null;
    private String clientSecret = null;
    private String enterpriseId = null;
    private String publicKeyId = null;
    private String privateKey = null;
    private String keyPass = null;
    private int maxCacheEntries;

    /**
     * Get default parameters for the sampler client
     */
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(CLIENT_ID, "${"+ CLIENT_ID + "}");
        defaultParameters.addArgument(CLIENT_SECRET, "${" + CLIENT_SECRET + "}");
        defaultParameters.addArgument(ENTERPRISE_ID, "${" + ENTERPRISE_ID + "}");
        defaultParameters.addArgument(PUBLIC_KEY_ID, "${" + PUBLIC_KEY_ID + "}");
        defaultParameters.addArgument(PRIVATE_KEY_PATH, "${" + PRIVATE_KEY_PATH + "}");
        defaultParameters.addArgument(KEY_PASS, "${" + KEY_PASS + "}");
        defaultParameters.addArgument(MAX_CACHE_ENTRIES, "${" + MAX_CACHE_ENTRIES + "}");

        return defaultParameters;
    }

    /**
     * Setup the sampler client and the necessary parameters to create a Box connection.
     */
    public void setupTest(JavaSamplerContext setupContext) {
        try {
            clientId = setupContext.getParameter(CLIENT_ID);
            clientSecret = setupContext.getParameter(CLIENT_SECRET);
            enterpriseId = setupContext.getParameter(ENTERPRISE_ID);
            publicKeyId = setupContext.getParameter(PUBLIC_KEY_ID);
            String privateKeyPath = setupContext.getParameter(PRIVATE_KEY_PATH);
            keyPass = setupContext.getParameter(KEY_PASS);
            privateKey = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            maxCacheEntries = setupContext.getIntParameter(MAX_CACHE_ENTRIES);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the sampler client.
     * - Create the JWT encryption preferences
     * - Create a Box connection.
     */
    public SampleResult runTest(JavaSamplerContext runContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();

        try{
            // Create JWT Encryption preferences
            JWTEncryptionPreferences encryptionPref = new JWTEncryptionPreferences();
            encryptionPref.setPublicKeyID(publicKeyId);
            encryptionPref.setPrivateKey(privateKey);
            encryptionPref.setPrivateKeyPassword(keyPass);
            encryptionPref.setEncryptionAlgorithm(EncryptionAlgorithm.RSA_SHA_256);
            IAccessTokenCache accessTokenCache = new InMemoryLRUAccessTokenCache(maxCacheEntries);

            // Instantiate Box Connection
            BoxDeveloperEditionAPIConnection api = BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(
                    enterpriseId, clientId, clientSecret, encryptionPref, accessTokenCache);

            if(api != null){
                result.sampleEnd();
                result.setSuccessful(true);
                result.setResponseMessage("Successfully created Box platform connection with state: " + api.save());
                result.setResponseCodeOK();
            } else{
                result.setSuccessful(false);
                result.setResponseMessage("Failed to get a valid Box connection state.");

                StringWriter stringWriter = new StringWriter();
                stringWriter.append("Failed to get valid Box connection state");
                result.setResponseData(stringWriter.toString().getBytes());
                result.setDataType(SampleResult.TEXT);
                result.setResponseCode("500");
            }
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to create Box platform connection: " + e);

            StringWriter stringWriter = new StringWriter();
            e.printStackTrace( new PrintWriter(stringWriter));
            result.setResponseData(stringWriter.toString().getBytes());
            result.setDataType(SampleResult.TEXT);
            result.setResponseCode("500");
        }
        return result;
    }

    /**
     * Tear down the sampler client
     */
    public void teardownTest(JavaSamplerContext teardownContext) {
    }
}
