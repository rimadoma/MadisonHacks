package org.bonej.ops.thresholdFraction;

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
import org.bonej.ops.thresholdFraction.ThresholdElementFraction.Results;
import org.bonej.ops.thresholdFraction.ThresholdElementFraction.Settings;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        final LongType foregroundCutOff = new LongType(1L);
        final LongType minThreshold = new LongType(5L);
        final LongType maxThreshold = new LongType(9L);
        final Settings<LongType> settings = new Settings<>(foregroundCutOff, minThreshold, maxThreshold);

        // Create test set
        final long intervalSize = 11L;
        final Img<LongType> img = imgCreator.compute1(new FinalDimensions(intervalSize));
        RandomAccess<LongType> access = img.randomAccess();
        for (long i = 0; i < intervalSize; i++) {
            access.get().set(i);
            access.move(1, 0);
        }

        // Get and assert results
        Results results = (Results) ij.op().run(ThresholdElementFraction.class, img, settings);

        assertTrue("Number of threshold elements cannot be greater than foreground elements",
                results.thresholdElements <= results.foregroundElements);
        assertEquals("Incorrect number of foreground elements", 10L, results.foregroundElements);
        assertEquals("Incorrect number of elements within thresholds", 5L, results.thresholdElements);
        assertEquals("Incorrect ratio of elements", 0.5, results.elementRatio, 1E-12);
    }
}