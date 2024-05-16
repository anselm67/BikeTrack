import typing
import matplotlib.pyplot as plt
import json
import geopy.distance

def load(recordFile: str):
    with open(f"data/{recordFile}") as f:
        text = "[" + f.read() + "]"
    return json.loads(text)

def extract(points, label):
    return [(o["time"] - points[0]["time"])/ (60 * 1000) for o in points], [o[label] for o in points]

def speed(data):
    (t, y) = extract(data, "speed")
    # Convert speed from m/s to km/h - / 1000 * 3600 = * 3.6
    y = [3.6 * speed for speed in y]

    fig, ax = plt.subplots()
    ax.plot(t, y, linewidth=2.0)
    plt.xlabel('Time (min)')
    plt.ylabel('Speed (km/h)')
    plt.show()

def extract_distance(data):
    coords = [(o["latitude"], o["longitude"]) for o in data]
    chunks = [geopy.distance.geodesic(c1, c0).km for (c1, c0) in zip(coords[1:], coords[:-1])]
    total = chunks[0]
    dists = [total]
    for d in chunks[1:]:
        total += d 
        dists.append(total)
    return dists

def distance(data):
    (t, _) = extract(data, "speed")
    d = extract_distance(data)
    fig, ax = plt.subplots()
    ax.plot(t[:-1], d, linewidth=2.0)
    plt.xlabel('Time (min)')
    plt.ylabel('Distance (km)')
    plt.show()

def altitude(data):
    (t, y) = extract(data, "altitude")
    d = extract_distance(data)
    fig, ax = plt.subplots()
    ax.plot(d, y[:-1], linewidth=2.0)
    plt.xlabel('Dist (m)')
    plt.ylabel('Altitude (m)')
    plt.show()

data = load("recording-2024-05-14-10-14-37.json")
#data = load("recording-2024-05-15-18-54-46.json")

altitude(data)
