package org.bonej.utilities;


import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import org.apache.commons.math.exception.NullArgumentException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the CalibratedAxisUtil class
 *
 * @author Richard Domander
 * @todo Use DatasetCreator
 */
public class CalibratedAxisUtilTest {
    private static final ImageJ IMAGE_J = new ImageJ();
    private static BinaryFunctionOp<Dimensions, BitType, Img<BitType>> imgCreator;

    @BeforeClass
    public static void oneTimeSetUp() {
        imgCreator = (BinaryFunctionOp) Functions
                .binary(IMAGE_J.op(), Ops.Create.Img.class, Img.class, Dimensions.class, new BitType());
    }

    @Test
    public void testCountSpatialDimensions() throws AssertionError {
        final FinalDimensions dimensions = new FinalDimensions(10, 10, 10);
        final int expectedDimensions = dimensions.numDimensions();
        final Img<BitType> img = imgCreator.compute1(dimensions);
        // If you call the constructor with a single argument,
        // the axis types by default will be Axes.X, Axes.Y and *unknown*
        final ImgPlus<BitType> imgPlus = new ImgPlus<>(img, "", new AxisType[]{Axes.X, Axes.Y, Axes.Z});

        final long result = CalibratedAxisUtil.countSpatialDimensions(imgPlus);

        assertEquals("Wrong number of spatial dimensions counted", expectedDimensions, result);
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

        assertTrue("Image should contain non-spatial dimensions", result);
    }

    @Test
    public void testHasNonSpatialDimensionsReturnsFalseWithOnlySpatialDimensions() throws AssertionError {
        final FinalDimensions dimensions = new FinalDimensions(10, 10);
        final Img<BitType> img = imgCreator.compute1(dimensions);
        final ImgPlus<BitType> imgPlus = new ImgPlus<>(img, "", new AxisType[]{Axes.X, Axes.Y});

        final boolean result = CalibratedAxisUtil.hasNonSpatialDimensions(imgPlus);

        assertFalse("Image should contain only spatial dimensions", result);
    }

    @Test(expected = NullPointerException.class)
    public void testHasNonSpatialDimensionsThrowsNullPointerExceptionIfSpaceIsNull() throws AssertionError {
        CalibratedAxisUtil.hasNonSpatialDimensions(null);
    }
}