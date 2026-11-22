package duoc.cl.ms_classes.repository;

import duoc.cl.ms_classes.model.Classe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClasseRepositoryCapacityTest {

    @Autowired
    private ClasseRepository repository;

    @Test
    void reserveSpotAtomicallyStopsAtZero() {
        Classe classe = new Classe();
        classe.setMaximumCapacity(2);
        classe.setSpotsAvailable(1);
        Long id = repository.saveAndFlush(classe).getId();

        assertThat(repository.reserveSpot(id)).isEqualTo(1);
        assertThat(repository.reserveSpot(id)).isZero();
        assertThat(repository.findById(id).orElseThrow().getSpotsAvailable()).isZero();
    }

    @Test
    void releaseSpotNeverExceedsMaximumCapacity() {
        Classe classe = new Classe();
        classe.setMaximumCapacity(2);
        classe.setSpotsAvailable(1);
        Long id = repository.saveAndFlush(classe).getId();

        assertThat(repository.releaseSpot(id)).isEqualTo(1);
        assertThat(repository.releaseSpot(id)).isZero();
        assertThat(repository.findById(id).orElseThrow().getSpotsAvailable()).isEqualTo(2);
    }
}
