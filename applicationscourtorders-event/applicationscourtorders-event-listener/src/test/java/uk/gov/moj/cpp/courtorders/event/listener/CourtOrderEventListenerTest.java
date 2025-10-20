package uk.gov.moj.cpp.courtorders.event.listener;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.CourtOrderRemoved;
import uk.gov.justice.core.courts.CourtOrderRequested;
import uk.gov.justice.core.courts.CourtOrderValidityUpdated;
import uk.gov.justice.core.courts.JudicialChildResults;
import uk.gov.justice.core.courts.JudicialChildResultsUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.courtorders.command.CreateCourtOrder;
import uk.gov.moj.cpp.courtorders.persistence.entity.CourtOrderEntity;
import uk.gov.moj.cpp.courtorders.persistence.repository.CourtOrderRepository;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class CourtOrderEventListenerTest {

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CourtOrderRepository courtOrderRepository;

    @InjectMocks
    private CourtOrderEventListener courtOrderEventListener;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void removeCourtOrder() {
        final UUID courtOrderId = UUID.randomUUID();
        final CourtOrderRemoved courtOrderRemoved = CourtOrderRemoved.courtOrderRemoved().withCourtOrderId(courtOrderId).build();
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        courtOrderEntity.setCourtOrderId(courtOrderId);
        courtOrderEntity.setRemoved(false);
        when(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId)).thenReturn(Lists.newArrayList(courtOrderEntity));
        courtOrderEventListener.removeCourtOrder(envelopeFrom(metadataWithRandomUUID("courtorders.event.court-order-removed"),
                objectToJsonObjectConverter.convert(courtOrderRemoved)));
        assertTrue(courtOrderEntity.isRemoved());
        verify(courtOrderRepository).save(courtOrderEntity);
    }

    @Test
    public void removeCourtOrderWhenThereIsNoCourtOrderInDb() {
        final UUID courtOrderId = UUID.randomUUID();
        final CourtOrderRemoved courtOrderRemoved = CourtOrderRemoved.courtOrderRemoved().withCourtOrderId(courtOrderId).build();
        when(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId)).thenReturn(Collections.emptyList());
        courtOrderEventListener.removeCourtOrder(envelopeFrom(metadataWithRandomUUID("courtorders.event.court-order-removed"),
                objectToJsonObjectConverter.convert(courtOrderRemoved)));

        verify(courtOrderRepository, never()).save(any());
    }

    @Test
    public void saveCourtOrder() {
        final UUID courtOrderId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final LocalDate orderDate = LocalDate.now();
        final CreateCourtOrder courtOrder = CreateCourtOrder.createCourtOrder().withId(courtOrderId).withOrderingHearingId(hearingId)
                .withMasterDefendantId(defendantId).withOrderDate(orderDate).build();
        final CourtOrderRequested courtOrderRemoved = CourtOrderRequested.courtOrderRequested().withCourtOrderId(courtOrderId)
                .withCourtOrder(courtOrder).build();
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        courtOrderEntity.setCourtOrderId(courtOrderId);
        courtOrderEntity.setRemoved(false);
        when(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId)).thenReturn(Lists.newArrayList(courtOrderEntity));
        courtOrderEventListener.saveCourtOrder(envelopeFrom(metadataWithRandomUUID("courtorders.event.court-order-requested"),
                objectToJsonObjectConverter.convert(courtOrderRemoved)));
        assertFalse(courtOrderEntity.isRemoved());
        assertThat(stringToJsonObjectConverter.convert(courtOrderEntity.getPayload()), is(objectToJsonObjectConverter.convert(courtOrder)));
        verify(courtOrderRepository).save(courtOrderEntity);
    }

    @Test
    public void saveCourtOrderForFirstTime() {
        final UUID courtOrderId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final LocalDate orderDate = LocalDate.now();
        final LocalDate expiryDate = LocalDate.now().plusYears(1);
        final CreateCourtOrder courtOrder = CreateCourtOrder.createCourtOrder().withId(courtOrderId).withOrderingHearingId(hearingId)
                .withMasterDefendantId(defendantId).withOrderDate(orderDate).withExpiryDate(expiryDate).build();
        final CourtOrderRequested courtOrderRemoved = CourtOrderRequested.courtOrderRequested().withCourtOrderId(courtOrderId)
                .withCourtOrder(courtOrder).build();
        ArgumentCaptor<CourtOrderEntity> entityArgumentCaptor = ArgumentCaptor.forClass(CourtOrderEntity.class);
        when(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId)).thenReturn(Lists.newArrayList());
        courtOrderEventListener.saveCourtOrder(envelopeFrom(metadataWithRandomUUID("courtorders.event.court-order-requested"),
                objectToJsonObjectConverter.convert(courtOrderRemoved)));
        verify(courtOrderRepository).save(entityArgumentCaptor.capture());
        final CourtOrderEntity courtOrderEntity = entityArgumentCaptor.getValue();
        assertThat(courtOrderEntity.isRemoved(), is(false));
        assertThat(courtOrderEntity.getCourtOrderId(), is(courtOrderId));
        assertThat(courtOrderEntity.getHearingId(), is(hearingId));
        assertThat(courtOrderEntity.getSittingDate(), is(orderDate));
        assertThat(courtOrderEntity.getExpiryDate(), is(expiryDate));
        assertThat(stringToJsonObjectConverter.convert(courtOrderEntity.getPayload()), is(objectToJsonObjectConverter.convert(courtOrder)));
    }

    @Test
    void shouldSaveCourtOrderWhenExpiryDateIsNull() {
        final UUID courtOrderId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final LocalDate orderDate = LocalDate.now();
        final LocalDate endDate = LocalDate.now().plusYears(1);
        final CreateCourtOrder courtOrder = CreateCourtOrder.createCourtOrder().withId(courtOrderId).withOrderingHearingId(hearingId)
                .withMasterDefendantId(defendantId).withOrderDate(orderDate).withEndDate(endDate).build();
        final CourtOrderRequested courtOrderRemoved = CourtOrderRequested.courtOrderRequested().withCourtOrderId(courtOrderId)
                .withCourtOrder(courtOrder).build();
        ArgumentCaptor<CourtOrderEntity> entityArgumentCaptor = ArgumentCaptor.forClass(CourtOrderEntity.class);
        when(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId)).thenReturn(Lists.newArrayList());
        courtOrderEventListener.saveCourtOrder(envelopeFrom(metadataWithRandomUUID("courtorders.event.court-order-requested"),
                objectToJsonObjectConverter.convert(courtOrderRemoved)));
        verify(courtOrderRepository).save(entityArgumentCaptor.capture());
        final CourtOrderEntity courtOrderEntity = entityArgumentCaptor.getValue();
        assertThat(courtOrderEntity.isRemoved(), is(false));
        assertThat(courtOrderEntity.getCourtOrderId(), is(courtOrderId));
        assertThat(courtOrderEntity.getExpiryDate(), is(endDate));
        assertThat(stringToJsonObjectConverter.convert(courtOrderEntity.getPayload()), is(objectToJsonObjectConverter.convert(courtOrder)));
    }

    @Test
    public void updateCourtOrder() {
        final UUID courtOrderId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final LocalDate newEndDate = LocalDate.now();
        final LocalDate expiryDate = LocalDate.now().plusYears(1);
        final CourtOrderValidityUpdated courtOrderValidityUpdated = CourtOrderValidityUpdated.courtOrderValidityUpdated()
                .withCourtOrderId(courtOrderId)
                .withApplicationId(applicationId)
                .withNewEndDate(newEndDate)
                .withExpiryDate(expiryDate)
                .build();
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        courtOrderEntity.setCourtOrderId(courtOrderId);
        courtOrderEntity.setRemoved(false);
        courtOrderEntity.setPayload(getPayload("json/court-order-payload.json"));
        when(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId)).thenReturn(Lists.newArrayList(courtOrderEntity));
        courtOrderEventListener.updateCourtOrder(envelopeFrom(metadataWithRandomUUID("applicationscourtorders.event.court-order-validity-updated"),
                objectToJsonObjectConverter.convert(courtOrderValidityUpdated)));
        ArgumentCaptor<CourtOrderEntity> entityArgumentCaptor = ArgumentCaptor.forClass(CourtOrderEntity.class);
        verify(courtOrderRepository).save(entityArgumentCaptor.capture());
        final CourtOrderEntity updatedCourtOrderEntity = entityArgumentCaptor.getValue();
        assertThat(updatedCourtOrderEntity.getExpiryDate(), is(expiryDate));
        assertThat(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getString("endDate"), is(LocalDate.now().toString()));
        assertThat(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getString("expiryDate"), is(expiryDate.toString()));
    }

    @Test
    void updateCourtOrderWhenExpiryDateIsNull() {
        final UUID courtOrderId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final LocalDate newEndDate = LocalDate.now();
        final CourtOrderValidityUpdated courtOrderValidityUpdated = CourtOrderValidityUpdated.courtOrderValidityUpdated()
                .withCourtOrderId(courtOrderId)
                .withApplicationId(applicationId)
                .withNewEndDate(newEndDate)
                .build();
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        courtOrderEntity.setCourtOrderId(courtOrderId);
        courtOrderEntity.setRemoved(false);
        courtOrderEntity.setPayload(getPayload("json/court-order-payload.json"));
        when(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId)).thenReturn(Lists.newArrayList(courtOrderEntity));
        courtOrderEventListener.updateCourtOrder(envelopeFrom(metadataWithRandomUUID("applicationscourtorders.event.court-order-validity-updated"),
                objectToJsonObjectConverter.convert(courtOrderValidityUpdated)));
        ArgumentCaptor<CourtOrderEntity> entityArgumentCaptor = ArgumentCaptor.forClass(CourtOrderEntity.class);
        verify(courtOrderRepository).save(entityArgumentCaptor.capture());
        final CourtOrderEntity updatedCourtOrderEntity = entityArgumentCaptor.getValue();
        assertThat(updatedCourtOrderEntity.getExpiryDate(), is(newEndDate));
        assertThat(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getString("endDate"), is(newEndDate.toString()));
        assertThat(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getString("expiryDate"), is(newEndDate.toString()));
    }

    @Test
    public void resetCourtOrder() {
        final UUID courtOrderId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final LocalDate newEndDate = LocalDate.now();
        final CourtOrderValidityUpdated courtOrderValidityUpdated = CourtOrderValidityUpdated.courtOrderValidityUpdated()
                .withCourtOrderId(courtOrderId)
                .withApplicationId(applicationId)
                .withNewEndDate(newEndDate)
                .build();
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        courtOrderEntity.setCourtOrderId(courtOrderId);
        courtOrderEntity.setRemoved(false);
        courtOrderEntity.setPayload(getPayload("json/court-order-payload.json"));
        when(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId)).thenReturn(Lists.newArrayList(courtOrderEntity));
        courtOrderEventListener.updateCourtOrder(envelopeFrom(metadataWithRandomUUID("applicationscourtorders.event.court-order-validity-updated"),
                objectToJsonObjectConverter.convert(courtOrderValidityUpdated)));
        ArgumentCaptor<CourtOrderEntity> entityArgumentCaptor = ArgumentCaptor.forClass(CourtOrderEntity.class);
        verify(courtOrderRepository).save(entityArgumentCaptor.capture());
        final CourtOrderEntity updatedCourtOrderEntity = entityArgumentCaptor.getValue();
        assertThat(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getString("endDate"), is(LocalDate.now().toString()));
    }

    @Test
    void shouldUpdateJudicialChildResults() {
        final UUID courtOrderId = UUID.randomUUID();
        final LocalDate expiryDate = LocalDate.now().plusYears(1);
        final UUID judicialResultId = UUID.randomUUID();
        final UUID judicialResultTypeId = UUID.randomUUID();
        final List<JudicialChildResults> judicialChildResults = singletonList(JudicialChildResults.judicialChildResults()
                .withJudicialResultId(judicialResultId)
                .withJudicialResultTypeId(judicialResultTypeId)
                .build());
        final JudicialChildResultsUpdated judicialChildResultsUpdated = JudicialChildResultsUpdated.judicialChildResultsUpdated()
                .withCourtOrderId(courtOrderId)
                .withJudicialChildResults(judicialChildResults)
                .withExpiryDate(expiryDate)
                .withIsUnpaidWork(true)
                .build();
        final CourtOrderEntity courtOrderEntity = new CourtOrderEntity();
        courtOrderEntity.setCourtOrderId(courtOrderId);
        courtOrderEntity.setRemoved(false);
        courtOrderEntity.setPayload(getPayload("json/court-order-payload.json"));
        when(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId)).thenReturn(Lists.newArrayList(courtOrderEntity));

        courtOrderEventListener.updateJudicialChildResults(envelopeFrom(metadataWithRandomUUID("applicationscourtorders.event.judicial-child-results-updated"),
                objectToJsonObjectConverter.convert(judicialChildResultsUpdated)));

        ArgumentCaptor<CourtOrderEntity> entityArgumentCaptor = ArgumentCaptor.forClass(CourtOrderEntity.class);
        verify(courtOrderRepository).save(entityArgumentCaptor.capture());

        final CourtOrderEntity updatedCourtOrderEntity = entityArgumentCaptor.getValue();
        assertThat(updatedCourtOrderEntity.getExpiryDate(), is(expiryDate));
        assertThat(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getString("expiryDate"), is(expiryDate.toString()));
        assertThat(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getBoolean("isUnpaidWork"), is(true));
        assertThat(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getJsonArray("judicialChildResults").size(), is(1));
        assertThat(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getJsonArray("judicialChildResults").get(0).asJsonObject(), JudicialChildResults.class).getJudicialResultTypeId(), is(judicialResultTypeId));
        assertThat(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedCourtOrderEntity.getPayload()).getJsonArray("judicialChildResults").get(0).asJsonObject(), JudicialChildResults.class).getJudicialResultId(), is(judicialResultId));
    }

    private static String getPayload(final String path) {
        String request = null;
        try {
            final InputStream inputStream = CourtOrderEventListenerTest.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }
}
