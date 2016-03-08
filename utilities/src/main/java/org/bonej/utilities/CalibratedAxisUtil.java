package org.bonej.utilities;

import com.google.common.base.Strings;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.AnnotatedSpace;
import net.imglib2.Dimensions;

import javax.annotation.Nullable;
import java.util.Arrays;
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

    /**
     * Returns the size of a single calibrated element in the given space,
     * e.g. the volume of an element in a 3D space
     *
     * @throws NullPointerException if space == null
     * @implNote Only works with linear axes
     * @implNote Ignores non-spatial axes
     * @todo Can an AnnotatedSpace have zero dims?
     * @todo Check that units match
     */
    public static <T extends AnnotatedSpace<CalibratedAxis> & Dimensions> double calibratedElementSize(final T space)
            throws NullPointerException {
        checkNotNull(space, "Cannot determine element size in a null space");

        final int numDimensions = space.numDimensions();
        double calibratedElementSize = 1.0;

        for (int d = 0; d < numDimensions; d++) {
            final CalibratedAxis axis = space.axis(d);
            if (!axis.type().isSpatial()) {
                continue;
            }

            final double dimensionSize = space.dimension(d);
            final double axisScale = axis.averageScale(0, dimensionSize);
            calibratedElementSize = calibratedElementSize * axisScale;
        }

        return calibratedElementSize;
    }

    /**
     * Returns the unit of the calibration used in the given space
     *
     * @return  The Optional is empty if the space is null.
     *          The Optional is empty if the units of the axes in the space don't match,
     *          or any of them is null or empty
     * @todo make spatialUnitOfSpace etc
     */
    public static <T extends AnnotatedSpace<CalibratedAxis>> Optional<String> unitOfSpace(@Nullable final T space) {
        final Optional<CalibratedAxis[]> axes = allocateAndGetAxis(space);

        if (!isAxisSameUnit(axes)) {
            return Optional.empty();
        }

        final String unit = axes.get()[0].unit();
        return Optional.of(unit);
    }

    private static boolean isAxisSameUnit(final Optional<CalibratedAxis[]> axes) {
        if (!axes.isPresent()) {
            return false;
        }

        final CalibratedAxis[] axesArray = axes.get();

        boolean noUnitAxis = Arrays.stream(axesArray).anyMatch(a -> Strings.isNullOrEmpty(a.unit()));
        if (noUnitAxis) {
            return false;
        }

        final String firstUnit = axesArray[0].unit();
        return Arrays.stream(axesArray).allMatch(a -> a.unit().equals(firstUnit));
    }
}
