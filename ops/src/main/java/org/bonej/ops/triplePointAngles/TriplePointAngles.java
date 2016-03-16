package org.bonej.ops.triplePointAngles;

import java.util.ArrayList;

import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Point;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geometric.PointCollection;

import org.scijava.plugin.Plugin;

import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.analyzeSkeleton.Vertex;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.google.common.collect.ImmutableList;

/**
 * @author Richard Domander
 * @todo Vertex-to-Vertex special case from BoneJ1
 * @todo is Vertex-to-Vertex the same as nthPoint == 0?
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
			throw new NotImplementedException();
		} else {
			thetas[0] = vertexAngle(vertex, edge0, edge1);
			thetas[1] = vertexAngle(vertex, edge0, edge2);
			thetas[2] = vertexAngle(vertex, edge1, edge2);
		}

		return thetas;
	}

	private double vertexAngle(final Vertex vertex, final Edge edge0, final Edge edge1) {
		final Point p0 = getNthPointOfEdge(vertex, edge0);
		final Point p1 = getNthPointOfEdge(vertex, edge1);
		final PointCollection points = new PointCollection(ImmutableList.of(p0, p1));

		final RealLocalizable centroid = ops().geom().centroid(points);

		return joinedVectorAngle(p0, p1, centroid);
	}

	private double joinedVectorAngle(final Point p0, final Point p1, final RealLocalizable centroid) {
		throw new NotImplementedException();
	}

	private static Point getNthPointOfEdge(final Vertex vertex, final Edge edge) {
		final ArrayList<Point> vertexPoints = toImgLib2Points(vertex.getPoints());
		final ArrayList<Point> edgePoints = toImgLib2Points(edge.getSlabs());

		final Point edgeStart = edgePoints.get(0);
		final boolean startAtZero = vertexPoints.stream().anyMatch(p -> is26Connected(edgeStart, p));

		throw new NotImplementedException();
	}

	/** @todo Offer to iarganda */
	private static ArrayList<Point> toImgLib2Points(final ArrayList<sc.fiji.analyzeSkeleton.Point> points) {
		ArrayList<Point> imgLib2Points = new ArrayList<>(points.size());

		for (sc.fiji.analyzeSkeleton.Point point : points) {
			imgLib2Points.add(toImgLib2Point(point));
		}

		return imgLib2Points;
	}

	private static Point toImgLib2Point(final sc.fiji.analyzeSkeleton.Point point) {
		return new Point(point.x, point.y, point.z);
	}

	/**
	 * @todo Make a binaryFunctionOp (RealLocalizable, RealLocalizable) -> boolean
	 * @todo Generalize for different dimensionalities (distance in each dim <= 1)
	 */
	private static boolean is26Connected(final RealLocalizable p0, final RealLocalizable p1) {
		for (int d = 0; d < 3; d++) {
			double position0 = p0.getDoublePosition(d);
			double position1 = p1.getDoublePosition(d);
			double diff = Math.abs(position0 - position1);
			if (diff > 1) {
				return false;
			}
		}

		return true;
	}

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
}
