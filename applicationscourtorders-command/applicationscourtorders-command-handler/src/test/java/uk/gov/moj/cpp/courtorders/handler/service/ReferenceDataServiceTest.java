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
class ReferenceDataServiceTest {

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;


    @Test
    void shouldGetResultDefinition() {
        final JsonObject jsonObject = Json.createObjectBuilder().add("id", randomUUID().toString()).build();
        final UUID resultDefinitionId = randomUUID();
        final Metadata metadata = metadataBuilder().withId(randomUUID()).withName("ids").build();
        ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(requester.request(any())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);

        final JsonObject result = referenceDataService.getResultDefinition(metadata, resultDefinitionId);
        assertThat(result.equals(jsonObject), is(true));

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope argumentCaptorValue = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(argumentCaptorValue.metadata().id(), is(metadata.id()));
        assertThat(argumentCaptorValue.metadata().name(), is("referencedata.get-result-definition"));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("resultDefinitionId"), is(resultDefinitionId.toString()));
    }

}
