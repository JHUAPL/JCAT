package util;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class ImageUtils {

    public enum STRETCH {
        LINEAR, GAUSSIAN
    }

    /**
     * Get red, green, blue min and max values from the input image
     *
     * @param bi
     * @param type      {@link STRETCH}
     * @param magnitude percent for linear, std dev for gaussian
     * @return
     */
    public static List<Double> getStretchValues(BufferedImage bi, STRETCH type, double magnitude) {
        JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getStretchValues");
        int rgb;

        DescriptiveStatistics r = new DescriptiveStatistics();
        DescriptiveStatistics g = new DescriptiveStatistics();
        DescriptiveStatistics b = new DescriptiveStatistics();

        for (int i = 0; i < bi.getHeight(); i++) {
            for (int j = 0; j < bi.getWidth(); j++) {
                rgb = bi.getRGB(j, i);
                r.addValue((rgb >> 16) & 0x000000FF);
                g.addValue((rgb >> 8) & 0x000000FF);
                b.addValue((rgb) & 0x000000FF);
            }
        }
        double rMin, gMin, bMin;
        double rDiff, gDiff, bDiff;

        JCATLog.getInstance().getLogger().log(Level.FINE, "Red stats: " + r);
        JCATLog.getInstance().getLogger().log(Level.FINE, "Green stats: " + g);
        JCATLog.getInstance().getLogger().log(Level.FINE, "Blue stats: " + b);

        if (type == STRETCH.GAUSSIAN) {
            rMin = (int) (r.getMin() - magnitude * r.getStandardDeviation());
            gMin = (int) (g.getMin() - magnitude * g.getStandardDeviation());
            bMin = (int) (b.getMin() - magnitude * b.getStandardDeviation());

            rDiff = (int) (2 * magnitude * r.getStandardDeviation());
            gDiff = (int) (2 * magnitude * g.getStandardDeviation());
            bDiff = (int) (2 * magnitude * b.getStandardDeviation());
        } else {
            if (magnitude == 0) {
                rMin = (int) r.getMin();
                gMin = (int) g.getMin();
                bMin = (int) b.getMin();
                rDiff = (int) (r.getMax() - r.getMin());
                gDiff = (int) (g.getMax() - g.getMin());
                bDiff = (int) (b.getMax() - b.getMin());
            } else {
                rMin = r.getPercentile(magnitude);
                gMin = g.getPercentile(magnitude);
                bMin = b.getPercentile(magnitude);
                rDiff = (r.getPercentile(100 - magnitude) - rMin);
                gDiff = (g.getPercentile(100 - magnitude) - gMin);
                bDiff = (b.getPercentile(100 - magnitude) - bMin);
            }
        }

        JCATLog.getInstance().getLogger().log(Level.FINE, String.format("Setting red min, max to %f, %f", rMin, rDiff + rMin));
        JCATLog.getInstance().getLogger().log(Level.FINE, String.format("Setting green min, max to %f, %f", gMin, gDiff + gMin));
        JCATLog.getInstance().getLogger().log(Level.FINE, String.format("Setting blue min, max to %f, %f", bMin, bDiff + bMin));

        return new ArrayList<>(Arrays.asList(rMin, gMin, bMin, rDiff, gDiff, bDiff));
    }

}
