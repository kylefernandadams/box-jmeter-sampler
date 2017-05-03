package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
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
 *  JMeter sampler client which searches for Box files based on file metadata.
 */
public class SearchByMetadata implements JavaSamplerClient {
    private BoxDeveloperEditionAPIConnection api = null;

    private static final String CLIENT_ID = "client.id";
    private static final String CLIENT_SECRET = "client.secret";
    private static final String ENTERPRISE_ID = "enterprise.id";
    private static final String PUBLIC_KEY_ID = "public.key.id";
    private static final String PRIVATE_KEY_PATH = "private.key.path";
    private static final String KEY_PASS = "key.pass";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String SEARCH_RESULT_LIMIT = "search.result.limit";
    private static final String SEARCH_RESULT_LOOP = "search.result.loop";
    private static final String METADATA_TEMPLATE_KEY = "metadata.template.key";
    private static final String METADATA_SCOPE = "metadata.scope";
    private static final String METADATA_FILTER_KEY = "metadata.filter.key";
    private static final String METADATA_FILTER_VALUE = "metadata.filter.value";

    private BoxMetadataFilter boxMetadataFilter = new BoxMetadataFilter();
    private int searchResultLimit = -1;
    private boolean loopThroughSearchResults = false;

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
        defaultParameters.addArgument(SEARCH_RESULT_LIMIT, "${" + SEARCH_RESULT_LIMIT + "}");
        defaultParameters.addArgument(SEARCH_RESULT_LOOP, "${" + SEARCH_RESULT_LOOP + "}");
        defaultParameters.addArgument(METADATA_TEMPLATE_KEY, "${" + METADATA_TEMPLATE_KEY + "}");
        defaultParameters.addArgument(METADATA_SCOPE, "${" + METADATA_SCOPE + "}");
        defaultParameters.addArgument(METADATA_FILTER_KEY, "${" + METADATA_FILTER_KEY + "}");
        defaultParameters.addArgument(METADATA_FILTER_VALUE, "${" + METADATA_FILTER_VALUE + "}");

        return defaultParameters;
    }

    /**
     * Setup the sampler client.
     *  - Get a Box connection
     *  - Get the search result limit
     *  - Get the boolean value to determine if we're going to include looping through the search results
     *  - Get metadata template key, scope, filter key, and filter value
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

            searchResultLimit = setupContext.getIntParameter(SEARCH_RESULT_LIMIT);
            loopThroughSearchResults = Boolean.valueOf(setupContext.getParameter(SEARCH_RESULT_LOOP));

            String metadataTemplateKey = setupContext.getParameter(METADATA_TEMPLATE_KEY);
            String metadataScope = setupContext.getParameter(METADATA_SCOPE);
            String metadataFilterKey = setupContext.getParameter(METADATA_FILTER_KEY);
            String metadataFilterValue = setupContext.getParameter(METADATA_FILTER_VALUE);
            boxMetadataFilter.setScope(metadataScope);
            boxMetadataFilter.setTemplateKey(metadataTemplateKey);
            boxMetadataFilter.addFilter(metadataFilterKey, metadataFilterValue);

            BoxConnectionUtil util = new BoxConnectionUtil(publicKeyId, privateKey, keyPass, enterpriseId, clientId, clientSecret, maxCacheEntries);
            this.api = util.getAppUserConnection(userLogin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the sampler client.
     * - Set the BoxSearchParameters
     * - Execute the search
     * - Optionally loop through the results
     */
    @Override
    public SampleResult runTest(JavaSamplerContext runContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try{

            BoxSearch boxSearch = new BoxSearch(api);
            BoxSearchParameters boxSearchParameters = new BoxSearchParameters();
            boxSearchParameters.setMetadataFilter(boxMetadataFilter);

            long offset = 0;
            long fullSizeOfResult = 0;

            PartialCollection<BoxItem.Info> searchResults;

            if(loopThroughSearchResults){
                while (offset <= fullSizeOfResult) {
                    searchResults = boxSearch.searchRange(offset, searchResultLimit, boxSearchParameters);
                    fullSizeOfResult = searchResults.fullSize();

                    for(BoxItem.Info itemInfo: searchResults){
                        System.out.println("Found item: " + itemInfo.getName() + " with id: " + itemInfo.getID());
                    }
                    offset += searchResultLimit;
                }
            } else{
                searchResults = boxSearch.searchRange(offset, searchResultLimit, boxSearchParameters);
                System.out.println("Found search results: " + searchResults.fullSize());
            }
            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseMessage("Successfully executed Box Search.");
            result.setResponseCodeOK();
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to execute Box Search: " + e);

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