package uk.gov.moj.cpp.applications.helper;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.applications.util.AbstractTestHelper.getReadUrl;

import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;

public class CourtOrderVerificationHelper {

    private static final int TIMEOUT_IN_SECONDS = 60;

    public static String assertCourtOrderForCaseId(final String caseId, final String defendantId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(format("/court-order/caseId/%s/defendant/%s", caseId, defendantId)),
                "application/vnd.courtorders.query.court-order-by-case-and-defendant-id+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }
}
