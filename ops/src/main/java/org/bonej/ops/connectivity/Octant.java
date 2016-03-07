package org.bonej.ops.connectivity;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.Views;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A convenience class for storing a special 8-neighborhood in a 3D BitType interval
 *
 * @author Richard Domander
 * @author Mark Hiner
 * @todo Unit tests
 */
public final class Octant {
    private final boolean[] neighborhood = new boolean[8];
    private long foregroundNeighbors;
    private RandomAccess<BitType> access;

    public Octant(final RandomAccessibleInterval<BitType> interval) {
        setInterval(interval);
    }

    public void setInterval(RandomAccessibleInterval<BitType> interval) throws NullPointerException {
        checkNotNull(interval, "Interval cannot be set null");

        access = Views.extendZero(interval).randomAccess();
    }

    /**
     * Check if the nth neighbor is foreground
     * @param n 1 <= n <= 8
     */
    public boolean isNeighborForeground(int n) throws ArrayIndexOutOfBoundsException {
        return neighborhood[n - 1];
    }

    public boolean isNeighborhoodEmpty() {
        return foregroundNeighbors == 0;
    }

    public void setNeighborhood(final long u, final long v, final long w) {
        neighborhood[0] = getAtLocation(access, u - 1, v - 1, w - 1);
        neighborhood[1] = getAtLocation(access, u - 1, v, w - 1);
        neighborhood[2] = getAtLocation(access, u, v - 1, w - 1);
        neighborhood[3] = getAtLocation(access, u, v, w - 1);
        neighborhood[4] = getAtLocation(access, u - 1, v - 1, w);
        neighborhood[5] = getAtLocation(access, u - 1, v, w);
        neighborhood[6] = getAtLocation(access, u, v - 1, w);
        neighborhood[7] = getAtLocation(access, u, v, w);

        countForegroundNeighbors();
    }

    private void countForegroundNeighbors() {
        foregroundNeighbors = 0;
        for (boolean neighbor : neighborhood) {
            if (neighbor) {
                foregroundNeighbors++;
            }
        }
    }

    private boolean getAtLocation(RandomAccess<BitType> access, long u, long v, long w) {
        access.setPosition(u, 0);
        access.setPosition(v, 1);
        access.setPosition(w, 2);
        return access.get().get();
    }
}
