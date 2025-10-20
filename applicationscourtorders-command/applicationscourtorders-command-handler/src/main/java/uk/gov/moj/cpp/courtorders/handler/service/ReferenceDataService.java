package uk.gov.moj.cpp.courtorders.handler.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    private static final String REFERENCEDATA_GET_RESULT_DEFINITION = "referencedata.get-result-definition";
    private static final String RESULT_DEFINITION_ID = "resultDefinitionId";


    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    public JsonObject getResultDefinition(final Metadata metadata, final UUID resultDefinitionId) {
        final JsonObject payload = createObjectBuilder().add(RESULT_DEFINITION_ID, resultDefinitionId.toString()).build();

        final JsonEnvelope jsonEnvelope = requester.request(envelopeFrom(metadataFrom(metadata).withName(REFERENCEDATA_GET_RESULT_DEFINITION), payload));

        return jsonEnvelope.payloadAsJsonObject();
    }
}
