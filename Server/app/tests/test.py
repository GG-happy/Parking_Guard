import requests, base64, os

# # detect1 test
# url = "http://localhost:8000/api/parking-guard/detect1"
# files = {
#     "image": open("images/KakaoTalk_20250616_231203074_01.jpg", "rb")
# }
# data = {
#     "latitude": 37.123,
#     "longitude": 127.456
# }
# response = requests.post(url, files=files, data=data)

# response_dict = response.json()
# yolo_result_image_base64 = response_dict['yolo_result_image']

# image_data = base64.b64decode(yolo_result_image_base64)

# save_dir = "backup"
# os.makedirs(save_dir, exist_ok=True)
# save_path = os.path.join(save_dir, "result_image1.jpg")

# with open(save_path, "wb") as f:
#     f.write(image_data)

# print(response_dict['car_number'])
# print(response_dict['violation_type'])

# detect2 test
url = "http://localhost:8000/api/parking-guard/detect2"
files = {
    "image": open("images/KakaoTalk_20250616_231203074.jpg", "rb")
}
data = {
    "latitude1": 37.123,
    "longitude1": 127.456,
    "latitude2": 37.123027,
    "longitude2": 127.456000,
    "prev_car_number": "215누7633"
}
response = requests.post(url, files=files, data=data)

response_dict = response.json()
yolo_result_image_base64 = response_dict['yolo_result_image']

image_data = base64.b64decode(yolo_result_image_base64)

save_dir = "backup"
os.makedirs(save_dir, exist_ok=True)
save_path = os.path.join(save_dir, "result_image2.jpg")

with open(save_path, "wb") as f:
    f.write(image_data)

print(response_dict['car_number_match'])
print(response_dict['within_5m'])