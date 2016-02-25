package org.bonej.ops.connectivity;

import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.type.logic.BitType;
import org.bonej.ops.testImageGenerators.WireFrameCuboidCreator;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Connectivity class
 *
 * @author Richard Domander
 * @todo write a tests with images that touche the interval boundaries to properly test deltaChi
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
     *
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

        final double[] CALIBRATION = {0.2, 0.2, 0.2};
        final double ELEMENT_VOLUME = Arrays.stream(CALIBRATION).reduce((i, j) -> i * j).getAsDouble();
        final double EXPECTED_CONNECTIVITY = 5.0;
        final double EXPECTED_DENSITY = EXPECTED_CONNECTIVITY / (CUBOID_VOLUME * ELEMENT_VOLUME);

        ImgPlus<BitType> cuboid = (ImgPlus<BitType>) ij.op()
                .run(WireFrameCuboidCreator.class, null, CUBOID_WIDTH, CUBOID_HEIGHT, CUBOID_DEPTH, PADDING,
                        CALIBRATION);

        Connectivity.Characteristics results = (Connectivity.Characteristics) ij.op().run(Connectivity.class, cuboid);
        assertEquals(-4.0, results.eulerCharacteristic, ERROR_MARGIN);
        assertEquals(-4.0, results.deltaChi, ERROR_MARGIN);
        assertEquals(EXPECTED_CONNECTIVITY, results.connectivity, ERROR_MARGIN);
        assertEquals(EXPECTED_DENSITY, results.connectivityDensity, ERROR_MARGIN);
    }
}