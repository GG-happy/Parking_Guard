from sqlalchemy import Column, Integer, String, DateTime, Double, CheckConstraint
from app.db.base import Base

class ViolationParking(Base):
    __tablename__ = "violation_parking"

    id = Column(Integer, primary_key=True, index=True)
    car_number = Column(String(20), nullable=False)
    captured_at = Column(DateTime, nullable=False)
    latitude = Column(Double, nullable=False)
    longitude = Column(Double, nullable=False)
    violation_type = Column(Integer, nullable=False)

    __table_args__ = (
        CheckConstraint('violation_type BETWEEN 0 AND 7', name='check_violation_type_range'),
    )
