package cl.duoc.MS_Usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name= "id_user")
    private Long idUser;

    @NotBlank
    @Column(unique = true,nullable = false)
    private String username;

    @Column(name = "paternal_surname")
    private String paternalSurname;

    @Column(name ="maternal_surname")
    private String maternalSurname;

    @Email
    @NotBlank
    @Column(nullable = false)
    private String email;

    @Column(name ="phone")
    private String phone;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String rut;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "plan_id")
    private Long planId;

    private String specialty;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(columnDefinition = "TEXT")
    private String avatar;

    @Column(name = "registration_date")
    private java.time.LocalDate registrationDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_user", referencedColumnName = "id")
    private TypeUser typeUser;



}
