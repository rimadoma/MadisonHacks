package org.bonej.testImageGenerators;

import net.imagej.ImageJ;
import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.stream.LongStream;

/**
 * An Op which draws a wire-frame cuboid.
 * Can be used, e.g. for testing other Ops or Plugins.
 *
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "wireFrameCuboidCreator")
public class WireFrameCuboidCreator extends AbstractOp {
    private long[] cuboidLocation = null;
    private long u0 = 0;
    private long u1 = 0;
    private long v0 = 0;
    private long v1 = 0;
    private long w0 = 0;
    private long w1 = 0;
    private long paddedUSize = 0;
    private long paddedVSize = 0;
    private long paddedWSize = 0;

    @Parameter
    OpService opService;

    @Parameter(type = ItemIO.INPUT)
    private long uSize = 0;

    @Parameter(type = ItemIO.INPUT)
    private long vSize = 0;

    @Parameter(type = ItemIO.INPUT)
    private long wSize = 0;

    @Parameter(type = ItemIO.INPUT, required = false)
    private long padding = 0;

    @Parameter(type = ItemIO.OUTPUT)
    private Img<BitType> cuboid;

    @Override
    public void run() {
        paddedUSize = uSize + 2 * padding;
        paddedVSize = vSize + 2 * padding;
        paddedWSize = wSize + 2 * padding;

        cuboid = opService.create().img(new FinalDimensions(paddedUSize, paddedVSize, paddedWSize), new BitType());
        cuboidLocation = new long[cuboid.numDimensions()];

        u0 = padding;
        u1 = padding + uSize - 1;
        v0 = padding;
        v1 = padding + vSize - 1;
        w0 = padding;
        w1 = padding + wSize - 1;

        drawCuboidEdges();
    }

    public static void main(String... args) {
        final ImageJ ij = net.imagej.Main.launch(args);
        Object cuboid = ij.op().run(WireFrameCuboidCreator.class, 100, 100, 10, 5);
        ij.ui().show(cuboid);
    }

    private void drawCuboidEdges() {
        setCuboidLocation(u0, v0, w0);
        drawLine(0, uSize);
        drawLine(1, vSize);
        drawLine(2, wSize);
        setCuboidLocation(u1, v0, w0);
        drawLine(1, vSize);
        drawLine(2, wSize);
        setCuboidLocation(u1, v1, w0);
        drawLine(2, wSize);
        setCuboidLocation(u0, v1, w0);
        drawLine(0, vSize);
        drawLine(2, wSize);

        setCuboidLocation(u0, v0, w1);
        drawLine(0, uSize);
        drawLine(1, vSize);
        setCuboidLocation(u1, v0, w1);
        drawLine(1, vSize);
        setCuboidLocation(u0, v1, w1);
        drawLine(0, vSize);
    }

    private void setCuboidLocation(long... location) {
        System.arraycopy(location, 0, cuboidLocation, 0, location.length);
    }

    private void drawLine(final int moveDim, final long length) {
        final RandomAccess<BitType> randomAccess = cuboid.randomAccess();
        randomAccess.setPosition(cuboidLocation);

        int counter = 0;
        while(counter < length) {
            randomAccess.get().setOne();
            randomAccess.fwd(moveDim);
            counter++;
        }
    }
}
