package uk.gov.moj.cpp.courtorders.handler;

import static java.util.Objects.isNull;

import uk.gov.justice.core.courts.JudicialChildResults;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.courtorders.aggregate.CourtOrderAggregate;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;
import uk.gov.moj.cpp.courtorders.command.PatchUpdateJudicialChildResults;
import uk.gov.moj.cpp.courtorders.command.PatchUpdateJudicialChildResultsV2;
import uk.gov.moj.cpp.courtorders.command.RemoveCourtOrder;
import uk.gov.moj.cpp.courtorders.command.UpdateCourtOrderValidity;
import uk.gov.moj.cpp.courtorders.handler.service.ProgressionService;
import uk.gov.moj.cpp.courtorders.handler.service.ReferenceDataService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CourtOrderRequestHandler extends AbstractCommandHandler {

    private static final String UNPAID_WORK_EXTENSION_COMMUNITY_ORD_YRO = "unpaidWorkExtensionCommunityOrdYro";

    @Inject
    @Value(key = "unpaidWorkJudicialTypeIds", defaultValue = "9bec5977-1796-4645-9b9e-687d4f23d37d,5ab456c8-d272-4082-87ed-cd1f44a0603a")
    private String unpaidWorkJudicialTypeIds;

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("courtorders.command.create-court-order")
    public void handleAddCourtRegister(final Envelope<CreateCourtOrder> createCourtOrderEnvelope) throws EventStreamException {
        final CreateCourtOrder createCourtOrder = createCourtOrderEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(createCourtOrder.getId());
        final CourtOrderAggregate courtOrderAggregate = aggregateService.get(eventStream, CourtOrderAggregate.class);
        final Stream<Object> events = courtOrderAggregate.createCourtOrder(createCourtOrder.getId(), createCourtOrder, isUnpaidWorkCommunityOrd(createCourtOrderEnvelope.metadata(), createCourtOrder), covertToUUIDs(unpaidWorkJudicialTypeIds));
        appendEventsToStream(createCourtOrderEnvelope, eventStream, events);
    }



    @Handles("courtorders.command.remove-court-order")
    public void handleRemoveCourtRegister(final Envelope<RemoveCourtOrder> removeCourtOrderEnvelope) throws EventStreamException {
        final RemoveCourtOrder removeCourtOrder = removeCourtOrderEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(removeCourtOrder.getCourtOrderId());
        final CourtOrderAggregate courtOrderAggregate = aggregateService.get(eventStream, CourtOrderAggregate.class);
        final Stream<Object> events = courtOrderAggregate.removeCourtOrder(removeCourtOrder.getCourtOrderId());
        appendEventsToStream(removeCourtOrderEnvelope, eventStream, events);
    }

    @Handles("courtorders.command.update-court-order-validity")
    public void handleUpdateCourtRegister(final Envelope<UpdateCourtOrderValidity> updateCourtOrderEnvelope) throws EventStreamException {
        final UpdateCourtOrderValidity updateCourtOrderValidityPayload = updateCourtOrderEnvelope.payload();
        final Boolean resetToOriginalEndDate = updateCourtOrderValidityPayload.getResetToOriginalEndDate();
        final EventStream eventStream = eventSource.getStreamById(updateCourtOrderValidityPayload.getCourtOrderId());
        final CourtOrderAggregate courtOrderAggregate = aggregateService.get(eventStream, CourtOrderAggregate.class);
        final Stream<Object> events = (isNull(resetToOriginalEndDate) || !resetToOriginalEndDate) ? courtOrderAggregate.updateCourtOrder(updateCourtOrderValidityPayload.getCourtOrderId(), updateCourtOrderValidityPayload.getApplicationId(), updateCourtOrderValidityPayload.getNewEndDate())
                : courtOrderAggregate.resetCourtOrder(updateCourtOrderValidityPayload.getCourtOrderId(), updateCourtOrderValidityPayload.getApplicationId());
        appendEventsToStream(updateCourtOrderEnvelope, eventStream, events);
    }

    /**
     * This aims to update historical data and set correct expiry date and unpaidwork flag
     * @param patchUpdateJudicialChildResultsEnvelope
     * @throws EventStreamException
     */
    @Handles("courtorders.command.patch-update-judicial-child-results")
    public void handlePatchUpdateJudicialChildResults(final Envelope<PatchUpdateJudicialChildResults> patchUpdateJudicialChildResultsEnvelope) throws EventStreamException {
        final PatchUpdateJudicialChildResults payload = patchUpdateJudicialChildResultsEnvelope.payload();
        final List<UUID> courtOrderIds = payload.getCourtOrderIds();
        for (final UUID courtOrderId : courtOrderIds) {
            final EventStream eventStream = eventSource.getStreamById(courtOrderId);
            final CourtOrderAggregate courtOrderAggregate = aggregateService.get(eventStream, CourtOrderAggregate.class);
            final List<JudicialChildResults> judicialChildResults = getJudicialChildResults(patchUpdateJudicialChildResultsEnvelope.metadata(), courtOrderAggregate.getActualCourtOrder());
            if (!judicialChildResults.isEmpty()) {
                final boolean isUnpaidWorkCommunityOrd = isUnpaidWorkCommunityOrd(patchUpdateJudicialChildResultsEnvelope.metadata(), courtOrderAggregate.getActualCourtOrder());
                final Stream<Object> events = courtOrderAggregate.updateJudicialChildResult(courtOrderId, judicialChildResults, isUnpaidWorkCommunityOrd, covertToUUIDs(unpaidWorkJudicialTypeIds));
                appendEventsToStream(patchUpdateJudicialChildResultsEnvelope, eventStream, events);
            }
        }
    }

    /**
     * This aims to update historical data and set correct expiry date and unpaidwork flag
     * @param patchUpdateJudicialChildResultsEnvelope
     * @throws EventStreamException
     */
    @Handles("courtorders.command.patch-update-judicial-child-results-v2")
    public void handlePatchUpdateJudicialChildResultsV2(final Envelope<PatchUpdateJudicialChildResultsV2> patchUpdateJudicialChildResultsEnvelope) throws EventStreamException {
        final PatchUpdateJudicialChildResultsV2 payload = patchUpdateJudicialChildResultsEnvelope.payload();
        final List<UUID> courtOrderIds = payload.getCourtOrderIds();
        for (final UUID courtOrderId : courtOrderIds) {
            final EventStream eventStream = eventSource.getStreamById(courtOrderId);
            final CourtOrderAggregate courtOrderAggregate = aggregateService.get(eventStream, CourtOrderAggregate.class);
            final CreateCourtOrder actualCourtOrder = courtOrderAggregate.getActualCourtOrder();
            JsonObject response = progressionService.getJudicialChildResultsV2(patchUpdateJudicialChildResultsEnvelope.metadata(), actualCourtOrder.getOrderingHearingId(), actualCourtOrder.getMasterDefendantId(), actualCourtOrder.getJudicialResultTypeId());

            final List<JudicialChildResults> judicialChildResults = response.getJsonArray("judicialChildResults").stream()
                    .map(JsonValue::asJsonObject)
                    .map(judicialChildResult -> jsonObjectToObjectConverter.convert(judicialChildResult, JudicialChildResults.class))
                    .toList();
            final LocalDate latestEndDate= LocalDate.parse(response.getString("latestEndDate"));
            if (!judicialChildResults.isEmpty()) {
                final boolean isUnpaidWorkCommunityOrd = isUnpaidWorkCommunityOrd(patchUpdateJudicialChildResultsEnvelope.metadata(), courtOrderAggregate.getActualCourtOrder());
                final Stream<Object> events = courtOrderAggregate.updateJudicialChildResultV2(courtOrderId, judicialChildResults, isUnpaidWorkCommunityOrd, covertToUUIDs(unpaidWorkJudicialTypeIds), latestEndDate);
                appendEventsToStream(patchUpdateJudicialChildResultsEnvelope, eventStream, events);
            }
        }
    }

    private boolean isUnpaidWorkCommunityOrd(final Metadata metadata, final CreateCourtOrder createCourtOrder) {
        final JsonObject resultDefinition = referenceDataService.getResultDefinition(metadata, createCourtOrder.getJudicialResultTypeId());
        return resultDefinition.containsKey(UNPAID_WORK_EXTENSION_COMMUNITY_ORD_YRO) && resultDefinition.getBoolean(UNPAID_WORK_EXTENSION_COMMUNITY_ORD_YRO);
    }

    private List<JudicialChildResults> getJudicialChildResults(final Metadata metadata, final CreateCourtOrder actualCourtOrder) {
        JsonObject response = progressionService.getJudicialChildResults(metadata, actualCourtOrder.getOrderingHearingId(), actualCourtOrder.getMasterDefendantId(), actualCourtOrder.getJudicialResultTypeId());
        return response.getJsonArray("judicialChildResults").stream()
                .map(JsonValue::asJsonObject)
                .map(judicialChildResult -> jsonObjectToObjectConverter.convert(judicialChildResult, JudicialChildResults.class))
                .toList();
    }

    private List<UUID> covertToUUIDs(final String unpaidWorkJudicialTypeIds) {
        return Arrays.stream(unpaidWorkJudicialTypeIds.split(","))
                .map(UUID::fromString)
                .toList();
    }


}
