package uk.gov.moj.cpp.courtorders.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "court_order")
public class CourtOrderEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "court_order_id")
    private UUID courtOrderId;

    @Column(name = "defendant_id")
    private UUID defendantId;


    @Column(name = "hearing_id")
    private UUID hearingId;

    @Column(name = "sitting_date")
    private LocalDate sittingDate;

    @Column(name = "payload")
    private String payload;

    @Column(name = "is_removed")
    private boolean isRemoved;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(UUID hearingId) {
        this.hearingId = hearingId;
    }

    public LocalDate getSittingDate() {
        return sittingDate;
    }

    public void setSittingDate(LocalDate sittingDate) {
        this.sittingDate = sittingDate;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public UUID getCourtOrderId() {
        return courtOrderId;
    }

    public void setCourtOrderId(UUID courtOrderId) {
        this.courtOrderId = courtOrderId;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(final LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }


}
