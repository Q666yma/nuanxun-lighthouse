package com.maka.service.impl;

import com.maka.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.json.JSONObject;

@Service
public class AddressServiceImpl implements AddressService {

    @org.springframework.beans.factory.annotation.Value("${AMAP_SERVER_KEY:}")
    private String amapServerKey;

    private final Address[] addresses = {
        new Address("青岛市公安局", System.getenv().getOrDefault("RESCUE_CONTACT_EMAIL", "")),
        new Address("青岛市救援中心", System.getenv().getOrDefault("RESCUE_CONTACT_EMAIL", ""))
    };

    @Override
    public String findClosestAddressEmail(double lostLongitude, double lostLatitude) {
        String closestEmail = null;
        double minDistance = Double.MAX_VALUE;

        for (Address address : addresses) {
            double[] latLng = getCoordinates(address.getLocation());
            double addressLongitude = latLng[0];
            double addressLatitude = latLng[1];

            double distance = calculateDistance(lostLongitude, lostLatitude, addressLongitude, addressLatitude);
            if (distance < minDistance) {
                minDistance = distance;
                closestEmail = address.getEmail();
            }
        }
        return closestEmail;
    }

    private double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double[] getCoordinates(String address) {
        RestTemplate restTemplate = new RestTemplate();
        String URL = "https://restapi.amap.com/v3/geocode/geo?address=" + address + "&key=" + amapServerKey;
        ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
        String jsonResponse = response.getBody();
        JSONObject jsonObject = new JSONObject(jsonResponse);
        if (jsonObject.getJSONArray("geocodes").length() > 0) {
            String location = jsonObject.getJSONArray("geocodes").getJSONObject(0).getString("location");
            String[] latLng = location.split(",");
            return new double[]{Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1])};
        }
        return new double[]{0.0, 0.0};
    }

    private static class Address {
        private String location;
        private String email;

        public Address(String location, String email) {
            this.location = location;
            this.email = email;
        }

        public String getLocation() { return location; }
        public String getEmail() { return email; }
    }
}