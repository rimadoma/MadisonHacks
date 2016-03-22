package org.bonej.ops.triplePointAngles;

import com.google.common.collect.ImmutableList;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.bonej.ops.triplePointAngles.TriplePointAngles.TriplePoint;
import org.junit.Test;
import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.skeletonize3D.Skeletonize3D_;

import static org.junit.Assert.assertEquals;

/**
 * @author Richard Domander
 */
public class TriplePointAnglesTest {
    private static final double HALF_PI = Math.PI / 2.0;

    /** @todo Make a regression test and/or speed up by generating skeletonized test image */
    @Test
    public void testTriplePointAnglesVertexToVertex() throws AssertionError {
        ImageJ IMAGE_J = new ImageJ();

        // Generate test image
        /** @todo Use testImageOps when ImgPlus-ImagePlus conversion OK, or plugins move to ImgPlus */
        ImagePlus imagePlus = StaticImagePlusGenerator.wireFrameCuboid(3, 3, 3, 1);

        // Skeletonize image
        final Skeletonize3D_ skeletonize3D = new Skeletonize3D_();
        skeletonize3D.setup("", imagePlus);
        skeletonize3D.run(null);

        // AnalyzeSkeleton
        AnalyzeSkeleton_ analyzeSkeleton = new AnalyzeSkeleton_();
        analyzeSkeleton.setup("", imagePlus);
        analyzeSkeleton.run(AnalyzeSkeleton_.NONE, false, false, null, true, false);

        // Run TriplePointAngles
        Graph[] graphs = analyzeSkeleton.getGraphs();
        ImmutableList<ImmutableList<TriplePoint>> results =
                (ImmutableList<ImmutableList<TriplePoint>>) IMAGE_J.op().run(TriplePointAngles.class, graphs, -1);

        // Assert results
        // @todo rewrite with clearer loops
        results.listIterator().forEachRemaining(l -> l.listIterator()
                .forEachRemaining(p -> p.angles.listIterator().forEachRemaining(a -> assertEquals(HALF_PI, a, 1e-12))));
    }
}