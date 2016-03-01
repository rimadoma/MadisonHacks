package org.bonej.wrapperCommands;

import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import org.bonej.ops.testImageGenerators.CuboidCreator;
import org.bonej.ops.thresholdFraction.ThresholdVolumeFraction;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;

import java.io.IOException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Richard Domander
 * @todo Check for spatial dimensions?
 * @todo Confirm that there's no real reason for checking bit depth
 * @todo What's the suitable generic type for ImgPlus<T> activeImage?
 * @todo One or two wrappers for ThresholdFraction Ops in BoneJ2?
 * @todo How to determine thresholds?
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>Volume Fraction")
public class ThresholdVolumeFractionWrapper<T extends RealType<T>> extends ContextCommand {
    private long thresholdBoundary;

    /**
     * @implNote Set required = false to disable the default error message
     * @implNote Use ImgPlus because we need calibration info
     */
    @Parameter(initializer = "initializeActiveImage", required = false, persist = false)
    private ImgPlus<T> activeImage;

    @Parameter(label = "Foreground cut-off", description = "Elements above this value are considered foreground",
            callback = "enforceThresholds")
    private long foregroundCutOff = 1;

    @Parameter(label = "Minimum threshold value", min = "0", callback = "enforceThresholds")
    private long minThreshold;

    @Parameter(label = "Maximum threshold value", min = "0", callback = "enforceThresholds")
    private long maxThreshold;

    @Parameter(label = "Help", persist = false, min = "0", callback = "openHelpPage")
    private Button helpButton;

    @Parameter
    private PlatformService platformService;

    @Parameter
    private UIService uiService;

    @Override
    public void run() {
        final ThresholdVolumeFraction.Settings<LongType> settings =
                new ThresholdVolumeFraction.Settings<LongType>(new LongType(foregroundCutOff),
                        new LongType(minThreshold), new LongType(maxThreshold));

        //final ThresholdVolumeFraction.Results results =
        //        (ThresholdVolumeFraction.Results) ij.op().run(ThresholdVolumeFraction.class, cuboid, settings);
    }

    //region --Utility methods--
    public static void main(String... args) {
        final ImageJ ij = net.imagej.Main.launch(args);
        final Object cuboid = ij.op().run(CuboidCreator.class, null, 100L, 100L, 100L, 10L);
        ij.ui().show(cuboid);
    }
    //endregion

    //region --Helper methods--
    private void enforceThresholds() {
        /*if (maxThreshold > thresholdBoundary) {
            maxThreshold = thresholdBoundary;
        }

        if (minThreshold > maxThreshold) {
            minThreshold = maxThreshold;
        }
        if (foregroundCutOff > minThreshold) {
            foregroundCutOff = minThreshold;
        }*/
    }

    private void initializeActiveImage() {
        try {
            checkNotNull(activeImage, "No image open");
            checkArgument(activeImage.numDimensions() == 3, "Image must be three dimensional");

            //@todo fix thresholdBoundary to something sensible
            thresholdBoundary = 255;
            maxThreshold = thresholdBoundary; //@todo maxThreshold becomes 1?!
            minThreshold = maxThreshold / 2;
        } catch (IllegalArgumentException | NullPointerException e) {
            cancel(e.getMessage());
        }
    }

    /**
     * @todo Always fails
     */
    private void openHelpPage() {
        try {
            URL helpUrl = new URL("http://bonej.org/volumefraction");
            platformService.open(helpUrl);
        } catch (final IOException e) {
            uiService.showDialog("An error occurred while trying to open the help page");
        }
    }

    //endregion
}