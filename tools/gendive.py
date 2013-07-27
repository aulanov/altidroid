#!/usr/bin/python

import random

timestamp = 1000
t_step = 200
steps_per_s = 1000/t_step
noise_sigma = 0.5 # meters

def write(altitude):
    global timestamp, t_step
    print "%d %d %f %f" % (timestamp,
                           int(random.normalvariate(altitude,
                                                    noise_sigma) * 1000),
                           noise_sigma, 0)
    timestamp = timestamp + t_step

ground = 100

for i in xrange(0, 100):
    write(ground)

exit_altitude = 13000/3.28
climb_rate = 1000/3.28/60 # 1000 ft/min
step = climb_rate / steps_per_s
climb_steps = int((exit_altitude - ground) / step)

for i in xrange(0, 100):
    write(ground + i*step)

for i in xrange(climb_steps-100, climb_steps):
    write(ground + i*step)

for i in xrange(0, 30*steps_per_s):
    write(exit_altitude)

# Freefall
freefall_speed = 50 / steps_per_s
open_alt = 1500 / 3.28
freefall_time = (exit_altitude - open_alt) / freefall_speed

for i in xrange(0, int(freefall_time)):
    write(exit_altitude - freefall_speed * i)

# Canopy ride
canopy_speed = 8.0 / steps_per_s
alt = open_alt
while alt > ground:
    write(alt)
    alt = alt - canopy_speed

# Ground
for i in xrange(0, 100):
    write(ground)
