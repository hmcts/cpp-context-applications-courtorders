package uk.gov.moj.cpp.applications;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.applications.util.RestHelper.HOST;
import static uk.gov.moj.cpp.applications.util.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.applications.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.applications.util.WireMockStubUtils.setupAsSystemUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import java.io.IOException;
import java.util.UUID;

public class AbstractIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIT.class);

    protected static final UUID USER_ID_VALUE = randomUUID();
    protected static final UUID USER_ID_VALUE_AS_ADMIN = randomUUID();
    protected static ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil = null;
    protected static ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;


    /**
     * NOTE: this approach is employed to enabled massive savings in test execution test.
     * All tests will need to extend AbstractIT thus ensuring the static initialisation block is fired just once before any test runs
     * Mock reset and stub for all reference data happens once per VM.  If parallel test run is considered, this approach will be tweaked.
     */

    static {
        try{
            configureFor(HOST, 8080);
            reset(); // will need to be removed when things are being run in parallel
            defaultStubs();
            setUpElasticSearch();
        } catch (Throwable e) {
            LOGGER.error("Error occurred while setting up test: ", e);
        }
    }


    private static void setUpElasticSearch() {
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);
        elasticSearchIndexRemoverUtil  = new ElasticSearchIndexRemoverUtil();
        deleteAndCreateIndex();
    }

    protected static void deleteAndCreateIndex() {
        try {
            elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();
        }catch (final IOException e){
            LOGGER.error("Error while creating index ", e);
        }
    }


    private static void defaultStubs() {
        setupAsAuthorisedUser(USER_ID_VALUE);
        setupAsSystemUser(USER_ID_VALUE_AS_ADMIN);
        setupUsersGroupQueryStub();
    }

}
