package org.bonej.utilities;

import net.imagej.axis.CalibratedAxis;
import net.imagej.space.AnnotatedSpace;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for dealing with CalibratedAxis
 *
 * @author Richard Domander
 * @todo Can an AnnotatedSpace have zero dims? What happens then?
 */
public final class CalibratedAxisUtil {
    private CalibratedAxisUtil() {} // There's no reason to create an instance of this class

    /**
     * Counts the number of spatial dimensions in the given space
     *
     * @throws NullPointerException if space == null
     */
    public static <T extends AnnotatedSpace<CalibratedAxis>> long countSpatialDimensions(final T space)
            throws NullPointerException {
        checkNotNull(space, "Cannot determine dimensions of a null space");

        final CalibratedAxis[] axes = new CalibratedAxis[space.numDimensions()];
        space.axes(axes);
        return Arrays.stream(axes).filter(a -> a.type().isSpatial()).count();
    }

    /**
     * Returns true if the given space has dimensions that are not spatial, e.g. time or channel
     *
     * @throws NullPointerException
     */
    public static <T extends AnnotatedSpace<CalibratedAxis>> boolean hasNonSpatialDimensions(final T space)
            throws NullPointerException {
        checkNotNull(space, "Cannot determine dimensions of a null space");

        final CalibratedAxis[] axes = new CalibratedAxis[space.numDimensions()];
        space.axes(axes);
        return Arrays.stream(axes).anyMatch(a -> !a.type().isSpatial());
    }
}
