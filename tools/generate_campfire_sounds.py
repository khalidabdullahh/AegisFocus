#!/usr/bin/env python3
"""
Generates the Campfire ambient loops (seamless, mono, 22.05 kHz, OGG Vorbis).

Every sound is synthesized from scratch, so there is no third-party copyright.
Usage:  python3 tools/generate_campfire_sounds.py
Needs:  numpy, scipy, ffmpeg (with libvorbis)
Output: app/src/main/res/raw/*.ogg
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 22050
DUR = 40            # seconds per loop
FADE = 3 * SR       # crossfade used to make the loop seamless
N = DUR * SR
rng = np.random.default_rng(7)
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")


# ----------------------------------------------------------------- helpers
def white(n):
    return rng.standard_normal(n)


def lp(x, fc, order=4):
    return signal.sosfilt(signal.butter(order, fc, "low", fs=SR, output="sos"), x)


def hp(x, fc, order=4):
    return signal.sosfilt(signal.butter(order, fc, "high", fs=SR, output="sos"), x)


def bp(x, lo, hi, order=3):
    return signal.sosfilt(signal.butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def brown(n):
    x = np.cumsum(white(n))
    return hp(x, 20, 2)


def pink(n):
    spec = np.fft.rfft(white(n))
    f = np.fft.rfftfreq(n)
    f[0] = f[1]
    return np.fft.irfft(spec / np.sqrt(f), n)


def slow_env(n, hz, depth=1.0, floor=0.0):
    """Smooth random envelope 0..1 varying at roughly `hz`."""
    e = lp(white(n), hz, 2)
    e = (e - e.min()) / (e.max() - e.min() + 1e-9)
    return floor + (1 - floor) * (e ** depth)


def make_loop(x):
    """x has N+FADE samples. Equal-power crossfade of the tail into the head."""
    out = x[:N].copy()
    t = np.linspace(0, np.pi / 2, FADE)
    out[:FADE] = x[:FADE] * np.sin(t) + x[N:N + FADE] * np.cos(t)
    return out


def finish(x, rms=None, peak=None):
    x = x - np.mean(x)
    if rms:
        x = x / (np.sqrt(np.mean(x ** 2)) + 1e-9) * rms
        x = np.tanh(x * 1.2) / np.tanh(1.2)       # soft limiter
    if peak:
        x = x / (np.max(np.abs(x)) + 1e-9) * peak
    return np.clip(x, -1, 1)


def reverb(x, secs=1.4, wet=0.35):
    t = np.arange(int(secs * SR)) / SR
    ir = lp(white(len(t)), 2500, 2) * np.exp(-t * 3.2)
    ir[0] = 0
    wetsig = signal.fftconvolve(x, ir)[: len(x)]
    wetsig = wetsig / (np.max(np.abs(wetsig)) + 1e-9) * np.max(np.abs(x))
    return x * (1 - wet) + wetsig * wet


def sparse_hits(n, rate, amp_sigma=0.6):
    """Random impulse train: Poisson-ish, log-normal amplitudes."""
    imp = np.zeros(n)
    k = int(rate * n / SR)
    idx = rng.integers(0, n, k)
    imp[idx] = rng.lognormal(0, amp_sigma, k) * rng.choice([-1, 1], k)
    return imp


# ----------------------------------------------------------------- sounds
def campfire():
    n = N + FADE
    rumble = lp(brown(n), 220, 3)
    rumble = rumble / np.std(rumble) * 0.55
    rumble *= 0.75 + 0.25 * slow_env(n, 0.6)
    hiss = bp(white(n), 2500, 7500) * 0.05 * slow_env(n, 1.5, floor=0.5)

    def kernel(ms, lo, hi):
        m = int(ms / 1000 * SR)
        k = bp(white(m), lo, hi, 2) * np.exp(-np.arange(m) / (m / 4))
        return k / (np.max(np.abs(k)) + 1e-9)

    crackle = np.zeros(n)
    # many small ticks
    for _ in range(4):
        k = kernel(rng.uniform(2, 6), 1800, 8000)
        crackle += signal.fftconvolve(sparse_hits(n, 14), k)[:n] * 0.5
    # medium snaps
    for _ in range(3):
        k = kernel(rng.uniform(8, 20), 900, 4500)
        crackle += signal.fftconvolve(sparse_hits(n, 3.2, 0.5), k)[:n] * 0.95
    # occasional big pops with a little body
    k = kernel(40, 250, 2500)
    crackle += signal.fftconvolve(sparse_hits(n, 0.5, 0.3), k)[:n] * 1.6
    # crackle comes in bursts like real flames
    crackle *= 0.4 + 0.6 * slow_env(n, 0.8, depth=1.5)
    return finish(make_loop(rumble + hiss + crackle), rms=0.13)


def crickets():
    n = N + FADE
    out = np.zeros(n)
    for _ in range(6):
        f0 = rng.uniform(4300, 5100)
        pulse_len = int(0.011 * SR)
        tp = np.arange(pulse_len) / SR
        pulse = np.sin(2 * np.pi * f0 * tp) * np.hanning(pulse_len) ** 0.7
        pulses = int(rng.integers(3, 5))
        gap = rng.uniform(0.026, 0.034)
        period = rng.uniform(0.55, 0.95)
        t = rng.uniform(0, period)
        gain_env = slow_env(n, 0.08, depth=1.0, floor=0.15)
        while t < DUR + 3:
            for p in range(pulses):
                s = int((t + p * gap) * SR)
                if s + pulse_len < n:
                    out[s:s + pulse_len] += pulse * gain_env[s] * rng.uniform(0.8, 1.0)
            t += period * rng.uniform(0.95, 1.05)
    bed = lp(pink(n), 900, 2) * 0.01
    return finish(make_loop(out + bed), rms=0.07)


def hoot(dur, f0, drop):
    t = np.arange(int(dur * SR)) / SR
    f = f0 * (1 - drop * t / dur)
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(ph) + 0.35 * np.sin(2 * ph) + 0.12 * np.sin(3 * ph)
    env = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 1.6
    return s * env


def owl():
    n = N + FADE
    out = np.zeros(n)
    pattern = [(0.0, 0.22, 1.00), (0.40, 0.55, 0.9), (1.20, 0.30, 0.85), (1.75, 0.30, 0.8)]
    for start, f0 in ((6.0, 370), (25.0, 330)):
        for off, dur, amp in pattern:
            h = hoot(dur, f0, 0.10) * amp
            s = int((start + off) * SR)
            out[s:s + len(h)] += h
    out = lp(out, 1800, 2)
    out = reverb(out, 1.6, 0.5)
    bed = lp(pink(n), 700, 2) * 0.012
    return finish(make_loop(out + bed * np.max(np.abs(out))), peak=0.5)


def wind():
    n = N + FADE
    low = lp(white(n), 350, 3) * slow_env(n, 0.12, depth=1.4, floor=0.2)
    mid = bp(white(n), 350, 1100) * slow_env(n, 0.17, depth=1.8, floor=0.1) * 0.7
    high = bp(white(n), 1100, 3200) * slow_env(n, 0.22, depth=2.2, floor=0.05) * 0.25
    return finish(make_loop(low + mid + high), rms=0.12)


def rain():
    n = N + FADE
    base = bp(pink(n), 400, 9000)
    base *= 0.85 + 0.15 * slow_env(n, 0.3)
    drops = signal.fftconvolve(
        sparse_hits(n, 260, 0.5),
        bp(white(40), 2500, 9000, 2) * np.exp(-np.arange(40) / 8))[:n] * 1.5
    patter = bp(white(n), 150, 600) * 0.12   # soft drumming on a tent
    return finish(make_loop(base + drops + patter), rms=0.12)


def thunder():
    n = N + FADE
    out = np.zeros(n)
    for start, length in ((7.0, 11.0), (27.0, 9.0)):
        m = int(length * SR)
        t = np.arange(m) / SR
        env = (1 - np.exp(-t / 0.8)) * np.exp(-t / (length / 3.2))
        env *= 0.65 + 0.35 * slow_env(m, 1.2)
        rumble = lp(brown(m), 130, 3)
        rumble = rumble / np.std(rumble)
        s = int(start * SR)
        out[s:s + m] += rumble * env
    out += lp(brown(n), 90, 2) * 0.01
    return finish(make_loop(out), peak=0.6)


def stream():
    n = N + FADE
    flow = bp(white(n), 350, 4200) * (0.55 + 0.45 * slow_env(n, 9, 1.0))
    flow2 = bp(white(n), 900, 2600) * (0.4 + 0.6 * slow_env(n, 5, 1.2)) * 0.6
    bubbles = np.zeros(n)
    blen = int(0.04 * SR)
    tb = np.arange(blen) / SR
    for s in rng.integers(0, n - blen, int(22 * n / SR)):
        f = rng.uniform(500, 1100) * (1 + 3.5 * tb / 0.04)
        bub = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tb / 0.012)
        bubbles[s:s + blen] += bub * rng.uniform(0.1, 0.45)
    return finish(make_loop(flow + flow2 + bubbles * 0.5), rms=0.11)


def brown_noise():
    return finish(make_loop(brown(N + FADE)), rms=0.13)


def pink_noise():
    return finish(make_loop(lp(pink(N + FADE), 9500, 2)), rms=0.12)


SOUNDS = {
    "campfire_crackle": campfire,
    "night_crickets": crickets,
    "distant_owl": owl,
    "soft_wind": wind,
    "rain_on_tent": rain,
    "far_thunder": thunder,
    "forest_stream": stream,
    "brown_noise": brown_noise,
    "pink_noise": pink_noise,
}


def encode(name, x):
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, name + ".wav")
        wavfile.write(wav, SR, (x * 32767).astype(np.int16))
        dest = os.path.join(OUT, name + ".ogg")
        subprocess.run(
            ["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1",
             "-c:a", "libvorbis", "-q:a", "2", dest], check=True)
        return dest


if __name__ == "__main__":
    for name, fn in SOUNDS.items():
        x = fn()
        seam = abs(x[0] - x[-1])
        path = encode(name, x)
        print(f"{name:18s} rms={np.sqrt(np.mean(x**2)):.3f} peak={np.max(np.abs(x)):.2f} "
              f"seam_step={seam:.3f} size={os.path.getsize(path)//1024} KB")
