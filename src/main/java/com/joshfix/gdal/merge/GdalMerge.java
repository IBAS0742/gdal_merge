package com.joshfix.gdal.merge;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;


import static org.gdal.gdalconst.gdalconstConstants.*;

/**
 * @author joshfix
 * Created on 6/27/2019
 */
public class GdalMerge {

    private static Logger logger = Logger.getLogger(GdalMerge.class.getName());

    public static void main(String... args) {
        GdalMerge gm = new GdalMerge();
        gdal.AllRegister();
        gm.commandLine(args);
    }

    public static void help() {
        System.out.println("默认为 tif 类型");
        System.out.println("Usage: gdal_merge.py [-o out_filename] [-of out_format] [-co NAME=VALUE]*\n" +
                "                     [-ps pixelsize_x pixelsize_y] [-tap] [-separate] [-q] [-v] [-pct]\n" +
                "                     [-ul_lr ulx uly lrx lry] [-init \"value [value...]\"]\n" +
                "                     [-n nodata_value] [-a_nodata output_nodata_value]\n" +
                "                     [-ot datatype] [-createonly] input_files\n" +
                "                     [--help-general]");
    }

    public Dataset commandLine(String... argv) {
        GdalMergeConfig config = new GdalMergeConfig();
        List<String> names = new ArrayList<>();
        int i = 0;
        while (i < argv.length) {
            String arg = argv[i];
            if (arg.equalsIgnoreCase("-v")) {
                config.setVerbose(true);
            } else if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {
                config.setHelp(true);
            } else if (arg.equalsIgnoreCase("-q") || arg.equalsIgnoreCase("-quiet")) {
                config.setQuiet(true);
            } else if (arg.equalsIgnoreCase("-createOnly")) {
                config.setCreateOnly(true);
            } else if (arg.equalsIgnoreCase("-o")) {
                i++;
                config.setOutDatasetName(argv[i]);
            } else if (arg.equalsIgnoreCase("-separate") || arg.equalsIgnoreCase("-seperate")) {
                config.setSeparate(true);
            } else if (arg.equalsIgnoreCase("-pct")) {
                config.setCopyPct(true);
            } else if (arg.equalsIgnoreCase("-ot")) {
                i++;
                int band_type = gdal.GetDataTypeByName(argv[i]);
                if (band_type == GDT_Unknown) {
                    throw new RuntimeException("Unknown GDAL data type: " + argv[i]);
                }
            } else if (arg.equalsIgnoreCase("-init")) {
                i++;
                String[] str_pre_init = argv[i].split(" ");
                for (String pre_init : str_pre_init) {
                    config.getPreInit().add(Double.valueOf(pre_init));
                }
            } else if (arg.equalsIgnoreCase("-n")) {
                i++;
                config.setNodata(Double.valueOf(argv[i]));
            } else if (arg.equalsIgnoreCase("-aNodata")) {
                i++;
                config.setNodata(Double.valueOf(argv[i]));
            } else if (arg.equalsIgnoreCase("-f") || arg.equalsIgnoreCase("-of")) {
                i++;
                config.setFormat(argv[i]);
            } else if (arg.equalsIgnoreCase("-co")) {
                i++;
                config.getCreateOptions().add(argv[i]);
            } else if (arg.equalsIgnoreCase("-ps")) {
                config.setPsizeX(Double.valueOf(argv[i + 1]));
                config.setPsizeY(Double.valueOf(-Math.abs(Double.valueOf(argv[i + 2]))));
                i = i + 2;
            } else if (arg.equalsIgnoreCase("-tap")) {
                config.setbTargetAlignedPixels(true);
            } else if (arg.equalsIgnoreCase("-ul_lr")) {
                config.setUlx(Double.valueOf(argv[i + 1]));
                config.setUly(Double.valueOf(argv[i + 2]));
                config.setLrx(Double.valueOf(argv[i + 3]));
                config.setLry(Double.valueOf(argv[i + 4]));
                i = i + 4;
            } else if (arg.startsWith("-")) {
                throw new RuntimeException("Unrecognized command option: " + arg);
            } else {
                names.add(arg);
            }
            i++;
        }
        if (config.getHelp()) {
            help();
            return null;
        } else {
            return execute(config, names);
        }
    }

