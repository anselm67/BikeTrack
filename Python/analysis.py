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

def extract_chunk(data):
    coords = [(o["latitude"], o["longitude"]) for o in data]
    return [geopy.distance.geodesic(c1, c0).km for (c1, c0) in zip(coords[1:], coords[:-1])]

def extract_distance(data):
    chunks = extract_chunk(data)
    total = chunks[0]
    dists = [total]
    for d in chunks[1:]:
        total += d 
        dists.append(total)
    return dists

def extract_grade(data, min_distance=5):
    # Distance in meters, like altitude
    chunks = [d * 1000.0 for d in extract_chunk(data)]  
    (t, h) = extract(data, "altitude")
    grade = []
    pending_distance = -1.0
    start_altitude = 0.0
    last_grade = 0.0
    for (h1, h0, d) in zip(h[1:], h[:-1], chunks):
        if pending_distance >= 0.0:
            pending_distance += d
            if pending_distance > min_distance:
                grade.append(100.0 * (h1 - start_altitude) / pending_distance)
                pending_distance = -1.0
            else:
                grade.append(last_grade)
        else:
            if ( d < min_distance ):
                pending_distance = d
                start_altitude = h0
                grade.append(last_grade)
            else: 
                last_grade = 100.0 * (h1 - h0) / d
                grade.append(last_grade)
    #grade = [min(25.0, max(g, -25.0)) for g in grade]
    return t[1:], grade

def print_values(title, xValues, yValues = None, num_values = 10):
    print(title)
    for i in range(0, num_values): 
        if ( yValues == None):
            print(f"{xValues[i]}")
        else:
            print(f"{xValues[i]} {yValues[i]}")

def distance(data):
    (t, _) = extract(data, "speed")
    d = extract_distance(data)
    fig, ax = plt.subplots()
    ax.plot(t[:-1], d, linewidth=2.0)
    plt.xlabel('Time (min)')
    plt.ylabel('Distance (km)')
    plt.show()

def altitude_by_distance(data):
    (_, y) = extract(data, "altitude")
    d = extract_distance(data)
    print_values("Distance", d)
    fig, ax = plt.subplots()
    ax.plot(d, y[1:], linewidth=2.0, marker=".", label="Real time")
    (xValues, yValues) = average3(d, y[1:])
    print_values("Average3", xValues, yValues)
    ax.plot(xValues, yValues, linewidth=2.0, marker="x", label="average3")
    ax.legend()
    plt.xlabel('Dist (m)')
    plt.ylabel('Altitude (m)')
    plt.show()

def grade_by_distance(data):
    (_, grade) = extract_grade(data)
    d = extract_distance(data)
    fig, ax = plt.subplots()
    ax.plot(d, grade, linewidth=2.0, marker=".", label="Real time")
    ax.legend()
    plt.xlabel('Dist (m)')
    plt.ylabel('Altitude (m)')
    plt.show()


def altitude_by_time(ax, data):
    (t, y) = extract(data, "altitude")
    ax.plot(t, y, linewidth=2.0, marker=".", label="Real time")
    (xValues, yValues) = average3(t, y)
    ax.plot(xValues, yValues, linewidth=2.0, marker="o", label="average3")
    ax.legend()

def integrate(steps, values):
    total = 0
    result = []
    for (step, value) in zip(steps, values):
        total += (step * value / 100.0)
        result.append(total)
    return result

def altitude_vs_grade(data):
    fig, ax = plt.subplots()
    d = [d * 1000.0 for d in extract_chunk(data)]
    (t, g) = extract_grade(data, min_distance=5)
    ax.plot(t, integrate(d, g), linewidth=2.0, marker="x", label="5m")
    (t, g) = extract_grade(data, min_distance=10)
    ax.plot(t, integrate(d, g), linewidth=2.0, marker="o", label="10m")
    ax.legend()
    plt.xlabel('Time (min)')
    plt.ylabel('Altitude (m)')
    plt.show()


def grade_by_time(ax, data): 
    (t, g) = extract_grade(data, min_distance=5)
    ax.plot(t, g, linewidth=2.0, marker="x", label="5m")
    (t, g) = extract_grade(data, min_distance=10)
    ax.plot(t, g, linewidth=2.0, marker="o", label="10m")
    ax.plot(t, [25.0] * len(t))
    ax.plot(t, [-25.0] * len(t))
    ax.legend()

def data_average3(data, label):
    (_, values) = extract(data, label)
    (_, values) = average3([0] * len(values), values)
    average3_data = []
    for (sample, value) in zip(data[:-2], values):
        average3_sample = sample.copy()
        average3_sample[label] = value
        average3_data.append(average3_sample)
    return average3_data

def average2(xValues, yValues): 
    return xValues[1:], [(x + y) / 2.0 for (x, y) in zip(yValues[1:], yValues[:-1])]

def average3(xValues, yValues): 
    return xValues[2:], [(x + y + z) / 3.0 for (x, y, z) in zip(yValues[2:], yValues[1:-1], yValues[:-2])]

def altitude(data):
    fig, (ax1, ax2) = plt.subplots(2)
    grade_by_time(ax1, data)
    grade_by_time(ax2, data_average3(data, "altitude"))
    plt.show()

#data = load("recording-2024-05-14-10-14-37.json")
#data = load("recording-2024-05-15-18-54-46.json")
#data = load("2024-05-18-descente.json")

data = load('2024-05-19-montee.json')

#altitude_by_time(data)
#altitude_by_distance(data)
grade_by_distance(data)
#distance(data)
#altitude(data)
#altitude_vs_grade(data)

