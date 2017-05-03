package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *  JMeter sampler client which uploads files to Box.
 */
public class UploadFile implements JavaSamplerClient {
    private BoxDeveloperEditionAPIConnection api = null;

    private static final String CLIENT_ID = "client.id";
    private static final String CLIENT_SECRET = "client.secret";
    private static final String ENTERPRISE_ID = "enterprise.id";
    private static final String PUBLIC_KEY_ID = "public.key.id";
    private static final String PRIVATE_KEY_PATH = "private.key.path";
    private static final String KEY_PASS = "key.pass";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String CURRENT_FILE_COUNT = "current.file.count";
    private static final String CURRENT_FILE_ID = "current.file.id";
    private static final String CURRENT_FOLDER_ID = "current.folder.id";
    private static final String SAMPLE_FILE_PATH = "sample.file.path";
    private static final String TEST_FILE_NAME = ". TestFile.pdf";

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
        defaultParameters.addArgument(CURRENT_FILE_COUNT,"${" + CURRENT_FILE_COUNT + "}");
        defaultParameters.addArgument(SAMPLE_FILE_PATH, "${" + SAMPLE_FILE_PATH + "}");

        return defaultParameters;
    }

    /**
     * Setup the sampler client.
     *  - Get a Box connection
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the sampler client.
     * - Get the current folder id from thread group
     * - Get the current folder based on the id
     * - Get the current file count
     * - Get the sample file path
     * - Create FileInputStream based on the file path and upload the file to Box
     * - Set a thread group parameter for the newly created file id
     */
    @Override
    public SampleResult runTest(JavaSamplerContext runContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try{
            JMeterContext jMeterContext = JMeterContextService.getContext();
            String currentFolderId = jMeterContext.getThreadGroup().getPropertyAsString(CURRENT_FOLDER_ID);

            BoxFolder folder = new BoxFolder(api, currentFolderId);
            String currentFileCount = runContext.getParameter(CURRENT_FILE_COUNT);
            String sampleFilePath = runContext.getParameter(SAMPLE_FILE_PATH);
            FileInputStream stream = new FileInputStream(sampleFilePath);
            BoxFile.Info fileInfo = folder.uploadFile(stream, currentFileCount + TEST_FILE_NAME);
            stream.close();

            jMeterContext.getThreadGroup().setProperty(CURRENT_FILE_ID, fileInfo.getID());

            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseMessage("Successfully created Box file with id: " + fileInfo.getID());
            result.setResponseCodeOK();
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to create Box files: " + e);

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
