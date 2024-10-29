#!/usr/bin/env python3
#type: ignore
 
import datetime
import json
import os
import sys
from pathlib import Path

import gpxpy
from fitparse import FitFile


def change_extension(file_path: str, new_extension: str) -> str:
    # Create a Path object
    path = Path(file_path)
    # Change the extension and return the new path as a string
    return str(path.with_suffix(f'.{new_extension}'))

def convert_timestamp(dt: datetime) -> int :
    return int(dt.timestamp() * 1000)

class Ride:

    def __init__(self):
        self.locations = []
        self.pause = False
        
    def parse_trackpoint(self, trackpoint):
        if self.pause:
            return
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

    def write(self, filename): 
        prolog_str = ""
        with open(filename, "w+") as out:
            for location in self.locations:
                out.write(prolog_str)
                json.dump(location, out)
                out.write("\n")
                prolog_str = ","
        
def process(file: str) -> Ride :
    target_file = change_extension(file, 'json')
    if os.path.isfile(target_file):
        print(f"{file}: {target_file} exists, skipped.")
        return
    ride = Ride()
    with open(file, 'r') as gpx_file:
        gpx = gpxpy.parse(gpx_file)

    # Accessing tracks
    for track in gpx.tracks:
        for segment in track.segments:
            for point in segment.points:
                ride.parse_trackpoint(point)
    ride.write(target_file)
    
if __name__ == "__main__":
    for file in sys.argv[1:]:
        ride = process(file)
