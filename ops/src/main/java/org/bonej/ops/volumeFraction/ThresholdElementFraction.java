package org.bonej.ops.volumeFraction;

import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import org.scijava.plugin.Plugin;

/**
 * Counts the number of foreground elements in the interval that are within the given thresholds
 *
 * @author Richard Domander
 * @apiNote The plugin assumes that foregroundCutOff.compareTo(minThreshold) <= 0,
 *          and minThreshold.compareTo(maxThreshold) <= 0
 * @todo How to implement the limit calculations to ROIs option from BoneJ1?
 * @todo How to apply calculations only to areas defined by masks (irregular rois)?
 * @todo How to display Results?
 */
@Plugin(type = Op.class)
public class ThresholdElementFraction<S, T extends Comparable<S>> extends
        AbstractBinaryFunctionOp<IterableInterval<T>, ThresholdElementFraction.Settings<S>, ThresholdElementFraction.Results> {
    @Override
    public Results compute2(final IterableInterval<T> interval, final Settings<S> settings) {
        long foregroundElements = 0;
        long thresholdElements = 0;

        Cursor<T> cursor = interval.cursor();
        while (cursor.hasNext()) {
            cursor.fwd();
            T element = cursor.get();
            if (element.compareTo(settings.foregroundCutOff) < 0) {
                continue;
            }

            foregroundElements++;

            if ((element.compareTo(settings.minThreshold) >= 0) && (element.compareTo(settings.maxThreshold) <= 0)) {
                thresholdElements++;
            }
        }

        return new Results(thresholdElements, foregroundElements);
    }

    //region -- Helper classes --
    /**
     * A helper class for passing the input settings of the Op type safely,
     * without having to memorize array indices etc.
     *
     * @todo How are these kinds @Parameters stored persistently?
     */
    public static final class Settings<S> {
        /** Minimum value for elements within threshold */
        public final S minThreshold;
        /** Maximum value for elements within threshold */
        public final S maxThreshold;
        /** Elements whose values >= foregroundCutOff are considered foreground */
        public final S foregroundCutOff;

        public Settings(final S foregroundCutOff, final S minThreshold, final S maxThreshold) {
            this.foregroundCutOff = foregroundCutOff;
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
        }
    }

    /**
     * A helper class for passing the output results of the Op type safely,
     * without having to memorize array indices etc.
     */
    public static final class Results {
        /** Number of elements found whose value is within thresholds */
        public final long thresholdElements;
        /** Number of elements which are foreground */
        public final long foregroundElements;
        /** Ratio of thresholdElements / foregroundElements */
        public final double elementRatio;

        private Results(final long thresholdElements, final long foregroundElements) {
            this.thresholdElements = thresholdElements;
            this.foregroundElements = foregroundElements;
            elementRatio = ((double) thresholdElements) / foregroundElements;
        }
    }
    // endregion
}
