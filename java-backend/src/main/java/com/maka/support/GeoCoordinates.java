package com.maka.support;
import java.util.*;
import java.util.regex.*;
import com.fasterxml.jackson.databind.ObjectMapper;
/** Location form convention is (latitude,longitude); GeoJSON is [longitude,latitude]. */
public final class GeoCoordinates {
    private static final Pattern PAIR=Pattern.compile("[（(]\\s*(-?\\d+(?:\\.\\d+)?)\\s*[,，]\\s*(-?\\d+(?:\\.\\d+)?)\\s*[）)]");
    public static double[] valid(double lng,double lat){return Double.isFinite(lng)&&Double.isFinite(lat)&&Math.abs(lng)<=180&&Math.abs(lat)<=90?new double[]{lng,lat}:null;}
    public static double[] location(String value){
        if(value==null)return null;Matcher pair=PAIR.matcher(value);if(!pair.find())return null;
        try{return valid(Double.parseDouble(pair.group(2)),Double.parseDouble(pair.group(1)));}catch(NumberFormatException e){return null;}
    }
    public static double[] geometry(String json){
        if(json==null)return null;
        try {
            Map<?,?> shape=new ObjectMapper().readValue(json,Map.class);Object coords=shape.get("coordinates");
            if("Polygon".equals(shape.get("type")))coords=((List<?>)coords).get(0);
            if(!"Point".equals(shape.get("type")))coords=((List<?>)coords).get(0);
            List<?> pair=(List<?>)coords;return valid(((Number)pair.get(0)).doubleValue(),((Number)pair.get(1)).doubleValue());
        }catch(Exception invalid){return null;}
    }
}
