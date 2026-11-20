package cl.duoc.tacticaltrack.backend;

import jakarta.persistence.*;

/** Registro de visita y estado de revisión del supervisor. */
@Entity
@Table(name = "visits")
public class Visit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String supervisorId;
    public String operatorName;
    public String operatorRole;
    public String pointId;
    public String pointName;
    public String assetId;
    public String assetCode;
    public String assetType;
    public String visitType;
    public String equipmentStatus;
    @Column(length = 2000) public String observations;
    public double latitude;
    public double longitude;
    public Float locationAccuracyMeters;
    public float distanceToPointMeters;
    public boolean presenceVerified;
    public long timestamp;
    public String supervisorStatus = "Pendiente";
    @Lob public String evidenceBase64;
    @Column(unique = true, nullable = false) public String clientRequestId;
}
