package uk.gov.moj.cpp.applications.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.applications.util.FileUtil.getPayload;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.common.http.HeaderConstants;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Class to set up stub.
 */
public class StubUtil {

    protected static final String DEFAULT_JSON_CONTENT_TYPE = "application/json";

    private static final int HTTP_STATUS_OK = 200;
    private static final String MATERIAL_QUERY_URL = "/material-service/query/api/rest/material";

    public static void setupUsersGroupQueryStub() {
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user.json"))));

    }

    public static void setupMaterialStub(String materialId) {
        InternalEndpointMockUtils.stubPingFor("material-service");
        stubFor(get(urlMatching("/material-service/query/api/rest/material/material/" + materialId + "/metadata"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/material.query.material-metadata.json").replace("MATERIAL_ID", materialId))));
    }

    public static void stubMaterialContent(final UUID materialId, final byte[] materialContent, final String mimeType) {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(get(urlPathEqualTo(MATERIAL_QUERY_URL + "/material/" + materialId))
                .withQueryParam("stream", equalTo("true"))
                .withQueryParam("requestPdf", equalTo("false"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, mimeType)
                        .withBody(materialContent)));
    }


    public static String getJsonBodyStr(final String path, final String caseId,
                                        final String defendantId, final String defendant2Id) {
        final String payload = getPayload(path);
        return payload.replace("RANDOM_CASE_ID", caseId)
                .replace("DEF_ID_1", defendantId)
                .replace("DEF_ID_2", defendant2Id)
                .replace("DEF_PRG_ID", defendantId)
                .replace("TODAY", LocalDate.now().toString());
    }
}
