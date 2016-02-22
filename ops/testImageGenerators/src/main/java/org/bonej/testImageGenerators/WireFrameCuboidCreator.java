package org.bonej.testImageGenerators;

import net.imagej.ImageJ;
import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.logic.BitType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * An Op which draws a wire-frame cuboid.
 * Can be used, e.g. for testing other Ops or Plugins.
 *
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "wireFrameCuboidCreator")
public class WireFrameCuboidCreator extends AbstractOp {
    private long[] cuboidLocation;

    @Parameter
    OpService opService;

    @Parameter(type = ItemIO.INPUT)
    private long size0 = 0;

    @Parameter(type = ItemIO.INPUT)
    private long size1 = 0;

    @Parameter(type = ItemIO.INPUT)
    private long size2 = 0;

    @Parameter(type = ItemIO.OUTPUT)
    private Img<BitType> cuboid;

    @Override
    public void run() {
        cuboid = opService.create().img(new FinalDimensions(size0, size1, size2), new BitType());
        cuboidLocation = new long[cuboid.numDimensions()];

        drawRect(0);

        LongStream.range(0, size2).forEach(this::drawCorners);

        drawRect(size2 - 1);
    }

    public static void main(String... args) {
        final ImageJ ij = net.imagej.Main.launch(args);
        Object cuboid = ij.op().run(WireFrameCuboidCreator.class, 100, 100, 10);
        ij.ui().show(cuboid);
    }

    private void setCuboidLocation(long... location) {
        Arrays.fill(cuboidLocation, 0);
        System.arraycopy(location, 0, cuboidLocation, 0, location.length);
    }

    private void drawRect(final long plane) {
        setCuboidLocation(0, 0, plane);
        drawLine(0, size0);
        drawLine(1, size1);
        setCuboidLocation(0, size1 - 1, plane);
        drawLine(0, size0);
        setCuboidLocation(size0 - 1, 0, plane);
        drawLine(1, size1);
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

    private void drawCorners(final long plane) {
        setCuboidLocation(0, 0, plane);
        drawLine(0, 1);
        setCuboidLocation(0, size1 - 1, plane);
        drawLine(0, 1);
        setCuboidLocation(size0 - 1, size1 - 1, plane);
        drawLine(0, 1);
        setCuboidLocation(size0 - 1, 0, plane);
        drawLine(0, 1);
    }
}
