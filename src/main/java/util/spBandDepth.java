package util;

public class spBandDepth {

    private final float[][] summaryProduct;

    public spBandDepth(String imgfile, float[][][] mappeddata, int wav0, int ker0, int wav1, int ker1, int wav2, int ker2) {
        arrayMedian alpha = new arrayMedian(imgfile, mappeddata, wav0, ker0);
        float[][] sp1 = alpha.getToast();
        double wave1 = alpha.getWave();
        alpha.resetArrayMedian(imgfile, mappeddata, wav1, ker1);
        float[][] sp2 = alpha.getToast();
        double wave2 = alpha.getWave();
        alpha.resetArrayMedian(imgfile, mappeddata, wav2, ker2);
        float[][] sp3 = alpha.getToast();
        double wave3 = alpha.getWave();

        float aVal = ((float) wave2 - (float) wave1) / ((float) wave3 - (float) wave1);
        float bVal = (float) 1 - aVal;

        summaryProduct = new float[sp1.length][sp1[0].length];
        for (int i = 0; i < sp1.length; i++) {
            for (int j = 0; j < sp1[0].length; j++) {
                summaryProduct[i][j] = (float) 1 - sp2[i][j] / (aVal * sp3[i][j] + bVal * sp1[i][j]);
            }
        }
    }

    public float[][] getSummaryProduct() {
        return summaryProduct;
    }
}
