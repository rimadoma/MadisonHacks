package org.bonej.ops.volumeFraction;

import net.imagej.ops.Op;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import org.scijava.plugin.Plugin;

/**
 * Counts the number of elements within the given threshold
 *
 * @author Richard Domander
 * @apiNote The plugin assumes that minThreshold.compareTo(foregroundCutOff) <= 0,
 *          and foregroundCutOff.compareTo(maxThreshold) <= 0
 * @todo How to implement the limit volume to ROIs option from BoneJ1?
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
    public final static class Settings<S> {
        public S minThreshold;
        public S maxThreshold;
        public S foregroundCutOff;

        public Settings(final S foregroundCutOff, final S minThreshold, final S maxThreshold) {
            this.foregroundCutOff = foregroundCutOff;
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
        }
    }

    public static final class Results {
        public final long thresholdElements;
        public final long foregroundElements;
        public final double elementRatio;

        private Results(final long thresholdElements, final long foregroundElements) {
            this.thresholdElements = thresholdElements;
            this.foregroundElements = foregroundElements;
            elementRatio = ((double) thresholdElements) / foregroundElements;
        }
    }
    // endregion
}
