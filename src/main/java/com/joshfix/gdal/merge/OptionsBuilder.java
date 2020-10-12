package com.joshfix.gdal.merge;

import java.util.ArrayList;
import java.util.Vector;

/**
 * @author joshfix
 * Created on 6/27/2019
 */
public class OptionsBuilder extends ArrayList<String> {

    public OptionsBuilder addOption(String option) {
        this.add(option);
        return this;
    }

    public Vector toVector() {
        return new Vector(this);
    }
}
