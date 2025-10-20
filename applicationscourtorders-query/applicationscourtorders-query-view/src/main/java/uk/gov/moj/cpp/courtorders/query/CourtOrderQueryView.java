package uk.gov.moj.cpp.courtorders.query;

import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.courtorders.query.CourtOrder;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;
import uk.gov.moj.cpp.courtorders.persistence.entity.CourtOrderEntity;
import uk.gov.moj.cpp.courtorders.persistence.repository.CourtOrderRepository;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CourtOrderQueryView {

    private static final String HEARING_DATE = "hearingDate";
    @Inject
    private CourtOrderRepository courtOrderRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public JsonEnvelope getCourtOrdersByDefendant(final JsonEnvelope query) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonObject payloadAsJsonObject = query.payloadAsJsonObject();
        final UUID defendantId = UUID.fromString(payloadAsJsonObject.getString("defendantId"));
        final LocalDate expiryDate = payloadAsJsonObject.containsKey(HEARING_DATE) ?
               parse(payloadAsJsonObject.getString(HEARING_DATE)) : LocalDate.now();
        final List<CourtOrderEntity> courtOrders = courtOrderRepository.findByDefendantIdAndExpiryDate(defendantId, expiryDate);
        courtOrders.forEach(courtOrderEntity ->
            jsonArrayBuilder.add(convertToQueryCourtOrder(courtOrderEntity.getPayload(), expiryDate))
        );
        jsonObjectBuilder.add("courtOrders", jsonArrayBuilder.build());
        return envelopeFrom(query.metadata(), jsonObjectBuilder.build());
    }

    public JsonEnvelope getCourtOrdersByDefendantAndOffenceDate(final JsonEnvelope query) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonObject payloadAsJsonObject = query.payloadAsJsonObject();
        final List<String> defendantIdsAndOffenceDates = Arrays.stream(payloadAsJsonObject.getString("filterCriteria").split(",")).toList();
        final LocalDate hearingDate = parse(payloadAsJsonObject.getString(HEARING_DATE));
        final List<CourtOrderEntity> courtOrders = new ArrayList<>();
        defendantIdsAndOffenceDates.forEach(defendantIdAndOffenceDate -> {
            final String[] defendantIdAndOffenceDateArray = defendantIdAndOffenceDate.split(":");
            courtOrders.addAll(courtOrderRepository.findByDefendantIdAndExpiryDate(UUID.fromString(defendantIdAndOffenceDateArray[0]), parse(defendantIdAndOffenceDateArray[1])));
        });

        courtOrders.forEach(courtOrderEntity ->
                jsonArrayBuilder.add(convertToQueryCourtOrder(courtOrderEntity.getPayload(), hearingDate))
        );
        jsonObjectBuilder.add("courtOrders", jsonArrayBuilder.build());
        return envelopeFrom(query.metadata(), jsonObjectBuilder.build());
    }

    public JsonEnvelope getCourtOrdersByCase(final JsonEnvelope query) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonObject payloadAsJsonObject = query.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(payloadAsJsonObject.getString("caseId"));
        final UUID defendantId = UUID.fromString(payloadAsJsonObject.getString("defendantId"));
        final List<CourtOrderEntity> courtOrders = courtOrderRepository.findByCaseAndDefendantId(caseId.toString(), defendantId);
        courtOrders.forEach(courtOrderEntity ->
            jsonArrayBuilder.add(stringToJsonObjectConverter.convert(courtOrderEntity.getPayload()))
        );
        jsonObjectBuilder.add("courtOrders", jsonArrayBuilder.build());
        return envelopeFrom(query.metadata(), jsonObjectBuilder.build());
    }

    public JsonEnvelope getCourtOrdersByHearingAndDefendant(final JsonEnvelope query) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonObject payloadAsJsonObject = query.payloadAsJsonObject();
        final UUID defendantId = UUID.fromString(payloadAsJsonObject.getString("defendantId"));
        final UUID hearingId = UUID.fromString(payloadAsJsonObject.getString("hearingId"));
        final LocalDate sittingDate = parse(payloadAsJsonObject.getString("sittingDate"));

        final List<CourtOrderEntity> courtOrders = courtOrderRepository.findByHearingDefendantIdAndSittingDate(defendantId,
                hearingId, sittingDate);
        courtOrders.forEach(courtOrderEntity ->
            jsonArrayBuilder.add(stringToJsonObjectConverter.convert(courtOrderEntity.getPayload()))
        );
        jsonObjectBuilder.add("courtOrders", jsonArrayBuilder.build());
        return envelopeFrom(query.metadata(), jsonObjectBuilder.build());
    }

    private JsonObject convertToQueryCourtOrder(final String payload, final LocalDate hearingDate) {
        final CreateCourtOrder courtOrder = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(payload), CreateCourtOrder.class);
        return objectToJsonObjectConverter.convert(CourtOrder.courtOrder()
                .withCanBeSubjectOfBreachProceedings(courtOrder.getCanBeSubjectOfBreachProceedings())
                .withCanBeSubjectOfVariationProceedings(courtOrder.getCanBeSubjectOfVariationProceedings())
                .withCourtOrderOffences(courtOrder.getCourtOrderOffences())
                .withDefendantIds(courtOrder.getDefendantIds())
                .withEndDate(courtOrder.getEndDate())
                .withExpiryDate(courtOrder.getExpiryDate())
                .withId(courtOrder.getId())
                .withIsSJPOrder(courtOrder.getIsSJPOrder())
                .withJudicialResultTypeId(courtOrder.getJudicialResultTypeId())
                .withLabel(courtOrder.getLabel())
                .withMasterDefendantId(courtOrder.getMasterDefendantId())
                .withOrderDate(courtOrder.getOrderDate())
                .withOrderingCourt(courtOrder.getOrderingCourt())
                .withOrderingHearingId(courtOrder.getOrderingHearingId())
                .withIsUnpaidWork(courtOrder.getIsUnpaidWork())
                .withShowUnpaidWorkWarning(showUnpaidWorkWarning(courtOrder, hearingDate))
                .withStartDate(courtOrder.getStartDate())
                .build());

    }

    private boolean showUnpaidWorkWarning(final CreateCourtOrder createCourtOrder, final LocalDate hearingDate) {
        return nonNull(createCourtOrder.getIsUnpaidWork()) && createCourtOrder.getIsUnpaidWork() &&
                hearingDate.isAfter(createCourtOrder.getEndDate());
    }
}
