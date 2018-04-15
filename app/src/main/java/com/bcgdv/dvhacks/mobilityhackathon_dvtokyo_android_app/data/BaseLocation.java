package com.bcgdv.dvhacks.mobilityhackathon_dvtokyo_android_app.data;


import java.util.ArrayList;
import com.bcgdv.dvhacks.mobilityhackathon_dvtokyo_android_app.R;

/**
 * Dummy data for base locations
 */
public class BaseLocation {

    private double mLat;
    private double mLon;
    private String mName;
    private String mModel;
    private float mRate;
    private int mIconResource;

    public double getLat() {
        return mLat;
    }

    public double getLon() {
        return mLon;
    }

    public String getName() {
        return mName;
    }

    public String getModel() {
        return mModel;
    }

    public float getRate() {
        return mRate;
    }

    public int getIconResource() {
        return mIconResource;
    }


    BaseLocation(double lat, double lon, String name, String model,
                 float rate, int iconResource) {
        this.mLat = lat;
        this.mLon = lon;
        this.mName = name;
        this.mModel = model;
        this.mRate = rate;
        this.mIconResource = iconResource;

    }

    public static ArrayList<BaseLocation> generateDummyData() {
        ArrayList<BaseLocation> ret = new ArrayList<BaseLocation>();
        BaseLocation place1 = new BaseLocation(33.903005, -118.390021,
                "Honda Vitz",
                "39ZLT0M1",
                0.8f,
                R.drawable.ic_car);
        BaseLocation place2 = new BaseLocation(33.904466, -118.392986,
                "Nissan Leaf",
                "350Z",
                0.8f,
                R.drawable.ic_car);
        BaseLocation place3 = new BaseLocation(33.898589, -118.392128,
                "Ford Focus Electric",
                "Some Model",
                0.8f,
                R.drawable.ic_car);
        BaseLocation place4 = new BaseLocation(33.896635, -118.389935,
                "Honda Vitz",
                "39ZLT0M1",
                0.8f,
                R.drawable.ic_car);

        ret.add(place1);
        ret.add(place2);
        ret.add(place3);
        ret.add(place4);

        return ret;
    }
}
