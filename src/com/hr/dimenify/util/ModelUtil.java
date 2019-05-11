package com.hr.dimenify.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hr.dimenify.model.Dimen;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anvithbhat on 25/03/17.
 */
public class ModelUtil {

    static Gson gson = new Gson();

    public static String toJson(List<Dimen> dimenList) {
        return gson.toJson(dimenList);
    }

    public static ArrayList<Dimen> fromJson(String data) {
        ArrayList<Dimen> dimenList = gson.fromJson(data, new TypeToken<ArrayList<Dimen>>() {
        }.getType());
        return dimenList;
    }
}
