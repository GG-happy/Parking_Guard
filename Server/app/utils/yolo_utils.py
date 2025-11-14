from sqlalchemy.orm import Session
from ultralytics import YOLO
from PIL import Image
from app.schemas.response import DetectObjectResponse1, DetectObjectResponse2
from app.utils.geo_utils import is_near_school_zone, haversine, is_near_bus_stop
from app.crud.crud_school_zone import get_all_zones
import numpy as np
import io, time, uuid, json, requests, cv2, base64
from app.core.config import OCR_API_URL, OCR_SECRET_KEY
import re
from app.crud.crud_violation_parking import select_violation_parking
from datetime import datetime

model = YOLO("yolo/best.pt")

def detect_object1(
    db: Session,
    latitude: float,
    longitude: float,
    image_bytes: bytes
) -> DetectObjectResponse1:
    img = Image.open(io.BytesIO(image_bytes))
    result = model(img)[0]

    masks = result.masks.data.cpu().numpy()
    classes = result.boxes.cls.cpu().numpy()
    class_names = model.names

    car_mask = None
    detected_classes = set()
    masks_by_class = {}

    for i, cls_id in enumerate(classes):
        cls_name = class_names[int(cls_id)]
        detected_classes.add(cls_name)
        masks_by_class[cls_name] = masks[i]
        if cls_name == 'car':
            car_mask = masks[i]

    ILLEGAL_ZONE_CLASSES = {
        'firehydrant', 'solid_yellow_lane', 'double_yellow_lane', 'dotted_yellow_lane'
    }
    illegal_zone_detected = 'car' in detected_classes and any(
        zone in detected_classes for zone in ILLEGAL_ZONE_CLASSES
    )

    def mask_overlap_pixels(mask1, mask2, pixel_threshold=300):
        overlap = np.logical_and(mask1, mask2)
        return overlap.sum() > pixel_threshold

    def bottom_y_overlap(car_mask, target_mask, margin=15):
        y_coords = np.where(car_mask)[0]
        if len(y_coords) == 0:
            return False
        y_max = np.max(y_coords)
        for y in range(y_max, max(y_max - margin, 0), -1):
            if np.any(np.logical_and(car_mask[y], target_mask[y])):
                return True
        return False

    on_crosswalk = False
    if 'crosswalk' in masks_by_class:
        crosswalk_mask = masks_by_class['crosswalk']
        on_crosswalk = (
            mask_overlap_pixels(car_mask, crosswalk_mask, pixel_threshold=100) or
            bottom_y_overlap(car_mask, crosswalk_mask, margin=20)
        )

    on_sidewalk = False
    if 'sidewalk' in masks_by_class:
        sidewalk_mask = masks_by_class['sidewalk']
        on_sidewalk = (
            mask_overlap_pixels(car_mask, sidewalk_mask, pixel_threshold=100) or
            bottom_y_overlap(car_mask, sidewalk_mask, margin=20)
        )

    is_school_zone = is_near_school_zone([latitude, longitude], get_all_zones(db))
    is_bus_stop = is_near_bus_stop([latitude, longitude], get_all_zones(db))

    if car_mask is None:
        violation_type = [-1] # 자동차 객체 감지 실패
    else:
        if is_school_zone:
            violation_type = [1] # 어린이 보호구역
        elif 'firehydrant' in detected_classes:
            violation_type = [2] # 소화전
        elif is_bus_stop:
            violation_type = [6] # 버스 정류장
        elif on_crosswalk:
            violation_type = [3] # 횡단보도
        elif on_sidewalk:
            violation_type = [4] # 인도
        elif illegal_zone_detected:
            violation_type = [5, 7] # 교차로 모퉁이 / 일반
        else:
            violation_type = [0] # 정상 주차
    print(f'유형: {violation_type}')
    license_plate_crop = None
    image = result.orig_img  # ← YOLO에서 받은 원본 이미지

    # license_plate 클래스 찾기
    for i, cls_id in enumerate(classes):
        cls_name = class_names[int(cls_id)]
        if cls_name == 'license_plate':
            box = result.boxes.xyxy[i].cpu().numpy().astype(int)
            x1, y1, x2, y2 = box
            padding = 50

            # 유효한 이미지 범위로 클램핑
            h, w = image.shape[:2]
            x1, y1 = max(0, x1 - padding), max(0, y1 - padding)
            x2, y2 = min(w, x2 + padding), min(h, y2 + padding)

            # 번호판 크롭
            license_plate_crop = image[y1:y2, x1:x2]
            break

    car_number = None

    # OCR 수행
    if license_plate_crop is not None:
        _, buffer = cv2.imencode('.jpg', license_plate_crop)
        jpg_bytes = buffer.tobytes()

        # Clova OCR 설정
        API_URL = OCR_API_URL
        SECRET_KEY = OCR_SECRET_KEY

        headers = {
            "X-OCR-SECRET": SECRET_KEY,
        }

        payload = {
            "version": "V1",
            "requestId": str(uuid.uuid4()),
            "timestamp": int(time.time() * 1000),
            "images": [
                {
                    "format": "jpg",
                    "name": "license_plate"
                }
            ]
        }

        files = {
            'message': (None, json.dumps(payload), 'application/json'),
            'file': ('license_plate.jpg', jpg_bytes, 'image/jpeg')
        }

        try:
            response = requests.post(API_URL, headers=headers, files=files)
            result_json = response.json()
            

            if 'images' in result_json and len(result_json['images']) > 0:
                fields = result_json['images'][0].get('fields', [])
                if fields:
                    car_number = re.sub(r'[^0-9가-힣]', '', ''.join([field['inferText'] for field in fields]))
                    print("추출된 번호판:", car_number)
                else:
                    print("번호판 텍스트를 인식하지 못했습니다.")
            else:
                print("OCR 응답이 비정상입니다:", result_json)
        except Exception as e:
            print("OCR 요청 중 오류 발생:", str(e))
    else:
        print("license_plate 객체가 감지되지 않아 OCR을 수행하지 않습니다.")

    result_image = result.plot()  # 대부분의 YOLOv8 파생 패키지에서 지원
    # result_image: numpy.ndarray(BGR) 형식

    # 이미지를 base64로 변환
    _, buffer = cv2.imencode('.jpg', result_image)
    yolo_result_image_base64 = base64.b64encode(buffer).decode('utf-8')

    response_data = DetectObjectResponse1(
        car_number=car_number,
        violation_type=violation_type,
        yolo_result_image=yolo_result_image_base64,
        is_violation = select_violation_parking(db, car_number, datetime.now(), latitude, longitude))
    return response_data

