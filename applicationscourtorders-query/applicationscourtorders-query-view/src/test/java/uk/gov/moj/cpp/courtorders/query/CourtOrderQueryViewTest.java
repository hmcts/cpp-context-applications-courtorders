package uk.gov.moj.cpp.courtorders.query;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.courtorders.persistence.entity.CourtOrderEntity;
import uk.gov.moj.cpp.courtorders.persistence.repository.CourtOrderRepository;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class CourtOrderQueryViewTest {

    @Mock
    private JsonEnvelope query;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope response;

    @InjectMocks
    private CourtOrderQueryView courtOrderQueryView;

    @Mock
    private CourtOrderRepository courtOrderRepository;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void getCourtOrdersByDefendant() {
        final UUID defendantId = UUID.randomUUID();
        final LocalDate hearingDate = LocalDate.now().plusDays(2);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("hearingDate", hearingDate.toString())
                .build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName("applicationscourtorders.query.court-order-by-defendant-id").build(),
                jsonObject);
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        final UUID courtOrderId = randomUUID();
        final JsonObject courtOrderPayload = Json.createObjectBuilder().add("id", courtOrderId.toString()).build();
        courtOrderEntity.setPayload(courtOrderPayload.toString());
        Mockito.when(courtOrderRepository.findByDefendantIdAndExpiryDate(defendantId, hearingDate)).thenReturn(Lists.newArrayList(courtOrderEntity));
        final JsonEnvelope envelope = courtOrderQueryView.getCourtOrdersByDefendant(jsonEnvelope);
        final JsonArray payloadAsJsonObject = envelope.payloadAsJsonObject().getJsonArray("courtOrders");

        assertThat(payloadAsJsonObject.getValuesAs(JsonObject.class).size(), is(1));
        assertThat(payloadAsJsonObject.getJsonObject(0).getString("id"), is(courtOrderId.toString()));
        assertThat(payloadAsJsonObject.getJsonObject(0).getBoolean("showUnpaidWorkWarning"), is(false));
    }

    @Test
    void shouldGetCourtOrdersByDefendantReturnShowUnpaidWorkWarningAsTrueWhenHearingDateIsAfterEndDate() {
        final UUID defendantId = UUID.randomUUID();
        final LocalDate endDate = LocalDate.now().minusDays(2);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("hearingDate", LocalDate.now().toString())
                .build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName("applicationscourtorders.query.court-order-by-defendant-id").build(),
                jsonObject);
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        final UUID courtOrderId = randomUUID();
        final JsonObject courtOrderPayload = Json.createObjectBuilder().add("id", courtOrderId.toString())
                .add("isUnpaidWork", true)
                .add("endDate", endDate.toString())
                .build();
        courtOrderEntity.setPayload(courtOrderPayload.toString());
        Mockito.when(courtOrderRepository.findByDefendantIdAndExpiryDate(defendantId, LocalDate.now())).thenReturn(Lists.newArrayList(courtOrderEntity));
        final JsonEnvelope envelope = courtOrderQueryView.getCourtOrdersByDefendant(jsonEnvelope);
        final JsonArray payloadAsJsonObject = envelope.payloadAsJsonObject().getJsonArray("courtOrders");

        assertThat(payloadAsJsonObject.getValuesAs(JsonObject.class).size(), is(1));
        assertThat(payloadAsJsonObject.getJsonObject(0).getString("id"), is(courtOrderId.toString()));
        assertThat(payloadAsJsonObject.getJsonObject(0).getBoolean("showUnpaidWorkWarning"), is(true));
    }

    @Test
    void shouldGetCourtOrdersByDefendantReturnShowUnpaidWorkWarningAsFalseWhenHearingDateIsBeforeEndDate() {
        final UUID defendantId = UUID.randomUUID();
        LocalDate endDate = LocalDate.now().plusMonths(2);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("hearingDate", LocalDate.now().toString())
                .build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName("applicationscourtorders.query.court-order-by-defendant-id").build(),
                jsonObject);
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        final UUID courtOrderId = randomUUID();
        final JsonObject courtOrderPayload = Json.createObjectBuilder().add("id", courtOrderId.toString())
                .add("isUnpaidWork", true)
                .add("endDate", endDate.toString())
                .build();
        courtOrderEntity.setPayload(courtOrderPayload.toString());
        Mockito.when(courtOrderRepository.findByDefendantIdAndExpiryDate(defendantId, LocalDate.now())).thenReturn(Lists.newArrayList(courtOrderEntity));
        final JsonEnvelope envelope = courtOrderQueryView.getCourtOrdersByDefendant(jsonEnvelope);
        final JsonArray payloadAsJsonObject = envelope.payloadAsJsonObject().getJsonArray("courtOrders");

        assertThat(payloadAsJsonObject.getValuesAs(JsonObject.class).size(), is(1));
        assertThat(payloadAsJsonObject.getJsonObject(0).getString("id"), is(courtOrderId.toString()));
        assertThat(payloadAsJsonObject.getJsonObject(0).getBoolean("showUnpaidWorkWarning"), is(false));
    }

    @Test
    void shouldGetCourtOrdersByDefendantAndOffenceDate() {
        final UUID defendantId1 = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID defendantId3 = UUID.randomUUID();
        final LocalDate offenceDate1 = LocalDate.now().minusMonths(1);
        final LocalDate offenceDate2 = LocalDate.now().minusMonths(2);
        final LocalDate offenceDate3 = LocalDate.now().minusMonths(1);
        LocalDate endDate1 = LocalDate.now().minusMonths(2);
        LocalDate endDate2 = LocalDate.now().plusMonths(1);
        LocalDate endDate3 = LocalDate.now().minusMonths(2);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("filterCriteria", defendantId1 + ":" + offenceDate1 + "," + defendantId2 + ":" + offenceDate2 + "," + defendantId3 + ":" + offenceDate3)
                .add("hearingDate", LocalDate.now().toString())
                .build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName("applicationscourtorders.query.court-order-by-defendant-id").build(),
                jsonObject);
        final CourtOrderEntity courtOrderEntity1 = new CourtOrderEntity();
        final UUID courtOrderId = randomUUID();
        final JsonObject courtOrderPayload = Json.createObjectBuilder().add("id", courtOrderId.toString())
                .add("isUnpaidWork", true)
                .add("endDate", endDate1.toString())
                .build();
        courtOrderEntity1.setPayload(courtOrderPayload.toString());
        Mockito.when(courtOrderRepository.findByDefendantIdAndExpiryDate(defendantId1, offenceDate1)).thenReturn(List.of(courtOrderEntity1));
        final CourtOrderEntity courtOrderEntity2 = new CourtOrderEntity();
        final UUID courtOrderId2 = randomUUID();
        final JsonObject courtOrderPayload2 = Json.createObjectBuilder().add("id", courtOrderId2.toString())
                .add("isUnpaidWork", true)
                .add("endDate", endDate2.toString())
                .build();
        courtOrderEntity2.setPayload(courtOrderPayload2.toString());
        Mockito.when(courtOrderRepository.findByDefendantIdAndExpiryDate(defendantId2, offenceDate2)).thenReturn(List.of(courtOrderEntity2));
        final CourtOrderEntity courtOrderEntity3 = new CourtOrderEntity();
        final UUID courtOrderId3 = randomUUID();
        final JsonObject courtOrderPayload3 = Json.createObjectBuilder().add("id", courtOrderId3.toString())
                .add("isUnpaidWork", false)
                .add("endDate", endDate3.toString())
                .build();
        courtOrderEntity3.setPayload(courtOrderPayload3.toString());
        Mockito.when(courtOrderRepository.findByDefendantIdAndExpiryDate(defendantId3, offenceDate3)).thenReturn(List.of(courtOrderEntity3));
        final JsonEnvelope envelope = courtOrderQueryView.getCourtOrdersByDefendantAndOffenceDate(jsonEnvelope);
        final JsonArray payloadAsJsonObject = envelope.payloadAsJsonObject().getJsonArray("courtOrders");

        assertThat(payloadAsJsonObject.getValuesAs(JsonObject.class).size(), is(3));
        assertThat(payloadAsJsonObject.getJsonObject(0).getString("id"), is(courtOrderId.toString()));
        assertThat(payloadAsJsonObject.getJsonObject(0).getBoolean("showUnpaidWorkWarning"), is(true));

        assertThat(payloadAsJsonObject.getJsonObject(1).getString("id"), is(courtOrderId2.toString()));
        assertThat(payloadAsJsonObject.getJsonObject(1).getBoolean("showUnpaidWorkWarning"), is(false));

        assertThat(payloadAsJsonObject.getJsonObject(2).getString("id"), is(courtOrderId3.toString()));
        assertThat(payloadAsJsonObject.getJsonObject(2).getBoolean("showUnpaidWorkWarning"), is(false));

    }

    @Test
    public void getCourtOrdersByCaseId() {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", defendantId.toString())
                .build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName("applicationscourtorders.query.court-order-by-case-and-defendant-id").build(),
                jsonObject);
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        final UUID courtOrderId = randomUUID();
        final JsonObject courtOrderPayload = Json.createObjectBuilder().add("id", courtOrderId.toString()).build();
        courtOrderEntity.setPayload(courtOrderPayload.toString());
        Mockito.when(courtOrderRepository.findByCaseAndDefendantId(caseId.toString(), defendantId)).thenReturn(Lists.newArrayList(courtOrderEntity));
        final JsonEnvelope envelope = courtOrderQueryView.getCourtOrdersByCase(jsonEnvelope);
        final JsonArray payloadAsJsonObject = envelope.payloadAsJsonObject().getJsonArray("courtOrders");

        assertThat(payloadAsJsonObject.getValuesAs(JsonObject.class).size(), is(1));
        assertThat(payloadAsJsonObject.getJsonObject(0).getString("id"), is(courtOrderId.toString()));
    }

    @Test
    public void getCourtOrdersByHearingAndDefendant() {
        final UUID defendantId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final LocalDate sittingDate = LocalDate.now();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("hearingId", hearingId.toString())
                .add("sittingDate", sittingDate.toString())
                .build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName("applicationscourtorders.query.court-order-by-hearing-and-defendant-id").build(),
                jsonObject);
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        final UUID courtOrderId = randomUUID();
        final JsonObject courtOrderPayload = Json.createObjectBuilder().add("id", courtOrderId.toString()).build();
        courtOrderEntity.setPayload(courtOrderPayload.toString());
        Mockito.when(courtOrderRepository.findByHearingDefendantIdAndSittingDate(defendantId, hearingId, sittingDate)).thenReturn(Lists.newArrayList(courtOrderEntity));
        final JsonEnvelope envelope = courtOrderQueryView.getCourtOrdersByHearingAndDefendant(jsonEnvelope);
        final JsonArray payloadAsJsonObject = envelope.payloadAsJsonObject().getJsonArray("courtOrders");

        assertThat(payloadAsJsonObject.getValuesAs(JsonObject.class).size(), is(1));
        assertThat(payloadAsJsonObject.getJsonObject(0).getString("id"), is(courtOrderId.toString()));
    }
}
