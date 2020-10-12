package com.joshfix.gdal.merge;

import org.gdal.gdal.ColorTable;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;

/**
 * @author joshfix
 * Created on 6/27/2019
 */

public class FileInfo {

    private GdalMerge gdalMerge;
    private Dataset dataset;
    private int bands;
    private int xSize;
    private int ySize;
    private int bandType;
    private String projection;
    private double[] geotransform;
    private double ulx;
    private double uly;
    private double lrx;
    private double lry;
    private ColorTable colorTable;

    public FileInfo(GdalMerge gdalMerge, Dataset dataset) {
        this.setGdalMerge(gdalMerge);
        this.setDataset(dataset);
        setBands(dataset.GetRasterCount());
        setxSize(dataset.GetRasterXSize());
        setySize(dataset.GetRasterYSize());
        setBandType(dataset.GetRasterBand(1).getDataType());
        setProjection(dataset.GetProjection());
        setGeotransform(dataset.GetGeoTransform());
        setUlx(getGeotransform()[0]);
        setUly(getGeotransform()[3]);
        setLrx(getUlx() + getGeotransform()[1] * getxSize());
        setLry(getUly() + getGeotransform()[5] * getySize());
        setColorTable(dataset.GetRasterBand(1).GetColorTable());
    }

    public FileInfo(GdalMerge gdalMerge, String filename) {
        this.setGdalMerge(gdalMerge);
        setDataset(gdal.Open(filename));
        if (null == getDataset()) {
            return;
        }

        setBands(getDataset().GetRasterCount());
        setxSize(getDataset().GetRasterXSize());
        setySize(getDataset().GetRasterYSize());
        setBandType(getDataset().GetRasterBand(1).getDataType());
        setProjection(getDataset().GetProjection());
        setGeotransform(getDataset().GetGeoTransform());
        setUlx(getGeotransform()[0]);
        setUly(getGeotransform()[3]);
        setLrx(getUlx() + getGeotransform()[1] * getxSize());
        setLry(getUly() + getGeotransform()[5] * getySize());
        setColorTable(getDataset().GetRasterBand(1).GetColorTable());
    }

    /*
        Copy this files image into target file.

        This method will compute the overlap area of the file_info objects
        file, and the target gdal.Dataset object, and copy the image data
        for the common window area.  It is assumed that the files are in
        a compatible projection ... no checking or warping is done.  However,
        if the destination file is a different resolution, or different
        image pixel type, the appropriate resampling and conversions will
        be done (using normal GDAL promotion/demotion rules).

        targetDataset -- gdal.Dataset object for the file into which some or all
        of this file may be copied.

        Returns 1 on success (or if nothing needs to be copied), and zero one
        failure.
     */
    public int copy_into(Dataset targetDataset, int sourceBand, int targetBand, Double nodataValue) {
        double[] targetGeotransform = targetDataset.GetGeoTransform();
        double targetUlx = targetGeotransform[0];
        double targetUly = targetGeotransform[3];
        double targetLrx = targetGeotransform[0] + targetDataset.GetRasterXSize() * targetGeotransform[1];
        double targetLry = targetGeotransform[3] + targetDataset.GetRasterYSize() * targetGeotransform[5];

        // figure out intersection region
        double tgw_uly;
        double tgw_lry;
        double tgw_ulx = Math.max(targetUlx, this.getUlx());
        double tgw_lrx = Math.min(targetLrx, this.getLrx());
        if (targetGeotransform[5] < 0) {
            tgw_uly = Math.min(targetUly, this.getUly());
            tgw_lry = Math.max(targetLry, this.getLry());
        } else {
            tgw_uly = Math.max(targetUly, this.getUly());
            tgw_lry = Math.min(targetLry, this.getLry());
        }

        // do they even intersect?
        if (tgw_ulx >= tgw_lrx) {
            return 1;
        }

        if (targetGeotransform[5] < 0 && tgw_uly <= tgw_lry) {
            return 1;
        }

        if (targetGeotransform[5] > 0 && tgw_uly >= tgw_lry) {
            return 1;
        }

        // compute target window in pixel coordinates.
        int tw_xoff = (int) Math.round((tgw_ulx - targetGeotransform[0]) / targetGeotransform[1] + 0.1);
        int tw_yoff = (int) Math.round((tgw_uly - targetGeotransform[3]) / targetGeotransform[5] + 0.1);
        // 因为四舍五入，所有这里不加 0.5
        int tw_xsize = (int) Math.round((tgw_lrx - targetGeotransform[0])/targetGeotransform[1]) - tw_xoff;
        int tw_ysize = (int) Math.round((tgw_lry - targetGeotransform[3])/targetGeotransform[5]) - tw_yoff;

        if (tw_xsize < 1 || tw_ysize < 1) {
            return 1;
        }

        // Compute source window in pixel coordinates.
        int sw_xoff = (int) Math.round((tgw_ulx - this.getGeotransform()[0]) / this.getGeotransform()[1]);
        int sw_yoff = (int) Math.round((tgw_uly - this.getGeotransform()[3]) / this.getGeotransform()[5]);
        // 因为四舍五入，所有这里不加 0.5
        int sw_xsize = (int) Math.round((tgw_lrx - this.getGeotransform()[0]) / this.getGeotransform()[1]) - sw_xoff;
        int sw_ysize = (int) Math.round((tgw_lry - this.getGeotransform()[3]) / this.getGeotransform()[5]) - sw_yoff;

        if (sw_xsize < 1 || sw_ysize < 1) {
            return 1;
        }

        // Open the source file, and copy the selected region.
        return getGdalMerge().raster_copy(
                getDataset(), sw_xoff, sw_yoff, sw_xsize, sw_ysize, sourceBand,
                targetDataset, tw_xoff, tw_yoff, tw_xsize, tw_ysize, targetBand,
                nodataValue);
    }

    public int getxSize() {
        return xSize;
    }

    public void setxSize(int xSize) {
        this.xSize = xSize;
    }

    public int getySize() {
        return ySize;
    }

    public void setySize(int ySize) {
        this.ySize = ySize;
    }

    public GdalMerge getGdalMerge() {
        return gdalMerge;
    }

    public void setGdalMerge(GdalMerge gdalMerge) {
        this.gdalMerge = gdalMerge;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public int getBands() {
        return bands;
    }

    public void setBands(int bands) {
        this.bands = bands;
    }

    public int getBandType() {
        return bandType;
    }

    public void setBandType(int bandType) {
        this.bandType = bandType;
    }

    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

    public double[] getGeotransform() {
        return geotransform;
    }

    public void setGeotransform(double[] geotransform) {
        this.geotransform = geotransform;
    }

    public double getUlx() {
        return ulx;
    }

    public void setUlx(double ulx) {
        this.ulx = ulx;
    }

    public double getUly() {
        return uly;
    }

    public void setUly(double uly) {
        this.uly = uly;
    }

    public double getLrx() {
        return lrx;
    }

    public void setLrx(double lrx) {
        this.lrx = lrx;
    }

    public double getLry() {
        return lry;
    }

    public void setLry(double lry) {
        this.lry = lry;
    }

    public ColorTable getColorTable() {
        return colorTable;
    }

    public void setColorTable(ColorTable colorTable) {
        this.colorTable = colorTable;
    }
}
