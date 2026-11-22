package duoc.cl.ms_classes.repository;

import duoc.cl.ms_classes.model.Classe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ClasseRepository extends JpaRepository<Classe, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE Classe c SET c.spotsAvailable = c.spotsAvailable - 1 "
            + "WHERE c.id = :id AND c.spotsAvailable > 0")
    int reserveSpot(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Classe c SET c.spotsAvailable = c.spotsAvailable + 1 "
            + "WHERE c.id = :id AND c.spotsAvailable < c.maximumCapacity")
    int releaseSpot(@Param("id") Long id);
}
