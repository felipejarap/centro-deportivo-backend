package duoc.cl.ms_plans.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter @Getter @AllArgsConstructor @NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Plans {
    public Plans(Long id, String name, double price, int durationDays) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.durationDays = durationDays;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double price;
    @Column(name = "duration_days")
    private int durationDays;
    private String period;
    @Column(name = "classes_per_month")
    private String classesPerMonth;
    @Column(columnDefinition = "TEXT")
    private String benefits;
    private boolean featured;
    private boolean active = true;
}
