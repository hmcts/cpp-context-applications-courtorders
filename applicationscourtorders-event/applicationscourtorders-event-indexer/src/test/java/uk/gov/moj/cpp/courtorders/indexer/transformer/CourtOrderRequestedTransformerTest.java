package uk.gov.moj.cpp.courtorders.indexer.transformer;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.format;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.courtorders.indexer.helper.JsonHelper.readJson;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.validation.JsonDocumentValidator;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtOrderRequestedTransformerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    final ObjectToJsonObjectConverter objectToJsonObjectConvert1 = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private CourtOrderRequestedTransformer courtOrderRequestedTransformer;

    @Inject
    private JsonDocumentValidator jsonValidator = new JsonDocumentValidator();

    private static final String OUTPUT_CASE_JSON_PATH = "$.caseDocuments[%d]";

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldTransformCourtOrderRequested() {

        final JsonObject inputJson = readJson("/applicationscourtorders.event.court-order-requested.json");
        final DocumentContext inputCourtOrder = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final javax.json.JsonObject output = objectToJsonObjectConvert1.convert(courtOrderRequestedTransformer.transform(input));

        jsonValidator.validate(output, "/json/schema/crime-case-index-schema.json");

        final String jsonBasePathForCourtOrder = "$.courtOrder";
        final String outputCaseDocumentsPath = "$.caseDocuments[0]";
        final String courtOrderLabel = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".label")).getString();
        final String defendantId1 = ((JsonArray) inputCourtOrder.read(jsonBasePathForCourtOrder + ".defendantIds")).getString(0);
        final String defendantId2 = ((JsonArray) inputCourtOrder.read(jsonBasePathForCourtOrder + ".defendantIds")).getString(1);
        final String defendantId3 = ((JsonArray) inputCourtOrder.read(jsonBasePathForCourtOrder + ".defendantIds")).getString(2);
        final String courtOrderStartDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".startDate")).getString();
        final String courtOrderEndDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".endDate")).getString();
        final String courtOrderDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".orderDate")).getString();
        final String courtOrderJudicialResultTypeId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".judicialResultTypeId")).getString();
        final String courtOrderOrderingHearingId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".orderingHearingId")).getString();
        final String courtOrderId = ((JsonString) inputCourtOrder.read("$.courtOrderId")).getString();
        final Boolean courtOrderCanBeSubjectOfBreachProceedings = Boolean.valueOf(inputCourtOrder.read(jsonBasePathForCourtOrder + ".canBeSubjectOfBreachProceedings").toString());
        final Boolean courtOrderCanBeSubjectOfVariationProceedings = Boolean.valueOf(inputCourtOrder.read(jsonBasePathForCourtOrder + ".canBeSubjectOfVariationProceedings").toString());

        final String prosecutionCaseId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].prosecutionCaseId")).getString();

        with(output.toString())
                .assertThat(outputCaseDocumentsPath + ".caseId", equalTo(prosecutionCaseId))
                .assertThat(outputCaseDocumentsPath + ".parties[0].partyId", equalTo(defendantId1))
                .assertThat(outputCaseDocumentsPath + ".parties[1].partyId", equalTo(defendantId2))
                .assertThat(outputCaseDocumentsPath + ".parties[2].partyId", equalTo(defendantId3))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].id", equalTo(courtOrderId))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].judicialResultTypeId", equalTo(courtOrderJudicialResultTypeId))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].orderDate", equalTo(courtOrderDate))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].label", equalTo(courtOrderLabel))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].orderingHearingId", equalTo(courtOrderOrderingHearingId))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].canBeSubjectOfBreachProceedings", equalTo(courtOrderCanBeSubjectOfBreachProceedings))
                .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].courtOrders[0].canBeSubjectOfVariationProceedings", equalTo(courtOrderCanBeSubjectOfVariationProceedings))
                .assertThat(outputCaseDocumentsPath + "._case_type", equalTo("PROSECUTION"))
                .assertNotDefined(outputCaseDocumentsPath + ".caseStatus");
    }
    @Test
    public void shouldTransformCourtOrderRequestedWithPleaAndVerdict() {

        final JsonObject inputJson = readJson("/applicationscourtorders.event.court-order-requested-with-plea-verdict.json");
        final DocumentContext inputCourtOrder = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final javax.json.JsonObject output = objectToJsonObjectConvert1.convert(courtOrderRequestedTransformer.transform(input));

        jsonValidator.validate(output, "/json/schema/crime-case-index-schema.json");

        final String jsonBasePathForCourtOrder = "$.courtOrder";
        final String outputCaseDocumentsPath = "$.caseDocuments[0]";
        final String courtOrderPleaValue = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].offence.plea.pleaValue")).getString();
        final String courtOrderPleaDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].offence.plea.pleaDate")).getString();
        final String courtOrderVerdictDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].offence.verdict.verdictDate")).getString();
        final String courtOrderVerdictCategory = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].offence.verdict.verdictType.category")).getString();
        final String courtOrderVerdictCategoryType = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].offence.verdict.verdictType.categoryType")).getString();
        final String courtOrderVerdictDescription = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].offence.verdict.verdictType.description")).getString();

        with(output.toString())
                   .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].pleas[0].pleaValue", equalTo(courtOrderPleaValue))
                   .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].pleas[0].pleaDate", equalTo(courtOrderPleaDate))

                   .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictDate", equalTo(courtOrderVerdictDate))
                   .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictType.category", equalTo(courtOrderVerdictCategory))
                   .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictType.categoryType", equalTo(courtOrderVerdictCategoryType))
                   .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictType.description", equalTo(courtOrderVerdictDescription))
        ;
    }

    @Test
    public void shouldTransformCourtOrderRequestedForMultipleCourtOrders() {

        final JsonObject inputJson = readJson("/applicationscourtorders.event.court-order-requested-multiple.json");
        final DocumentContext inputCourtOrder = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final javax.json.JsonObject output = objectToJsonObjectConvert1.convert(courtOrderRequestedTransformer.transform(input));

        jsonValidator.validate(output, "/json/schema/crime-case-index-schema.json");

        final String jsonBasePathForCourtOrder = "$.courtOrder";
        final String courtOrderLabel = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".label")).getString();
        final String defendantId1 = ((JsonArray) inputCourtOrder.read(jsonBasePathForCourtOrder + ".defendantIds")).getString(0);
        final String defendantId2 = ((JsonArray) inputCourtOrder.read(jsonBasePathForCourtOrder + ".defendantIds")).getString(1);
        final String courtOrderStartDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".startDate")).getString();
        final String courtOrderEndDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".endDate")).getString();
        final String courtOrderDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".orderDate")).getString();
        final String courtOrderJudicialResultTypeId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".judicialResultTypeId")).getString();
        final String courtOrderOrderingHearingId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".orderingHearingId")).getString();
        final String courtOrderId = ((JsonString) inputCourtOrder.read("$.courtOrderId")).getString();
        final Boolean courtOrderCanBeSubjectOfBreachProceedings = Boolean.valueOf(inputCourtOrder.read(jsonBasePathForCourtOrder + ".canBeSubjectOfBreachProceedings").toString());
        final Boolean courtOrderCanBeSubjectOfVariationProceedings = Boolean.valueOf(inputCourtOrder.read(jsonBasePathForCourtOrder + ".canBeSubjectOfVariationProceedings").toString());

        final String courtOrderIndex0prosecutionCaseId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].prosecutionCaseId")).getString();
        final String courtOrderIndex1prosecutionCaseId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[1].prosecutionCaseId")).getString();

        final String outputCaseDocumentsPathIndex0 = format(OUTPUT_CASE_JSON_PATH, 0);
        final String outputCaseDocumentsPathIndex1 = format(OUTPUT_CASE_JSON_PATH, 1);

        with(output.toString())
                .assertThat(outputCaseDocumentsPathIndex0 + ".caseId", anyOf(equalTo(courtOrderIndex0prosecutionCaseId), equalTo(courtOrderIndex1prosecutionCaseId)))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].partyId", equalTo(defendantId1))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[1].partyId", equalTo(defendantId2))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].id", equalTo(courtOrderId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].judicialResultTypeId", equalTo(courtOrderJudicialResultTypeId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].orderDate", equalTo(courtOrderDate))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].label", equalTo(courtOrderLabel))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].orderingHearingId", equalTo(courtOrderOrderingHearingId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].canBeSubjectOfBreachProceedings", equalTo(courtOrderCanBeSubjectOfBreachProceedings))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].canBeSubjectOfVariationProceedings", equalTo(courtOrderCanBeSubjectOfVariationProceedings))
                .assertThat(outputCaseDocumentsPathIndex0 + "._case_type", equalTo("PROSECUTION"));

        with(output.toString())
                .assertThat(outputCaseDocumentsPathIndex1 + ".caseId", anyOf(equalTo(courtOrderIndex0prosecutionCaseId), equalTo(courtOrderIndex1prosecutionCaseId)))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].partyId", equalTo(defendantId1))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[1].partyId", equalTo(defendantId2))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].id", equalTo(courtOrderId))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].judicialResultTypeId", equalTo(courtOrderJudicialResultTypeId))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].orderDate", equalTo(courtOrderDate))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].label", equalTo(courtOrderLabel))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].orderingHearingId", equalTo(courtOrderOrderingHearingId))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].canBeSubjectOfBreachProceedings", equalTo(courtOrderCanBeSubjectOfBreachProceedings))
                .assertThat(outputCaseDocumentsPathIndex1 + ".parties[0].offences[0].courtOrders[0].canBeSubjectOfVariationProceedings", equalTo(courtOrderCanBeSubjectOfVariationProceedings))
                .assertThat(outputCaseDocumentsPathIndex1 + "._case_type", equalTo("PROSECUTION"));
    }

    @Test
    public void shouldTransformCourtOrderRequestedForMultipleCourtOrdersWithSameProsecutionCase() {

        final JsonObject inputJson = readJson("/applicationscourtorders.event.court-order-requested-multiple-with-same-prosecution-case.json");
        final DocumentContext inputCourtOrder = JsonPath.parse(inputJson);
        final Map<String, Object> input = JsonUtils.jsonToMap(new ByteArrayInputStream(inputJson.toString().getBytes()));
        final javax.json.JsonObject output = objectToJsonObjectConvert1.convert(courtOrderRequestedTransformer.transform(input));

        jsonValidator.validate(output, "/json/schema/crime-case-index-schema.json");

        final String jsonBasePathForCourtOrder = "$.courtOrder";
        final String courtOrderLabel = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".label")).getString();
        final String defendantId1 = ((JsonArray) inputCourtOrder.read(jsonBasePathForCourtOrder + ".defendantIds")).getString(0);
        final String defendantId2 = ((JsonArray) inputCourtOrder.read(jsonBasePathForCourtOrder + ".defendantIds")).getString(1);
        final String courtOrderStartDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".startDate")).getString();
        final String courtOrderEndDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".endDate")).getString();
        final String courtOrderDate = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".orderDate")).getString();
        final String courtOrderJudicialResultTypeId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".judicialResultTypeId")).getString();
        final String courtOrderOrderingHearingId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".orderingHearingId")).getString();
        final String courtOrderId = ((JsonString) inputCourtOrder.read("$.courtOrderId")).getString();
        final Boolean courtOrderCanBeSubjectOfBreachProceedings = Boolean.valueOf(inputCourtOrder.read(jsonBasePathForCourtOrder + ".canBeSubjectOfBreachProceedings").toString());
        final Boolean courtOrderCanBeSubjectOfVariationProceedings = Boolean.valueOf(inputCourtOrder.read(jsonBasePathForCourtOrder + ".canBeSubjectOfVariationProceedings").toString());

        final String courtOrderIndex0offenceId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].offence.id")).getString();
        final String courtOrderIndex1offenceId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[1].offence.id")).getString();

        final String courtOrderIndex0prosecutionCaseId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[0].prosecutionCaseId")).getString();
        final String courtOrderIndex1prosecutionCaseId = ((JsonString) inputCourtOrder.read(jsonBasePathForCourtOrder + ".courtOrderOffences[1].prosecutionCaseId")).getString();

        final String outputCaseDocumentsPathIndex0 = format(OUTPUT_CASE_JSON_PATH, 0);

        with(output.toString())
                .assertThat(outputCaseDocumentsPathIndex0 + ".caseId", anyOf(equalTo(courtOrderIndex0prosecutionCaseId), equalTo(courtOrderIndex1prosecutionCaseId)))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].offenceId", equalTo(courtOrderIndex0offenceId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[1].offenceId", equalTo(courtOrderIndex1offenceId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[1].offences[0].offenceId", equalTo(courtOrderIndex0offenceId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[1].offences[1].offenceId", equalTo(courtOrderIndex1offenceId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].partyId", equalTo(defendantId1))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[1].partyId", equalTo(defendantId2))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].id", equalTo(courtOrderId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].judicialResultTypeId", equalTo(courtOrderJudicialResultTypeId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].startDate", equalTo(courtOrderStartDate))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].endDate", equalTo(courtOrderEndDate))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].orderDate", equalTo(courtOrderDate))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].label", equalTo(courtOrderLabel))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].orderingHearingId", equalTo(courtOrderOrderingHearingId))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].canBeSubjectOfBreachProceedings", equalTo(courtOrderCanBeSubjectOfBreachProceedings))
                .assertThat(outputCaseDocumentsPathIndex0 + ".parties[0].offences[0].courtOrders[0].canBeSubjectOfVariationProceedings", equalTo(courtOrderCanBeSubjectOfVariationProceedings))
                .assertThat(outputCaseDocumentsPathIndex0 + "._case_type", equalTo("PROSECUTION"));
    }
}
