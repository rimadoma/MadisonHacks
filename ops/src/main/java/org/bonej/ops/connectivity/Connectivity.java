package org.bonej.ops.connectivity;

import net.imagej.ImgPlus;
import net.imagej.ops.Contingent;
import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.Views;
import org.bonej.utilities.CalibratedAxisUtil;
import org.scijava.plugin.Plugin;

import java.util.Arrays;

/**
 * An Op which determines the number of connected structures in an ImgPlus image
 * by calculating the Euler characteristics of its elements.
 * The euler characteristic of an element is determined from its special 8-neighborhood.
 * The euler characteristics of the elements are summed the get the characteristic of the whole image.
 *
 * The algorithms here are based on the following articles:
 * Toriwaki J, Yonekura T (2002) Euler Number and Connectivity Indexes of a Three Dimensional Digital Picture.
 * Forma 17: 183-209.
 * <a href="http://www.scipress.org/journals/forma/abstract/1703/17030183.html">
 * http://www.scipress.org/journals/forma/abstract/1703/17030183.html</a>
 *
 * Odgaard A, Gundersen HJG (1993) Quantification of connectivity in cancellous bone,
 * with special emphasis on 3-D reconstructions.
 * Bone 14: 173-182.
 * <a href="http://dx.doi.org/10.1016/8756-3282(93)90245-6">doi:10.1016/8756-3282(93)90245-6</a>
 *
 * Lee TC, Kashyap RL, Chu CN (1994) Building Skeleton Models via 3- Medial Surface Axis Thinning Algorithms.
 * CVGIP: Graphical Models and Image Processing 56: 462-478.
 * <a href="http://dx.doi.org/10.1006/cgip.1994.1042">doi:10.1006/cgip.1994.1042</a>
 *
 * Several of the methods are based on Ignacio Arganda-Carreras's
 * Skeletonize3D_ plugin: <a href="http://imagejdocu.tudor.lu/doku.php?id=plugin:morphology:skeletonize3d:start">
 * Skeletonize3D homepage</a>
 *
 * @author Michael Doube
 * @author Richard Domander
 * @implNote Assuming that all axis are linear
 * @implNote Assuming that all dimensions are spatial
 * @implNote Assuming that there's only one continuous foreground particle in the image
 * @todo Split into smaller ops
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
        final double eulerCharacteristic = calculateEulerCharacteristic(imgPlus);
        final ConnectivityAccess connectivityAccess = new ConnectivityAccess(imgPlus);
        final double deltaChi = calculateDeltaChi(eulerCharacteristic, connectivityAccess);
        /** The connectivity of the sample = 1 - Δ(χ) */
        final double connectivity = 1 - deltaChi;
        final double connectivityDensity = calculateConnectivityDensity(connectivity, imgPlus);

        return new Characteristics(eulerCharacteristic, deltaChi, connectivity, connectivityDensity);
    }

    @Override
    public boolean conforms() {
        return in().numDimensions() == 3;
    }

    //region -- Helper methods --

    /** Calculates the Euler characteristic of the object in the image as though floating in space (χ). */
    private static double calculateEulerCharacteristic(final ImgPlus<BitType> imgPlus) {
        final int[] eulerSums = new int[(int) imgPlus.dimension(W_INDEX)];
        final Cursor<BitType> cursor = imgPlus.localizingCursor();
        final Octant octant = new Octant(imgPlus);

        cursor.forEachRemaining(c -> {
            long u = cursor.getLongPosition(U_INDEX);
            long v = cursor.getLongPosition(V_INDEX);
            long w = cursor.getLongPosition(W_INDEX);
            octant.setNeighborhood(u, v, w);
            eulerSums[(int) w] += getDeltaEuler(octant);
        });

        return Arrays.stream(eulerSums).sum() / 8.0;
    }

    private static int getDeltaEuler(final Octant octant) {
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

    /**
     * Calculates Δ(χ): the sample's contribution to the Euler characteristic of the structure to which it's connected.
     * Calculated by counting the intersections of foreground elements, and the edges of the imgPlus.
     */
    private static double calculateDeltaChi(final double eulerCharacteristic,
                                            final ConnectivityAccess connectivityAccess) {
        final double edgeCorrection = calculateEdgeCorrection(connectivityAccess);
        return eulerCharacteristic - edgeCorrection;
    }

    /**
     * Calculate a correction value to convert the Euler number of a stack to
     * the stack's contribution to the Euler number of whatever it is cut from.
     */
    private static double calculateEdgeCorrection(final ConnectivityAccess connectivityAccess) {
        final long chiZero = countBorderCornerForegroundElements(connectivityAccess);
        final long e = countBorderEdgeForegroundElements(connectivityAccess) + 3 * chiZero;
        final long c = countBorderFaceForegroundElements(connectivityAccess) + 2 * e - 3 * chiZero;

        // there are already 6 * chiZero in 2 * e, so remove 3 * chiZero

        final long d = countStackEdgeVertexIntersections(connectivityAccess) + chiZero;
        final long a = countStackFaceVertexIntersections(connectivityAccess);
        final long b = getFaceEdges(connectivityAccess);

        final double chiOne = d - e;
        final double chiTwo = a - b + c;

        return chiTwo / 2.0 + chiOne / 4.0 + chiZero / 8.0;
    }

    /**
     * "Count the number of intersections between voxel edges and stack faces."
     *
     * @implNote Blindly copying legacy code because I don't know what's happening here
     */
    private static long getFaceEdges(final ConnectivityAccess connectivityAccess) {
        long intersections = 0;

        // uv-plane faces
        for (long w = 0; w < connectivityAccess.wSize; w += connectivityAccess.wInc) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 0; v <= connectivityAccess.vSize; v++) {
                for (long u = 0; u <= connectivityAccess.uSize; u++) {
                    connectivityAccess.access.setPosition(u, U_INDEX);
                    connectivityAccess.access.setPosition(v, V_INDEX);
                    final boolean a = connectivityAccess.access.get().get();
                    if (a) {
                        intersections = intersections + 2;
                        continue;
                    }

                    connectivityAccess.access.setPosition(u - 1, U_INDEX);
                    connectivityAccess.access.setPosition(v, V_INDEX);
                    final boolean b = connectivityAccess.access.get().get();
                    if (b) {
                        intersections++;
                    }

                    connectivityAccess.access.setPosition(u, U_INDEX);
                    connectivityAccess.access.setPosition(v - 1, V_INDEX);
                    final boolean c = connectivityAccess.access.get().get();
                    if (c) {
                        intersections++;
                    }
                }
            }
        }

        // @todo any reason this stuff can't be done in the same loop?
        // uw-plane faces
        for (long v = 0; v <= connectivityAccess.vSize; v += connectivityAccess.vInc) {
            connectivityAccess.access.setPosition(v, V_INDEX);
            for (long u = 0; u <= connectivityAccess.uSize; u++) {
                connectivityAccess.access.setPosition(u, U_INDEX);
                // @todo why does w (z) start from 1?
                for (long w = 1; w <= connectivityAccess.wSize; w++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, w, W_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        for (long v = 0; v <= connectivityAccess.vSize; v += connectivityAccess.vInc) {
            connectivityAccess.access.setPosition(v, V_INDEX);
            for (long w = 0; w <= connectivityAccess.wSize; w++) {
                connectivityAccess.access.setPosition(w, W_INDEX);
                // @todo should u start from 1 here?
                for (long u = 0; u <= connectivityAccess.uSize; u++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, u, U_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        // @todo any reason this stuff can't be done in the same loop?
        // vw-plane faces
        for (long u = 0; u < connectivityAccess.uSize; u += connectivityAccess.uInc) {
            connectivityAccess.access.setPosition(u, U_INDEX);
            for (long v = 0; v <= connectivityAccess.vSize; v++) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long w = 1; w <= connectivityAccess.wSize; w++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, w, W_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        for (long u = 0; u < connectivityAccess.uSize; u += connectivityAccess.uInc) {
            connectivityAccess.access.setPosition(u, U_INDEX);
            for (long w = 0; w <= connectivityAccess.wSize; w++) {
                connectivityAccess.access.setPosition(w, W_INDEX);
                for (long v = 1; v <= connectivityAccess.vSize; v++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, v, W_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        return intersections;
    }

    /**
     * Count the number of intersections between foreground voxel vertices
     * and the border faces of the interval
     */
    private static long countStackFaceVertexIntersections(final ConnectivityAccess connectivityAccess) {
        long intersections = 0;

        // uv-plane faces
        for (long w = 0; w < connectivityAccess.wSize; w += connectivityAccess.wInc) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 0; v < connectivityAccess.vSize; v++) {
                for (long u = 0; u < connectivityAccess.uSize; u++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, u, v, U_INDEX, V_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        // vw-plane faces
        for (long u = 0; u < connectivityAccess.uSize; u += connectivityAccess.uInc) {
            connectivityAccess.access.setPosition(u, U_INDEX);
            for (long v = 0; v < connectivityAccess.vSize; v++) {
                for (long w = 1; w < connectivityAccess.wSize; w++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, v, w, V_INDEX, W_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        // uw-plane faces
        for (long v = 0; v < connectivityAccess.vSize; v += connectivityAccess.vInc) {
            connectivityAccess.access.setPosition(v, V_INDEX);
            for (long u = 1; u < connectivityAccess.uSize; u++) {
                for (long w = 1; w < connectivityAccess.wSize; w++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, u, w, U_INDEX, W_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        return intersections;
    }

    /**
     * Check if any of the elements in the given 4-neighborhood are foreground
     *
     * @param access A zero extended RandomAccess
     * @param pos0   The starting position of the neighborhood in the first dimension
     * @param pos1   The starting position of the neighborhood in the second dimension
     * @param dim0   The first dimension of the neighborhood
     * @param dim1   The second dimension of the neighborhood
     */
    private static boolean isNeighborhoodForeground(final RandomAccess<BitType> access, final long pos0,
                                                    final long pos1, final int dim0, final int dim1) {
        access.setPosition(pos0, dim0);
        access.setPosition(pos1, dim1);
        boolean a = access.get().get();

        access.setPosition(pos0 - 1, dim0);
        access.setPosition(pos1, dim1);
        boolean b = access.get().get();

        access.setPosition(pos0, dim0);
        access.setPosition(pos1 - 1, dim1);
        boolean c = access.get().get();

        access.setPosition(pos0 - 1, dim0);
        access.setPosition(pos1 - 1, dim1);
        boolean d = access.get().get();

        return a || b || c || d;
    }

    /**
     * Count the number of intersections between foreground voxel vertices
     * and the border edges of the interval
     */
    private static long countStackEdgeVertexIntersections(final ConnectivityAccess connectivityAccess) {
        long intersections = 0;

        // uv-plane edges
        for (long w = 0; w < connectivityAccess.wSize; w += connectivityAccess.wInc) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 0; v < connectivityAccess.vSize; v += connectivityAccess.vInc) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long u = 1; u < connectivityAccess.uSize; u++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, u, U_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        // vw-plane edges
        for (long w = 0; w < connectivityAccess.wSize; w += connectivityAccess.wInc) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long u = 0; u < connectivityAccess.uSize; u += connectivityAccess.uInc) {
                connectivityAccess.access.setPosition(u, U_INDEX);
                for (long v = 1; v < connectivityAccess.vSize; v++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, v, V_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        // uw-plane edges
        for (long u = 0; u < connectivityAccess.uSize; u += connectivityAccess.uInc) {
            connectivityAccess.access.setPosition(u, U_INDEX);
            for (long v = 0; v < connectivityAccess.vSize; v += connectivityAccess.vInc) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long w = 1; w < connectivityAccess.wSize; w++) {
                    if (isNeighborhoodForeground(connectivityAccess.access, w, W_INDEX)) {
                        intersections++;
                    }
                }
            }
        }

        return intersections;
    }

    /**
     * Check if any of the elements at the given 2-neighborhood are foreground
     *
     * @param access A zero extended RandomAccess
     * @param pos    The starting position of the neighborhood in the given dimension
     * @param dim    The dimension of the neighborhood
     */
    private static boolean isNeighborhoodForeground(final RandomAccess<BitType> access, final long pos, final int dim) {
        access.setPosition(pos, dim);
        boolean a = access.get().get();

        access.setPosition(pos - 1, dim);
        boolean b = access.get().get();

        return a || b;
    }

    /** Count the number of foreground elements on the faces that touch the borders of the Img interval */
    private static long countBorderFaceForegroundElements(final ConnectivityAccess connectivityAccess) {
        long foregroundElements = 0;

        // front and back (uv-plane) faces
        for (long w = 0; w < connectivityAccess.wSize; w += connectivityAccess.wInc) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 1; v < connectivityAccess.vSize - 1; v++) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long u = 1; u < connectivityAccess.uSize - 1; u++) {
                    connectivityAccess.access.setPosition(u, U_INDEX);
                    foregroundElements += connectivityAccess.access.get().getIntegerLong();
                }
            }
        }

        // top and bottom (uw-plane) faces
        for (long w = 1; w < connectivityAccess.wSize - 1; w++) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 0; v < connectivityAccess.vSize; v += connectivityAccess.vInc) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long u = 1; u < connectivityAccess.uSize - 1; u++) {
                    connectivityAccess.access.setPosition(u, U_INDEX);
                    foregroundElements += connectivityAccess.access.get().getIntegerLong();
                }
            }
        }

        // left and right (vw-plane) faces
        for (long w = 1; w < connectivityAccess.wSize - 1; w++) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 1; v < connectivityAccess.vSize - 1; v++) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long u = 0; u < connectivityAccess.uSize; u += connectivityAccess.uInc) {
                    connectivityAccess.access.setPosition(u, U_INDEX);
                    foregroundElements += connectivityAccess.access.get().getIntegerLong();
                }
            }
        }

        return foregroundElements;
    }

    /** Count the number of foreground elements in the edges that line the borders of the Img interval */
    private static long countBorderEdgeForegroundElements(final ConnectivityAccess connectivityAccess) {
        long foregroundElements = 0;

        // left to right (u-axis) edges
        for (long w = 0; w < connectivityAccess.wSize; w += connectivityAccess.wInc) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 0; v < connectivityAccess.vSize; v += connectivityAccess.vInc) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long u = 1; u < connectivityAccess.uSize - 1; u++) {
                    connectivityAccess.access.setPosition(u, U_INDEX);
                    foregroundElements += connectivityAccess.access.get().getIntegerLong();
                }
            }
        }

        // top to bottom (v-axis) edges
        for (long w = 0; w < connectivityAccess.wSize; w += connectivityAccess.wInc) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 1; v < connectivityAccess.vSize - 1; v++) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long u = 0; u < connectivityAccess.uSize; u += connectivityAccess.uInc) {
                    connectivityAccess.access.setPosition(u, U_INDEX);
                    foregroundElements += connectivityAccess.access.get().getIntegerLong();
                }
            }
        }

        // back to front (w-axis) edges
        for (long w = 1; w < connectivityAccess.wSize - 1; w++) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 0; v < connectivityAccess.vSize; v += connectivityAccess.vInc) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long u = 0; u < connectivityAccess.uSize; u += connectivityAccess.uInc) {
                    connectivityAccess.access.setPosition(u, U_INDEX);
                    foregroundElements += connectivityAccess.access.get().getIntegerLong();
                }
            }
        }

        return foregroundElements;
    }

    /** Count the number of foreground elements in the bordering corners of the Img interval */
    private static long countBorderCornerForegroundElements(final ConnectivityAccess connectivityAccess) {
        long foregroundElements = 0;

        for (long w = 0; w < connectivityAccess.wSize; w += connectivityAccess.wInc) {
            connectivityAccess.access.setPosition(w, W_INDEX);
            for (long v = 0; v < connectivityAccess.vSize; v += connectivityAccess.vInc) {
                connectivityAccess.access.setPosition(v, V_INDEX);
                for (long u = 0; u < connectivityAccess.uSize; u += connectivityAccess.uInc) {
                    connectivityAccess.access.setPosition(u, U_INDEX);
                    foregroundElements += connectivityAccess.access.get().getIntegerLong();
                }
            }
        }

        return foregroundElements;
    }

    /** Calculates the connectivity density of the sample by dividing connectivity by the calibrated size of the interval */
    private static double calculateConnectivityDensity(final double connectivity, final ImgPlus<BitType> imgPlus) {
        final double calibratedImgVolume = CalibratedAxisUtil.calibratedSpaceSize(imgPlus);
        return connectivity / calibratedImgVolume;
    }
    //endregion

    //region -- Helper classes --
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

    /**
     * A Convenience class used to traverse the plugin's input ImgPlus.
     * If these fields were part of the parent class,
     * it wouldn't be threadsafe
     *
     * @implNote element sizes are accurate only if axes are linear
     */
    private static final class ConnectivityAccess {
        public final RandomAccess<BitType> access;
        /** The calibrated size of an element in the 1st dimension */
        public final double uElementSize;
        /** The calibrated size of an element in the 2nd dimension */
        public final double vElementSize;
        /** The calibrated size of an element in the 3rd dimension */
        public final double wElementSize;

        /** Number of elements (voxels) in the 1st dimension */
        public final long uSize;
        /** Number of elements (voxels) in the 2nd dimension */
        public final long vSize;
        /** Number of elements (voxels) in the 3rd dimension */
        public final long wSize;

        /** Increment value to jump to the end of 1st dimension in a loop */
        public final long uInc;
        /** Increment value to jump to the end of 2nd dimension in a loop */
        public final long vInc;
        /** Increment value to jump to the end of 3rd dimension in a loop */
        public final long wInc;

        public ConnectivityAccess(final ImgPlus<BitType> img) {
            access = Views.extendZero(img).randomAccess();

            uSize = img.dimension(U_INDEX);
            vSize = img.dimension(V_INDEX);
            wSize = img.dimension(W_INDEX);

            uElementSize = img.axis(U_INDEX).averageScale(0.0, uSize);
            vElementSize = img.axis(V_INDEX).averageScale(0.0, vSize);
            wElementSize = img.axis(W_INDEX).averageScale(0.0, wSize);

            uInc = Math.max(1L, uSize - 1L);
            vInc = Math.max(1L, vSize - 1L);
            wInc = Math.max(1L, wSize - 1L);
        }
    }
    //endregion
}
