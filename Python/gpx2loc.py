#!/usr/bin/env python3
#type: ignore
 
import argparse
import datetime
import json
from pathlib import Path

import gpxpy
from tqdm import tqdm

parser = argparse.ArgumentParser(
    prog='gp2loc.py',
    description="Converts GPX files to the Location app JSON format."
)
parser.add_argument('--dest', type=str, default='recordings',
                    help='Destination direcory for .json files')
parser.add_argument('files', nargs='+', help='Files to be converted')

args = parser.parse_args()

LAST_SAMPLE = {
    "seqno": 0,
      "location": {
        "time": 0,
        "latitude": 0,
        "longitude": 0,
        "altitude": 0,
        "accuracy": 0,
        "speed": 0,
        "bearing": 0
      },
      "elapsedTime": 0,
      "distance": 0,
      "totalDistance": 0,
      "avgSpeed": 0,
      "maxSpeed": 0,
      "altitude": 0,
      "avgAltitude": 0,
      "verticalDistance": 0,
      "climb": 0,
      "descent": 0,
      "grade": -0
}

def convert_timestamp(dt: datetime) -> int :
    return int(dt.timestamp() * 1000)

class Ride:

    def __init__(self):
        self.locations = []
        self.start_time = None
        self.time = 0
        self.pause = False
        self.title = None
                
    def parse_trackpoint(self, trackpoint):
        if self.pause:
            return
        if self.start_time is None:
            self.start_time = trackpoint.time
        location = {
            "time": convert_timestamp(trackpoint.time),
            "latitude": trackpoint.latitude,
            "longitude": trackpoint.longitude,
            "altitude": trackpoint.elevation,
            "accuracy": 0,
            "speed": 0,
            "bearing": 0,
        }
        self.locations.append(location)

        
    def finish(self):
        timestr = self.start_time.strftime("%Y-%m-%d-%H-%M-%S")
        self.id = f"recording-{timestr}.json"
        self.time = convert_timestamp(self.start_time)
        return self
        
    def write(self, path): 
        prolog_str = ""
        with open(path, 'w+') as out:
            for location in self.locations:
                out.write(prolog_str)
                json.dump(location, out)
                out.write("\n")
                prolog_str = ","
                        
def process(file: str) -> Ride :
    ride = Ride()
    with open(file, 'r') as gpx_file:
        gpx = gpxpy.parse(gpx_file)

    # Accessing tracks
    for track in gpx.tracks:
        if track.name is not None:
            ride.title = track.name
        for segment in track.segments:
            for point in segment.points:
                ride.parse_trackpoint(point)
    return ride.finish()

if __name__ == "__main__":
    catalog = { }
    Path(args.dest).mkdir(parents=True, exist_ok=True)
    for file in tqdm(args.files, desc="Processing files..."):
        ride = process(file)
        ride.write(Path(args.dest) / ride.id)

        if ride.title is not None:
            catalog[ride.id] = ride

        with open(Path(args.dest) / 'catalog.json', 'w+') as out:
            json.dump({
                "rides": [{
                    "id": id,
                    "title": ride.title,
                    "time": ride.time,
                    "description": "Imported from Strava.",
                    "tags": [],
                    "lastSample": LAST_SAMPLE
                } for id, ride in sorted(catalog.items(), key=lambda it: it[1].start_time)],
                "weeklyStats": [],
                "monthlyStats": [],
                "annualStats": []
            }, out, indent=2)