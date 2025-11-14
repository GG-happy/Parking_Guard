from fastapi import APIRouter, Form, UploadFile, File, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.utils.yolo_utils import detect_object1, detect_object2
from app.crud.crud_school_zone import load_csv_to_db
from app.crud.crud_bus_stop import load_csv_to_db_bus
from app.crud.crud_violation_parking import insert_violation_parking
from datetime import datetime

router = APIRouter()

@router.post("/load-school-zone")
async def load_school_zone(db: Session = Depends(get_db)):
    load_csv_to_db(db)

@router.post("/load-bus-stop")
async def load_bus_stop(db: Session = Depends(get_db)):
    load_csv_to_db_bus(db)

# 첫 번째 촬영 클라이언트 측에서 서버로 위도, 경도, 이미지를 받아옴
@router.post("/detect1")
async def detect1(
    db: Session = Depends(get_db),
    latitude: float = Form(...),
    longitude: float = Form(...),
    image: UploadFile = File(...)
):
    image_bytes = await image.read()
    return detect_object1(db, latitude, longitude, image_bytes)

# 두 번째 촬영(첫 번째 촬영에서의 위도 경도 차 번호, 두 번째 촬영에서의 위도 경도 이미지)
@router.post("/detect2")
async def detect2(
    latitude1: float = Form(...),
    longitude1: float = Form(...),
    latitude2: float = Form(...),
    longitude2: float = Form(...),
    prev_car_number: str = Form(...),
    image: UploadFile = File(...)
):
    image_bytes = await image.read()
    return detect_object2(latitude1, longitude1, latitude2, longitude2, prev_car_number, image_bytes)

# 제출 버튼 누르면 호출될 요청 메소드(차 번호, 촬영 일자, 위도, 경도, 유형)
@router.post("/report")
async def report(
    car_number: str = Form(...), 
    captured_at: datetime = Form(...), 
    latitude: float = Form(...), 
    longitude: float = Form(...), 
    violation_type: int = Form(...),
    db: Session = Depends(get_db)
):
    if not (-1 <= violation_type <= 7):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="violation_type must be between -1 and 7"
        )
    
    return insert_violation_parking(db=db, car_number=car_number, captured_at=captured_at, latitude=latitude, longitude=longitude, violation_type=violation_type)