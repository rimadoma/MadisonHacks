package org.bonej.ops.triplePointAngles;

import static org.junit.Assert.assertEquals;

import net.imagej.ImageJ;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;

import org.bonej.ops.triplePointAngles.TriplePointAngles.TriplePoint;
import org.junit.AfterClass;
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
 * @todo Write mock Graph[] instead of running test image trough tool chain
 * @todo Write tests for a Graph[] of a 2D image
 */
public class TriplePointAnglesTest {
	private static final double HALF_PI = Math.PI / 2.0;
	private static final ImageJ IMAGE_J = new ImageJ();
	private static Graph[] cuboidGraphs;
	private static BinaryFunctionOp<Graph[], Integer, ImmutableList<ImmutableList<TriplePoint>>> triplePointAnglesOp;

	@BeforeClass
	public static void oneTimeSetup() {
		// Generate test image
		Object imagePlus = StaticImagePlusGenerator.wireFrameCuboid(5, 5, 5, 1);

		// Skeletonize image
		final Skeletonize3D_ skeletonize3D = new Skeletonize3D_();
		skeletonize3D.setup("", (ImagePlus) imagePlus);
		skeletonize3D.run(null);

		// Get skeleton graphs
		AnalyzeSkeleton_ analyzeSkeleton = new AnalyzeSkeleton_();
		analyzeSkeleton.setup("", (ImagePlus) imagePlus);
		analyzeSkeleton.run(AnalyzeSkeleton_.NONE, false, false, null, true, false);

		cuboidGraphs = analyzeSkeleton.getGraphs();

		// Match op
		triplePointAnglesOp = (BinaryFunctionOp) Functions.binary(IMAGE_J.op(), TriplePointAngles.class,
																  ImmutableList.class, Graph.class, Integer.class);
	}

	@AfterClass
	public static void oneTimeTearDown() {
		IMAGE_J.context().dispose();
	}

	/** Regression test */
	@Test
	public void testTriplePointAnglesNthPoint() throws AssertionError {
		final int nthPoint = 2;

		final ImmutableList<ImmutableList<TriplePoint>> graphs = triplePointAnglesOp.compute2(cuboidGraphs, nthPoint);

		assertEquals("Wrong number of skeletons (graphs)", 1, graphs.size());
		final ImmutableList<TriplePoint> triplePoints = graphs.get(0);
		assertEquals("Wrong number of triple points", 8, triplePoints.size());
		for (int t = 0; t < triplePoints.size(); t++) {
			final ImmutableList<Double> angles = triplePoints.get(t).angles;
			assertEquals("Wrong number of angles", angles.size(), 3);
			angles.forEach(a -> assertEquals("Triple point angle should be a right angle", HALF_PI, a, 1e-12));
		}
	}

	/** Regression test */
	@Test
	public void testTriplePointAnglesVertexToVertex() throws AssertionError {
		final ImmutableList<ImmutableList<TriplePoint>> graphs = triplePointAnglesOp.compute2(cuboidGraphs,
																							  TriplePointAngles.VERTEX_TO_VERTEX);

		assertEquals("Wrong number of skeletons (graphs)", 1, graphs.size());
		final ImmutableList<TriplePoint> triplePoints = graphs.get(0);
		assertEquals("Wrong number of triple points", 8, triplePoints.size());
		for (int t = 0; t < triplePoints.size(); t++) {
			final ImmutableList<Double> angles = triplePoints.get(t).angles;
			assertEquals("Wrong number of angles", angles.size(), 3);
			angles.forEach(a -> assertEquals("Triple point angle should be a right angle", HALF_PI, a, 1e-12));
		}
	}
}