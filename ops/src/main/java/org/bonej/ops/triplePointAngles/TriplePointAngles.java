package org.bonej.ops.triplePointAngles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.imagej.ImageJ;
import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.RealLocalizable;

import org.bonej.ops.geom.CentroidVecMath3d;
import org.scijava.plugin.Plugin;
import org.scijava.vecmath.Tuple3d;
import org.scijava.vecmath.Vector3d;

import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.analyzeSkeleton.Point;
import sc.fiji.analyzeSkeleton.Vertex;

import com.google.common.collect.ImmutableList;

import ij.ImagePlus;
import ij.io.Opener;

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
		centroidOp = (UnaryFunctionOp) Functions.unary(ops(), CentroidVecMath3d.class, Tuple3d.class, List.class);
	}

	@Override
	public ImmutableList<ImmutableList<TriplePoint>> compute2(final Graph[] graphs, final Integer nthPoint) {
		final List<TriplePoint> triplePoints = new ArrayList<>();
		final List<ImmutableList<TriplePoint>> skeletons = new ArrayList<>();

		for (int g = 0; g < graphs.length; g++) {
			final List<Vertex> vertices = graphs[g].getVertices();
			for (int v = 0; v < vertices.size(); v++) {
				final Vertex vertex = vertices.get(v);

				if (!isTriplePoint(vertex)) {
					continue;
				}

				double[] angles = triplePointAngles(vertex, nthPoint);
				triplePoints.add(new TriplePoint(g, v, ImmutableList.of(angles[0], angles[1], angles[2])));
			}
			skeletons.add(ImmutableList.copyOf(triplePoints));
		}

		return ImmutableList.copyOf(skeletons);
	}

	// region -- Utility methods --

	public static void main(String... args) {
		// Open test image
		File file = new File("./../SkeletonizedWFcuboid.tif");
		final ImageJ ij = new ImageJ();
		ImagePlus imagePlus = new Opener().openImage(file.getAbsolutePath());

		// AnalyzeSkeleton
		AnalyzeSkeleton_ analyzeSkeleton = new AnalyzeSkeleton_();
		analyzeSkeleton.setup("", imagePlus);
		analyzeSkeleton.run(AnalyzeSkeleton_.NONE, false, false, null, true, false);

		// Run TriplePointAngles
		Graph[] graphs = analyzeSkeleton.getGraphs();
		ImmutableList<ImmutableList<TriplePoint>> results = (ImmutableList<ImmutableList<TriplePoint>>) ij.op()
				.run(TriplePointAngles.class, graphs, -1);

		// Print results
		results.listIterator().forEachRemaining(l -> l.listIterator().forEachRemaining(r -> {
			System.out.println("Skeleton #" + r.skeletonNumber);
			System.out.println("Vertex #" + r.vertexNumber);
			r.angles.forEach(a -> System.out.print(a + " "));
			System.out.println("\n");
		}));

		ij.context().dispose();
	}

	// region -- Helper methods --
	private static boolean isTriplePoint(final Vertex vertex) {
		return vertex.getBranches().size() == 3;
	}

	private double[] triplePointAngles(final Vertex vertex, final int nthPoint) {
		ArrayList<Edge> edges = vertex.getBranches();
		Edge edge0 = edges.get(0);
		Edge edge1 = edges.get(1);
		Edge edge2 = edges.get(2);

		double thetas[] = new double[3];

		if (nthPoint == VERTEX_TO_VERTEX) {
			thetas[0] = vertexToVertexAngle(vertex, edge0, edge1);
			thetas[1] = vertexToVertexAngle(vertex, edge0, edge2);
			thetas[2] = vertexToVertexAngle(vertex, edge1, edge2);
		} else {
			thetas[0] = vertexAngle(vertex, edge0, edge1, nthPoint);
			thetas[1] = vertexAngle(vertex, edge0, edge2, nthPoint);
			thetas[2] = vertexAngle(vertex, edge1, edge2, nthPoint);
		}

		return thetas;
	}

	/**
	 * Measure the angle between edge0, edge1 at vertex The measuring point is
	 * the opposing vertex at each edge
	 *
	 * @todo refactor to angle(vector c, vector 0, vector 1) where 0, 1 change
	 *       based on nthPoint
	 */
	private double vertexToVertexAngle(final Vertex vertex, final Edge edge0, final Edge edge1) {
		final List<Vector3d> vertexPoints = toVector3d(vertex.getPoints());
		final List<Vector3d> oppositePoints0 = toVector3d(edge0.getOppositeVertex(vertex).getPoints());
		final List<Vector3d> oppositePoints1 = toVector3d(edge1.getOppositeVertex(vertex).getPoints());

		final Vector3d centroid = (Vector3d) centroidOp.compute1(vertexPoints);
		final Vector3d oppositeCentroid0 = (Vector3d) centroidOp.compute1(oppositePoints0);
		final Vector3d oppositeCentroid1 = (Vector3d) centroidOp.compute1(oppositePoints1);

		return joinedVectorAngle(oppositeCentroid0, oppositeCentroid1, centroid);
	}

	private List<Vector3d> toVector3d(final List<Point> points) {
		return points.stream().map(this::toVector3d).collect(Collectors.toList());
	}

	private Vector3d toVector3d(final Point point) {
		return new Vector3d(point.x, point.y, point.z);
	}

	/**
	 * Measure the angle between edges 0 & 1 at vertex The measuring point is
	 * the nth slab element of each edge
	 */
	private double vertexAngle(final Vertex vertex, final Edge edge0, final Edge edge1, final int nthPoint) {
		final Vector3d p0 = getNthPointOfEdge(vertex, edge0, nthPoint);
		final Vector3d p1 = getNthPointOfEdge(vertex, edge1, nthPoint);
		final Vector3d vertexCentroid = (Vector3d) centroidOp.compute1(toVector3d(vertex.getPoints()));

		return joinedVectorAngle(p0, p1, vertexCentroid);
	}

	private double joinedVectorAngle(final Vector3d p0, final Vector3d p1, final Vector3d tail) {
		p0.sub(tail);
		p1.sub(tail);

		return p0.angle(p1);
	}

	/** Return the edge element n steps away from the given vertex */
	private Vector3d getNthPointOfEdge(final Vertex vertex, final Edge edge, final int nthPoint) {
		final List<Vector3d> edgeSlabPoints = toVector3d(edge.getSlabs());

		if (edgeSlabPoints.isEmpty()) {
			// No slabs, edge has only an end-point and a junction point
			final List<Vector3d> endPoints = toVector3d(edge.getOppositeVertex(vertex).getPoints());
			return (Vector3d) centroidOp.compute1(endPoints);
		}

		final Vector3d edgeStart = edgeSlabPoints.get(0);
		final List<Vector3d> vertexPoints = toVector3d(vertex.getPoints());
		final boolean startsAtVertex = vertexPoints.stream().anyMatch(p -> is27Connected(edgeStart, p));

		final int edgeIndex = Math.min(Math.max(0, nthPoint), edgeSlabPoints.size() - 1);

		if (startsAtVertex) {
			return edgeSlabPoints.get(edgeIndex);
		} else {
			final int edgeIndexFromEnd = edgeSlabPoints.size() - edgeIndex - 1;
			return edgeSlabPoints.get(edgeIndexFromEnd);
		}
	}

	private static boolean is27Connected(final Vector3d p0, final Vector3d p1) {
		final double xDiff = Math.abs(p0.getX() - p1.getX());
		final double yDiff = Math.abs(p0.getY() - p1.getY());
		final double zDiff = Math.abs(p0.getZ() - p1.getZ());

		return xDiff <= 1 && yDiff <= 1 && zDiff <= 1;
	}

	private static Optional<Double> getPosition(final RealLocalizable r, final int dimension) {
		return dimension >= 0 && dimension < r.numDimensions()
				? Optional.of(r.getDoublePosition(dimension))
				: Optional.empty();
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
