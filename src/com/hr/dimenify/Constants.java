package com.hr.dimenify;

/**
 * Created by anvithbhat on 19/12/16.
 */
public interface Constants {
    String SAVE_PREFIX ="com.hr.dimenify.";
    String RESOURCES_TEXT = "<resources>\n</resources>";
    String RESOURCES_TAG = "resources";
    String DIMEN_TAG = "dimen";
    String NAME_TAG = "name";
    String PLACEHOLDER_DIMEN = "<dimen name=\"{0}\">{1}{2}</dimen>";
    float[] SCALE_FACTORS_DP = {1.0f, 1.2f, 1.8f, 2.4f, 3.0f};
    float[] SCALE_FACTORS_SP = {1.0f, 1.2f, 1.8f, 2.4f, 3.0f};
    String FILE_NAME = "dimens.xml";
    String[] DIRECTORIES = {"values", "values-hdpi", "values-xhdpi", "values-xxhdpi", "values-xxxhdpi"};
    String[] BUCKETS = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
    String DP = "dp";
    String SP = "sp";
    String TITLE = "Set scale factors";
}
