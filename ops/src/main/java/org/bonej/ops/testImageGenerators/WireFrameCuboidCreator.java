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
 * Creates an ImgPlus<BitType> of a wire-frame cuboid. The ImgPlus has 3 spatial dimensions {X,Y,Z}.
 * Can be used, e.g. for testing other Ops or Plugins.
 *
 * @author Richard Domander
 * @todo Add menu path?
 */
@Plugin(type = Op.class, name = "wireFrameCuboidCreator", menuPath = "Plugins>Test Images>Wire-frame cuboid")
public class WireFrameCuboidCreator extends AbstractNullaryHybridCF<ImgPlus<BitType>> {
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

    private BinaryFunctionOp<Dimensions, BitType, Img<BitType>> createImgOp;

    public static void main(String... args) {
        final ImageJ ij = net.imagej.Main.launch(args);
        // Call the hybrid op without a ready buffer (null)
        Object cuboid =
                ij.op().run(WireFrameCuboidCreator.class, null, 100L, 100L, 10L, 5L, new double[]{0.2, 0.2, 0.2});
        ij.ui().show(cuboid);
    }

    private void drawCuboidEdges(ImgPlus<BitType> cuboid, CuboidInfo info) {
        setCuboidLocation(info.cuboidLocation, info.u0, info.v0, info.w0);
        drawLine(cuboid, info.cuboidLocation, 0, uSize);
        drawLine(cuboid, info.cuboidLocation, 1, vSize);
        drawLine(cuboid, info.cuboidLocation, 2, wSize);
        setCuboidLocation(info.cuboidLocation, info.u1, info.v0, info.w0);
        drawLine(cuboid, info.cuboidLocation, 1, vSize);
        drawLine(cuboid, info.cuboidLocation, 2, wSize);
        setCuboidLocation(info.cuboidLocation, info.u1, info.v1, info.w0);
        drawLine(cuboid, info.cuboidLocation, 2, wSize);
        setCuboidLocation(info.cuboidLocation, info.u0, info.v1, info.w0);
        drawLine(cuboid, info.cuboidLocation, 0, vSize);
        drawLine(cuboid, info.cuboidLocation, 2, wSize);

        setCuboidLocation(info.cuboidLocation, info.u0, info.v0, info.w1);
        drawLine(cuboid, info.cuboidLocation, 0, uSize);
        drawLine(cuboid, info.cuboidLocation, 1, vSize);
        setCuboidLocation(info.cuboidLocation, info.u1, info.v0, info.w1);
        drawLine(cuboid, info.cuboidLocation, 1, vSize);
        setCuboidLocation(info.cuboidLocation, info.u0, info.v1, info.w1);
        drawLine(cuboid, info.cuboidLocation, 0, vSize);
    }

    private void setCuboidLocation(final long[] cuboidLocation, final long... location) {
        System.arraycopy(location, 0, cuboidLocation, 0, location.length);
    }

    private void drawLine(final ImgPlus<BitType> cuboid, final long[] cuboidLocation, final int moveDim,
                          final long length) {
        final RandomAccess<BitType> randomAccess = cuboid.randomAccess();
        randomAccess.setPosition(cuboidLocation);

        int counter = 0;
        while (counter < length) {
            randomAccess.get().setOne();
            randomAccess.fwd(moveDim);
            counter++;
        }
    }

    @Override
    public void initialize() {
        // match an Op which creates an Img from Dimensions and Type
        createImgOp = (BinaryFunctionOp) Functions
                .binary(ops(), Ops.Create.Img.class, Img.class, Dimensions.class, new BitType());
    }

    @Override
    public void compute0(ImgPlus<BitType> cuboid) {
        final CuboidInfo info = new CuboidInfo(cuboid.numDimensions(), uSize, vSize, wSize, padding);
        drawCuboidEdges(cuboid, info);
    }

    @Override
    public ImgPlus<BitType> createOutput() {
        final long paddedUSize = uSize + 2 * padding;
        final long paddedVSize = vSize + 2 * padding;
        final long paddedWSize = wSize + 2 * padding;
        final Img<BitType> img =
                createImgOp.compute2(new FinalDimensions(paddedUSize, paddedVSize, paddedWSize), new BitType());

        return new ImgPlus<>(img, "Wire-frame cuboid", new AxisType[]{Axes.X, Axes.Y, Axes.Z}, calibration);
    }

    private final static class CuboidInfo {
        public long u0;
        public long u1;
        public long v0;
        public long v1;
        public long w0;
        public long w1;
        public long paddedUSize;
        public long paddedVSize;
        public long paddedWSize;
        public long[] cuboidLocation;

        private CuboidInfo(int dimensions, final long uSize, final long vSize, final long wSize, final long padding) {
            cuboidLocation = new long[dimensions];
            u0 = padding;
            u1 = padding + uSize - 1;
            v0 = padding;
            v1 = padding + vSize - 1;
            w0 = padding;
            w1 = padding + wSize - 1;
            paddedUSize = uSize + 2 * padding;
            paddedVSize = vSize + 2 * padding;
            paddedWSize = wSize + 2 * padding;
        }
    }
}