    public Dataset execute(GdalMergeConfig config, List<String> names) {
        if (null == names || names.isEmpty()) {
            help();
            throw new RuntimeException("No input files provided.");
        }
        // Collect information on all the source files.
        List<FileInfo> file_infos = names_to_fileinfos(names);
        return executeInternal(config, file_infos);
    }

    public Dataset executeInternal(GdalMergeConfig config, List<FileInfo> file_infos) {
        if (null == config.getFormat()) {
            config.setFormat(GetOutputDriverFor(config.getOutDatasetName()));
        }
        if (null == config.getOutDatasetName()) {
            help();
            throw new RuntimeException("-o is require");
        }

        Driver Driver = gdal.GetDriverByName(config.getFormat());
        if (null == Driver) {
            help();
            throw new RuntimeException("Format driver " + config.getFormat() + " not found, pick a supported driver.");
        }

        //Vector DriverMD = Driver.GetMetadata_List();
        Hashtable DriverMD = Driver.GetMetadata_Dict();
        if (!DriverMD.containsKey("DCAP_CREATE")) {
            help();
            throw new RuntimeException("Format driver " + config.getFormat() + " does not support creation and piecewise writing.\n" +
                    "Please select a format that does, such as GTiff (the default) or HFA (Erdas Imagine).");
        }

        if (null == config.getUlx()) {
            config.setUlx(file_infos.get(0).getUlx());
            config.setUly(file_infos.get(0).getUly());
            config.setLrx(file_infos.get(0).getLrx());
            config.setLry(file_infos.get(0).getLry());

            for (FileInfo fi : file_infos) {
                config.setUlx(Math.min(config.getUlx(), fi.getUlx()));
                config.setUly(Math.max(config.getUly(), fi.getUly()));
                config.setLrx(Math.max(config.getLrx(), fi.getLrx()));
                config.setLry(Math.min(config.getLrx(), fi.getLry()));
            }
        }

        if (null == config.getPsizeX()) {
            config.setPsizeX(file_infos.get(0).getGeotransform()[1]);
            config.setPsizeY(file_infos.get(0).getGeotransform()[5]);
        }

        if (null == config.getBandType()) {
            config.setBandType(file_infos.get(0).getBandType());
        }

        // Try opening as an existing file.
        gdal.PushErrorHandler( "CPLQuietErrorHandler");
        Dataset targetDataset = gdal.Open(config.getOutDatasetName(), GA_Update);
        gdal.PopErrorHandler();

        int bands = 0;

        // Create output file if it does not already exist.
        if (null == targetDataset) {

            if (config.isbTargetAlignedPixels()) {
                config.setUlx(Math.floor(config.getUlx() / config.getPsizeX()) * config.getPsizeX());
                config.setLrx(Math.ceil(config.getLrx() / config.getPsizeX()) * config.getPsizeX());
                config.setLry(Math.floor(config.getLry() / -config.getPsizeY()) * -config.getPsizeY());
                config.setUly(Math.ceil(config.getUly() / -config.getPsizeY()) * -config.getPsizeY());
            }

            double[] geotransform = new double[]{config.getUlx(), config.getPsizeX(), 0d, config.getUly(), 0d, config.getPsizeY()};

            // 因为四舍五入，所有这里不加 0.5
            int xsize = (int) Math.round((config.getLrx() - config.getUlx()) / geotransform[1]);
            int ysize = (int) Math.round((config.getLry() - config.getUly()) / geotransform[5]);

            if (config.isSeparate()) {
                bands = 0;
                for (FileInfo fi : file_infos) {
                    bands = bands + fi.getBands();
                }
            } else {
                bands = file_infos.get(0).getBands();
            }

            targetDataset = Driver.Create(config.getOutDatasetName(), xsize, Math.abs(ysize), bands, config.getBandType(),
                    config.getCreateOptions().toArray(new String[config.getCreateOptions().size()]));
            if (null == targetDataset) {
                help();
                throw new RuntimeException("Creation failed, terminating gdal_merge.");
            }

            targetDataset.SetGeoTransform(geotransform);
            targetDataset.SetProjection(file_infos.get(0).getProjection());

            if (config.isCopyPct()) {
                targetDataset.GetRasterBand(1).SetRasterColorTable(file_infos.get(0).getColorTable());
            }
        } else {

            if (config.isSeparate()) {

                for (FileInfo fi : file_infos) {
                    bands = bands + fi.getBands();
                }
                if (targetDataset.getRasterCount() < bands) {
                    help();
                    throw new RuntimeException("Existing output file has less bands than the input files. You should delete it before. Terminating gdal_merge.");
                }
            } else {
                bands = Math.min(file_infos.get(0).getBands(), targetDataset.getRasterCount());
            }
        }

        // Do we need to set nodata value ?
        if (null != config.getaNodata()){
            for (int i = 0; i < targetDataset.getRasterCount(); i++) {
                targetDataset.GetRasterBand(i + 1).SetNoDataValue(config.getaNodata());
            }
        }

        // Do we need to pre-initialize the whole mosaic file to some value?
        if (null == config.getPreInit()) {
            if (targetDataset.getRasterCount() <= config.getPreInit().size()) {
                for (int i = 0; i < targetDataset.getRasterCount(); i++) {
                    targetDataset.GetRasterBand(i + 1).Fill(config.getPreInit().get(i));
                }
            } else if (config.getPreInit().size() == 1) {
                for (int i = 0; i < targetDataset.getRasterCount(); i++) {
                    targetDataset.GetRasterBand(i + 1).Fill(config.getPreInit().get(0));
                }
            }
        }

        // Copy data from source files into output file.
        int t_band = 1;

        for (FileInfo fi : file_infos) {
            if (config.isCreateOnly()) {
                continue;
            }

            if (!config.isSeparate()) {
                for (int band = 1; band <= bands; band++) {
                    fi.copy_into(targetDataset, band, band, config.getNodata());
                }
            } else {
                for (int band = 1; band <= fi.getBands(); band++) {
                    fi.copy_into(targetDataset, band, t_band, config.getNodata());
                    t_band++;
                }
            }
        }

        return targetDataset;

    }

