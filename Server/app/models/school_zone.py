from sqlalchemy import Column, Integer, Double
from app.db.base import Base

class SchoolZone(Base):
    __tablename__ = "school_zone"

    id = Column(Integer, primary_key=True, index=True)
    latitude = Column(Double, nullable=False)
    longitude = Column(Double, nullable=False)
