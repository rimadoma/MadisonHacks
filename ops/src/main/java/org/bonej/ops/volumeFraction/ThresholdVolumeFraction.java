package org.bonej.ops.volumeFraction;

import net.imagej.ImageJ;
import net.imagej.ops.Contingent;
import net.imagej.ops.Op;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import org.bonej.ops.testImageGenerators.CuboidCreator;
import org.scijava.plugin.Plugin;

/**
 * An Op which calculates the volumes thresholded and foreground elements in the interval.
 * The volumes are determined from meshes created with the marching cubes algorithm.
 *
 * @author Richard Domander
 * @todo How to implement the limit calculations to ROIs option from BoneJ1?
 * @todo How to apply calculations only to areas defined by masks (irregular rois)?
 * @todo How to display result data?
 * @todo How to display resulting meshes?
 * @todo Prematch Ops in initialize
 * @todo Resampling?
 * @todo Regression test
 * @todo Unit tests for creating masks?
 */
@Plugin(type = Op.class, name = "thresholdVolumeFraction")
public class ThresholdVolumeFraction<T extends NativeType<T> & Comparable<T>> extends
        AbstractBinaryFunctionOp<IterableInterval<T>, ThresholdVolumeFraction.Settings<T>, ThresholdVolumeFraction.Results>
        implements Contingent {
    @Override
    public Results compute2(final IterableInterval<T> interval, final Settings<T> settings) {
        final Img<BitType> thresholdMask = ops().create().img(interval, new BitType());
        final Img<BitType> foregroundMask = ops().create().img(interval, new BitType());
        final long[] location = new long[interval.numDimensions()];
        final Cursor<T> cursor = interval.localizingCursor();
        final RandomAccess<BitType> foregroundAccess = foregroundMask.randomAccess();
        final RandomAccess<BitType> thresholdAccess = thresholdMask.randomAccess();

        while (cursor.hasNext()) {
            cursor.fwd();
            T element = cursor.get();
            if (element.compareTo(settings.foregroundCutOff) < 0) {
                continue;
            }

            cursor.localize(location);
            foregroundAccess.setPosition(location);
            foregroundAccess.get().setOne();

            if ((element.compareTo(settings.minThreshold) >= 0) && (element.compareTo(settings.maxThreshold) <= 0)) {
                thresholdAccess.setPosition(location);
                thresholdAccess.get().setOne();
            }
        }

        final Mesh thresholdMesh = ops().geom().marchingcubes(thresholdMask);
        final double thresholdVolume = ops().geom().size(thresholdMesh).get();
        final Mesh foregroundMesh = ops().geom().marchingcubes(foregroundMask);
        final double foregroundVolume = ops().geom().size(foregroundMesh).get();

        return new Results(thresholdMesh, foregroundMesh, thresholdVolume, foregroundVolume);
    }

    @Override
    public boolean conforms() {
        return in1().numDimensions() == 3;
    }

    //region -- Utility methods --
    public static void main(String... args) {
        final ImageJ ij = new ImageJ();
        final Object cuboid = ij.op().run(CuboidCreator.class, null, 10L, 10L, 10L, 0L);
        final BitType foregroundCutoff = new BitType(true);
        final BitType minThreshold = new BitType(true);
        final BitType maxThreshold = new BitType(true);
        final Settings<BitType> settings = new Settings<>(foregroundCutoff, minThreshold, maxThreshold);

        final Results results = (Results) ij.op().run(ThresholdVolumeFraction.class, cuboid, settings);
        System.out.println("Thresholded surface volume " + results.thresholdVolume);
        System.out.println("Foreground surface volume " + results.foregroundVolume);
        System.out.println("Volume ratio " + results.volumeRatio);

        ij.context().dispose();
    }
    //endregion

    //region -- Helper classes --
    /**
     * @todo How are these kinds @Parameters stored persistently?
     */
    public static final class Settings<T> {
        public static final int DEFAULT_RESAMPLING = 6;
        /** Minimum value for elements within threshold */
        public final T minThreshold;
        /** Maximum value for elements within threshold */
        public final T maxThreshold;
        /** Elements whose values >= foregroundCutOff are considered foreground */
        public final T foregroundCutOff;

        public final int resampling;

        public Settings(final T foregroundCutOff, final T minThreshold, final T maxThreshold) {
            this(foregroundCutOff, minThreshold, maxThreshold, DEFAULT_RESAMPLING);
        }

        public Settings(final T foregroundCutOff, final T minThreshold, final T maxThreshold, final int resampling) {
            this.foregroundCutOff = foregroundCutOff;
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
            this.resampling = resampling;
        }
    }

    public static final class Results {
        public final Mesh thresholdMesh;
        public final Mesh foregroundMesh;
        /** Number of elements found whose value is within thresholds */
        public final double thresholdVolume;
        /** Number of elements which are foreground */
        public final double foregroundVolume;
        /** Ratio of thresholdElements / foregroundElements */
        public final double volumeRatio;

        public Results(final Mesh thresholdMesh, final Mesh foregroundMesh, final double thresholdVolume,
                       final double foregroundVolume) {
            this.thresholdMesh = thresholdMesh;
            this.foregroundMesh = foregroundMesh;
            this.thresholdVolume = thresholdVolume;
            this.foregroundVolume = foregroundVolume;
            volumeRatio = thresholdVolume / foregroundVolume;
        }
    }
    //endregion
}
