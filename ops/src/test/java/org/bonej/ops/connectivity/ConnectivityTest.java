package org.bonej.ops.connectivity;

import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import org.bonej.ops.testImageGenerators.WireFrameCuboidCreator;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Connectivity class
 *
 * @author Richard Domander
 */
public class ConnectivityTest {
    private static final ImageJ ij = new ImageJ();
    private static final double ERROR_MARGIN = 1E-12;

    @AfterClass
    public static void oneTimeTearDown() {
        ij.context().dispose();
    }

    /**
     * Use only to check connectivity density result has not changed.
     * @implNote Does not test deltaChi or connectivity
     */
    @Test
    public void regressionTestConnectivityDensity() {
        final int CUBOID_WIDTH = 10;
        final int CUBOID_HEIGHT = 10;
        final int CUBOID_DEPTH = 10;
        final int PADDING = 1;
        final int TOTAL_PADDING = 2 * PADDING;
        final int CUBOID_VOLUME =
                (CUBOID_WIDTH + TOTAL_PADDING) * (CUBOID_HEIGHT + TOTAL_PADDING) * (CUBOID_DEPTH + TOTAL_PADDING);

        final double ELEMENT_VOLUME = 1.0; //@todo figure how to set axis scale to cuboid
        final double EXPECTED_CONNECTIVITY = 5.0;
        final double EXPECTED_DENSITY = EXPECTED_CONNECTIVITY / (CUBOID_VOLUME * ELEMENT_VOLUME);

        Img<BitType> cuboid = (Img<BitType>) ij.op()
                .run(WireFrameCuboidCreator.class, null, CUBOID_WIDTH, CUBOID_HEIGHT, CUBOID_DEPTH, PADDING);

        Connectivity.Characteristics results = (Connectivity.Characteristics) ij.op().run(Connectivity.class, cuboid);
        assertEquals(-4.0, results.eulerCharacteristic, ERROR_MARGIN);
        assertEquals(-4.0, results.deltaChi, ERROR_MARGIN);
        assertEquals(EXPECTED_CONNECTIVITY, results.connectivity, ERROR_MARGIN);
        assertEquals(EXPECTED_DENSITY, results.connectivityDensity, ERROR_MARGIN);
    }
}