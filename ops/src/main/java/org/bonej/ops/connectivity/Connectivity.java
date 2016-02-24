package org.bonej.ops.connectivity;

import net.imagej.ImgPlus;
import net.imagej.ops.Contingent;
import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.type.logic.BitType;
import org.scijava.plugin.Plugin;

import java.util.Arrays;

/**
 * An Op which determines the number of connected structures in an ImgPlus image
 * by calculating the Euler characteristics of its elements.
 * The euler characteristic of an element is determined from its special 8-neighborhood.
 * The euler characteristics of the elements are summed the get the characteristic of the whole particle.
 * The Op assumes that there is only one continuous foreground particle in the image.
 *
 * @author Michael Doube
 * @author Richard Domander
 *
 *         The algorithms here are based on the following articles:
 *         Toriwaki J, Yonekura T (2002) Euler Number and Connectivity Indexes of a
 *         Three Dimensional Digital Picture. Forma 17: 183-209.
 *         <a href="http://www.scipress.org/journals/forma/abstract/1703/17030183.html">
 *         http://www.scipress.org/journals/forma/abstract/1703/17030183.html</a>
 *
 *         Odgaard A, Gundersen HJG (1993) Quantification of connectivity in
 *         cancellous bone, with special emphasis on 3-D reconstructions. Bone 14:
 *         173-182.
 *         <a href="http://dx.doi.org/10.1016/8756-3282(93)90245-6">doi:10.1016/8756-3282(93)90245-6</a>
 *
 *         Lee TC, Kashyap RL, Chu CN (1994) Building Skeleton Models via 3-D
 *         Medial Surface Axis Thinning Algorithms. CVGIP: Graphical Models and
 *         Image Processing 56: 462-478.
 *         <a href="http://dx.doi.org/10.1006/cgip.1994.1042">doi:10.1006/cgip.1994.1042</a>
 *
 *         Several of the methods are based on Ignacio Arganda-Carreras's
 *         Skeletonize3D_ plugin: <a href="http://imagejdocu.tudor.lu/doku.php?id=plugin:morphology:skeletonize3d:start">
 *         Skeletonize3D homepage</a>
 */
