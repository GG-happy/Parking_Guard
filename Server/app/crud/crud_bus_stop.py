from sqlalchemy.orm import Session
from sqlalchemy import delete
from pathlib import Path
from app.models.bus_stop import BusStop
import pandas as pd

def load_csv_to_db_bus(db: Session) -> None:
    BASE_DIR = Path(__file__).resolve().parent.parent
    DATA_PATH = BASE_DIR / "data" / "전국정거장.csv"

    df = pd.read_csv(DATA_PATH, encoding="utf-8-sig")[["위도", "경도"]].dropna()
    df.columns = ["latitude", "longitude"]

    df["latitude"] = df["latitude"].astype(float)
    df["longitude"] = df["longitude"].astype(float)

    db.execute(delete(BusStop))
    db.flush()

    db.bulk_insert_mappings(
        BusStop,
        df.to_dict(orient="records"),
    )
    db.commit()

def get_all_zones(db: Session) -> list[BusStop]:
    return db.query(BusStop).all()
