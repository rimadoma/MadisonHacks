package org.bonej.testImageGenerators;

import net.imagej.ImageJ;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
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
 * An Op which draws a wire-frame cuboid.
 * Can be used, e.g. for testing other Ops or Plugins.
 *
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "wireFrameCuboidCreator")
public class WireFrameCuboidCreator extends AbstractNullaryHybridCF<Img<BitType>> {
    @Parameter
    private OpService opService;

    @Parameter
    private long uSize;

    @Parameter
    private long vSize;

    @Parameter
    private long wSize;

    @Parameter(required = false)
    private long padding;

    private BinaryFunctionOp<Dimensions, BitType, Img<BitType>> createImgOp;

    public static void main(String... args) {
        final ImageJ ij = net.imagej.Main.launch(args);
        Object cuboid = ij.op().run(WireFrameCuboidCreator.class, 100, 100, 10, 5);
        ij.ui().show(cuboid);
    }

    private void drawCuboidEdges(Img<BitType> cuboid, long[] cuboidLocation, CuboidInfo info) {
        setCuboidLocation(cuboidLocation, info.u0, info.v0, info.w0);
        drawLine(cuboid, cuboidLocation, 0, uSize);
        drawLine(cuboid, cuboidLocation, 1, vSize);
        drawLine(cuboid, cuboidLocation, 2, wSize);
        setCuboidLocation(cuboidLocation, info.u1, info.v0, info.w0);
        drawLine(cuboid, cuboidLocation, 1, vSize);
        drawLine(cuboid, cuboidLocation, 2, wSize);
        setCuboidLocation(cuboidLocation, info.u1, info.v1, info.w0);
        drawLine(cuboid, cuboidLocation, 2, wSize);
        setCuboidLocation(cuboidLocation, info.u0, info.v1, info.w0);
        drawLine(cuboid, cuboidLocation, 0, vSize);
        drawLine(cuboid, cuboidLocation, 2, wSize);

        setCuboidLocation(cuboidLocation, info.u0, info.v0, info.w1);
        drawLine(cuboid, cuboidLocation, 0, uSize);
        drawLine(cuboid, cuboidLocation, 1, vSize);
        setCuboidLocation(cuboidLocation, info.u1, info.v0, info.w1);
        drawLine(cuboid, cuboidLocation, 1, vSize);
        setCuboidLocation(cuboidLocation, info.u0, info.v1, info.w1);
        drawLine(cuboid, cuboidLocation, 0, vSize);
    }

    private void setCuboidLocation(final long[] cuboidLocation, final long... location) {
        System.arraycopy(location, 0, cuboidLocation, 0, location.length);
    }

    private void drawLine(final Img<BitType> cuboid, final long[] cuboidLocation, final int moveDim, final long length) {
        final RandomAccess<BitType> randomAccess = cuboid.randomAccess();
        randomAccess.setPosition(cuboidLocation);

        int counter = 0;
        while(counter < length) {
            randomAccess.get().setOne();
            randomAccess.fwd(moveDim);
            counter++;
        }
    }

    @Override
    public void initialize() {
         createImgOp = (BinaryFunctionOp) Functions.binary(ops(), Ops.Create.Img.class, Img.class, Dimensions.class,
                 BitType.class);
    }

    @Override
    public void compute0(Img<BitType> cuboid) {
        final CuboidInfo info = new CuboidInfo(uSize, vSize, wSize, padding);

        final long[] cuboidLocation = new long[cuboid.numDimensions()];

        drawCuboidEdges(cuboid, cuboidLocation, info);
    }

    @Override
    public Img<BitType> createOutput() {
        final CuboidInfo info = new CuboidInfo(uSize, vSize, wSize, padding);
        return createImgOp.compute2(new FinalDimensions(info.paddedUSize, info.paddedVSize, info.paddedWSize),
                new BitType());
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

        CuboidInfo(final long uSize, final long vSize, final long wSize, final long padding) {
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
