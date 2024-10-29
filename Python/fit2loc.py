#!/usr/bin/env python3
#type: ignore
 
import datetime
import json
import os
import sys
from pathlib import Path

from fitparse import FitFile


def change_extension(file_path: str, new_extension: str) -> str:
    # Create a Path object
    path = Path(file_path)
    # Change the extension and return the new path as a string
    return str(path.with_suffix(f'.{new_extension}'))

def convert_timestamp(dt: datetime) -> int :
    return int(dt.timestamp() * 1000)

def convert_gps(ll: int) -> float:
    return ll * (180 / 2 ** 31)

def convert_speed(s) -> float :
    return s if s is not None else 0.0

class Ride:

    def __init__(self):
        self.locations = []
        self.pause = True
        
    def parse_session(self, session):
        self.start_time = session.get_value('start_time')        
        
    def parse_record(self, record):
        if self.pause:
            return
        if record.get_value('position_lat') is None:
            return
        location = {
            "time": convert_timestamp(record.get_value('timestamp')),
            "latitude": convert_gps(record.get_value('position_lat')),
            "longitude": convert_gps(record.get_value('position_long')),
            "altitude": record.get_value('enhanced_altitude'),
            "accuracy": record.get_value('gps_accuracy'),
            "speed": convert_speed(record.get_value('enhanced_speed')),
            "bearing": 0,
        }
        self.locations.append(location)

    def parse_event(self, event):
        eventType = event.get_value("event_type")
        if eventType == "start":
            self.pause = False
        elif eventType == "stop":
            self.pause = True
            
    def write(self, filename): 
        prolog_str = ""
        with open(filename, "w+") as out:
            for location in self.locations:
                out.write(prolog_str)
                json.dump(location, out)
                out.write("\n")
                prolog_str = ","
        
def all(which: str = None):
    # Iterate over all messages in the FIT file
    for record in fitfile.get_messages(which):
        # Print the record name and its data
        print(record.name)
        for field in record.fields:
            print(f"  {field.name}: {field.value}")
            
def process(file: str) -> Ride :
    target_file = change_extension(file, 'json')
    if os.path.isfile(target_file):
        print(f"{file}: {target_file} exists, skipped.")
        return
    fitfile = FitFile(file)
    ride = Ride()
    for record in fitfile.get_messages():
        if record.name == "session":
            ride.parse_session(record)
        elif record.name == "record":
            ride.parse_record(record)
        elif record.name == "event":
            ride.parse_event(record)
    ride.write(target_file)
    
    
if __name__ == "__main__":
    for file in sys.argv[1:]:
        ride = process(file)
