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

public class ProgressionService {

    private static final String PROGRESSION_QUERY_JUDICIAL_CHILD_RESULTS = "progression.query.judicial-child-results";
    private static final String PROGRESSION_QUERY_JUDICIAL_CHILD_RESULTS_V2 = "progression.query.judicial-child-results-v2";
    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    public JsonObject getJudicialChildResults(final Metadata metadata, final UUID hearingId, final UUID masterDefendantId, final UUID judicialResultTypeId ) {
        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("masterDefendantId", masterDefendantId.toString())
                .add("judicialResultTypeId", judicialResultTypeId.toString())
                .build();

        final JsonEnvelope jsonEnvelope = requester.request(envelopeFrom(metadataFrom(metadata).withName(PROGRESSION_QUERY_JUDICIAL_CHILD_RESULTS), payload));

        return jsonEnvelope.payloadAsJsonObject();
    }

    public JsonObject getJudicialChildResultsV2(final Metadata metadata, final UUID hearingId, final UUID masterDefendantId, final UUID judicialResultTypeId ) {
        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("masterDefendantId", masterDefendantId.toString())
                .add("judicialResultTypeId", judicialResultTypeId.toString())
                .build();

        final JsonEnvelope jsonEnvelope = requester.request(envelopeFrom(metadataFrom(metadata).withName(PROGRESSION_QUERY_JUDICIAL_CHILD_RESULTS_V2), payload));

        return jsonEnvelope.payloadAsJsonObject();
    }

}
