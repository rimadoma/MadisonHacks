package org.bonej.ops.connectivity;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * @author Richard Domander
 * @todo preconditions & tests etc
 */
public class Octant {
    private long foregroundNeighbors;
    private final boolean[] neighborhood = new boolean[8];
    private RandomAccessibleInterval<BitType> interval;

    public Octant(final RandomAccessibleInterval<BitType> interval, final long u, final long v, final long w) {
        setInterval(interval);
        setNeighborhood(u, v, w);
        countForegroundNeighbors();
    }

    public boolean isNeighborForeground(int n) {
        return neighborhood[n - 1];
    }

    public boolean isNeighborhoodEmpty() {
        return foregroundNeighbors == 0;
    }

    public void setInterval(RandomAccessibleInterval<BitType> interval) {
        this.interval = interval;
    }

    public void setNeighborhood(final long u, final long v, final long w) {
        final long[] location = {u, v, w};
        final RandomAccess<BitType> access = Views.extendZero(interval).randomAccess();
        access.setPosition(location);

        neighborhood[0] = getAndRestore(access, new long[]{u - 1, v - 1, w - 1}, location);
        neighborhood[1] = getAndRestore(access, new long[]{u - 1, v, w - 1}, location);
        neighborhood[2] = getAndRestore(access, new long[]{u, v - 1, w - 1}, location);
        neighborhood[3] = getAndRestore(access, new long[]{u, v, w - 1}, location);
        neighborhood[4] = getAndRestore(access, new long[]{u - 1, v - 1, w}, location);
        neighborhood[5] = getAndRestore(access, new long[]{u - 1, v, w}, location);
        neighborhood[6] = getAndRestore(access, new long[]{u, v - 1, w}, location);
        neighborhood[7] = access.get().get();
    }

    private void countForegroundNeighbors() {
        foregroundNeighbors = 0;
        for (boolean neighbor : neighborhood) {
            if (neighbor) {
                foregroundNeighbors++;
            }
        }
    }

    private boolean getAndRestore(RandomAccess<BitType> access, long[] location, long[] originalLocation) {
        access.setPosition(location);
        boolean value = access.get().get();
        access.setPosition(originalLocation);
        return value;
    }
}
