package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxFolder;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *  JMeter sampler client which creates Box folders.
 */
public class CreateFolder implements JavaSamplerClient {
    private BoxDeveloperEditionAPIConnection api = null;

    private static final String CLIENT_ID = "client.id";
    private static final String CLIENT_SECRET = "client.secret";
    private static final String ENTERPRISE_ID = "enterprise.id";
    private static final String PUBLIC_KEY_ID = "public.key.id";
    private static final String PRIVATE_KEY_PATH = "private.key.path";
    private static final String KEY_PASS = "key.pass";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String CURRENT_FOLDER_COUNT = "current.folder.count";
    private static final String CURRENT_FOLDER_ID = "current.folder.id";

    private static final String BASE_JMETER_BOX_FOLDER = "JMeterTest";
    private static final String TEST_FOLDER_NAME = ". TestFolder";
    private static final String DATE_TIME_FORMAT = "yyyyMMdd-HHmmss";
    private String baseFolderId = null;

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
        defaultParameters.addArgument(CURRENT_FOLDER_COUNT,"${" + CURRENT_FOLDER_COUNT + "}");

        return defaultParameters;
    }

    /**
     * Setup the sampler client.
     *  - Get a Box connection
     *  - Create the base JMeterTest folder and append the current date/time
     *  - Set the base JMeter folder id
     */
    @Override
    public void setupTest(JavaSamplerContext setupContext) {
        try {
            String clientId = setupContext.getParameter(CLIENT_ID);
            String clientSecret = setupContext.getParameter(CLIENT_SECRET);
            String enterpriseId = setupContext.getParameter(ENTERPRISE_ID);
            String publicKeyId = setupContext.getParameter(PUBLIC_KEY_ID);
            String privateKeyPath = setupContext.getParameter(PRIVATE_KEY_PATH);
            String keyPass = setupContext.getParameter(KEY_PASS);
            String privateKey = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            int maxCacheEntries = setupContext.getIntParameter(MAX_CACHE_ENTRIES);
            String userLogin = setupContext.getParameter(USER_LOGIN);

            BoxConnectionUtil util = new BoxConnectionUtil(publicKeyId, privateKey, keyPass, enterpriseId, clientId, clientSecret, maxCacheEntries);
            this.api = util.getAppUserConnection(userLogin);
            BoxFolder rootFolder = BoxFolder.getRootFolder(this.api);

            LocalDateTime localDateTime = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
            String now = localDateTime.format(dateTimeFormatter);

            BoxFolder.Info rootFolderInfo = rootFolder.createFolder(BASE_JMETER_BOX_FOLDER + "-" + now);
            baseFolderId = rootFolderInfo.getID();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the sampler client.
     * - Create the test folder
     * - Set add the newly created folder id to a thread group property
     */
    @Override
    public SampleResult runTest(JavaSamplerContext runContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try{
            BoxFolder baseFolder = new BoxFolder(api, baseFolderId);

            String currentFolderCount = runContext.getParameter(CURRENT_FOLDER_COUNT);
            BoxFolder.Info folderInfo = baseFolder.createFolder(currentFolderCount + TEST_FOLDER_NAME);
            String currentFolderId = folderInfo.getID();

            JMeterContext jMeterContext = JMeterContextService.getContext();
            jMeterContext.getThreadGroup().setProperty(CURRENT_FOLDER_ID, currentFolderId);

            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseMessage("Successfully created Box folders.");
            result.setResponseCodeOK();
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to create Box folders: " + e);

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
