package org.bonej.ops.triplePointAngles;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
 * An Op which calculates the angles at each triple point in the given Graph
 * array. A triple point is a point where three edges in meet. The Graph array
 * is an output of AnalyzeSkeleton_ plugin.
 *
 * The second option of the Op controls the point from which the angle is
 * measured. Measuring the angle from the opposite vertices of the triple point
 * may be misleading if the edges are highly curved.
 *
 * @author Michael Doube
 * @author Richard Domander
 */
@Plugin(type = Op.class)
public class TriplePointAngles
		extends
			AbstractBinaryFunctionOp<Graph[], Integer, ImmutableList<ImmutableList<TriplePointAngles.Angles>>> {
	private static final int VERTEX_TO_VERTEX = -1;
	private UnaryFunctionOp<List<Vector3d>, Tuple3d> centroidOp;

	@Override
	public void initialize() {
		centroidOp = (UnaryFunctionOp) Functions.unary(ops(), CentroidLinAlg3d.class, Tuple3d.class, List.class);
	}

	/**
	 * Calculates the angles at the triple points in the given graphs
	 *
	 * @param graphs
	 *            An array of Graphs produced by the AnalyzeSkeleton_ plugin
	 * @param measurementPoint
	 *            if >= 0, then measure angle from the nth voxel (slab) of the
	 *            edge if == -1, then measure angle from the opposite vertex
	 * @return Lists of measured angles of the triple points in the graphs
	 */
	@Override
	public ImmutableList<ImmutableList<Angles>> compute2(final Graph[] graphs, final Integer measurementPoint) {
		final List<Angles> triplePoints = new ArrayList<>();
		final List<ImmutableList<Angles>> graphList = new ArrayList<>();

		for (int g = 0; g < graphs.length; g++) {
			final List<Vertex> vertices = graphs[g].getVertices();
			for (int v = 0; v < vertices.size(); v++) {
				final Vertex vertex = vertices.get(v);

				if (!isTriplePoint(vertex)) {
					continue;
				}

				double[] angles = triplePointAngles(vertex, measurementPoint);
				triplePoints.add(new Angles(g, v, ImmutableList.of(angles[0], angles[1], angles[2])));
			}
			graphList.add(ImmutableList.copyOf(triplePoints));
		}

		return ImmutableList.copyOf(graphList);
	}

	// region -- Helper methods --
	private static boolean isTriplePoint(final Vertex vertex) {
		return vertex.getBranches().size() == 3;
	}

	/**
	 * Calculates the angles of the triple point
	 * 
	 * @param vertex
	 *            A triple point in a Graph - must have three branches
	 * @param measurementPoint
	 *            if >= 0, then measure angle from the nth voxel (slab) of the
	 *            edge if == -1, then measure angle from the opposite vertex
	 * @return The three angles in an array
	 */
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

	/**
	 * Calculates the angle between the given edges at the given vertex
	 * 
	 * @param vertex
	 *            The meeting point of the edges
	 * @param edge0
	 *            One of the edges in the triple point
	 * @param edge1
	 *            Another edge in the triple point
	 * @param measurementPoint
	 *            if >= 0, then measure angle from the nth voxel (slab) of the
	 *            edge if == -1, then measure angle from the opposite vertices
	 * @return Angle in radians
	 */
	private double measureAngle(final Vertex vertex, final Edge edge0, final Edge edge1, final int measurementPoint) {
		final Vector3d anglePoint = (Vector3d) centroidOp.compute1(toVector3d(vertex.getPoints()));
		final Vector3d oppositePoint0 = getMeasurementPoint(vertex, edge0, measurementPoint);
		final Vector3d oppositePoint1 = getMeasurementPoint(vertex, edge1, measurementPoint);

		return joinedVectorAngle(oppositePoint0, oppositePoint1, anglePoint);
	}

	/**
	 * Returns the point from which the angle for the given edge is measured
	 *
	 * @param vertex
	 *            Point where the edge meets another edge (triple point)
	 * @param edge
	 *            Edge in the graph
	 * @param measurementPoint
	 *            if >= 0, then measure angle from the nth voxel (slab) of the
	 *            edge (counting from the vertex). if == -1, then measure angle
	 *            from the opposite vertices
	 * @return A Vector3d for @see joinedVectorAngle
	 */
	private Vector3d getMeasurementPoint(final Vertex vertex, final Edge edge, final int measurementPoint) {
		if (measurementPoint == VERTEX_TO_VERTEX || edge.getSlabs().isEmpty()) {
			return getOppositeCentroid(vertex, edge);
		}

		return getNthSlabOfEdge(vertex, edge, measurementPoint);
	}

	/**
	 * Returns the centroid of the opposite vertex of the given vertex along the
	 * given edge
	 */
	private Vector3d getOppositeCentroid(final Vertex vertex, final Edge edge) {
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

	/**
	 * Returns a copy of the given vector where each coordinate has been rounded
	 */
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
	 * than one. Can be used to check if vector p1 is in the 27-neighborhood of
	 * p0.
	 */
	private static boolean isAxesDistancesOne(final Vector3d p0, final Vector3d p1) {
		final double xDiff = Math.abs(p0.getX() - p1.getX());
		final double yDiff = Math.abs(p0.getY() - p1.getY());
		final double zDiff = Math.abs(p0.getZ() - p1.getZ());

		return xDiff <= 1 && yDiff <= 1 && zDiff <= 1;
	}
	// endregion

	// region -- Helper classes --
	/**
	 * A simple "struct record" class that contains the angles of a triple point
	 */
	public static final class Angles {
		/** The number of the graph in the image */
		public final int graphNumber;
		/** The number of the triple point in the graph */
		public final int triplePointNumber;
		/** The angles at the triple point in radians */
		public final ImmutableList<Double> angles;

        /**
         * @throws NullPointerException if angles == null
         * @throws IllegalArgumentException if angles.size() != 3
         */
		public Angles(final int graphNumber, final int triplePointNumber, final ImmutableList<Double> angles)
				throws NullPointerException, IllegalArgumentException {
			checkNotNull(angles, "Angles cannot be null");
			checkArgument(angles.size() == 3, "Must have three angles");

			this.graphNumber = graphNumber;
			this.triplePointNumber = triplePointNumber;
			this.angles = angles;
		}
	}
	// endregion
}
