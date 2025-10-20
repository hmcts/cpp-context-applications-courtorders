package uk.gov.moj.cpp.applications.ingester;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.applications.helper.CourtOrderVerificationHelper.assertCourtOrderForCaseId;
import static uk.gov.moj.cpp.applications.util.RestHelper.postCommand;
import static uk.gov.moj.cpp.applications.util.WireMockStubUtils.setupResultDefinition;
import static uk.gov.moj.cpp.unifiedsearch.test.util.ingest.UnifiedSearchIndexSearchHelper.findBy;

import uk.gov.moj.cpp.applications.AbstractIT;
import uk.gov.moj.cpp.applications.util.AbstractTestHelper;
import uk.gov.moj.cpp.applications.util.FileUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtOrderRequestedIngesterIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtOrderRequestedIngesterIT.class);

    private static final String COMMAND_CREATE_COURT_ORDER_JSON = "ingestion/applicationscourtorders.command.create-court-order.json";

    private static final String COMMAND_CREATE_COURT_ORDER_FOR_TWO_OFFENCES_DEFENDANTS_JSON = "ingestion/applicationscourtorders.command.create-court-order-for-two-offences-defendants.json";

    @BeforeEach
    public void setUp() {
        deleteAndCreateIndex();
        setupResultDefinition("f1fa0821-29be-4ec5-ad56-13eafe93021d");
    }

    @Test
    public void shouldIngestCourtOrderRequested() throws IOException {
        final String courtOrderId = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();
        final String prosecutionCaseId = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String offenceId = randomUUID().toString();
        final String courtOrderStartDate = LocalDate.of(2021, 1, 10).toString();
        final String courtOrderEndDate = LocalDate.now().plusMonths(6).toString();
        final String offenceCode = "TTH105HY";

        createCourtOrder(COMMAND_CREATE_COURT_ORDER_JSON,
                courtOrderId, prosecutionCaseId, masterDefendantId,
                defendantId1, defendantId2, offenceId, offenceCode, courtOrderStartDate, courtOrderEndDate);

        verifyCourtOrderCreated(prosecutionCaseId, masterDefendantId, courtOrderId, offenceCode);

        final Matcher[] matchers = {withJsonPath("$.caseId", equalTo(prosecutionCaseId)),
                withJsonPath("$.parties[0].offences[0].courtOrders[0].id", isOneOf(courtOrderId))};

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = findBy(matchers);

        assertTrue(prosecutionCaseResponseJsonObject.isPresent());

        final JsonObject indexedCaseDocument = prosecutionCaseResponseJsonObject.get();

        with(indexedCaseDocument.toString())
                .assertThat("caseId", equalTo(prosecutionCaseId))
                .assertThat("parties[0].partyId", equalTo(defendantId1))
                .assertThat("parties[1].partyId", equalTo(defendantId2))
                .assertThat("parties[0].offences[0].offenceCode", equalTo(offenceCode))
                .assertThat("parties[0].offences[0].courtOrders[0].id", equalTo(courtOrderId))
                .assertThat("parties[0].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat("parties[1].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[1].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat("_case_type", equalTo("PROSECUTION"));
    }

    @Test
    public void shouldIngestMultipleCourtOrderAgainstSameOffence() throws IOException {
        final String courtOrderId1 = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();
        final String prosecutionCaseId = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String offenceId = randomUUID().toString();
        final String courtOrderStartDate = LocalDate.of(2021, 1, 10).toString();
        final String courtOrderEndDate = LocalDate.of(2021, 2, 10).toString();
        final String offenceCode = "TTH105HY";

        createCourtOrder(COMMAND_CREATE_COURT_ORDER_JSON,
                courtOrderId1, prosecutionCaseId, masterDefendantId,
                defendantId1, defendantId2, offenceId, offenceCode, courtOrderStartDate, courtOrderEndDate);

        final String courtOrderId2 = randomUUID().toString();

        createCourtOrder(COMMAND_CREATE_COURT_ORDER_JSON,
                courtOrderId2, prosecutionCaseId, masterDefendantId,
                defendantId1, defendantId2, offenceId, offenceCode, courtOrderStartDate, courtOrderEndDate);

        assertCourtOrderForCaseId(prosecutionCaseId, masterDefendantId,
                allOf(withJsonPath("$.courtOrders[0].id", anyOf(equalTo(courtOrderId1), equalTo(courtOrderId2))),
                        withJsonPath("$.courtOrders[1].id", anyOf(equalTo(courtOrderId1), equalTo(courtOrderId2))),
                        withJsonPath("$.courtOrders[0].courtOrderOffences[0].offence.id", equalTo(offenceId)),
                        withJsonPath("$.courtOrders[1].courtOrderOffences[0].offence.id", equalTo(offenceId))
                ));

        final Matcher[] matchers = {withJsonPath("$.caseId", equalTo(prosecutionCaseId)),
                withJsonPath("$.parties[0].offences[0].courtOrders[0].id", isOneOf(courtOrderId1, courtOrderId2)),
                withJsonPath("$.parties[0].offences[0].courtOrders[1].id", isOneOf(courtOrderId1, courtOrderId2))};

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = findBy(matchers);

        assertTrue(prosecutionCaseResponseJsonObject.isPresent());

        final JsonObject indexedCaseDocument = prosecutionCaseResponseJsonObject.get();

        with(indexedCaseDocument.toString())
                .assertThat("caseId", equalTo(prosecutionCaseId))
                .assertThat("parties[0].partyId", equalTo(defendantId1))
                .assertThat("parties[1].partyId", equalTo(defendantId2))
                .assertThat("parties[0].offences[0].offenceCode", equalTo(offenceCode))
                .assertThat("parties[0].offences[0].courtOrders[0].id", isOneOf(courtOrderId1, courtOrderId2))
                .assertThat("parties[0].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat("parties[0].offences[0].courtOrders[1].id", isOneOf(courtOrderId1, courtOrderId2))
                .assertThat("parties[0].offences[0].courtOrders[1].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[0].offences[0].courtOrders[1].endDate", equalTo(courtOrderEndDate))
                .assertThat("parties[1].offences[0].courtOrders[0].id", isOneOf(courtOrderId1, courtOrderId2))
                .assertThat("parties[1].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[1].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat("parties[1].offences[0].courtOrders[1].id", isOneOf(courtOrderId1, courtOrderId2))
                .assertThat("parties[1].offences[0].courtOrders[1].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[1].offences[0].courtOrders[1].endDate", equalTo(courtOrderEndDate))
                .assertThat("_case_type", equalTo("PROSECUTION"));
    }

    @Test
    public void shouldIngestMultipleCourtOrderEachGoesDifferentOffencesAndDefendants() throws IOException {
        final String courtOrderId1 = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();
        final String prosecutionCaseId = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();
        final String offenceId1 = randomUUID().toString();
        final String offenceCode1 = "TTH105HY";
        final String courtOrderStartDate = LocalDate.of(2021, 1, 10).toString();
        final String courtOrderEndDate = LocalDate.of(2021, 2, 10).toString();

        createCourtOrder(COMMAND_CREATE_COURT_ORDER_JSON,
                courtOrderId1, prosecutionCaseId, masterDefendantId,
                defendantId1, defendantId2, offenceId1, offenceCode1, courtOrderStartDate, courtOrderEndDate);

        assertCourtOrderForCaseId(prosecutionCaseId, masterDefendantId,
                allOf(withJsonPath("$.courtOrders[0].id", equalTo(courtOrderId1)),
                        withJsonPath("$.courtOrders[0].courtOrderOffences[0].offence.id", equalTo(offenceId1))
                ));

        final String offenceId2 = randomUUID().toString();
        final String offenceCode2 = "OF61131";
        final String courtOrderId2 = randomUUID().toString();

        createCourtOrderForMultipleOffences(COMMAND_CREATE_COURT_ORDER_FOR_TWO_OFFENCES_DEFENDANTS_JSON,
                courtOrderId2, prosecutionCaseId, masterDefendantId,
                defendantId2, defendantId3, offenceId1, offenceId2,
                offenceCode1, offenceCode2, courtOrderStartDate, courtOrderEndDate);

        assertCourtOrderForCaseId(prosecutionCaseId, masterDefendantId,
                allOf(withJsonPath("$.courtOrders[1].id", anyOf(equalTo(courtOrderId1), equalTo(courtOrderId2))),
                        withJsonPath("$.courtOrders[1].courtOrderOffences[1].offence.id", equalTo(offenceId2))
                ));

        final Matcher[] matchers = {withJsonPath("$.caseId", equalTo(prosecutionCaseId)),
                withJsonPath("$.parties[0].offences[0].courtOrders[0].id", equalTo(courtOrderId1)),
                withJsonPath("$.parties[1].offences[0].courtOrders[0].id", isOneOf(courtOrderId1, courtOrderId2)),
                withJsonPath("$.parties[1].offences[0].courtOrders[1].id", isOneOf(courtOrderId1, courtOrderId2)),
                withJsonPath("$.parties[2].offences[0].courtOrders[0].id", isOneOf(courtOrderId2)),
                withJsonPath("$.parties[2].offences[1].courtOrders[0].id", isOneOf(courtOrderId2))};

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = findBy(matchers);

        assertTrue(prosecutionCaseResponseJsonObject.isPresent());

        final JsonObject indexedCaseDocument = prosecutionCaseResponseJsonObject.get();

        with(indexedCaseDocument.toString())
                .assertThat("caseId", equalTo(prosecutionCaseId))
                .assertThat("parties[0].partyId", equalTo(defendantId1))
                .assertThat("parties[1].partyId", equalTo(defendantId2))
                .assertThat("parties[2].partyId", equalTo(defendantId3))
                .assertThat("parties[0].offences[0].offenceCode", equalTo(offenceCode1))
                .assertThat("parties[0].offences[0].courtOrders[0].id", equalTo(courtOrderId1))
                .assertThat("parties[0].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))

                .assertThat("parties[1].offences[0].courtOrders[0].id", isOneOf(courtOrderId1, courtOrderId2))
                .assertThat("parties[1].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[1].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))

                .assertThat("parties[1].offences[0].courtOrders[1].id", isOneOf(courtOrderId1, courtOrderId2))
                .assertThat("parties[1].offences[0].courtOrders[1].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[1].offences[0].courtOrders[1].endDate", equalTo(courtOrderEndDate))

                .assertThat("parties[1].offences[1].courtOrders[0].id", equalTo(courtOrderId2))
                .assertThat("parties[1].offences[1].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[1].offences[1].courtOrders[0].endDate", equalTo(courtOrderEndDate))

                .assertThat("parties[2].offences[0].courtOrders[0].id", equalTo(courtOrderId2))
                .assertThat("parties[2].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[2].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))

                .assertThat("parties[2].offences[1].courtOrders[0].id", equalTo(courtOrderId2))
                .assertThat("parties[2].offences[1].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat("parties[2].offences[1].courtOrders[0].endDate", equalTo(courtOrderEndDate))

                .assertThat("_case_type", equalTo("PROSECUTION"));
    }

    private void createCourtOrder(final String fileName,
                                  final String courtOrderId,
                                  final String prosecutionCaseId,
                                  final String masterDefendantId,
                                  final String defendantId1,
                                  final String defendantId2,
                                  final String offenceId,
                                  final String offenceCode,
                                  final String courtOrderStartDate,
                                  final String courtOrderEndDate) throws IOException {

        final String requestBody = FileUtil.getPayload(fileName)
                .replaceAll("COURT_ORDER_ID", courtOrderId)
                .replaceAll("PROSECUTION_CASE_ID", prosecutionCaseId)
                .replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replace("DEFENDANT_ID_1", defendantId1)
                .replace("DEFENDANT_ID_2", defendantId2)
                .replace("OFFENCE_ID", offenceId)
                .replaceAll("OFFENCE_CODE", offenceCode)
                .replace("COURT_ORDER_START_DATE", courtOrderStartDate)
                .replace("COURT_ORDER_END_DATE", courtOrderEndDate);

        LOGGER.info("createCourtOrder - requestBody : {}", requestBody);

        postCommand(AbstractTestHelper.getWriteUrl("/court-order"),
                "application/vnd.courtorders.create-court-order+json", requestBody);
    }

    private void createCourtOrderForMultipleOffences(final String fileName,
                                                     final String courtOrderId,
                                                     final String prosecutionCaseId,
                                                     final String masterDefendantId,
                                                     final String defendantId1,
                                                     final String defendantId2,
                                                     final String offenceId1,
                                                     final String offenceId2,
                                                     final String offenceCode1,
                                                     final String offenceCode2,
                                                     final String courtOrderStartDate,
                                                     final String courtOrderEndDate) throws IOException {

        final String requestBody = FileUtil.getPayload(fileName)
                .replaceAll("COURT_ORDER_ID", courtOrderId)
                .replaceAll("PROSECUTION_CASE_ID", prosecutionCaseId)
                .replaceAll("MASTER_DEFENDANT_ID", masterDefendantId)
                .replace("DEFENDANT_ID_1", defendantId1)
                .replace("DEFENDANT_ID_2", defendantId2)
                .replaceAll("OFFENCE_ID_1", offenceId1)
                .replaceAll("OFFENCE_CODE_1", offenceCode1)
                .replaceAll("OFFENCE_ID_2", offenceId2)
                .replaceAll("OFFENCE_CODE_2", offenceCode2)
                .replace("COURT_ORDER_START_DATE", courtOrderStartDate)
                .replace("COURT_ORDER_END_DATE", courtOrderEndDate);

        LOGGER.info("createCourtOrder - requestBody : {}", requestBody);

        postCommand(AbstractTestHelper.getWriteUrl("/court-order"),
                "application/vnd.courtorders.create-court-order+json", requestBody);
    }

    private void verifyCourtOrderCreated(final String caseId, final String masterDefendantId, final String courtOrderId, final String offenceCode) {
        assertCourtOrderForCaseId(caseId, masterDefendantId, allOf(
                withJsonPath("$.courtOrders[0].id", equalTo(courtOrderId))
        ));

    }
}
