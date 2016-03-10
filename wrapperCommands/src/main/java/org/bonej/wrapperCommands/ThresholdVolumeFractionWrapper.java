package org.bonej.wrapperCommands;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import org.bonej.ops.testImageGenerators.CuboidCreator;
import org.bonej.ops.thresholdFraction.ThresholdVolumeFraction;
import org.bonej.utilities.CalibratedAxisUtil;
import org.bonej.utilities.ResultsInserter;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.scijava.ui.DialogPrompt.MessageType;
import static org.scijava.ui.DialogPrompt.OptionType;
import static org.scijava.ui.DialogPrompt.Result;

/**
 * @author Richard Domander
 * @todo Confirm that there's no real reason to prevent color / 32-bit images
 * @todo One or two wrappers for ThresholdFraction Ops in BoneJ2?
 * @todo How to determine thresholds? What are min & max?
 * @todo Change widgets based on range of dataset's type? callbacks may need tweaking for floating (new > image... > 32-bit)
 * @todo How to display resulting meshes? (Kyle's 3D Viewer branch?)
 * @todo Resampling option as in BoneJ1
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>Volume Fraction")
public class ThresholdVolumeFractionWrapper extends ContextCommand {
    private static final String BIT_DEPTH_LABEL = "Image bit-depth: ";

    @Parameter(visibility = ItemVisibility.MESSAGE, description = "Maximum element value for the image is 2^depth - 1")
    private static String bitDepthMessage = BIT_DEPTH_LABEL + "N/A";

    private long thresholdBoundary;

    /**
     * @implNote Set required = false to disable the default error message
     * @implNote Use Dataset for now because validBitDepth has incompatible implementation in ImgPlus
     */
    @Parameter(initializer = "checkImage", required = false)
    private Dataset activeImage;

    @Parameter(label = "Foreground cut-off", persist = false, callback = "enforceThresholds",
            description = "Voxels values above this cut-off are considered foreground (bone)")
    private double foregroundCutOff;

    @Parameter(label = "Threshold minimum", persist = false, min = "0", callback = "enforceThresholds",
            description = "Minimum value for mineralized bone voxels")
    private double minThreshold;

    @Parameter(label = "Maximum threshold value", persist = false, min = "0", callback = "enforceThresholds")
    private double maxThreshold;

    @Parameter(label = "Show 3D surfaces", description = "Show the sample and bone surfaces in 3D")
    private boolean show3DResult = false;

    @Parameter(label = "Help", callback = "openHelpPage")
    private Button helpButton;

    @Parameter
    private OpService opService;

    @Parameter
    private PlatformService platformService;

    @Parameter
    private UIService uiService;

    //region --Utility methods--
    public static void main(String... args) {
        final ImageJ ij = net.imagej.Main.launch(args);
        final Object cuboid = ij.op().run(CuboidCreator.class, null, 10L, 10L, 10L);
        ij.ui().show(cuboid);
    }

    @Override
    public void run() {
        final ThresholdVolumeFraction.Settings settings =
                new ThresholdVolumeFraction.Settings(foregroundCutOff, minThreshold, maxThreshold);

        final ThresholdVolumeFraction.Results results = (ThresholdVolumeFraction.Results) opService
                .run(ThresholdVolumeFraction.class, activeImage.getImgPlus(), settings);

        displayResults(results);

        if (show3DResult) {
            showSurfaces(results);
        }
    }
    //endregion

    //region --Helper methods--

    /** Display volume data in the IJ results table */
    private void displayResults(final ThresholdVolumeFraction.Results results) {
        final ResultsInserter resultInserter = new ResultsInserter();
        final String label = activeImage.getName();
        final double elementVolume = CalibratedAxisUtil.calibratedElementSize(activeImage);
        final double sampleVolume = results.foregroundMeshVolume * elementVolume;
        final double boneVolume = results.thresholdMeshVolume * elementVolume;
        final Optional<String> unit = CalibratedAxisUtil.unitOfSpace(activeImage);
        final String unitSuffix;

        if (!unit.isPresent()) {
            uiService.showDialog("Could not determine the unit of calibration - showing plain values",
                    MessageType.WARNING_MESSAGE);
            unitSuffix = "";
        } else {
            char thirdPower = '\u00B3';
            unitSuffix = "(" + unit.get() + thirdPower + ")";
        }

        resultInserter.setMeasurementInFirstFreeRow(label, "Bone volume " + unitSuffix, boneVolume);
        resultInserter.setMeasurementInFirstFreeRow(label, "Sample volume " + unitSuffix, sampleVolume);
        resultInserter.setMeasurementInFirstFreeRow(label, "Volume ratio", results.volumeRatio);
        resultInserter.showTable();
    }

    /** Visualize the 3D surfaces produced */
    private void showSurfaces(final ThresholdVolumeFraction.Results results) {
        throw new NotImplementedException();
    }

    @SuppressWarnings("unused")
    private void enforceThresholds() {
        if (maxThreshold > thresholdBoundary) {
            maxThreshold = thresholdBoundary;
        }

        if (minThreshold > maxThreshold) {
            minThreshold = maxThreshold;
        }
        if (foregroundCutOff > minThreshold) {
            foregroundCutOff = minThreshold;
        }
    }

    @SuppressWarnings("unused")
    private void checkImage() {
        try {
            checkNotNull(activeImage, "No image open");
            checkArgument(CalibratedAxisUtil.countSpatialDimensions(activeImage) == 3, "Image must be 3D");
        } catch (IllegalArgumentException | NullPointerException e) {
            cancel(e.getMessage());
        }

        if (CalibratedAxisUtil.hasNonSpatialDimensions(activeImage)) {
            boolean runPlugin = uiService.showDialog(
                    "The image has non-spatial dimensions, which may affect the results.\n Do you want to continue?",
                    MessageType.WARNING_MESSAGE, OptionType.OK_CANCEL_OPTION) == Result.OK_OPTION;
            if (!runPlugin) {
                cancel(null);
            }
        }

        initThresholds();
    }

    private void initThresholds() {
        final int bitDepth = activeImage.getValidBits();
        final boolean validBitDepth = bitDepth > 0;
        bitDepthMessage = validBitDepth ? BIT_DEPTH_LABEL + bitDepth : BIT_DEPTH_LABEL + "N/A";

        checkArgument(validBitDepth, "Cannot determine threshold values from bit-depth");

        //@todo get from min & max values
        thresholdBoundary = (1 << bitDepth) - 1;
        maxThreshold = thresholdBoundary;
        minThreshold = Math.round(maxThreshold / 2.0);
        foregroundCutOff = 1;
    }

    /** @todo Problems with Chrome? */
    @SuppressWarnings("unused")
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