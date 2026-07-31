package uk.gov.moj.cpp.courtorders.persistence.repository;

import uk.gov.moj.cpp.courtorders.persistence.entity.CourtOrderEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * JPA repository for {@link CourtOrderEntity}.
 *
 * <p>Migrated from a DeltaSpike Data {@code @Repository} interface to a concrete JPA repository as part of the
 * Java 25 / Jakarta EE 11 upgrade — DeltaSpike is EOL and unavailable under CDI 4. The JPQL/native queries below
 * preserve the exact semantics of the previous {@code @Query}-annotated methods.</p>
 */
@ApplicationScoped
public class CourtOrderRepository {

    @PersistenceContext(unitName = "applicationscourtorders-persistence-unit")
    EntityManager entityManager;

    public CourtOrderEntity save(final CourtOrderEntity entity) {
        return entityManager.merge(entity);
    }

    public List<CourtOrderEntity> findByDefendantIdAndExpiryDate(final UUID defendantId, final LocalDate expiryDate) {
        return entityManager.createQuery(
                        "SELECT c FROM CourtOrderEntity c WHERE c.defendantId = :defendantId AND c.isRemoved = false AND c.expiryDate >= :expiryDate",
                        CourtOrderEntity.class)
                .setParameter("defendantId", defendantId)
                .setParameter("expiryDate", expiryDate)
                .getResultList();
    }

    public List<CourtOrderEntity> findByHearingDefendantIdAndSittingDate(final UUID defendantId,
                                                                         final UUID hearingId,
                                                                         final LocalDate sittingDate) {
        return entityManager.createQuery(
                        "SELECT c FROM CourtOrderEntity c WHERE c.defendantId = :defendantId AND c.hearingId = :hearingId AND c.sittingDate = :sittingDate AND c.isRemoved = false",
                        CourtOrderEntity.class)
                .setParameter("defendantId", defendantId)
                .setParameter("hearingId", hearingId)
                .setParameter("sittingDate", sittingDate)
                .getResultList();
    }

    public List<CourtOrderEntity> findByCourtOrderIdNotRemoved(final UUID courtOrderId) {
        return entityManager.createQuery(
                        "SELECT c FROM CourtOrderEntity c WHERE c.courtOrderId = :courtOrderId AND c.isRemoved = false",
                        CourtOrderEntity.class)
                .setParameter("courtOrderId", courtOrderId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<CourtOrderEntity> findByCaseAndDefendantId(final String caseId, final UUID defendantId) {
        return entityManager.createNativeQuery(
                        "select * from court_order c, json_array_elements(payload::json ->'courtOrderOffences') courtOffence where courtOffence ->>'prosecutionCaseId' = :caseId and defendant_id = :defendantId and is_removed is false",
                        CourtOrderEntity.class)
                .setParameter("caseId", caseId)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }
}
