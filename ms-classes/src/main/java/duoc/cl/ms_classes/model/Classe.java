package duoc.cl.ms_classes.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Classe {

    public Classe(Long id, LocalDateTime startDate, LocalDateTime endDate, Integer maximumCapacity,
                  Integer spotsAvailable, TypeClasses typeClasse) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maximumCapacity = maximumCapacity;
        this.spotsAvailable = spotsAvailable;
        this.typeClasse = typeClasse;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "start_date", columnDefinition = "DATETIME")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;
    @Column(name = "end_date", columnDefinition = "DATETIME")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;
    @Column(name = "maximum_capacity")
    private Integer maximumCapacity;
    @Column(name = "spots_available")
    private Integer spotsAvailable;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private Long coachId;
    private String coachName;
    private String intensity;
    private String room;
    @Column(columnDefinition = "TEXT")
    private String image;
    private boolean active = true;
    @ManyToOne
    @JoinColumn(name = "id_type_classe", referencedColumnName = "id")
    private TypeClasses typeClasse;
}
