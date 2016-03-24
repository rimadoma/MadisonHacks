package org.bonej.ops.triplePointAngles;

import static org.junit.Assert.assertEquals;

import net.imagej.ImageJ;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;

import org.bonej.ops.triplePointAngles.TriplePointAngles.TriplePoint;
import org.junit.BeforeClass;
import org.junit.Test;

import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.skeletonize3D.Skeletonize3D_;

import com.google.common.collect.ImmutableList;
import ij.ImagePlus;

/**
 * Unit tests for TriplePointAngles
 *
 * @author Richard Domander
 */
public class TriplePointAnglesTest {
    private static final double HALF_PI = Math.PI / 2.0;
	private static final ImageJ IMAGE_J = new ImageJ();
	private static Graph[] cuboidGraphs;
	private static BinaryFunctionOp<Graph[], Integer, ImmutableList<ImmutableList<TriplePoint>>> triplePointAnglesOp;

	@BeforeClass
	public static void oneTimeSetup() {
		// Generate test image
		ImagePlus imagePlus = StaticImagePlusGenerator.wireFrameCuboid(128, 128, 128, 32);

		// Skeletonize image
		final Skeletonize3D_ skeletonize3D = new Skeletonize3D_();
		skeletonize3D.setup("", imagePlus);
		skeletonize3D.run(null);

		// Get skeleton cuboidGraphs
		AnalyzeSkeleton_ analyzeSkeleton = new AnalyzeSkeleton_();
		analyzeSkeleton.setup("", imagePlus);
		analyzeSkeleton.run(AnalyzeSkeleton_.NONE, false, false, null, true, false);

		cuboidGraphs = analyzeSkeleton.getGraphs();

		// Match op
		triplePointAnglesOp = (BinaryFunctionOp) Functions.binary(IMAGE_J.op(), TriplePointAngles.class,
				ImmutableList.class, Graph.class, Integer.class);
	}

	/** Regression test */
	@Test
	public void testTriplePointAnglesNthPoint() throws AssertionError {
		final int nthPoint = 32;

		final ImmutableList<ImmutableList<TriplePoint>> results = triplePointAnglesOp.compute2(cuboidGraphs, nthPoint);

		results.listIterator().forEachRemaining(l -> l.listIterator().forEachRemaining(
				p -> p.angles.listIterator().forEachRemaining(a -> assertEquals(HALF_PI, a, 1e-12))));
	}

	/** Regression test */
	@Test
	public void testTriplePointAnglesVertexToVertex() throws AssertionError {
		final ImmutableList<ImmutableList<TriplePoint>> results = triplePointAnglesOp.compute2(cuboidGraphs, -1);

		// @todo rewrite with clearer loops
		results.listIterator().forEachRemaining(l -> l.listIterator()
				.forEachRemaining(p -> p.angles.listIterator().forEachRemaining(a -> assertEquals(HALF_PI, a, 1e-12))));
	}
}