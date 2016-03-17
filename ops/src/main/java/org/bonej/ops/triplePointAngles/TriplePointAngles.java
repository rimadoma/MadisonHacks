package org.bonej.ops.triplePointAngles;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import net.imagej.ImageJ;
import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geometric.PointCollection;

import org.scijava.plugin.Plugin;
import org.scijava.vecmath.Vector3d;

import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.analyzeSkeleton.Vertex;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.google.common.collect.ImmutableList;

import ij.ImagePlus;
import ij.io.Opener;

/**
 * @author Richard Domander
 * @todo Vertex-to-Vertex special case from BoneJ1
 * @todo is Vertex-to-Vertex the same as nthPoint == 0?
 * @todo Rewrite with Vector3D?
 * @todo prematch centroid op
 * @todo Write a Centroid Op for Vector3D?
 */
@Plugin(type = Op.class)
public class TriplePointAngles
		extends
			AbstractBinaryFunctionOp<Graph[], Integer, ImmutableList<ImmutableList<TriplePointAngles.TriplePoint>>> {
	private static final int VERTEX_TO_VERTEX = -1;

	@Override
	public ImmutableList<ImmutableList<TriplePoint>> compute2(final Graph[] graphs, final Integer nthPoint) {
		final ArrayList<TriplePoint> triplePoints = new ArrayList<>();
		final ArrayList<ImmutableList<TriplePoint>> skeletons = new ArrayList<>();

		for (int g = 0; g < graphs.length; g++) {
			final ArrayList<Vertex> vertices = graphs[g].getVertices();
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

    private double vertexToVertexAngle(final Vertex vertex, final Edge edge0, final Edge edge1) {
        final PointCollection vertexPoints = toImgLib2PointCollection(vertex.getPoints());
        final PointCollection oppositePoints0 = toImgLib2PointCollection(edge0.getOppositeVertex(vertex).getPoints());
        final PointCollection oppositePoints1 = toImgLib2PointCollection(edge1.getOppositeVertex(vertex).getPoints());

        final RealLocalizable centroid = ops().geom().centroid(vertexPoints);
        final RealLocalizable oppositeCentroid0 = ops().geom().centroid(oppositePoints0);
        final RealLocalizable oppositeCentroid1 = ops().geom().centroid(oppositePoints1);

        return joinedVectorAngle(oppositeCentroid0, oppositeCentroid1, centroid);
    }

    private double vertexAngle(final Vertex vertex, final Edge edge0, final Edge edge1, final int nthPoint) {
		final Point p0 = getNthPointOfEdge(vertex, edge0, nthPoint);
		final Point p1 = getNthPointOfEdge(vertex, edge1, nthPoint);

        final RealLocalizable vertexCentroid = ops().geom().centroid(toImgLib2PointCollection(vertex.getPoints()));

        return joinedVectorAngle(p0, p1, vertexCentroid);
	}

	private double joinedVectorAngle(final RealLocalizable p0, final RealLocalizable p1, final RealLocalizable tail) {
        Vector3d u = realLocalizableToVector3d(p0);
        Vector3d v = realLocalizableToVector3d(p1);
        Vector3d t = realLocalizableToVector3d(tail);

        u.sub(t);
        v.sub(t);

		return u.angle(v);
	}

    private static Vector3d realLocalizableToVector3d(final RealLocalizable localizable) {
        final double[] coordinates = new double[localizable.numDimensions()];
        localizable.localize(coordinates);
        return new Vector3d(coordinates);
    }

    private Point getNthPointOfEdge(final Vertex vertex, final Edge edge, final int nthPoint) {
        final ArrayList<Point> edgeSlabPoints = toImgLib2Points(edge.getSlabs());

		if (edgeSlabPoints.isEmpty()) {
			// No slabs, edge has only an end-point and a junction point
			final PointCollection endPoints = toImgLib2PointCollection(edge.getOppositeVertex(vertex).getPoints());
			return (Point) roundRealLocalizable(ops().geom().centroid(endPoints));
		}


		final Point edgeStart = edgeSlabPoints.get(0);
		final ArrayList<Point> vertexPoints = toImgLib2Points(vertex.getPoints());
		final boolean startAtZero = vertexPoints.stream().anyMatch(p -> isOneStepConnected(edgeStart, p));

		final int edgeIndex = Math.min(Math.max(0, nthPoint), edgeSlabPoints.size() - 1);

		if (startAtZero) {
			return edgeSlabPoints.get(edgeIndex);
		} else {
			final int edgeIndexFromEnd = edgeSlabPoints.size() - edgeIndex - 1;
			return edgeSlabPoints.get(edgeIndexFromEnd);
		}
	}

    /** @todo Move to util Class / make Op */
	private static Localizable roundRealLocalizable(final RealLocalizable localizable) {
		final int numDimensions = localizable.numDimensions();
		Point point = new Point(numDimensions);
		final double[] coordinates = new double[numDimensions];
		localizable.localize(coordinates);

		for (int c = 0; c < coordinates.length; c++) {
			point.setPosition(Math.round(c), 0);
		}

		return point;
	}

	/** @todo Offer to iarganda */
	private static PointCollection toImgLib2PointCollection(final ArrayList<sc.fiji.analyzeSkeleton.Point> points) {
		return new PointCollection(toImgLib2Points(points));
	}

	private static ArrayList<Point> toImgLib2Points(final ArrayList<sc.fiji.analyzeSkeleton.Point> points) {
		ArrayList<Point> imgLib2Points = new ArrayList<>(points.size());

		imgLib2Points.addAll(points.stream().map(TriplePointAngles::toImgLib2Point).collect(Collectors.toList()));

		return imgLib2Points;
	}

	private static Point toImgLib2Point(final sc.fiji.analyzeSkeleton.Point point) {
		return new Point(point.x, point.y, point.z);
	}

	/**
     * @todo Make a binaryFunctionOp (RealLocalizable, RealLocalizable) -> boolean
	 * @todo Generalize for different dimensionalities (distance in each dim <= 1)
	 */
	private static boolean isOneStepConnected(final RealLocalizable p0, final RealLocalizable p1) {
		final int numDimensions = Math.max(p0.numDimensions(), p1.numDimensions());

		for (int d = 0; d < numDimensions; d++) {
			double position0 = getPosition(p0, d).orElse(0.0);
			double position1 = getPosition(p1, d).orElse(0.0);
			double diff = Math.abs(position0 - position1);
			if (diff > 1) {
				return false;
			}
		}

		return true;
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
