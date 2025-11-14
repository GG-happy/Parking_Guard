from sqlalchemy import Column, Integer, Double
from app.db.base import Base

class BusStop(Base):
    __tablename__ = "bus_stop"

    id = Column(Integer, primary_key=True, index=True)
    latitude = Column(Double, nullable=False)
    longitude = Column(Double, nullable=False)
