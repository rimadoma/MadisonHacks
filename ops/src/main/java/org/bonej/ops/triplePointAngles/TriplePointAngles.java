package org.bonej.ops.triplePointAngles;

import com.google.common.collect.ImmutableList;
import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import org.scijava.plugin.Plugin;
import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.analyzeSkeleton.Vertex;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;

/**
 * @author Richard Domander
 * @todo Vertex-to-Vertex special case from BoneJ1
 * @todo is Vertex-to-Vertex the same as nthPoint == 0?
 */
@Plugin(type = Op.class)
public class TriplePointAngles
        extends AbstractBinaryFunctionOp<Graph[], Integer, ArrayList<TriplePointAngles.TriplePointResult>> {
    private static final int VERTEX_TO_VERTEX = -1;

    @Override
    public ArrayList<TriplePointResult> compute2(final Graph[] graphs, final Integer nthPoint) {
        final ArrayList<TriplePointResult> results = new ArrayList<>();

        for (int g = 0; g < graphs.length; g++) {
            final ArrayList<Vertex> vertices = graphs[g].getVertices();
            for (int v = 0; v < vertices.size(); v++) {
                final Vertex vertex = vertices.get(v);

                if (!isTriplePoint(vertex)) {
                    continue;
                }

                double[] angles = triplePointAngles(vertex, nthPoint);
                results.add(new TriplePointResult(g, v, ImmutableList.of(angles[0], angles[1], angles[2])));
            }
        }

        return results;
    }

    private static boolean isTriplePoint(final Vertex vertex) {
        return vertex.getBranches().size() == 3;
    }

    private static double[] triplePointAngles(final Vertex vertex, final int nthPoint) {
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

    private static double vertexAngle(final Vertex vertex, final Edge edge0, final Edge edge1) {
        return 0;
    }

    public static final class TriplePointResult {
        public final int skeletonNumber;
        public final int vertexNumber;
        public final ImmutableList<Double> angles;

        public TriplePointResult(final int skeletonNumber, final int vertexNumber, final ImmutableList<Double> angles) {
            this.skeletonNumber = skeletonNumber;
            this.vertexNumber = vertexNumber;
            this.angles = angles;
        }
    }
}
