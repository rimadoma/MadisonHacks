package org.bonej.ops.connectivity;

import net.imagej.ops.Contingent;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import java.util.Arrays;
import java.util.stream.LongStream;

/**
 * An Op which determines the number of connected structures in an Img by calculating the Euler characteristic.
 * The Euler characteristic is determined from voxel neighborhoods.
 * The Op assumes that there is only one continuous foreground structure in the Img.
 *
 * @todo Img Calibration?
 * @author Richard Domander
 */
public class Connectivity extends AbstractUnaryFunctionOp implements Contingent {
    /** Euler characteristic of the sample as though floating in space (χ). */
    private double eulerCharacteristic = 0.0;

    /** Δ(χ): the sample's contribution to the Euler characteristic of the structure to which it was connected.
     *  Calculated by counting the intersections of voxels and the edges of the image stack. */
    private double deltaChi = 0.0;

    /** The connectivity of the image = 1 - Δ(χ) */
    private double connectivity = 0.0;

    /** The connectivity density of the image = connectivity / sample volume */
    private double connectivityDensity = 0.0;

    @Parameter
    private RandomAccessibleInterval<BooleanType> interval;

    @Parameter(type = ItemIO.OUTPUT)
    private final double[] connectivityCharacteristics = new double[4];

    @Override
    public double[] compute1(Object o) {
        calculateEulerCharacteristic();

        fillCharacteristics();
        return connectivityCharacteristics;
    }

    private void calculateEulerCharacteristic() {
        final long uSize = interval.dimension(0);
        final long vSize = interval.dimension(1);
        final long wSize = interval.dimension(2);
        final RandomAccess<BooleanType> access = interval.randomAccess();
        final LongStream wRange = LongStream.range(0, wSize); // make parallel

        wRange.forEach( w -> {
            for (long v = 0; v < vSize; v++) {
                access.setPosition(v, 1);
                for (long u = 0; u < uSize; u++) {
                    access.setPosition(u, 0);
                    Octant octant = getOctant(u, v, w);
                }
            }
        });
    }

    private Octant getOctant(long u, long v, long w) {
        final long[] location = {u, v, w};
        final RandomAccess<BooleanType> access = interval.randomAccess();
        access.setPosition(location);

        final Octant octant = new Octant();
        octant.neighborhood[0] = getAndRestore(access, new long[]{u - 1, v - 1, w - 1}, location);
        octant.neighborhood[1] = getAndRestore(access, new long[]{u - 1, v, w - 1}, location);
        octant.neighborhood[2] = getAndRestore(access, new long[]{u, v - 1, w - 1}, location);
        octant.neighborhood[3] = getAndRestore(access, new long[]{u, v, w - 1}, location);
        octant.neighborhood[4] = getAndRestore(access, new long[]{u - 1, v - 1, w}, location);
        octant.neighborhood[5] = getAndRestore(access, new long[]{u - 1, v, w}, location);
        octant.neighborhood[6] = getAndRestore(access, new long[]{u, v - 1, w}, location);
        octant.neighborhood[7] = access.get();
        octant.countNeighbors();

        return octant;
    }

    private BooleanType getAndRestore(RandomAccess<BooleanType> access, long[] location, long[] originalLocation) {
        access.setPosition(location);
        BooleanType value = access.get();
        access.setPosition(originalLocation);
        return value;
    }

    @Override
    public boolean conforms() {
        //@todo are all the dimensions spatial?
        return interval.numDimensions() == 3;
    }

    private void fillCharacteristics() {
        connectivityCharacteristics[0] = eulerCharacteristic;
        connectivityCharacteristics[1] = deltaChi;
        connectivityCharacteristics[2] = connectivity;
        connectivityCharacteristics[3] = connectivityDensity;
    }

    private static final class Octant {
        public long neighbors;
        public final BooleanType[] neighborhood = new BooleanType[8];

        public void countNeighbors() {
            neighbors = Arrays.stream(neighborhood).filter(BooleanType::get).count();
        }
    }
}
