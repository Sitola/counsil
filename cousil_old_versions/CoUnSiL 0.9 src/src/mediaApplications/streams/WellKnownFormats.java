package mediaApplications.streams;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class stores only enumeration of several well known stream formats.
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public class WellKnownFormats implements Serializable {
    
    private static final int resMultiplier = 1500 / 24 * 1000000;
    
    static public enum Format implements Serializable {
        
        UNCOMPRESSED_FULL_HD(1, new StreamFormat("Uncompressed Full HD", resMultiplier * 24, 100, 0, 0)),
        DXT5_FULL_HD(2, new StreamFormat("DXT Full HD", resMultiplier * 8, 80, 30, 30)),
        JPEG_100_FULL_HD(3, new StreamFormat("JPEG Q100 Full HD", (long) (resMultiplier * 1.4), 60, 20, 20)),
        JPEG_90_FULL_HD(4, new StreamFormat("JPEG Q90 Full HD", (long) (resMultiplier * .96), 55, 15, 15)),
        JPEG_80_FULL_HD(5, new StreamFormat("JPEG Q80 Full HD", (long) (resMultiplier * .77), 50, 13, 13)),
        JPEG_70_FULL_HD(6, new StreamFormat("JPEG Q70 Full HD", (long) (resMultiplier * .7), 45, 12, 12)),
        JPEG_60_FULL_HD(7, new StreamFormat("JPEG Q60 Full HD", (long) (resMultiplier * .6), 40, 11, 11)),
        JPEG_50_FULL_HD(8, new StreamFormat("JPEG Q50 Full HD", (long) (resMultiplier * .5), 35, 10, 10)),
        JPEG_40_FULL_HD(9, new StreamFormat("JPEG Q40 Full HD", (long) (resMultiplier * .4), 30, 9, 9)),
        JPEG_30_FULL_HD(10, new StreamFormat("JPEG Q30 Full HD", (long) (resMultiplier * .3), 25, 8, 8));
        
        public final int order;
        public final StreamFormat format;

        private Format(int order, StreamFormat format) {
            this.order = order;
            this.format = format;
        }
                
        public static ArrayList<StreamFormat> getFormatList() {
            ArrayList<StreamFormat> list = new ArrayList();
            for (Format f : Format.values()) {
                list.add(f.format);
            }
            return list;
        }
        
        public static ArrayList<StreamFormat> getFormatList(int n) {
            ArrayList<StreamFormat> list = new ArrayList(n);
            int i = 0;
            for (Format f : Format.values()) {
                list.add(f.format);
                if (++i >= n) break;
            }
            return list;
        }
        
        public static ArrayList<StreamFormat> getFormatList(Iterable<Integer> idList) {
            ArrayList<StreamFormat> ret = new ArrayList<StreamFormat>();
            for (int id : idList) {
                ret.add(getByFormat(id));
            }
            return ret;
        }
        
        public static StreamFormat getByFormat(int order) {
            for (Format f : Format.values()) {
                if (order == f.order) {
                    return f.format;
                }
            }
            return null;
        }
    }
}
