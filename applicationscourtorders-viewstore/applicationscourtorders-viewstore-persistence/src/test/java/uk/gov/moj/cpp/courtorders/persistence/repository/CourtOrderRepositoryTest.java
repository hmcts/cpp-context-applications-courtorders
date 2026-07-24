package uk.gov.moj.cpp.courtorders.persistence.repository;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.courtorders.persistence.entity.CourtOrderEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class CourtOrderRepositoryTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private CourtOrderRepository courtOrderRepository;

    @BeforeEach
    public void createRepositoryWithMockedEntityManager() {
        courtOrderRepository = new CourtOrderRepository();
        courtOrderRepository.entityManager = entityManager;
    }

    @Test
    public void shouldSaveByMergingTheEntity() {
        final CourtOrderEntity entity = mock(CourtOrderEntity.class);
        final CourtOrderEntity merged = mock(CourtOrderEntity.class);
        when(entityManager.merge(entity)).thenReturn(merged);

        assertThat(courtOrderRepository.save(entity), is(merged));

        verify(entityManager).merge(entity);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFindByDefendantIdAndExpiryDateExcludingRemoved() {
        final TypedQuery<CourtOrderEntity> query = mock(TypedQuery.class);
        final ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
        final List<CourtOrderEntity> expected = singletonList(mock(CourtOrderEntity.class));

        when(entityManager.createQuery(jpqlCaptor.capture(), eq(CourtOrderEntity.class))).thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(expected);

        final UUID defendantId = randomUUID();
        final LocalDate expiryDate = LocalDate.of(2026, 7, 28);

        assertThat(courtOrderRepository.findByDefendantIdAndExpiryDate(defendantId, expiryDate), is(expected));

        assertThat(jpqlCaptor.getValue(), is("SELECT c FROM CourtOrderEntity c WHERE c.defendantId = :defendantId AND c.isRemoved = false AND c.expiryDate >= :expiryDate"));
        verify(query).setParameter("defendantId", defendantId);
        verify(query).setParameter("expiryDate", expiryDate);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFindByHearingDefendantIdAndSittingDateExcludingRemoved() {
        final TypedQuery<CourtOrderEntity> query = mock(TypedQuery.class);
        final ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
        final List<CourtOrderEntity> expected = singletonList(mock(CourtOrderEntity.class));

        when(entityManager.createQuery(jpqlCaptor.capture(), eq(CourtOrderEntity.class))).thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(expected);

        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final LocalDate sittingDate = LocalDate.of(2026, 7, 28);

        assertThat(courtOrderRepository.findByHearingDefendantIdAndSittingDate(defendantId, hearingId, sittingDate), is(expected));

        assertThat(jpqlCaptor.getValue(), is("SELECT c FROM CourtOrderEntity c WHERE c.defendantId = :defendantId AND c.hearingId = :hearingId AND c.sittingDate = :sittingDate AND c.isRemoved = false"));
        verify(query).setParameter("defendantId", defendantId);
        verify(query).setParameter("hearingId", hearingId);
        verify(query).setParameter("sittingDate", sittingDate);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFindByCourtOrderIdExcludingRemoved() {
        final TypedQuery<CourtOrderEntity> query = mock(TypedQuery.class);
        final ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
        final List<CourtOrderEntity> expected = singletonList(mock(CourtOrderEntity.class));

        when(entityManager.createQuery(jpqlCaptor.capture(), eq(CourtOrderEntity.class))).thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(expected);

        final UUID courtOrderId = randomUUID();

        assertThat(courtOrderRepository.findByCourtOrderIdNotRemoved(courtOrderId), is(expected));

        assertThat(jpqlCaptor.getValue(), is("SELECT c FROM CourtOrderEntity c WHERE c.courtOrderId = :courtOrderId AND c.isRemoved = false"));
        verify(query).setParameter("courtOrderId", courtOrderId);
    }

    @Test
    public void shouldFindByCaseAndDefendantIdUsingNativeQuery() {
        final Query nativeQuery = mock(Query.class);
        final ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        final List<CourtOrderEntity> expected = singletonList(mock(CourtOrderEntity.class));

        when(entityManager.createNativeQuery(sqlCaptor.capture(), eq(CourtOrderEntity.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(any(String.class), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(expected);

        final String caseId = "case-1234";
        final UUID defendantId = randomUUID();

        assertThat(courtOrderRepository.findByCaseAndDefendantId(caseId, defendantId), is(expected));

        assertThat(sqlCaptor.getValue(), is("select * from court_order c, json_array_elements(payload::json ->'courtOrderOffences') courtOffence where courtOffence ->>'prosecutionCaseId' = :caseId and defendant_id = :defendantId and is_removed is false"));
        verify(nativeQuery).setParameter("caseId", caseId);
        verify(nativeQuery).setParameter("defendantId", defendantId);
    }
}