    // 第一个
    public boolean DoesDriverHandleExtension(Driver drv, String ext) {
        String exts = drv.GetMetadataItem(GDAL_DMD_EXTENSIONS);
        return (null != exts && exts.toLowerCase().contains(ext.toLowerCase()));
    }

    // 第二个
    public static String GetExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    // 第三个
    public List<String> GetOutputDriversFor(String filename) {
        List<String> drv_list = new ArrayList<>();
        String ext = GetExtension(filename);
        for (int i = 0; i < gdal.GetDriverCount(); i++) {
            Driver drv = gdal.GetDriver(i);
            /*if ((drv.GetMetadataItem(GDAL_DCAP_CREATE) != null || drv.GetMetadataItem(GDAL_DCAP_CREATECOPY) != null)
                && drv.GetMetadataItem(DCAP_RASTER) != null) {*/

            if ((drv.GetMetadataItem("DCAP_CREATE") != null || drv.GetMetadataItem("DCAP_CREATECOPY") != null)
                    && drv.GetMetadataItem("DCAP_RASTER") != null) {
                if (ext.length() > 0 && DoesDriverHandleExtension(drv, ext)) {
                    drv_list.add(drv.getShortName());
                } else{
                    String prefix = drv.GetMetadataItem(DMD_CONNECTION_PREFIX);
                    if (null != prefix && filename.toLowerCase().startsWith(prefix.toLowerCase())) {
                        drv_list.add(drv.getShortName());
                    }
                }
            }
        }

        // GMT is registered before netCDF for opening reasons, but we want netCDF to be used by default for output.
        if (ext.equalsIgnoreCase("nc") && drv_list.get(0).equalsIgnoreCase("GMT")
                && drv_list.get(1).equalsIgnoreCase("NETCDF")) {
                drv_list = Arrays.asList("NETCDF", "GMT");
        }

        return drv_list;
    }

    // 第四个
    public String GetOutputDriverFor(String filename) {
        List<String> drv_list = GetOutputDriversFor(filename);
        String ext = GetExtension(filename);
        if (drv_list.isEmpty()) {
//            if (ext.isEmpty() || filename.startsWith("/vsimem/")) {
//                return "GTiff";
//            } else {
//                throw new RuntimeException("Cannot guess driver for " + filename);
//            }
            return "GTiff";
        } else if (drv_list.size() > 1) {

            logger.info("Several drivers matching " + ext + " extension.  Using " + drv_list.get(0));
        }
        return drv_list.get(0);
    }

    // 第八个
    // Returns a list of file_info objects.  There may be less file_info objects
    // than names if some of the names could not be opened as GDAL files.
    public List<FileInfo> names_to_fileinfos(List<String> names) {
        List<FileInfo> fileInfos = new ArrayList<>();
        for (String name : names) {
            fileInfos.add(new FileInfo(this, name));
        }
        return fileInfos;
    }

