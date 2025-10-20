package uk.gov.moj.cpp.courtorders.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.courtorders.persistence.entity.CourtOrderEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CourtOrderRepository extends EntityRepository<CourtOrderEntity, UUID> {

    @Query("FROM CourtOrderEntity where defendantId=:defendantId and isRemoved is false and expiryDate > :expiryDate ")
    List<CourtOrderEntity> findByDefendantIdAndExpiryDate(@QueryParam("defendantId") final UUID defendantId, @QueryParam("expiryDate") final LocalDate expiryDate);


    @Query("FROM CourtOrderEntity where defendantId=:defendantId and hearingId=:hearingId and sittingDate=:sittingDate and isRemoved is false")
    List<CourtOrderEntity> findByHearingDefendantIdAndSittingDate(@QueryParam("defendantId") final UUID defendantId,
                                                                  @QueryParam("hearingId") final UUID hearingId,
                                                                  @QueryParam("sittingDate") final LocalDate sittingDate);

    @Query("FROM CourtOrderEntity where courtOrderId=:courtOrderId and isRemoved is false")
    List<CourtOrderEntity> findByCourtOrderIdNotRemoved(@QueryParam("courtOrderId") final UUID courtOrderId);

    @Query(value = "select * from court_order c, json_array_elements(payload\\:\\:json ->'courtOrderOffences') courtOffence where courtOffence ->>'prosecutionCaseId' = :caseId " +
            "and defendant_id = :defendantId and is_removed is false", isNative = true)
    List<CourtOrderEntity> findByCaseAndDefendantId(@QueryParam("caseId") final String caseId, @QueryParam("defendantId") final UUID defendantId);
}
