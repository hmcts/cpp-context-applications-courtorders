package uk.gov.moj.cpp.courtorders.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class CourtOrderRequestApi {

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("courtorders.create-court-order")
    public void createCourtOrderRequest(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("courtorders.command.create-court-order").build(),
                command.payloadAsJsonObject()));
    }


    @Handles("courtorders.remove-court-order")
    public void removeCourtOrderRequest(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("courtorders.command.remove-court-order").build(),
                command.payloadAsJsonObject()));
    }

    @Handles("courtorders.update-court-order-validity")
    public void updateCourtOrderRequest(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("courtorders.command.update-court-order-validity").build(),
                command.payloadAsJsonObject()));
    }

    @Handles("courtorders.patch-update-judicial-child-results")
    public void patchUpdateJudicialChildResultsRequest(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("courtorders.command.patch-update-judicial-child-results").build(),
                command.payloadAsJsonObject()));
    }

    @Handles("courtorders.patch-update-judicial-child-results-v2")
    public void patchUpdateJudicialChildResultsRequestV2(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("courtorders.command.patch-update-judicial-child-results-v2").build(),
                command.payloadAsJsonObject()));
    }

}
