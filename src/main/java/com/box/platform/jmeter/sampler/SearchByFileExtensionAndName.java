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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  JMeter sampler client which searches for Box files based on file extension and name.
 */
public class SearchByFileExtensionAndName implements JavaSamplerClient {
    private BoxDeveloperEditionAPIConnection api = null;

    private static final String CLIENT_ID = "client.id";
    private static final String CLIENT_SECRET = "client.secret";
    private static final String ENTERPRISE_ID = "enterprise.id";
    private static final String PUBLIC_KEY_ID = "public.key.id";
    private static final String PRIVATE_KEY_PATH = "private.key.path";
    private static final String KEY_PASS = "key.pass";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String SEARCH_FILE_EXTENSIONS = "search.file.extensions";
    private static final String INCLUDE_ANCESTOR_IDS = "inclue.ancestor.ids";
    private static final String SEARCH_ANCESTOR_IDS = "search.ancestor.ids";
    private static final String SEARCH_TERM = "search.term";
    private static final String SEARCH_RESULT_LIMIT = "search.result.limit";
    private static final String SEARCH_RESULT_LOOP = "search.result.loop";

    private List<String> fileExtensionsList = new ArrayList<String>();
    private boolean includeAncestorIds = false;
    private List<String> ancestorIdList = new ArrayList<String>();
    private String searchTerm = null;
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
        defaultParameters.addArgument(SEARCH_FILE_EXTENSIONS, "${" + SEARCH_FILE_EXTENSIONS + "}");
        defaultParameters.addArgument(INCLUDE_ANCESTOR_IDS, "${" + INCLUDE_ANCESTOR_IDS + "}");
        defaultParameters.addArgument(SEARCH_ANCESTOR_IDS, "${" + SEARCH_ANCESTOR_IDS + "}");
        defaultParameters.addArgument(SEARCH_TERM, "${" + SEARCH_TERM + "}");
        defaultParameters.addArgument(SEARCH_RESULT_LIMIT, "${" + SEARCH_RESULT_LIMIT + "}");
        defaultParameters.addArgument(SEARCH_RESULT_LOOP, "${" + SEARCH_RESULT_LOOP + "}");

        return defaultParameters;
    }

    /**
     * Setup the sampler client.
     *  - Get a Box connection
     *  - Get the file extension list
     *  - Get the ancestor folder id list
     *  - Get the search term
     *  - Get the search result limit
     *  - Get the boolean value to determine if we're going to include looping through the search results
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

            String fileExtensionsString = setupContext.getParameter(SEARCH_FILE_EXTENSIONS);
            fileExtensionsList = Arrays.asList(fileExtensionsString.split(","));

            includeAncestorIds = Boolean.valueOf(setupContext.getParameter(INCLUDE_ANCESTOR_IDS));
            String ancestorIdsString = setupContext.getParameter(SEARCH_ANCESTOR_IDS);
            ancestorIdList = Arrays.asList(ancestorIdsString.split(","));

            searchTerm = setupContext.getParameter(SEARCH_TERM);
            searchResultLimit = setupContext.getIntParameter(SEARCH_RESULT_LIMIT);
            loopThroughSearchResults = Boolean.valueOf(setupContext.getParameter(SEARCH_RESULT_LOOP));

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
            boxSearchParameters.setFileExtensions(fileExtensionsList);
            if(includeAncestorIds){
                boxSearchParameters.setAncestorFolderIds(ancestorIdList);
            }
            boxSearchParameters.setQuery(searchTerm);
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