@Plugin(type = Op.class, name = "connectivityCharacteristics")
public class Connectivity extends AbstractUnaryFunctionOp<ImgPlus<BitType>, Connectivity.Characteristics>
        implements Contingent {
    private static final int U_INDEX = 0;
    private static final int V_INDEX = 1;
    private static final int W_INDEX = 2;

    private static final int[] EULER_LUT = new int[256];

    //region fill EULER_LUT
    static {
        EULER_LUT[1] = 1;
        EULER_LUT[7] = -1;
        EULER_LUT[9] = -2;
        EULER_LUT[11] = -1;
        EULER_LUT[13] = -1;

        EULER_LUT[19] = -1;
        EULER_LUT[21] = -1;
        EULER_LUT[23] = -2;
        EULER_LUT[25] = -3;
        EULER_LUT[27] = -2;

        EULER_LUT[29] = -2;
        EULER_LUT[31] = -1;
        EULER_LUT[33] = -2;
        EULER_LUT[35] = -1;
        EULER_LUT[37] = -3;

        EULER_LUT[39] = -2;
        EULER_LUT[41] = -1;
        EULER_LUT[43] = -2;
        EULER_LUT[47] = -1;
        EULER_LUT[49] = -1;

        EULER_LUT[53] = -2;
        EULER_LUT[55] = -1;
        EULER_LUT[59] = -1;
        EULER_LUT[61] = 1;
        EULER_LUT[65] = -2;

        EULER_LUT[67] = -3;
        EULER_LUT[69] = -1;
        EULER_LUT[71] = -2;
        EULER_LUT[73] = -1;
        EULER_LUT[77] = -2;

        EULER_LUT[79] = -1;
        EULER_LUT[81] = -1;
        EULER_LUT[83] = -2;
        EULER_LUT[87] = -1;
        EULER_LUT[91] = 1;

        EULER_LUT[93] = -1;
        EULER_LUT[97] = -1;
        EULER_LUT[103] = 1;
        EULER_LUT[105] = 4;
        EULER_LUT[107] = 3;

        EULER_LUT[109] = 3;
        EULER_LUT[111] = 2;
        EULER_LUT[113] = -2;
        EULER_LUT[115] = -1;
        EULER_LUT[117] = -1;
        EULER_LUT[121] = 3;

        EULER_LUT[123] = 2;
        EULER_LUT[125] = 2;
        EULER_LUT[127] = 1;
        EULER_LUT[129] = -6;
        EULER_LUT[131] = -3;

        EULER_LUT[133] = -3;
        EULER_LUT[137] = -3;
        EULER_LUT[139] = -2;
        EULER_LUT[141] = -2;
        EULER_LUT[143] = -1;

        EULER_LUT[145] = -3;
        EULER_LUT[151] = 3;
        EULER_LUT[155] = 1;
        EULER_LUT[157] = 1;
        EULER_LUT[159] = 2;

        EULER_LUT[161] = -3;
        EULER_LUT[163] = -2;
        EULER_LUT[167] = 1;
        EULER_LUT[171] = -1;
        EULER_LUT[173] = 1;

        EULER_LUT[177] = -2;
        EULER_LUT[179] = -1;
        EULER_LUT[181] = 1;
        EULER_LUT[183] = 2;
        EULER_LUT[185] = 1;

        EULER_LUT[189] = 2;
        EULER_LUT[191] = 1;
        EULER_LUT[193] = -3;
        EULER_LUT[197] = -2;
        EULER_LUT[199] = 1;

        EULER_LUT[203] = 1;
        EULER_LUT[205] = -1;
        EULER_LUT[209] = -2;
        EULER_LUT[211] = 1;
        EULER_LUT[213] = -1;

        EULER_LUT[215] = 2;
        EULER_LUT[217] = 1;
        EULER_LUT[219] = 2;
        EULER_LUT[223] = 1;
        EULER_LUT[227] = 1;

        EULER_LUT[229] = 1;
        EULER_LUT[231] = 2;
        EULER_LUT[233] = 3;
        EULER_LUT[235] = 2;
        EULER_LUT[237] = 2;

        EULER_LUT[239] = 1;
        EULER_LUT[241] = -1;
        EULER_LUT[247] = 1;
        EULER_LUT[249] = 2;
        EULER_LUT[251] = 1;

        EULER_LUT[253] = 1;
    }
    //endregion

    @Override
    public Characteristics compute1(final ImgPlus<BitType> imgPlus) {
        /** Euler characteristic of the sample as though floating in space (χ). */
        double eulerCharacteristic = calculateEulerCharacteristic(imgPlus);

        /** Δ(χ): the sample's contribution to the Euler characteristic of the structure to which it was connected.
         * Calculated by counting the intersections of elements, and the edges of the imgPlus. */
        double deltaChi = calculateDeltaChi(imgPlus, eulerCharacteristic);

        /** The connectivity of the sample = 1 - Δ(χ) */
        double connectivity = 1 - deltaChi;

        /** The connectivity density of the sample = connectivity / calibratedIntervalVolume */
        double connectivityDensity = calculateConnectivityDensity(connectivity, imgPlus);

        return new Characteristics(eulerCharacteristic, deltaChi, connectivity, connectivityDensity);
    }

    @Override
    public boolean conforms() {
        //@todo are all the dimensions spatial?
        return in().numDimensions() == 3;
    }

    private double calculateEulerCharacteristic(final ImgPlus<BitType> imgPlus) {
        final int[] eulerSums = new int[(int) imgPlus.dimension(W_INDEX)];

        final Cursor<BitType> cursor = imgPlus.localizingCursor();
        cursor.forEachRemaining(c -> {
            long u = cursor.getLongPosition(U_INDEX);
            long v = cursor.getLongPosition(V_INDEX);
            int w = cursor.getIntPosition(W_INDEX);
            Octant octant = new Octant(imgPlus, u, v, w);
            eulerSums[w] += getDeltaEuler(octant);
        });

        return Arrays.stream(eulerSums).sum() / 8.0;
    }

    private int getDeltaEuler(final Octant octant) {
        if (octant.isNeighborhoodEmpty()) {
            return 0;
        }

        int index = 1;
        if (octant.isNeighborForeground(8)) {
            if (octant.isNeighborForeground(1)) { index |= 128; }
            if (octant.isNeighborForeground(2)) { index |= 64; }
            if (octant.isNeighborForeground(3)) { index |= 32; }
            if (octant.isNeighborForeground(4)) { index |= 16; }
            if (octant.isNeighborForeground(5)) { index |= 8; }
            if (octant.isNeighborForeground(6)) { index |= 4; }
            if (octant.isNeighborForeground(7)) { index |= 2; }
        } else if (octant.isNeighborForeground(7)) {
            if (octant.isNeighborForeground(2)) { index |= 128; }
            if (octant.isNeighborForeground(4)) { index |= 64; }
            if (octant.isNeighborForeground(1)) { index |= 32; }
            if (octant.isNeighborForeground(3)) { index |= 16; }
            if (octant.isNeighborForeground(6)) { index |= 8; }
            if (octant.isNeighborForeground(5)) { index |= 2; }
        } else if (octant.isNeighborForeground(6)) {
            if (octant.isNeighborForeground(3)) { index |= 128; }
            if (octant.isNeighborForeground(1)) { index |= 64; }
            if (octant.isNeighborForeground(4)) { index |= 32; }
            if (octant.isNeighborForeground(2)) { index |= 16; }
            if (octant.isNeighborForeground(5)) { index |= 4; }
        } else if (octant.isNeighborForeground(5)) {
            if (octant.isNeighborForeground(4)) { index |= 128; }
            if (octant.isNeighborForeground(3)) { index |= 64; }
            if (octant.isNeighborForeground(2)) { index |= 32; }
            if (octant.isNeighborForeground(1)) { index |= 16; }
        } else if (octant.isNeighborForeground(4)) {
            if (octant.isNeighborForeground(1)) { index |= 8; }
            if (octant.isNeighborForeground(3)) { index |= 4; }
            if (octant.isNeighborForeground(2)) { index |= 2; }
        } else if (octant.isNeighborForeground(3)) {
            if (octant.isNeighborForeground(2)) { index |= 8; }
            if (octant.isNeighborForeground(1)) { index |= 4; }
        } else if (octant.isNeighborForeground(2)) {
            if (octant.isNeighborForeground(1)) { index |= 2; }
        }

        return EULER_LUT[index];
    }

    private double calculateDeltaChi(final ImgPlus<BitType> img, final double eulerCharacteristic) {
        return 0.0;
    }

    /** @implNote elementVolume works correctly only if axes are linear */
    private double calculateConnectivityDensity(final double connectivity, final ImgPlus<BitType> imgPlus) {
        final long uSize = imgPlus.dimension(U_INDEX);
        final long vSize = imgPlus.dimension(V_INDEX);
        final long wSize = imgPlus.dimension(W_INDEX);

        final double uElementSize = imgPlus.axis(U_INDEX).averageScale(0, uSize);
        final double vElementSize = imgPlus.axis(V_INDEX).averageScale(0, vSize);
        final double wElementSize = imgPlus.axis(W_INDEX).averageScale(0, wSize);

        final double elementVolume = uElementSize * vElementSize * wElementSize;
        final double imgVolume = uSize * vSize * wSize;
        final double calibratedImgVolume = imgVolume / elementVolume;

        return connectivity / calibratedImgVolume;
    }

    public static final class Characteristics {
        public double eulerCharacteristic;
        public double deltaChi;
        public double connectivity;
        public double connectivityDensity;

        public Characteristics(final double eulerCharacteristic, final double deltaChi, final double connectivity,
                               final double connectivityDensity) {
            this.eulerCharacteristic = eulerCharacteristic;
            this.deltaChi = deltaChi;
            this.connectivity = connectivity;
            this.connectivityDensity = connectivityDensity;
        }
    }
}
