package org.bonej.ops.thresholdFraction;

import net.imagej.ImageJ;
import net.imagej.ops.create.img.CreateImgFromDimsAndType;
import net.imglib2.FinalDimensions;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.LongType;
import org.bonej.ops.testImageGenerators.CuboidCreator;
import org.bonej.ops.thresholdFraction.ThresholdVolumeFraction.Results;
import org.bonej.ops.thresholdFraction.ThresholdVolumeFraction.Settings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit & regression tests for ThresholdVolumeFraction
 *
 * @author Richard Domander
 */
public class ThresholdVolumeFractionTest {
    private final static ImageJ ij = new ImageJ();
    private static final double ERROR_MARGIN = 1e-12;

    @Test(expected = IllegalArgumentException.class)
    public void testThresholdVolumeFractionFailsMatchWith2DImage() {
        // to be called unary the 2nd argument for this BinaryFunction has to be set non null in the matcher
        Img img = (Img) ij.op().run(CreateImgFromDimsAndType.class, new FinalDimensions(10, 10), new LongType());
        final Settings settings = new Settings<>(new LongType(1L), new LongType(1L), new LongType(100L));
        ij.op().op(ThresholdVolumeFraction.class, img, settings);
    }

    @Test
    public void regressionTestUnitCube() throws AssertionError {
        /*  The surface created by the marching cubes algorithm in ThresholdVolumeFraction is " in between pixels".
         *  In the case of a (BitType) unit cube it creates an octahedron,
         *  whose vertices are in the middle of the faces of the cube.
         */
        final long width = 1L;
        final long height = 1L;
        final long depth = 1L;
        final double pyramidSide = Math.sqrt((width / 2.0) * (depth / 2.0) + (width / 2.0) * (depth / 2.0));
        final double pyramidVolume = pyramidSide * pyramidSide * (height / 2.0) / 3.0;
        final double octahedronVolume = pyramidVolume * 2.0;
        final IterableInterval<BitType> unitCube =
                (IterableInterval<BitType>) ij.op().run(CuboidCreator.class, null, width, height, depth);
        final Settings<BitType> settings = new Settings<>(new BitType(true), new BitType(true), new BitType(true));

        final Results results = (Results) ij.op().run(ThresholdVolumeFraction.class, unitCube, settings);

        assertEquals("Incorrect thresholded surface volume ", octahedronVolume, results.thresholdMeshVolume,
                ERROR_MARGIN);
        assertEquals("Incorrect foreground surface volume ", octahedronVolume, results.foregroundMeshVolume,
                ERROR_MARGIN);
        assertEquals("Incorrect volume ratio ", 1.0, results.volumeRatio, ERROR_MARGIN);
    }

    /**
     * Verify that if half of the foreground elements in an image are within the thresholds,
     * then the volume of the thresholdMesh is (about) half of the foregroundMesh
     */
    @Test
    public void regressionTestThresholdMeshVolume() throws AssertionError {
        final long wSize = 10;
        final long vSize = 10;
        final long uSize = 10;
        final Settings<LongType> settings = new Settings<>(new LongType(1L), new LongType(6L), new LongType(10L));
        final Img<LongType> testImg = (Img<LongType>) ij.op()
                .run(CreateImgFromDimsAndType.class, new FinalDimensions(uSize, vSize, wSize), new LongType());
        fillWithThirdDimGradient(testImg);

        final Results results = (Results) ij.op().run(ThresholdVolumeFraction.class, testImg, settings);

        // Volume will not be exactly half, because marching cubes will "round" / "smooth" the edges
        // of the mesh created from elements within the thresholds.
        // Otherwise this mesh would be the cuboid cut in half across the 3rd dimension plane
        assertEquals("Incorrect volume ratio ", 0.5, results.volumeRatio, 0.05);
    }

    /**
     * Fills the img with a gradient that grows along the third dimension axis
     * Gradient starts from 1
     */
    private void fillWithThirdDimGradient(final Img<LongType> img) {
        final long wSize = img.dimension(2);
        final long vSize = img.dimension(1);
        final long uSize = img.dimension(0);
        final RandomAccess<LongType> access = img.randomAccess();

        for (long w = 0; w < wSize; w++) {
            access.setPosition(w, 2);
            for (long v = 0; v < vSize; v++) {
                access.setPosition(v, 1);
                for (long u = 0; u < uSize; u++) {
                    access.setPosition(u, 0);
                    access.get().set(w + 1);
                }
            }
        }
    }
}