from math import radians, cos, sin, asin, sqrt
from typing import Sequence, Tuple
from app.models.school_zone import SchoolZone
from app.models.bus_stop import BusStop

def haversine(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    R = 6371000
    lat1, lon1, lat2, lon2 = map(radians, [lat1, lon1, lat2, lon2])
    dlat, dlon = lat2 - lat1, lon2 - lon1
    a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
    c = 2 * asin(sqrt(a))
    return R * c

def is_near_school_zone(
    user_gps: Tuple[float, float],
    school_zones: Sequence[SchoolZone],
    threshold: float = 300
) -> bool:
    user_lat, user_lon = user_gps
    for zone in school_zones:
        zone_lat, zone_lon = zone.latitude, zone.longitude
        distance = haversine(user_lat, user_lon, zone_lat, zone_lon)
        if distance <= threshold:
            return True
    return False

def is_near_bus_stop(
        user_gps: Tuple[float, float],
        bus_stop: Sequence[BusStop],
        threshold: float = 10
) -> bool:
    user_lat, user_lon = user_gps
    for bs in bus_stop:
        lat, lon = bs.latitude, bs.longitude
        if haversine(user_lat, user_lon, lat, lon) <= threshold:
            return True
    return False
