package util;

public class RBmaker {

    private final float[][][] bigarray;
    private final float[][] slope;
    private final float[][] yint;
    private final float[][] lowAnchor;
    private final double lowWave;
    public String img;

    public RBmaker(String imgfile, float[][][] mappeddata, float y1, float y2, int kernel) {
        img = imgfile;
        bigarray = mappeddata;

        arrayMedian Anchor = new arrayMedian(imgfile, bigarray, (int) y1, kernel);
        lowAnchor = Anchor.getToast();
        lowWave = Anchor.getWave();
        Anchor.resetArrayMedian(imgfile, bigarray, (int) y2, kernel);
        float[][] highAnchor = Anchor.getToast();
        double highWave = Anchor.getWave();

        slope = new float[lowAnchor.length][lowAnchor[0].length];
        yint = new float[lowAnchor.length][lowAnchor[0].length];
        for (int i = 0; i < lowAnchor.length; i++) {
            for (int j = 0; j < lowAnchor[0].length; j++) {
                slope[i][j] = (highAnchor[i][j] - lowAnchor[i][j]) / (float) (highWave - lowWave);
                yint[i][j] = highAnchor[i][j] - ((float) highWave * slope[i][j]); // b = y - mx
            }
        }
    }

    public float[][] findNewRB(int wavelength, int kernel) {
        arrayMedian newRB = new arrayMedian(img, bigarray, wavelength, kernel);
        float[][] rArray = newRB.getToast();
        double wave = newRB.getWave();

        float[][] rcArray = new float[rArray.length][rArray[0].length];
        float[][] rbArray = new float[rArray.length][rArray[0].length];
        for (int i = 0; i < rArray.length; i++) {
            for (int j = 0; j < rArray[0].length; j++) {
                rcArray[i][j] = (float) wave * slope[i][j] + yint[i][j];
                rbArray[i][j] = (rcArray[i][j] - rArray[i][j]) / Math.abs(rcArray[i][j]);
            }
        }
        return rbArray;
    }

    public float[][] findRRCratio(int wavelength, int kernel) {
        arrayMedian newRB = new arrayMedian(img, bigarray, wavelength, kernel);
        float[][] rArray = newRB.getToast();
        double wave = newRB.getWave();

        float[][] rcArray = new float[rArray.length][rArray[0].length];
        float[][] ratio = new float[rArray.length][rArray[0].length];
        for (int i = 0; i < rArray.length; i++) {
            for (int j = 0; j < rArray[0].length; j++) {
                rcArray[i][j] = (float) (wave - lowWave) * slope[i][j] + lowAnchor[i][j];
                ratio[i][j] = rArray[i][j] / rcArray[i][j];
            }
        }
        return ratio;
    }

}
