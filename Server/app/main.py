from fastapi import FastAPI
from contextlib import asynccontextmanager
from app.models import violation_parking, school_zone, bus_stop
from app.db.session import engine
from app.api.routers import api_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    violation_parking.Base.metadata.create_all(bind=engine)
    school_zone.Base.metadata.create_all(bind=engine)
    bus_stop.Base.metadata.create_all(bind=engine)
    yield

app = FastAPI(
    title="Parking Guard", 
    description="API documentation", 
    version="1.0.0",
    lifespan=lifespan
)

app.include_router(api_router, prefix="/api")
