package org.bonej.ops.testImageGenerators;

import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.hybrid.AbstractNullaryHybridCF;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Creates an ImgPlus<BitType> of a cuboid. The ImgPlus has 3 spatial dimensions {X,Y,Z}.
 * Can be used, e.g. for testing other Ops.
 *
 * @author Richard Domander
 * @todo Reduce reuse of code in cuboid creators
 * @todo Add menu path?
 */
@Plugin(type = Op.class, name = "cuboidCreator")
public class CuboidCreator extends AbstractNullaryHybridCF<ImgPlus<BitType>> {
    private static final int W_INDEX = 2;
    private static final int V_INDEX = 1;
    private static final int U_INDEX = 0;
    private static BinaryFunctionOp<Dimensions, BitType, Img<BitType>> createImgOp;

    @Parameter
    private long uSize;

    @Parameter
    private long vSize;

    @Parameter
    private long wSize;

    @Parameter(required = false)
    private long padding;

    @Parameter(required = false)
    private double[] calibration;

    @Override
    public void compute0(final ImgPlus<BitType> output) {
        drawCuboid(output);
    }

    @Override
    public ImgPlus<BitType> createOutput() {
        final long totalPadding = 2 * padding;
        final long paddedUSize = uSize + totalPadding;
        final long paddedVSize = vSize + totalPadding;
        final long paddedWSize = wSize + totalPadding;
        final Img<BitType> img =
                createImgOp.compute2(new FinalDimensions(paddedUSize, paddedVSize, paddedWSize), new BitType());

        return new ImgPlus<>(img, "Wire-frame cuboid", new AxisType[]{Axes.X, Axes.Y, Axes.Z}, calibration);
    }

    @Override
    public void initialize() {
        createImgOp = (BinaryFunctionOp) Functions
                .binary(ops(), Ops.Create.Img.class, Img.class, Dimensions.class, new BitType());
    }

    //region --Utility methods--
    public static void main(String... args) {
        final ImageJ ij = new ImageJ();
        Object cuboid = ij.op().run(CuboidCreator.class, null, 100L, 100L, 100L, 10L);
        ij.ui().show(cuboid);
    }
    //endregion

    //region --Helper methods--
    private void drawCuboid(final ImgPlus<BitType> output) {
        RandomAccess<BitType> access = output.randomAccess();

        for (long w = padding; w < padding + wSize; w++) {
            access.setPosition(w, W_INDEX);
            for (long v = padding; v < padding + vSize; v++) {
                access.setPosition(v, V_INDEX);
                for (long u = padding; u < padding + uSize; u++) {
                    access.setPosition(u, U_INDEX);
                    access.get().setOne();
                }
            }
        }
    }
    //endregion
}