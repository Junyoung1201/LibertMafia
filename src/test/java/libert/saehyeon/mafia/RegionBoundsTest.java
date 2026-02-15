package libert.saehyeon.mafia;

import libert.saehyeon.mafia.region.RegionBounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionBoundsTest {

    @Test
    void fromBuildsMinMaxCorrectly() {
        RegionBounds bounds = RegionBounds.from(10, 5, -3, 2, 9, -7);

        assertEquals(2, bounds.getMinX());
        assertEquals(10, bounds.getMaxX());
        assertEquals(5, bounds.getMinY());
        assertEquals(9, bounds.getMaxY());
        assertEquals(-7, bounds.getMinZ());
        assertEquals(-3, bounds.getMaxZ());
    }
}