def detect_object2(
    latitude1: float, 
    longitude1: float, 
    latitude2: float, 
    longitude2: float,
    prev_car_number: str,
    image_bytes: bytes
) -> DetectObjectResponse2:
    img = Image.open(io.BytesIO(image_bytes))
    result = model(img)[0]

    masks = result.masks.data.cpu().numpy()
    classes = result.boxes.cls.cpu().numpy()
    class_names = model.names

    car_mask = None
    detected_classes = set()
    masks_by_class = {}

    for i, cls_id in enumerate(classes):
        cls_name = class_names[int(cls_id)]
        detected_classes.add(cls_name)
        masks_by_class[cls_name] = masks[i]
        if cls_name == 'car':
            car_mask = masks[i]
    
    license_plate_crop = None
    image = result.orig_img  # ← YOLO에서 받은 원본 이미지

    # license_plate 클래스 찾기
    for i, cls_id in enumerate(classes):
        cls_name = class_names[int(cls_id)]
        if cls_name == 'license_plate':
            box = result.boxes.xyxy[i].cpu().numpy().astype(int)
            x1, y1, x2, y2 = box

            # 유효한 이미지 범위로 클램핑
            h, w = image.shape[:2]
            x1, y1 = max(0, x1), max(0, y1)
            x2, y2 = min(w, x2), min(h, y2)

            # 번호판 크롭
            license_plate_crop = image[y1:y2, x1:x2]
            break

    car_number = None

    # OCR 수행
    if license_plate_crop is not None:
        _, buffer = cv2.imencode('.jpg', license_plate_crop)
        jpg_bytes = buffer.tobytes()

        # Clova OCR 설정
        API_URL = OCR_API_URL
        SECRET_KEY = OCR_SECRET_KEY

        headers = {
            "X-OCR-SECRET": SECRET_KEY,
        }

        payload = {
            "version": "V1",
            "requestId": str(uuid.uuid4()),
            "timestamp": int(time.time() * 1000),
            "images": [
                {
                    "format": "jpg",
                    "name": "license_plate"
                }
            ]
        }

        files = {
            'message': (None, json.dumps(payload), 'application/json'),
            'file': ('license_plate.jpg', jpg_bytes, 'image/jpeg')
        }

        try:
            response = requests.post(API_URL, headers=headers, files=files)
            result_json = response.json()
            

            if 'images' in result_json and len(result_json['images']) > 0:
                fields = result_json['images'][0].get('fields', [])
                if fields:
                    car_number = re.sub(r'[^0-9가-힣]', '', ''.join([field['inferText'] for field in fields]))
                    print("추출된 번호판:", car_number)
                else:
                    print("번호판 텍스트를 인식하지 못했습니다.")
            else:
                print("OCR 응답이 비정상입니다:", result_json)
        except Exception as e:
            print("OCR 요청 중 오류 발생:", str(e))
    else:
        print("license_plate 객체가 감지되지 않아 OCR을 수행하지 않습니다.")
    
    result_image = result.plot()  # 대부분의 YOLOv8 파생 패키지에서 지원
    # result_image: numpy.ndarray(BGR) 형식

    # 이미지를 base64로 변환
    _, buffer = cv2.imencode('.jpg', result_image)
    yolo_result_image_base64 = base64.b64encode(buffer).decode('utf-8')

    car_number_match = False
    if car_number is not None and prev_car_number is not None:
        # 번호판을 문자열로 비교 (공백, 하이픈 등 클린업 필요시 추가)
        car_number_match = car_number.strip() == str(prev_car_number).strip()

    distance = haversine(latitude1, longitude1, latitude2, longitude2)
    within_5m = distance <= 5

    response_data = DetectObjectResponse2(
        car_number_match=car_number_match,
        within_5m=within_5m,
        yolo_result_image=yolo_result_image_base64
    )
    return response_data
