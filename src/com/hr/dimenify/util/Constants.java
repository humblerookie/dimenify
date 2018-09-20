package com.hr.dimenify.util;


public interface Constants {
    String SAVE_PREFIX = "com.hr.dimenify.";
    String MIGRATION_FLAG = "v2Tov3";
    String SAVE_PREFIX_V2 = "com.hr.dimenify.v2";
    String INIT_MODEL_JSON = "[{\"bucket\":\"mdpi\",\"directory\":\"values\",\"factorSp\":1,\"factorDp\":1,\"isSelected\":true,\"isMandatory\":true},{\"bucket\":\"hdpi\",\"factorSp\":1.2,\"factorDp\":1.2,\"directory\":\"values-hdpi\",\"isSelected\":true,\"isMandatory\":true},{\"bucket\":\"xhdpi\",\"factorSp\":1.8,\"factorDp\":1.8,\"directory\":\"values-xhdpi\",\"isSelected\":true,\"isMandatory\":true},{\"bucket\":\"xxhdpi\",\"factorSp\":2.4,\"factorDp\":2.4,\"directory\":\"values-xxhdpi\",\"isSelected\":true,\"isMandatory\":true},{\"bucket\":\"xxxhdpi\",\"factorSp\":3,\"factorDp\":3,\"directory\":\"values-xxxhdpi\",\"isSelected\":true,\"isMandatory\":true}]";
    String RESOURCES_TEXT = "<resources>\n</resources>";
    String RESOURCES_TAG = "resources";
    String DIMEN_TAG = "dimen";
    String MDPI = "mdpi";
    float TITLE_SIZE = 16.0f;
    String NAME_TAG = "name";
    String PLACEHOLDER_DIMEN = "\t<dimen name=\"{0}\">{1}{2}</dimen>\n";
    String FILE_NAME = "dimens.xml";
    String VALUES_PREFIX = "values-";
    String DP = "dp";
    String SP = "sp";
    String TITLE = "Set scale factors for ";
    String BULK_TITLE = "Your scaling factors are";
    String MESSAGES[] = {
            "There are duplicate buckets please fix before adding more."
            , "Custom buckets are restricted to 5"
            , "Could not map the resource folder to a density value"
            , "Could not convert the value into a number"
            , "Could not map xml the file to a density bucket.\n Please check if the source density bucket was added"
    };
    int ERROR_CODE[] = {1, 2};
    String ERROR_TITLE = "Error";
    String SCALE_TEXT_PREFIX = "Please a scale value for ";
    String NEW_BUCKET = "Add new bucket";
    String METRIC = " metric";
    String DEFAULT_BUCKET = "sw600dp-land";
    float DEFAULT_SCALE_FACTOR = 1.2f;

    enum Mode {
        SINGLE,
        BULK;
    }

}
