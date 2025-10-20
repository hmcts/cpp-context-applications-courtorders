package uk.gov.moj.cpp.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.applications.util.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.applications.util.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.applications.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.applications.util.RestHelper.postCommand;
import static uk.gov.moj.cpp.applications.util.WireMockStubUtils.setupResultDefinition;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CourtOrderIT extends AbstractIT {

    private String courtOrderId;
    private String defendantId;
    private String hearingId;
    private String caseId;
    private String applicationId;
    private LocalDate sittingDate = LocalDate.parse("2020-04-12");
    private LocalDate endDate = LocalDate.now().plusMonths(6);

    @BeforeEach
    public void onceBeforeEachTest() {
        courtOrderId = randomUUID().toString();
        defendantId = randomUUID().toString();
        hearingId = randomUUID().toString();
        caseId = randomUUID().toString();
        applicationId = randomUUID().toString();
        setupResultDefinition("f1fa0821-29be-4ec5-ad56-13eafe93021d");

    }

    @Test
    public void shouldCreateCourtOrder() throws IOException {
        courtOrderId = randomUUID().toString();
        final javax.ws.rs.core.Response writeResponse = submitCourtOrder();
        assertThat(writeResponse.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        assertCourtOrderForDefendant(defendantId, allOf(
                withJsonPath("$.courtOrders[0].id", equalTo(courtOrderId)),
                withJsonPath("$.courtOrders[0].courtOrderOffences[0].offence.offenceCode", equalTo("TTH105HY"))));

        final LocalDate newSittingDate = sittingDate.plusDays(1);
        final javax.ws.rs.core.Response courtOrderResponse = updateCourtOrderForDifferentSittingDate(newSittingDate);
        assertThat(courtOrderResponse.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));

        assertCourtOrderForHearingAndDefendant(defendantId, hearingId, newSittingDate, allOf(
                withJsonPath("$.courtOrders[0].id", equalTo(courtOrderId))
        ));

        assertCourtOrderForCaseId(caseId, defendantId, allOf(
                withJsonPath("$.courtOrders[0].id", equalTo(courtOrderId))
        ));
        final LocalDate newEndDate = sittingDate.plusDays(10);
        final javax.ws.rs.core.Response updatedCourtOrderResponse = updateCourtOrderWithNewEndDate(newEndDate);
        assertThat(updatedCourtOrderResponse.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));

        assertCourtOrderForCaseId(caseId, defendantId, allOf(
                withJsonPath("$.courtOrders[0].id", equalTo(courtOrderId)),
                withJsonPath("$.courtOrders[0].endDate", notNullValue()),
                withJsonPath("$.courtOrders[0].endDate", is(newEndDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
        ));
    }

    @Test
    public void shouldRemoveCourtOrder() throws IOException {
        courtOrderId = randomUUID().toString();
        final javax.ws.rs.core.Response writeResponse = submitCourtOrder();
        assertThat(writeResponse.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        assertCourtOrderForDefendant(defendantId, allOf(
                withJsonPath("$.courtOrders[0].id", equalTo(courtOrderId)),
                withJsonPath("$.courtOrders[0].courtOrderOffences[0].offence.offenceCode", equalTo("TTH105HY"))));

        final javax.ws.rs.core.Response writeResponseRemoved = removeCourtOrder();
        assertThat(writeResponseRemoved.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));

        assertCourtOrderForCourtOrderId(caseId, defendantId, allOf(
                withJsonPath("$.courtOrders.length()", equalTo(0))
        ));

    }

    private javax.ws.rs.core.Response removeCourtOrder() throws IOException {
        String removeCourtOrderPayload = getPayload("progression.remove-court-order.json")
                .replace("%COURT_ORDER_ID%", courtOrderId);
        final javax.ws.rs.core.Response writeResponse = postCommand(getWriteUrl(join("", "/court-order/", courtOrderId)),
                "application/vnd.courtorders.remove-court-order+json",
                removeCourtOrderPayload);
        return writeResponse;
    }

    private javax.ws.rs.core.Response submitCourtOrder() throws IOException {
        String createCourtOrderPayload = getPayload("progression.create-court-order.json")
                .replace("%COURT_ORDER_ID%", courtOrderId)
                .replace("%HEARING_ID%", hearingId)
                .replace("%ORDER_DATE%", sittingDate.toString())
                .replace("%CASE_ID%", caseId)
                .replace("%END_DATE%", endDate.toString())
                .replace("%DEFENDANT_ID%", defendantId);
        final javax.ws.rs.core.Response writeResponse = postCommand(getWriteUrl("/court-order"),
                "application/vnd.courtorders.create-court-order+json",
                createCourtOrderPayload);
        return writeResponse;
    }

    private javax.ws.rs.core.Response updateCourtOrderForDifferentSittingDate(LocalDate newSittingDate) throws IOException {
        String createCourtOrderPayload;
        createCourtOrderPayload = getPayload("progression.create-court-order1.json")
                .replace("%COURT_ORDER_ID%", courtOrderId)
                .replace("%HEARING_ID%", hearingId)
                .replace("%ORDER_DATE%", newSittingDate.toString())
                .replace("%CASE_ID%", caseId)
                .replace("%DEFENDANT_ID%", defendantId);
        final javax.ws.rs.core.Response courtOrderResponse = postCommand(getWriteUrl("/court-order"),
                "application/vnd.courtorders.create-court-order+json",
                createCourtOrderPayload);
        return courtOrderResponse;
    }

    private static String assertCourtOrderForDefendant(final String defendantId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(join("", "/court-order/defendant/", defendantId)),
                "application/vnd.courtorders.query.court-order-by-defendant-id+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    private static String assertCourtOrderForHearingAndDefendant(final String defendantId, final String hearingId, final LocalDate sittingDate, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(format("/court-order/hearing/%s/defendant/%s?sittingDate=%s", hearingId, defendantId, sittingDate)),
                "application/vnd.courtorders.query.court-order-by-hearing-and-defendant-id+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    private static String assertCourtOrderForCaseId(final String caseId, final String defendantId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(format("/court-order/caseId/%s/defendant/%s", caseId, defendantId)),
                "application/vnd.courtorders.query.court-order-by-case-and-defendant-id+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    private static String assertCourtOrderForCourtOrderId(final String caseId, final String defendantId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(format("/court-order/caseId/%s/defendant/%s", caseId, defendantId)),
                "application/vnd.courtorders.query.court-order-by-case-and-defendant-id+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    private javax.ws.rs.core.Response updateCourtOrderWithNewEndDate(LocalDate newEndDate) throws IOException {
        String updateCourtOrderPayload;
        updateCourtOrderPayload = getPayload("progression.update-court-order.json")
                .replace("%COURT_ORDER_ID%", courtOrderId)
                .replace("%APPLICATION_ID%", applicationId)
                .replace("%END_DATE%", newEndDate.toString());
        final javax.ws.rs.core.Response writeResponse = postCommand(getWriteUrl(join("", "/court-order")),
                "application/vnd.courtorders.update-court-order-validity+json",
                updateCourtOrderPayload);
        return writeResponse;
    }
}
