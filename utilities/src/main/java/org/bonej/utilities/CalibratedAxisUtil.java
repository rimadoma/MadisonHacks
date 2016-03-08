package org.bonej.utilities;

import net.imagej.axis.CalibratedAxis;
import net.imagej.space.AnnotatedSpace;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for dealing with CalibratedAxis
 *
 * @author Richard Domander
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

        final Stream<CalibratedAxis> axes = getAxisStream(space);
        return axes.filter(a -> a.type().isSpatial()).count();
    }

    /**
     * Returns the CalibratedAxis in the given space as a Stream,
     * or an empty Stream if the space is null
     */
    public static <T extends AnnotatedSpace<CalibratedAxis>> Stream<CalibratedAxis> getAxisStream(
            @Nullable final T space) {
        final Optional<CalibratedAxis[]> axes = allocateAndGetAxis(space);
        return axes.isPresent() ? Stream.of(axes.get()) : Stream.empty();
    }

    /**
     * Allocates and returns the CalibratedAxis in the given space as an Optional of array,
     * or an empty Optional if the space is null
     *
     * @todo Can an AnnotatedSpace have zero dims?
     */
    public static <T extends AnnotatedSpace<CalibratedAxis>> Optional<CalibratedAxis[]> allocateAndGetAxis(
            @Nullable final T space) {
        if (space == null) {
            return Optional.empty();
        }

        final CalibratedAxis[] axes = new CalibratedAxis[space.numDimensions()];
        space.axes(axes);
        return Optional.of(axes);
    }

    /**
     * Returns true if the given space has dimensions that are not spatial, e.g. time or channel
     *
     * @throws NullPointerException if space == null
     */
    public static <T extends AnnotatedSpace<CalibratedAxis>> boolean hasNonSpatialDimensions(final T space)
            throws NullPointerException {
        checkNotNull(space, "Cannot determine dimensions of a null space");

        final Stream<CalibratedAxis> axes = getAxisStream(space);
        return axes.anyMatch(a -> !a.type().isSpatial());
    }
}
