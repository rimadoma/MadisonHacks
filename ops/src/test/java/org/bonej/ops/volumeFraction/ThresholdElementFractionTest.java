package org.bonej.ops.volumeFraction;

import net.imagej.ImageJ;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.LongType;
import org.bonej.ops.volumeFraction.ThresholdElementFraction.Results;
import org.bonej.ops.volumeFraction.ThresholdElementFraction.Settings;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the ThresholdElementFraction class
 *
 * @author Richard Domander
 */
public class ThresholdElementFractionTest {
    private static final ImageJ ij = new ImageJ();
    private static BinaryFunctionOp<Dimensions, BitType, Img<LongType>> imgCreator;

    @BeforeClass
    public static void oneTimeSetUp() {
        imgCreator = (BinaryFunctionOp) Functions
                .binary(ij.op(), Ops.Create.Img.class, Img.class, Dimensions.class, new LongType());
    }

    @Test
    public void testThresholdElementFraction() {
        // Create threshold settings
        final LongType foregroundCutOff = new LongType(1);
        final LongType minThreshold = new LongType(5);
        final LongType maxThreshold = new LongType(9);
        final Settings<LongType> settings = new Settings<>(foregroundCutOff, minThreshold, maxThreshold);

        // Create test img
        final long intervalSize = 11;
        final Img<LongType> img = imgCreator.compute1(new FinalDimensions(intervalSize));
        RandomAccess<LongType> access = img.randomAccess();
        for (long i = 0; i < intervalSize; i++) {
            access.move(1, 0);
            access.get().set(i);
        }

        // Get and assert results
        Results results = (Results) ij.op().run(ThresholdElementFraction.class, img, settings);

        assertEquals("Incorrect number of foreground elements", 10, results.foregroundElements);
        assertEquals("Incorrect number of elements within thresholds", 5, results.thresholdElements);
    }
}