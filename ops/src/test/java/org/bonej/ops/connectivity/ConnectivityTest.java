package org.bonej.ops.connectivity;

import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import org.bonej.ops.testImageGenerators.WireFrameCuboidCreator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for the Connectivity class
 *
 * @author Richard Domander
 * @todo write a tests with images that touche the interval boundaries to properly test deltaChi
 */
public class ConnectivityTest {
    private static final ImageJ ij = new ImageJ();
    private static final double ERROR_MARGIN = 1E-12;
    private static BinaryFunctionOp<Dimensions, BitType, Img<BitType>> imgCreator;

    @BeforeClass
    public static void oneTimeSetUp() {
        imgCreator = (BinaryFunctionOp) Functions
                .binary(ij.op(), Ops.Create.Img.class, Img.class, Dimensions.class, new BitType());
    }

    @AfterClass
    public static void oneTimeTearDown() {
        ij.context().dispose();
    }

    /**
     * Test that the Connectivity Op gets matched with correct input.
     * Calling Connectivity.conforms() is a part of the matching process
     */
    @Test
    public void testConnectivityMatchesWith3DImage() {
        final double[] calibration = {0.2, 0.2, 0.2};
        Object cuboid = ij.op().run(WireFrameCuboidCreator.class, null, 10, 10, 10, 0, calibration);

        Connectivity connectivity = ij.op().op(Connectivity.class, cuboid);

        assertNotNull(connectivity);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectivityFailsMatchWith2DImage() {
        // to be called unary the 2nd argument for this BinaryFunction has to be set non null in the matcher
        Img<BitType> img = imgCreator.compute1(new FinalDimensions(10, 10));
        ImgPlus<BitType> testImage = new ImgPlus<>(img);

        ij.op().op(Connectivity.class, testImage);
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
        final double ELEMENT_VOLUME = 0.2 * 0.2 * 0.2;
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