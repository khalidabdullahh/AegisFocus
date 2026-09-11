#!/usr/bin/env python3
"""
ASR License Key Generator
Use this script to generate valid Pro license keys for customers paying via bKash, Nagad, Stripe, or early Reddit testers!

Usage:
    python3 tools/generate_license.py --tier LIFE --count 5
    python3 tools/generate_license.py --tier YEAR --customer "John Doe"
"""

import argparse
import hashlib
import random
import string

SECRET_SALT = "ASR_DISCIPLINE_PROGRESS_2026_SECRET_KEY"

def compute_checksum(tier: str, payload: str) -> str:
    msg = f"{tier}-{payload}-{SECRET_SALT}".encode('utf-8')
    h = hashlib.sha256(msg).digest()
    return "".join(f"{b:02X}" for b in h[:2])

def generate_key(tier: str, label: str = None) -> str:
    tier = tier.upper()
    if label:
        # 4-char alphanumeric payload derived from label
        payload = label.replace(" ", "").upper()[:4].ljust(4, 'X')
    else:
        payload = "".join(random.choices(string.ascii_uppercase + string.digits, k=4))
    
    checksum = compute_checksum(tier, payload)
    return f"ASR-{tier}-{payload}-{checksum}"

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate ASR License Keys")
    parser.add_argument("--tier", choices=["LIFE", "YEAR", "MNTH", "STUD", "VIP"], default="LIFE", help="License tier")
    parser.add_argument("--count", type=int, default=1, help="Number of keys to generate")
    parser.add_argument("--customer", type=str, default=None, help="Customer name / tag")

    args = parser.parse_args()

    print("=" * 50)
    print(f"  ASR PRO LICENSE KEY GENERATOR — Tier: {args.tier}")
    print("=" * 50)

    for i in range(args.count):
        key = generate_key(args.tier, args.customer)
        print(f"  Key #{i+1}:  {key}")

    print("=" * 50)
    print("✓ Ready to send to customer! Customer enters key in ASR app -> Pro is instantly unlocked.")