    public List<FileInfo> datasetsToFileInfos(List<Dataset> datasets) {
        List<FileInfo> fileInfos = new ArrayList<>();
        datasets.forEach(dataset -> fileInfos.add(new FileInfo(this, dataset)));
        return fileInfos;
    }

    // 第五个(ReadRaster_Direct)
    public int raster_copy(Dataset s_fh, int s_xoff, int s_yoff, int s_xsize, int s_ysize, int s_band_n,
                            Dataset t_fh, int t_xoff, int t_yoff, int t_xsize, int t_ysize, int t_band_n,
                            Double nodata) {

        if (null != nodata) {
            return raster_copy_with_nodata(
                    s_fh, s_xoff, s_yoff, s_xsize, s_ysize, s_band_n,
                    t_fh, t_xoff, t_yoff, t_xsize, t_ysize, t_band_n,
                    nodata);
        }

        Band s_band = s_fh.GetRasterBand(s_band_n);
        Band m_band = null;
        // Works only in binary mode and doesn't take into account intermediate transparency values for compositing.
        if (s_band.GetMaskFlags() != GMF_ALL_VALID) {
            m_band = s_band.GetMaskBand();
        }
        else if (s_band.GetColorInterpretation() == GCI_AlphaBand) {
            m_band = s_band;
        }

        if (null != m_band) {
            return raster_copy_with_mask(
                    s_fh, s_xoff, s_yoff, s_xsize, s_ysize, s_band_n,
                    t_fh, t_xoff, t_yoff, t_xsize, t_ysize, t_band_n,
                    m_band);
        }

        s_band = s_fh.GetRasterBand(s_band_n);
        Band t_band = t_fh.GetRasterBand(t_band_n);

        ByteBuffer data = s_band.ReadRaster_Direct(s_xoff, s_yoff, s_xsize, s_ysize, t_xsize, t_ysize, t_band.getDataType());
        t_band.WriteRaster_Direct( t_xoff, t_yoff, t_xsize, t_ysize, t_xsize, t_ysize, t_band.getDataType(), data);

        return 0;
    }

    // 第六个(不一致)
    public int raster_copy_with_nodata(Dataset s_fh, int s_xoff, int s_yoff, int s_xsize, int s_ysize, int s_band_n,
                                        Dataset t_fh, int t_xoff, int t_yoff, int t_xsize, int t_ysize, int t_band_n,
                                        Double nodata ){

        Band s_band = s_fh.GetRasterBand(s_band_n);
        Band t_band = t_fh.GetRasterBand(t_band_n);

        ByteBuffer data_src = s_band.ReadRaster_Direct(s_xoff, s_yoff, s_xsize, s_ysize,
                t_xsize, t_ysize, s_band.getDataType());


        t_band.WriteRaster_Direct( t_xoff, t_yoff, t_xsize, t_ysize, t_band.getDataType(), data_src);

        return 0;
    }

    // 第七个(不一致)
    public int raster_copy_with_mask(Dataset s_fh, int s_xoff, int s_yoff, int s_xsize, int s_ysize, int s_band_n,
                                     Dataset t_fh, int t_xoff, int t_yoff, int t_xsize, int t_ysize, int t_band_n,
                                      Band m_band ) {

        Band s_band = s_fh.GetRasterBand(s_band_n);
        Band t_band = t_fh.GetRasterBand(t_band_n);

        ByteBuffer data_src = s_band.ReadRaster_Direct(s_xoff, s_yoff, s_xsize, s_ysize,
                t_xsize, t_ysize, s_band.getDataType());
//        ByteBuffer data_mark = m_band.ReadRaster_Direct(s_xoff, s_yoff, s_xsize, s_ysize,
//                t_xsize, t_ysize, m_band.getDataType());
//        ByteBuffer data_dst = t_band.ReadRaster_Direct(t_xoff, t_yoff, t_xsize, t_ysize, t_band.getDataType());

        // NOTE not sure what to do with these yet... assumed nodata value was a double, but that can't equal an int array
        //mask_test = Numeric.equal(data_mask, 0)
        //to_write = Numeric.choose(mask_test, (data_src, data_dst) )

        t_band.WriteRaster_Direct( t_xoff, t_yoff, t_xsize, t_ysize, t_band.getDataType(), data_src);

        return 0;
    }
}
