package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
import com.box.sdk.*;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *  JMeter sampler client which creates metadata on Box files.
 */
public class CreateMetadataOnFile implements JavaSamplerClient {
    private BoxDeveloperEditionAPIConnection api = null;

    private static final String CLIENT_ID = "client.id";
    private static final String CLIENT_SECRET = "client.secret";
    private static final String ENTERPRISE_ID = "enterprise.id";
    private static final String PUBLIC_KEY_ID = "public.key.id";
    private static final String PRIVATE_KEY_PATH = "private.key.path";
    private static final String KEY_PASS = "key.pass";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String CURRENT_FILE_ID = "current.file.id";
    private static final String CURRENT_FILE_COUNT = "current.file.count";
    private static final String METADATA_TEMPLATE_KEY = "metadata.template.key";
    private static final String METADATA_KEYS = "metadata.keys";
    private static final String METADATA_VALUES = "metadata.values";

    private String enterpriseId = null;
    private String metadataTemplateKey = null;
    private Metadata metadata = null;

    /**
     * Get default parameters for the sampler client
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(CLIENT_ID, "${"+ CLIENT_ID + "}");
        defaultParameters.addArgument(CLIENT_SECRET, "${" + CLIENT_SECRET + "}");
        defaultParameters.addArgument(ENTERPRISE_ID, "${" + ENTERPRISE_ID + "}");
        defaultParameters.addArgument(PUBLIC_KEY_ID, "${" + PUBLIC_KEY_ID + "}");
        defaultParameters.addArgument(PRIVATE_KEY_PATH, "${" + PRIVATE_KEY_PATH + "}");
        defaultParameters.addArgument(KEY_PASS, "${" + KEY_PASS + "}");
        defaultParameters.addArgument(MAX_CACHE_ENTRIES, "${" + MAX_CACHE_ENTRIES + "}");
        defaultParameters.addArgument(USER_LOGIN, "${" + USER_LOGIN + "}");
        defaultParameters.addArgument(CURRENT_FILE_COUNT, "${" + CURRENT_FILE_COUNT + "}");
        defaultParameters.addArgument(METADATA_TEMPLATE_KEY, "${" + METADATA_TEMPLATE_KEY + "}");
        defaultParameters.addArgument(METADATA_KEYS, "${" + METADATA_KEYS + "}");
        defaultParameters.addArgument(METADATA_VALUES, "${" + METADATA_VALUES + "}");
        return defaultParameters;
    }

    /**
     * Setup the sampler client.
     *  - Get a Box connection
     *  - Get metadata keys and split into a String array
     *  - Get metadata values and split into a String array
     *  - Get the metadata template based on the provided key
     */
    @Override
    public void setupTest(JavaSamplerContext setupContext) {
        try {
            String clientId = setupContext.getParameter(CLIENT_ID);
            String clientSecret = setupContext.getParameter(CLIENT_SECRET);
            enterpriseId = setupContext.getParameter(ENTERPRISE_ID);
            String publicKeyId = setupContext.getParameter(PUBLIC_KEY_ID);
            String privateKeyPath = setupContext.getParameter(PRIVATE_KEY_PATH);
            String keyPass = setupContext.getParameter(KEY_PASS);
            String privateKey = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            int maxCacheEntries = setupContext.getIntParameter(MAX_CACHE_ENTRIES);
            String userLogin = setupContext.getParameter(USER_LOGIN);
            metadataTemplateKey = setupContext.getParameter(METADATA_TEMPLATE_KEY);
            String[] metadataKeysArray = setupContext.getParameter(METADATA_KEYS).split(",");
            String[] metadataValuesArray = setupContext.getParameter(METADATA_VALUES).split(",");

            BoxConnectionUtil util = new BoxConnectionUtil(publicKeyId, privateKey, keyPass, enterpriseId, clientId, clientSecret, maxCacheEntries);
            this.api = util.getAppUserConnection(userLogin);
            MetadataTemplate metadataTemplate = null;
            try{
                metadataTemplate = MetadataTemplate.getMetadataTemplate(api, metadataTemplateKey);
            }
            catch (BoxAPIException bae){
                System.out.println("Failed to get metadata template with key: " + metadataTemplateKey + " with exception: {}" + bae);
            }

            if(metadataTemplate != null){
                metadata = new Metadata();
                for (int i=0; i < metadataKeysArray.length; i++){
                    String metadataKey = metadataKeysArray[i];
                    String metadataValue = metadataValuesArray[i];
                    metadata.add("/" + metadataKey, metadataValue);

                    // This is extra logic to handle number and date metadata types.
                    // The Box Java SDK will be enhanced to include additional overloaded methods for Metadata.add(..)
                    // to support adding number and date metadata values.
//                    logger.info("Found metadata key: {} with value: {}", metadataKey, metadataValue);
//                    for(MetadataTemplate.Field field: metadataTemplate.getFields()){
//                        logger.info("Metadata template loop with field key: {}", field.getKey());
//                        if(metadataKey.equalsIgnoreCase(field.getKey())){
//                            logger.info("Found metadata field key: {} and type: {}", field.getKey(), field.getType());
//
//                            LocalDateTime localDateTime = LocalDateTime.now();
//                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
//                            if(field.getType().equalsIgnoreCase("date") && useCurrentDateTime){
//
//                                testDateString = localDateTime.format(dateTimeFormatter);
//                                metadata.add("/" + metadataKey, testDateString);
//                            } else {
//                                metadata.add("/" + metadataKey, metadataValue);
//                            }
//                        }
//                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the sampler client.
     * - Get the current file id from the thread group
     * - Get the BoxFile based on the current file id
     * - Create metadata on the file
     */
    @Override
    public SampleResult runTest(JavaSamplerContext runContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try{
            if(metadata != null){
                JMeterContext jMeterContext = JMeterContextService.getContext();
                String currentFileId = jMeterContext.getThreadGroup().getPropertyAsString(CURRENT_FILE_ID);
                BoxFile currentFile = new BoxFile(api, currentFileId);
                Metadata fileMetadata = currentFile.createMetadata(metadataTemplateKey, Metadata.ENTERPRISE_METADATA_SCOPE, metadata);

                result.sampleEnd();
                result.setSuccessful(true);
                result.setResponseMessage("Successfully created metadata on Box file: " + fileMetadata.toString());
                result.setResponseCodeOK();

            } else{
                result.sampleEnd();
                result.setSuccessful(false);
                result.setResponseMessage("Unable to create metadata key values for Box file.");
                result.setResponseCode("500");
            }
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to create metadata on Box files: " + e);

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
    @Override
    public void teardownTest(JavaSamplerContext teardownContext) {

    }
}
