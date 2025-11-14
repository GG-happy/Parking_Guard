from pydantic import BaseModel

# 첫 번째 촬영 후 클라이언트로 반환하는 데이터 모델
class DetectObjectResponse1(BaseModel):
    car_number: str | None
    violation_type: list[int]
    yolo_result_image: str
    is_violation : bool

# 두 번째 촬영 후 클라이언트로 반환하는 데이터 모델
class DetectObjectResponse2(BaseModel):
    car_number_match: bool
    within_5m: bool
    yolo_result_image: str
