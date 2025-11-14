from sqlalchemy.orm import Session
from datetime import datetime
from app.models.violation_parking import ViolationParking
from app.utils.geo_utils import haversine

def insert_violation_parking(
    db: Session,
    car_number: str,
    captured_at: datetime,
    latitude: float,
    longitude: float,
    violation_type: int,
) -> bool:
    violation_parking = ViolationParking(
        car_number=car_number,
        captured_at=captured_at,
        latitude=latitude,
        longitude=longitude,
        violation_type=violation_type
    )

    if select_violation_parking(db, car_number, captured_at, latitude, longitude):
        return False
            
    db.add(violation_parking)
    db.commit()

    return True

def get_all_violation_parking(db: Session) -> list[ViolationParking]:
    return db.query(ViolationParking).all()

def select_violation_parking(
    db: Session,
    car_number: str,
    captured_at: datetime,
    latitude: float,
    longitude: float
) -> bool:
    
    # 이미 신고된 사례인지
    for vp in db.query(ViolationParking).all():
        car_plate, day, lat, lon = vp.car_number, vp.captured_at, vp.latitude, vp.longitude
        if car_number == car_plate and day.date() == captured_at.date():
            if haversine(lat, lon, latitude, longitude) <= 20:
                return True
    return False