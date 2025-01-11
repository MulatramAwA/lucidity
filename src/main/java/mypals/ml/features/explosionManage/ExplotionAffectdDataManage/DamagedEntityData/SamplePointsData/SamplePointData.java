package mypals.ml.features.explosionManage.ExplotionAffectdDataManage.DamagedEntityData.SamplePointsData;

import mypals.ml.features.explosionManage.ExplotionAffectdDataManage.DamagedEntityData.SamplePointsData.RayCastPointInfo.RayCastData;

import java.util.List;

public class SamplePointData {
    public final List<RayCastData> castPointData;

    public SamplePointData(List<RayCastData> castPointData) {
        this.castPointData = castPointData;
    }
    public List<RayCastData> getCastPointData()
    {
        return castPointData;
    }

}