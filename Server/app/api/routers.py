from fastapi import APIRouter
from app.api.endpoints import parking_guard

api_router = APIRouter()
api_router.include_router(parking_guard.router, prefix="/parking-guard", tags=["parking-guard"])
