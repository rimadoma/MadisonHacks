package org.bonej.ops.connectivity;

import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
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
    private static final double DELTA = 1E-12;

    @AfterClass
    public static void oneTimeTearDown() {
        ij.context().dispose();
    }

    @Test
    public void testConnectivity() {
        final int CUBOID_WIDTH = 10;
        final int CUBOID_HEIGHT = 10;
        final int CUBOID_DEPTH = 10;
        final int PADDING = 1;

        RandomAccessibleInterval<BitType> cuboid = (RandomAccessibleInterval<BitType>) ij.op()
                .run(WireFrameCuboidCreator.class, null, CUBOID_WIDTH, CUBOID_HEIGHT, CUBOID_DEPTH, PADDING);

        Connectivity.Characteristics results = (Connectivity.Characteristics) ij.op().run(Connectivity.class, cuboid);
        assertEquals(-4.0, results.eulerCharacteristic, DELTA);
    }
}