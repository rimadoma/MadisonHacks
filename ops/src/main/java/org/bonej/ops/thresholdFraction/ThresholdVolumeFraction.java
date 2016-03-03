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
import net.imglib2.type.numeric.RealType;
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
public class ThresholdVolumeFraction<T extends NativeType<T> & RealType<T>> extends
        AbstractBinaryFunctionOp<IterableInterval<T>, ThresholdVolumeFraction.Settings, ThresholdVolumeFraction.Results>
        implements Contingent {
    /**
     * @throws NotImplementedException if interval is a Dataset
     */
    @Override
    public Results compute2(final IterableInterval<T> interval, final Settings settings) {
        final Img<BitType> thresholdMask = ops().create().img(interval, new BitType());
        final Img<BitType> foregroundMask = ops().create().img(interval, new BitType());
        final long[] location = new long[interval.numDimensions()];
        final Cursor<T> cursor = interval.localizingCursor();
        final RandomAccess<BitType> foregroundAccess = foregroundMask.randomAccess();
        final RandomAccess<BitType> thresholdAccess = thresholdMask.randomAccess();

        // Create elements of type T from settings that can be compared to type T in interval
        final T cutoff = interval.firstElement().createVariable();
        cutoff.setReal(settings.foregroundCutOff);
        final T minThreshold = interval.firstElement().createVariable();
        minThreshold.setReal(settings.minThreshold);
        final T maxThreshold = interval.firstElement().createVariable();
        maxThreshold.setReal(settings.maxThreshold);

        while (cursor.hasNext()) {
            cursor.fwd();
            T element = cursor.get();
            if (element.compareTo(cutoff) < 0) {
                continue;
            }

            cursor.localize(location);
            foregroundAccess.setPosition(location);
            foregroundAccess.get().setOne();

            if (element.compareTo(minThreshold) >= 0 && element.compareTo(maxThreshold) <= 0) {
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
        final Settings settings = new Settings(1.0, 1.0, 1.0);

        final Results results = (Results) ij.op().run(ThresholdVolumeFraction.class, cuboid, settings);
        System.out.println("Thresholded surface volume " + results.thresholdMeshVolume);
        System.out.println("Foreground surface volume " + results.foregroundMeshVolume);
        System.out.println("Volume ratio " + results.volumeRatio);

        ij.context().dispose();
    }
    //endregion

    /**
     * @todo    Make generic with type T, but with double constructor that has a T param, that
     *          can be used to copy / create elements of T?
     */
    //region -- Helper classes --
    public static final class Settings {
        /** Minimum value for elements within threshold */
        public final double minThreshold;
        /** Maximum value for elements within threshold */
        public final double maxThreshold;
        /** Elements whose values >= foregroundCutOff are considered foreground */
        public final double foregroundCutOff;

        public Settings(final double foregroundCutOff, final double minThreshold, final double maxThreshold) {
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
