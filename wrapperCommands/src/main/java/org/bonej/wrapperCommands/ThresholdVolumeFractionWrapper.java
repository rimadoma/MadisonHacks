package org.bonej.wrapperCommands;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.integer.LongType;
import org.bonej.ops.testImageGenerators.CuboidCreator;
import org.bonej.ops.thresholdFraction.ThresholdVolumeFraction;
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Richard Domander
 * @todo Check for spatial dimensions?
 * @todo Confirm that there's no real reason to prevent color / 32-bit images
 * @todo One or two wrappers for ThresholdFraction Ops in BoneJ2?
 * @todo How to determine thresholds? What are min & max?
 * @todo How to display result data?
 * @todo How to display resulting meshes?
 * @todo Dimension checking doesn't work
 * @todo Bit depth doesn't work
 * @todo Bit depth label doesn't work
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>Volume Fraction")
public class ThresholdVolumeFractionWrapper extends ContextCommand {
    private static final String BIT_DEPTH_LABEL = "Image bit-depth: ";
    private long thresholdBoundary;

    /**
     * @implNote    Set required = false to disable the default error message
     * @implNote    Use Dataset for now because only they can be automatically populated
     *              ImgPlus etc. ok in ImageJ POM >= 14.6.2
     */
    @Parameter(initializer = "checkImage", required = false)
    private Dataset activeImage;

    @Parameter(visibility = ItemVisibility.MESSAGE, description = "Maximum element value for the image is 2^depth - 1")
    private static String bitDepthMessage = BIT_DEPTH_LABEL + "N/A";

    @Parameter(label = "Foreground cut-off", persist = false, callback = "enforceThresholds",
            description = "Voxels values above this cut-off are considered foreground (bone)")
    private long foregroundCutOff;

    @Parameter(label = "Threshold minimum", persist = false, min = "0", callback = "enforceThresholds",
            description = "Minimum value for mineralized bone voxels")
    private long minThreshold;

    @Parameter(label = "Maximum threshold value", persist = false, min = "0", callback = "enforceThresholds")
    private long maxThreshold;

    @Parameter(label = "Show 3D surfaces", description = "Show the sample and mineralized bone surfaces in 3D")
    private boolean show3DResult = false;

    @Parameter(label = "Help", callback = "openHelpPage")
    private Button helpButton;

    @Parameter
    private OpService opService;

    @Parameter
    private PlatformService platformService;

    @Parameter
    private UIService uiService;

    @Override
    public void run() {
        final ThresholdVolumeFraction.Settings<LongType> settings =
                new ThresholdVolumeFraction.Settings<>(new LongType(foregroundCutOff), new LongType(minThreshold),
                        new LongType(maxThreshold));

        // Class cast exception
        ImgPlus<LongType> imgPlus = (ImgPlus<LongType>) activeImage.getImgPlus();
        final ThresholdVolumeFraction.Results results =
                (ThresholdVolumeFraction.Results) opService.run(ThresholdVolumeFraction.class, imgPlus, settings);

        displayResults(results);

        if (show3DResult) {
            showSurfaces(results);
        }
    }

    //region --Utility methods--
    public static void main(String... args) {
        final ImageJ ij = net.imagej.Main.launch(args);
        final Object cuboid = ij.op().run(CuboidCreator.class, null, 100L, 100L, 100L, 10L);
        ij.ui().show(cuboid);
    }
    //endregion

    //region --Helper methods--

    /** Display volume data in the IJ results table */
    private void displayResults(final ThresholdVolumeFraction.Results results) {
        System.out.println("Thresholded surface volume " + results.thresholdMeshVolume);
        System.out.println("Foreground surface volume " + results.foregroundMeshVolume);
        System.out.println("Volume ratio " + results.volumeRatio);
        throw new NotImplementedException();
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
            checkArgument(activeImage.numDimensions() == 3, "Image must be three dimensional");
            initThresholds();
        } catch (IllegalArgumentException | NullPointerException e) {
            cancel(e.getMessage());
        }
    }

    private void initThresholds() {
        final int bitDepth = activeImage.getValidBits();
        final boolean validBitDepth = bitDepth > 0;
        bitDepthMessage = validBitDepth ? BIT_DEPTH_LABEL + bitDepth: BIT_DEPTH_LABEL + "N/A";

        checkArgument(validBitDepth, "Cannot determine threshold values from bit-depth");

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