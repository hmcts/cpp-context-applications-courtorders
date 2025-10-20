package uk.gov.moj.cpp.courtorders.handler.service;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressionServiceTest {

    @InjectMocks
    private ProgressionService progressionService;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Test
    void shouldGetJudicialChildResults() {
        final JsonObject jsonObject = Json.createObjectBuilder().add("id", randomUUID().toString()).build();
        final UUID hearingId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID judicialResultTypeId = randomUUID();
        final Metadata metadata = metadataBuilder().withId(randomUUID()).withName("ids").build();
        ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(requester.request(any())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);

        final JsonObject result = progressionService.getJudicialChildResults(metadata, hearingId, masterDefendantId, judicialResultTypeId);
        assertThat(result.equals(jsonObject), is(true));

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope argumentCaptorValue = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(argumentCaptorValue.metadata().id(), is(metadata.id()));
        assertThat(argumentCaptorValue.metadata().name(), is("progression.query.judicial-child-results"));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("masterDefendantId"), is(masterDefendantId.toString()));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("judicialResultTypeId"), is(judicialResultTypeId.toString()));
    }


    @Test
    void shouldGetJudicialChildResultsV2() {
        final JsonObject jsonObject = Json.createObjectBuilder().add("id", randomUUID().toString()).build();
        final UUID hearingId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID judicialResultTypeId = randomUUID();
        final Metadata metadata = metadataBuilder().withId(randomUUID()).withName("ids").build();
        ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(requester.request(any())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);

        final JsonObject result = progressionService.getJudicialChildResultsV2(metadata, hearingId, masterDefendantId, judicialResultTypeId);
        assertThat(result.equals(jsonObject), is(true));

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope argumentCaptorValue = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(argumentCaptorValue.metadata().id(), is(metadata.id()));
        assertThat(argumentCaptorValue.metadata().name(), is("progression.query.judicial-child-results-v2"));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("masterDefendantId"), is(masterDefendantId.toString()));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("judicialResultTypeId"), is(judicialResultTypeId.toString()));
    }

}
