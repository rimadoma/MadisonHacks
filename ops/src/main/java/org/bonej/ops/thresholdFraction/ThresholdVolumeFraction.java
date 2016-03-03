package org.bonej.ops.thresholdFraction;

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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * An Op which calculates the volumes thresholded and foreground elements in the interval.
 * The volumes are determined from meshes created with the marching cubes algorithm.
 *
 * @author Richard Domander
 */
@Plugin(type = Op.class)
public class ThresholdVolumeFraction<S, T extends NativeType<T> & Comparable<S>> extends
        AbstractBinaryFunctionOp<IterableInterval<T>, ThresholdVolumeFraction.Settings<S>, ThresholdVolumeFraction.Results>
        implements Contingent {
    /**
     * @throws NotImplementedException if interval is a Dataset
     */
    @Override
    public Results compute2(final IterableInterval<T> interval, final Settings<S> settings) {
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
        System.out.println("Thresholded surface volume " + results.thresholdMeshVolume);
        System.out.println("Foreground surface volume " + results.foregroundMeshVolume);
        System.out.println("Volume ratio " + results.volumeRatio);

        ij.context().dispose();
    }
    //endregion

    //region -- Helper classes --
    public static final class Settings<T> {
        /** Minimum value for elements within threshold */
        public final T minThreshold;
        /** Maximum value for elements within threshold */
        public final T maxThreshold;
        /** Elements whose values >= foregroundCutOff are considered foreground */
        public final T foregroundCutOff;

        public Settings(final T foregroundCutOff, final T minThreshold, final T maxThreshold) {
            this.foregroundCutOff = foregroundCutOff;
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
        }
    }

    public static final class Results {
        /** A mesh created from the elements within the thresholds */
        public final Mesh thresholdMesh;
        /** A mesh created from the foreground elements */
        public final Mesh foregroundMesh;
        public final double thresholdMeshVolume;
        public final double foregroundMeshVolume;
        /** Ratio of threshold & foreground mesh volumes */
        public final double volumeRatio;

        public Results(final Mesh thresholdMesh, final Mesh foregroundMesh, final double thresholdMeshVolume,
                       final double foregroundMeshVolume) {
            this.thresholdMesh = thresholdMesh;
            this.foregroundMesh = foregroundMesh;
            this.thresholdMeshVolume = thresholdMeshVolume;
            this.foregroundMeshVolume = foregroundMeshVolume;
            volumeRatio = thresholdMeshVolume / foregroundMeshVolume;
        }
    }
    //endregion
}
