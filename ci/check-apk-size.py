#!/usr/bin/env python3
"""Fail when the release APK grew past the threshold in apk-size.json.

Paths resolve against the repository root, so the script runs from anywhere.
CI calls it as the last step of the release-size job in quality.yml; the same
file is what a local run checks, there is no second copy of the rule.
"""

import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CONFIG = os.path.join(HERE, "apk-size.json")


def main() -> int:
    with open(CONFIG, encoding="utf-8") as fh:
        cfg = json.load(fh)

    baseline = int(cfg["bytes"])
    percent = float(cfg["threshold_percent"])
    limit = int(baseline * (1 + percent / 100))

    apks = sorted(glob.glob(os.path.join(ROOT, cfg["apk_glob"])))
    if len(apks) != 1:
        print(
            "release-size: expected exactly 1 apk matching %s, found %d"
            % (cfg["apk_glob"], len(apks))
        )
        for apk in apks:
            print("  - " + apk)
        print("Did the assembleRelease step run and finish green?")
        return 2

    size = os.path.getsize(apks[0])
    delta = size - baseline
    print("release apk: %s (%s bytes)" % (os.path.basename(apks[0]), format(size, ",")))
    print(
        "baseline:    %s bytes, measured %s on %s (ci/apk-size.json)"
        % (format(baseline, ","), cfg["measured_at"], cfg["commit"])
    )
    print(
        "limit:       %s bytes (+%g%%, headroom %s bytes)"
        % (format(limit, ","), percent, format(limit - baseline, ","))
    )
    print("delta:       %+d bytes (%+.2f%%)" % (delta, 100.0 * delta / baseline))

    if size > limit:
        print()
        print("FAIL: the release APK is %s bytes over the limit." % format(size - limit, ","))
        print("If the growth is intended, raise \"bytes\" in ci/apk-size.json to %d" % size)
        print("and explain the growth in the commit message.")
        print("If app/google-services.json is present, this build packs Firebase on purpose")
        print("and runs ~7% heavier than the baseline: that file is gitignored, so CI never")
        print("has it. The baseline is the CI build.")
        return 1

    print("within threshold")
    return 0


if __name__ == "__main__":
    sys.exit(main())
