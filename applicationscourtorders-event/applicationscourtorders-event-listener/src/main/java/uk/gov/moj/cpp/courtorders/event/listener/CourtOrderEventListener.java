package uk.gov.moj.cpp.courtorders.event.listener;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CourtOrderValidityUpdated;
import uk.gov.justice.core.courts.JudicialChildResultsUpdated;
import uk.gov.justice.core.courts.UpdateCourtOrder;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;
import uk.gov.moj.cpp.courtorders.persistence.entity.CourtOrderEntity;
import uk.gov.moj.cpp.courtorders.persistence.repository.CourtOrderRepository;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ServiceComponent(EVENT_LISTENER)
public class CourtOrderEventListener {

    @Inject
    private CourtOrderRepository courtOrderRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Transactional
    @Handles("applicationscourtorders.event.court-order-removed")
    public void removeCourtOrder(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final List<CourtOrderEntity> courtOrders = courtOrderRepository
                .findByCourtOrderIdNotRemoved(fromString(payload.getString("courtOrderId")));

        if (!courtOrders.isEmpty()) {
            final CourtOrderEntity courtOrderEntity = courtOrders.get(0);
            courtOrderEntity.setRemoved(true);
            courtOrderRepository.save(courtOrderEntity);
        }
    }

    @Transactional
    @Handles("applicationscourtorders.event.court-order-requested")
    public void saveCourtOrder(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final JsonObject courtOrderJson = payload.getJsonObject("courtOrder");
        final CreateCourtOrder courtOrder =
                jsonObjectToObjectConverter.convert(courtOrderJson, CreateCourtOrder.class);
        final List<CourtOrderEntity> courtOrders = courtOrderRepository.findByCourtOrderIdNotRemoved(fromString(payload.getString("courtOrderId")));
        final CourtOrderEntity courtOrderEntity = courtOrders.isEmpty() ? new CourtOrderEntity() : courtOrders.get(0);
        if (courtOrders.isEmpty()) {
            courtOrderEntity.setId(UUID.randomUUID());
            courtOrderEntity.setCourtOrderId(courtOrder.getId());
            courtOrderEntity.setHearingId(courtOrder.getOrderingHearingId());
            courtOrderEntity.setSittingDate(courtOrder.getOrderDate());
            courtOrderEntity.setDefendantId(courtOrder.getMasterDefendantId());
            courtOrderEntity.setExpiryDate(nonNull(courtOrder.getExpiryDate()) ? courtOrder.getExpiryDate() : courtOrder.getEndDate());
        }
        courtOrderEntity.setPayload(courtOrderJson.toString());
        courtOrderEntity.setRemoved(false);
        courtOrderRepository.save(courtOrderEntity);
    }

    @Transactional
    @Handles("applicationscourtorders.event.court-order-validity-updated")
    public void updateCourtOrder(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final CourtOrderValidityUpdated courtOrderValidityUpdated = jsonObjectToObjectConverter.convert(payload, CourtOrderValidityUpdated.class);
        final List<CourtOrderEntity> courtOrders = courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderValidityUpdated.getCourtOrderId());
        if (!courtOrders.isEmpty()) {
            final CourtOrderEntity courtOrderEntity = courtOrders.get(0);
            final UpdateCourtOrder updateCourtOrder = UpdateCourtOrder.updateCourtOrder()
                    .withValuesFrom(jsonObjectToObjectConverter.convert(
                            stringToJsonObjectConverter.convert(courtOrderEntity.getPayload()), UpdateCourtOrder.class))
                    .withExpiryDate(nonNull(courtOrderValidityUpdated.getExpiryDate()) ? courtOrderValidityUpdated.getExpiryDate() : courtOrderValidityUpdated.getNewEndDate())
                    .withEndDate(courtOrderValidityUpdated.getNewEndDate()).build();
            courtOrderEntity.setPayload(objectToJsonObjectConverter.convert(updateCourtOrder).toString());
            courtOrderEntity.setExpiryDate(nonNull(updateCourtOrder.getExpiryDate()) ? updateCourtOrder.getExpiryDate() : updateCourtOrder.getEndDate());
            courtOrderRepository.save(courtOrderEntity);
        }
    }

    @Transactional
    @Handles("applicationscourtorders.event.judicial-child-results-updated")
    public void updateJudicialChildResults(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final JudicialChildResultsUpdated judicialChildResultsUpdated = jsonObjectToObjectConverter.convert(payload, JudicialChildResultsUpdated.class);
        final List<CourtOrderEntity> courtOrders = courtOrderRepository.findByCourtOrderIdNotRemoved(judicialChildResultsUpdated.getCourtOrderId());
        if (!courtOrders.isEmpty()) {
            final CourtOrderEntity courtOrderEntity = courtOrders.get(0);
            final CreateCourtOrder courtOrder = jsonObjectToObjectConverter.convert(
                    stringToJsonObjectConverter.convert(courtOrderEntity.getPayload()), CreateCourtOrder.class);

            final CreateCourtOrder updateCourtOrder = CreateCourtOrder.createCourtOrder()
                    .withValuesFrom(courtOrder)
                    .withIsUnpaidWork(judicialChildResultsUpdated.getIsUnpaidWork())
                    .withJudicialChildResults(judicialChildResultsUpdated.getJudicialChildResults())
                    .withExpiryDate(judicialChildResultsUpdated.getExpiryDate())
                    .build();
            courtOrderEntity.setPayload(objectToJsonObjectConverter.convert(updateCourtOrder).toString());
            courtOrderEntity.setExpiryDate(judicialChildResultsUpdated.getExpiryDate());
            courtOrderRepository.save(courtOrderEntity);
        }
    }
}
