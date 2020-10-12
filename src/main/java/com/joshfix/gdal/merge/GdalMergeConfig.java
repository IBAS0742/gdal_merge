package com.joshfix.gdal.merge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author joshfix
 * Created on 6/27/2019
 */

public class GdalMergeConfig {

    private boolean help;
    private String outDatasetName;
    private boolean createOnly;
    private boolean separate;
    private boolean copyPct;
    private boolean verbose;
    private boolean quiet;
    private Integer bandType;
    private List<Double> preInit = new ArrayList<>();
    private Double nodata;
    private Double aNodata;
    private String format;
    private List<String> createOptions = new ArrayList<>();
    private Double psizeX;
    private Double psizeY;
    private Double ulx;
    private Double uly;
    private Double lrx;
    private Double lry;
    private boolean bTargetAlignedPixels = false;

    public boolean getHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public String getOutDatasetName() {
        return outDatasetName;
    }
    public void setOutDatasetName(String odn) {
        this.outDatasetName = odn;
    }

    public boolean isCreateOnly() {
        return createOnly;
    }

    public void setCreateOnly(boolean createOnly) {
        this.createOnly = createOnly;
    }

    public boolean isSeparate() {
        return separate;
    }

    public void setSeparate(boolean separate) {
        this.separate = separate;
    }

    public boolean isCopyPct() {
        return copyPct;
    }

    public void setCopyPct(boolean copyPct) {
        this.copyPct = copyPct;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public Integer getBandType() {
        return bandType;
    }

    public void setBandType(Integer bandType) {
        this.bandType = bandType;
    }

    public List<Double> getPreInit() {
        return preInit;
    }

    public void setPreInit(List<Double> preInit) {
        this.preInit = preInit;
    }

    public Double getNodata() {
        return nodata;
    }

    public void setNodata(Double nodata) {
        this.nodata = nodata;
    }

    public Double getaNodata() {
        return aNodata;
    }

    public void setaNodata(Double aNodata) {
        this.aNodata = aNodata;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<String> getCreateOptions() {
        return createOptions;
    }

    public void setCreateOptions(List<String> createOptions) {
        this.createOptions = createOptions;
    }

    public Double getPsizeX() {
        return psizeX;
    }

    public void setPsizeX(Double psizeX) {
        this.psizeX = psizeX;
    }

    public Double getPsizeY() {
        return psizeY;
    }

    public void setPsizeY(Double psizeY) {
        this.psizeY = psizeY;
    }

    public Double getUlx() {
        return ulx;
    }

    public void setUlx(Double ulx) {
        this.ulx = ulx;
    }

    public Double getUly() {
        return uly;
    }

    public void setUly(Double uly) {
        this.uly = uly;
    }

    public Double getLrx() {
        return lrx;
    }

    public void setLrx(Double lrx) {
        this.lrx = lrx;
    }

    public Double getLry() {
        return lry;
    }

    public void setLry(Double lry) {
        this.lry = lry;
    }

    public boolean isbTargetAlignedPixels() {
        return bTargetAlignedPixels;
    }

    public void setbTargetAlignedPixels(boolean bTargetAlignedPixels) {
        this.bTargetAlignedPixels = bTargetAlignedPixels;
    }
}
