package duoc.cl.ms_classes.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TypeClassesTest {

    @Test
    void supportsDefaultAndExplicitConstructionAndAccessors() {
        TypeClasses empty = new TypeClasses();
        assertNull(empty.getId());
        assertNull(empty.getName());

        TypeClasses type = new TypeClasses(4L, "Yoga");
        assertEquals(4L, type.getId());
        assertEquals("Yoga", type.getName());

        type.setId(5L);
        type.setName("Pilates");
        assertEquals(5L, type.getId());
        assertEquals("Pilates", type.getName());
    }
}
