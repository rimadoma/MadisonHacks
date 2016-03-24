package org.bonej.ops.triplePointAngles;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;

import org.bonej.ops.geom.CentroidLinAlg3d;
import org.scijava.plugin.Plugin;
import org.scijava.vecmath.Tuple3d;
import org.scijava.vecmath.Vector3d;

import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.analyzeSkeleton.Point;
import sc.fiji.analyzeSkeleton.Vertex;

import com.google.common.collect.ImmutableList;

/**
 * @author Richard Domander
 */
@Plugin(type = Op.class)
public class TriplePointAngles
		extends
			AbstractBinaryFunctionOp<Graph[], Integer, ImmutableList<ImmutableList<TriplePointAngles.TriplePoint>>> {
	private static final int VERTEX_TO_VERTEX = -1;
	private UnaryFunctionOp<List<Vector3d>, Tuple3d> centroidOp;

	@Override
	public void initialize() {
		centroidOp = (UnaryFunctionOp) Functions.unary(ops(), CentroidLinAlg3d.class, Tuple3d.class, List.class);
	}

	@Override
	public ImmutableList<ImmutableList<TriplePoint>> compute2(final Graph[] graphs, final Integer measurementPoint) {
		final List<TriplePoint> triplePoints = new ArrayList<>();
		final List<ImmutableList<TriplePoint>> skeletons = new ArrayList<>();

		for (int g = 0; g < graphs.length; g++) {
			final List<Vertex> vertices = graphs[g].getVertices();
			for (int v = 0; v < vertices.size(); v++) {
				final Vertex vertex = vertices.get(v);

				if (!isTriplePoint(vertex)) {
					continue;
				}

				double[] angles = triplePointAngles(vertex, measurementPoint);
				triplePoints.add(new TriplePoint(g, v, ImmutableList.of(angles[0], angles[1], angles[2])));
			}
			skeletons.add(ImmutableList.copyOf(triplePoints));
		}

		return ImmutableList.copyOf(skeletons);
	}

	// region -- Helper methods --
	private static boolean isTriplePoint(final Vertex vertex) {
		return vertex.getBranches().size() == 3;
	}

	private double[] triplePointAngles(final Vertex vertex, final int measurementPoint) {
		ArrayList<Edge> edges = vertex.getBranches();
		Edge edge0 = edges.get(0);
		Edge edge1 = edges.get(1);
		Edge edge2 = edges.get(2);

		double thetas[] = new double[3];

		thetas[0] = measureAngle(vertex, edge0, edge1, measurementPoint);
		thetas[1] = measureAngle(vertex, edge0, edge2, measurementPoint);
		thetas[2] = measureAngle(vertex, edge1, edge2, measurementPoint);

		return thetas;
	}

	private double measureAngle(final Vertex vertex, final Edge edge0, final Edge edge1, final int measurementPoint) {
		final Vector3d anglePoint = (Vector3d) centroidOp.compute1(toVector3d(vertex.getPoints()));
		final Vector3d oppositePoint0 = getMeasurementPoint(vertex, edge0, measurementPoint);
		final Vector3d oppositePoint1 = getMeasurementPoint(vertex, edge1, measurementPoint);

		return joinedVectorAngle(oppositePoint0, oppositePoint1, anglePoint);
	}

	private Vector3d getMeasurementPoint(final Vertex vertex, final Edge edge, final int measurementPoint) {
		if (measurementPoint == VERTEX_TO_VERTEX || edge.getSlabs().isEmpty()) {
			return getOppositePoint(vertex, edge);
		}

		return getNthSlabOfEdge(vertex, edge, measurementPoint);
	}

	private Vector3d getOppositePoint(final Vertex vertex, final Edge edge) {
		final Vertex oppositeVertex = edge.getOppositeVertex(vertex);
		final List<Vector3d> oppositeVertexVectors = toVector3d(oppositeVertex.getPoints());
		return (Vector3d) centroidOp.compute1(oppositeVertexVectors);
	}

	private List<Vector3d> toVector3d(final List<Point> points) {
		return points.stream().map(this::toVector3d).collect(Collectors.toList());
	}

	private Vector3d toVector3d(final Point point) {
		return new Vector3d(point.x, point.y, point.z);
	}

	/** @todo Discuss rounding with mdoube - expected or unexpected results? */
	private double joinedVectorAngle(final Vector3d p0, final Vector3d p1, final Vector3d tail) {
		// Round vectors to whole numbers to avoid angle measurement errors
		Vector3d u = roundVector(p0);
		Vector3d v = roundVector(p1);
		Vector3d t = roundVector(tail);

		u.sub(t);
		v.sub(t);

		return u.angle(v);
	}

	private Vector3d roundVector(final Vector3d vector) {
		final long x = Math.round(vector.getX());
		final long y = Math.round(vector.getY());
		final long z = Math.round(vector.getZ());
		return new Vector3d(x, y, z);
	}

	/** Return the nth edge slab away from the given vertex */
	private Vector3d getNthSlabOfEdge(final Vertex vertex, final Edge edge, final int nthSlab) {
		final List<Vector3d> slabs = toVector3d(edge.getSlabs());
		final Vector3d firstSlab = slabs.get(0);
		final List<Vector3d> vertexPoints = toVector3d(vertex.getPoints());

		// Check if the given edge starts from the given vertex,
		// or its opposite vertex
		final boolean startsAtVertex = vertexPoints.stream().anyMatch(p -> isAxesDistancesOne(firstSlab, p));

		final int slabIndex = Math.min(Math.max(0, nthSlab), slabs.size() - 1);

		if (startsAtVertex) {
			return slabs.get(slabIndex);
		} else {
			final int slabIndexFromEnd = slabs.size() - slabIndex - 1;
			return slabs.get(slabIndexFromEnd);
		}
	}

	/**
	 * Returns true if the distance of the two vectors in each dimension is less
	 * than one. Can be used to check if vector p1 is in the 27-neighborhood of p0.
	 */
	private static boolean isAxesDistancesOne(final Vector3d p0, final Vector3d p1) {
		final double xDiff = Math.abs(p0.getX() - p1.getX());
		final double yDiff = Math.abs(p0.getY() - p1.getY());
		final double zDiff = Math.abs(p0.getZ() - p1.getZ());

		return xDiff <= 1 && yDiff <= 1 && zDiff <= 1;
	}
	// endregion

	// region -- Helper classes --
	public static final class TriplePoint {
		public final int skeletonNumber;
		public final int vertexNumber;
		public final ImmutableList<Double> angles;

		public TriplePoint(final int skeletonNumber, final int vertexNumber, final ImmutableList<Double> angles) {
			this.skeletonNumber = skeletonNumber;
			this.vertexNumber = vertexNumber;
			this.angles = angles;
		}
	}
	// endregion
}
