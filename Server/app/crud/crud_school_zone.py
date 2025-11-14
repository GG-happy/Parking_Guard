from sqlalchemy.orm import Session
from sqlalchemy import delete
from pathlib import Path
from app.models.school_zone import SchoolZone
import pandas as pd

def load_csv_to_db(db: Session) -> None:
    BASE_DIR = Path(__file__).resolve().parent.parent
    DATA_PATH = BASE_DIR / "data" / "전국어린이보호구역표준데이터.csv"

    df = pd.read_csv(DATA_PATH, encoding="cp949")[["위도", "경도"]].dropna()
    df.columns = ["latitude", "longitude"]

    db.execute(delete(SchoolZone))
    db.flush()

    db.bulk_insert_mappings(
        SchoolZone,
        df.to_dict(orient="records"),
    )
    db.commit()

def get_all_zones(db: Session) -> list[SchoolZone]:
    return db.query(SchoolZone).all()
