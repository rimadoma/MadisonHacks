package org.bonej.ops.geom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import net.imagej.ImageJ;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;

import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.vecmath.Tuple3d;
import org.scijava.vecmath.Vector3d;

import com.google.common.collect.ImmutableList;

/**
 * Unit tests for the CentroidLinAlg3d Op
 *
 * @author Richard Domander
 */
public class CentroidLinAlg3DTest {
	private static final ImageJ IMAGE_J = new ImageJ();
	private static UnaryFunctionOp<Collection<? extends Tuple3d>, Tuple3d> centroidOp;

	@BeforeClass
	public static void oneTimeSetUp() {
		centroidOp = (UnaryFunctionOp) Functions.unary(IMAGE_J.op(), CentroidLinAlg3d.class, Tuple3d.class,
				ImmutableList.class);
	}

	@Test (expected = NullPointerException.class)
	public void testCentroidLinAlg3dThrowsNullPointerExceptionIfCollectionIsNull() {
		centroidOp.compute1(null);
	}

	@Test
	public void testCentroidLinAlg3dWithEmptyCollection() {
		final ImmutableList<Vector3d> emptyVectors = ImmutableList.of();

		final Tuple3d result = centroidOp.compute1(emptyVectors);

		assertTrue("Result should be (NaN, NaN, NaN) - x is not", Double.isNaN(result.x));
		assertTrue("Result should be (NaN, NaN, NaN) - y is not", Double.isNaN(result.y));
		assertTrue("Result should be (NaN, NaN, NaN) - z is not", Double.isNaN(result.z));
	}

	@Test
	public void testCentroidLinAlg3dWithSingleVector() {
		final Vector3d expected = new Vector3d(1.0, 2.0, 3.0);
		final ImmutableList<Vector3d> vectors = ImmutableList.of(expected);

		final Tuple3d result = centroidOp.compute1(vectors);

		assertEquals("Result should equal the single input vector", expected, result);
	}

	@Test
	public void testCentroidLinAlg3d() {
		final Vector3d expected = new Vector3d(0.5, 0.5, 0.5);
		final ImmutableList<Vector3d> cubeVectors = ImmutableList.of(
                new Vector3d(0.0, 0.0, 0.0), new Vector3d(1.0, 0.0, 0.0),
                new Vector3d(1.0, 1.0, 0.0), new Vector3d(0.0, 1.0, 0.0),
				new Vector3d(0.0, 0.0, 1.0), new Vector3d(1.0, 0.0, 1.0),
                new Vector3d(1.0, 1.0, 1.0), new Vector3d(0.0, 1.0, 1.0));

		final Tuple3d result = centroidOp.compute1(cubeVectors);

		assertEquals("Incorrect centroid vector", expected, result);
	}
}