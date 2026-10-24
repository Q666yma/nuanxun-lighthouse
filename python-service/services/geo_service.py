"""Geocode addresses first and calculate actual straight-line distance."""
from math import radians, sin, cos, sqrt, atan2, isfinite
import re

import requests
from config import Config


class GeoService:
    def __init__(self, api_key=None):
        self.api_key = api_key if api_key is not None else Config.MAP_API_KEY
        self._coordinates = {}

    def get_coordinates(self, address):
        if not isinstance(address, str) or not address.strip():
            return None
        if address in self._coordinates:
            return self._coordinates[address]
        # Saved location forms use (latitude,longitude); bare API pairs retain
        # the existing longitude,latitude convention.
        pair = re.search(r'[（(]\s*(-?\d+(?:\.\d+)?)\s*[,，]\s*(-?\d+(?:\.\d+)?)\s*[）)]', address)
        if pair:
            lat, lon = map(float, pair.groups())
            if isfinite(lon) and isfinite(lat) and -180 <= lon <= 180 and -90 <= lat <= 90:
                return self._remember(address, lat, lon)
            return None
        try:
            parts = address.split(',')
            if len(parts) == 2:
                lon, lat = map(float, parts)
                if isfinite(lon) and isfinite(lat) and -180 <= lon <= 180 and -90 <= lat <= 90:
                    return self._remember(address, lat, lon)
                return None
        except ValueError:
            pass
        if not self.api_key:
            return None
        try:
            response = requests.get('https://restapi.amap.com/v3/geocode/geo',
                                    params={'address': address, 'output': 'json', 'key': self.api_key},
                                    timeout=(3, 5))
            response.raise_for_status()
            data = response.json()
            if data.get('status') == '1' and data.get('geocodes'):
                lon, lat = map(float, data['geocodes'][0]['location'].split(','))
                if isfinite(lon) and isfinite(lat) and -180 <= lon <= 180 and -90 <= lat <= 90:
                    return self._remember(address, lat, lon)
        except (requests.RequestException, ValueError, TypeError, KeyError):
            pass
        return None

    def _remember(self, address, lat, lon):
        # Cache confirmed locations only; a transient provider failure must be retryable.
        self._coordinates[address] = (lat, lon)
        if len(self._coordinates) > 512:
            self._coordinates.pop(next(iter(self._coordinates)))
        return lat, lon

    def calculate_distance(self, location1, location2):
        coords1, coords2 = self.get_coordinates(location1), self.get_coordinates(location2)
        if coords1 is None or coords2 is None:
            return None
        lat1, lon1 = map(radians, coords1)
        lat2, lon2 = map(radians, coords2)
        a = sin((lat2-lat1)/2)**2 + cos(lat1)*cos(lat2)*sin((lon2-lon1)/2)**2
        a = min(1, max(0, a))
        return 6371.0 * 2 * atan2(sqrt(a), sqrt(1-a))
