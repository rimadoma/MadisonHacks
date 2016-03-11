package org.bonej.utilities;

import com.google.common.base.Strings;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.AnnotatedSpace;
import net.imglib2.Dimensions;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for dealing with CalibratedAxis
 *
 * @author Richard Domander
 * @todo Can an AnnotatedSpace have zero dims?
 * @todo Can AnnotatedSpace have null CalibratedAxis?
 */
public final class CalibratedAxisUtil {
    private CalibratedAxisUtil() {} // There's no reason to create an instance of this class

    /**
     * Returns the size of a single calibrated spatial element in the given space,
     * e.g. the volume of an element in a 3D space
     *
     * @return Calibrated size of a spatial element, or 1.0 if calibration cannot be determined
     * (@see spatialAxisUnitsMatch)
     * @throws NullPointerException if space == null
     * @implNote Only works with linear axes
     */
    public static <T extends AnnotatedSpace<CalibratedAxis> & Dimensions> double calibratedSpatialElementSize(
            final T space) throws NullPointerException {
        checkNotNull(space, "Cannot determine element size in a null space");

        if (!spatialAxisUnitsMatch(space)) {
            return 1.0;
        }

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
     * Returns the calibrated spatial size of the given space
     *
     * Size is calculated by multiplying the sizes of each spatial dimension,
     * and then multiplying the result by @see calibratedSpatialElementSize
     * If calibration cannot be determined, returns the size of the spatial space
     *
     * @throws NullPointerException if space == null
     * @implNote Only works with linear axes
     * @todo unit tests
     */
    public static <T extends AnnotatedSpace<CalibratedAxis> & Dimensions> double calibratedSpatialSpaceSize(
            final T space) throws NullPointerException {
        double elementSize = calibratedSpatialElementSize(space);

        final int numDimensions = space.numDimensions();
        double spaceSize = 1.0;

        for (int d = 0; d < numDimensions; d++) {
            final CalibratedAxis axis = space.axis(d);
            if (!axis.type().isSpatial()) {
                continue;
            }

            final long dimensionSize = space.dimension(d);
            spaceSize = spaceSize * dimensionSize;
        }

        return elementSize * spaceSize;
    }

    /**
     * Counts the number of spatial dimensions in the given space
     *
     * @throws NullPointerException if space == null
     */
    public static <T extends AnnotatedSpace<CalibratedAxis>> long countSpatialDimensions(final T space)
            throws NullPointerException {
        checkNotNull(space, "Cannot determine dimensions of a null space");

        return generateAxisStream(space).filter(a -> a.type().isSpatial()).count();
    }

    /** Returns a Stream of the CalibratedAxis objects in the given space */
    public static Stream<CalibratedAxis> generateAxisStream(AnnotatedSpace<CalibratedAxis> space) {
        return StreamSupport.stream(new AxisSequence(space).spliterator(), false);
    }

    /**
     * Returns the unit of the calibration used in the given space
     *
     * @return The Optional is empty if the space is null.
     * The Optional is empty if the units of the axes in the space don't match,
     * or any of them is null
     * The Optional contains an empty string if all the axes are uncalibrated
     */
    public static <T extends AnnotatedSpace<CalibratedAxis>> Optional<String> getSpatialUnitOfSpace(
            @Nullable final T space) {
        if (space == null || !spatialAxisUnitsMatch(space)) {
            return Optional.empty();
        }

        final String unit = space.axis(0).unit();
        return Optional.of(unit);
    }

    /**
     * Returns true if the given space has non-null dimensions that are not spatial, e.g. time or channel
     *
     * @throws NullPointerException if space == null
     */
    public static <T extends AnnotatedSpace<CalibratedAxis>> boolean hasNonSpatialDimensions(final T space)
            throws NullPointerException {
        checkNotNull(space, "Cannot determine dimensions of a null space");

        return generateAxisStream(space).anyMatch(a -> a != null && !a.type().isSpatial());
    }

    /** Returns a Stream of the spatial axes in the given space */
    private static Stream<CalibratedAxis> generateSpatialAxisStream(final AnnotatedSpace<CalibratedAxis> space) {
        return generateAxisStream(space).filter(a -> a != null && a.type().isSpatial());
    }

    /** Returns a Stream of the units used by the spatial axes in the given space */
    private static Stream<String> generateSpatialUnitStream(final AnnotatedSpace<CalibratedAxis> space) {
        return generateSpatialAxisStream(space).map(CalibratedAxis::unit);
    }

    /**
     * Checks if all the spatial axis in the given space have the same unit
     *
     * @return true if all spatial units match,
     * or *all* axis units are empty or null (uncalibrated),
     * or there are no spatial axis in the space
     */
    private static boolean spatialAxisUnitsMatch(final AnnotatedSpace<CalibratedAxis> space) {
        boolean allUncalibrated = generateSpatialUnitStream(space).allMatch(Strings::isNullOrEmpty);
        if (allUncalibrated) {
            return true;
        }

        boolean allCalibrated = generateSpatialUnitStream(space).noneMatch(Strings::isNullOrEmpty);
        if (!allCalibrated) {
            return false;
        }

        long units = generateSpatialUnitStream(space).distinct().count();
        return units == 1;
    }

    private static final class AxisSequence implements Iterator<CalibratedAxis>, Iterable<CalibratedAxis> {
        private final int numDimensions;
        private AnnotatedSpace<CalibratedAxis> space;
        private int dimension = 0;

        public AxisSequence(final AnnotatedSpace<CalibratedAxis> space) {
            this.space = space;
            numDimensions = space.numDimensions();
        }

        @Override
        public Iterator<CalibratedAxis> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return dimension < numDimensions;
        }

        @Override
        public CalibratedAxis next() {
            CalibratedAxis axis = space.axis(dimension);
            dimension++;
            return axis;
        }
    }
}
