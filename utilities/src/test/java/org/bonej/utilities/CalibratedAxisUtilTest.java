package org.bonej.utilities;


import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the CalibratedAxisUtil class
 *
 * @author Richard Domander
 */
public class CalibratedAxisUtilTest {
    private static final ImageJ IMAGE_J = new ImageJ();
    private static final FinalDimensions DIMENSIONS = new FinalDimensions(10, 10, 10);
    private static final int EXPECTED_DIMENSIONS = DIMENSIONS.numDimensions();
    private static BinaryFunctionOp<Dimensions, BitType, Img<BitType>> imgCreator;
    private static ImgPlus<BitType> testImgPlus3D;

    @BeforeClass
    public static void oneTimeSetUp() {
        imgCreator = (BinaryFunctionOp) Functions
                .binary(IMAGE_J.op(), Ops.Create.Img.class, Img.class, Dimensions.class, new BitType());
        final Img<BitType> img = imgCreator.compute1(DIMENSIONS);
        testImgPlus3D = new ImgPlus<>(img, "", new AxisType[]{Axes.X, Axes.Y, Axes.Z});
    }

    @Test
    public void testCountSpatialDimensions() throws AssertionError {
        final long result = CalibratedAxisUtil.countSpatialDimensions(testImgPlus3D);

        assertEquals("Wrong number of spatial DIMENSIONS counted", EXPECTED_DIMENSIONS, result);
    }

    @Test(expected = NullPointerException.class)
    public void testCountSpatialDimensionsThrowsNullPointerExceptionIfSpaceIsNull() throws AssertionError {
        CalibratedAxisUtil.countSpatialDimensions(null);
    }

    @Test
    public void testHasNonSpatialDimensionsReturnsTrueWithNonSpatialDimensions() throws AssertionError {
        final FinalDimensions dimensions = new FinalDimensions(10, 10);
        final Img<BitType> img = imgCreator.compute1(dimensions);
        final ImgPlus<BitType> imgPlus = new ImgPlus<>(img, "", new AxisType[]{Axes.X, Axes.TIME});

        final boolean result = CalibratedAxisUtil.hasNonSpatialDimensions(imgPlus);

        assertTrue("Image should contain non-spatial DIMENSIONS", result);
    }

    @Test
    public void testHasNonSpatialDimensionsReturnsFalseWithOnlySpatialDimensions() throws AssertionError {
        final boolean result = CalibratedAxisUtil.hasNonSpatialDimensions(testImgPlus3D);

        assertFalse("Image should contain only spatial DIMENSIONS", result);
    }

    @Test(expected = NullPointerException.class)
    public void testHasNonSpatialDimensionsThrowsNullPointerExceptionIfSpaceIsNull() throws AssertionError {
        CalibratedAxisUtil.hasNonSpatialDimensions(null);
    }

    @Test
    public void testAllocateAndGetAxis() throws AssertionError {
        final Optional<CalibratedAxis[]> result = CalibratedAxisUtil.allocateAndGetAxis(testImgPlus3D);
        assertTrue("Array should be present", result.isPresent());
        assertEquals("Array has wrong length", EXPECTED_DIMENSIONS, result.get().length);
    }

    @Test
    public void testAllocateAndGetAxisReturnsEmptyIfSpaceIsNull() {
        final Optional<CalibratedAxis[]> result = CalibratedAxisUtil.allocateAndGetAxis(null);

        assertFalse("Optional should be empty", result.isPresent());
    }

    @Test
    public void testGetAxisStream() throws AssertionError {
        final Stream<CalibratedAxis> result = CalibratedAxisUtil.getAxisStream(testImgPlus3D);

        assertEquals("Stream has wrong number of elements", EXPECTED_DIMENSIONS, result.count());
    }

    @Test
    public void testGetAxisStreamReturnsEmptyIfSpaceIsNull() {
        final Stream<CalibratedAxis> result = CalibratedAxisUtil.getAxisStream(null);

        assertFalse(result.findAny().isPresent());
    }
}