#!/usr/bin/env python3
"""Collect representative CPU profiles and GPU telemetry for Issue #7."""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

benchmark_workspace = Path(
    os.environ.get("RENY_BENCHMARK_WORKSPACE", Path.cwd() / "benchmark-work")
)
sys.path.append(str(benchmark_workspace / "scripts"))
import run_campaign
from run_campaign import CampaignRunner, ROOT, TARGET, CONFIG_IDS, run_tool


EVIDENCE_DIR = Path(__file__).resolve().parents[1] / "benchmarks" / "baseline-0.0" / "evidence"
ASPROF_BIN = Path.home() / ".local" / "bin" / "asprof"
GPU_ACT_FREQ = Path("/sys/class/drm/card1/gt_act_freq_mhz")
GPU_CUR_FREQ = Path("/sys/class/drm/card1/gt_cur_freq_mhz")


def collect_evidence_case(
    runner: CampaignRunner,
    config: str,
    scenario: str,
    output_dir: Path,
    profile_seconds: int = 30,
) -> Dict[str, Any]:
    output_dir.mkdir(parents=True, exist_ok=True)
    prefix = f"{config}_{scenario}"
    print(f"=== Collecting evidence for {prefix} ({CONFIG_IDS[config]}) ===")

    attempt_id = f"evidence-{int(time.time() * 1000)}-{config}-{scenario}"
    attempt_dir = ROOT / "benchmark-work" / "raw" / attempt_id
    attempt_dir.mkdir(parents=True, exist_ok=True)

    runner.stage_profile(config, attempt_dir)
    world_name = "RenyBaseline00" if config in ("A", "B") else "New World"
    launch_time = runner.launch_game(attempt_dir)
    runner.select_world(launch_time, world_name, attempt_dir)
    time.sleep(8.0)

    run_id = runner.start_command(scenario, launch_time, attempt_dir, preserve_focus=True)
    print(f"[{prefix}] benchmark run {run_id} started. Waiting for MEASURING transition...")

    warmup_pattern = r"benchmark " + re.escape(run_id) + r" state WARMUP -> MEASURING"
    runner.wait_log(warmup_pattern, launch_time, 100.0, f"{run_id} MEASURING transition")
    print(f"[{prefix}] Entered MEASURING! Beginning CPU and GPU profiling for {profile_seconds}s...")

    pids = runner.minecraft_pids()
    if not pids:
        raise RuntimeError("Minecraft PID not found during measurement")
    pid = pids[0]

    # Sample GPU frequency
    gpu_samples = []
    stop_event = threading.Event()

    def sample_gpu():
        while not stop_event.is_set():
            act = float(GPU_ACT_FREQ.read_text().strip()) if GPU_ACT_FREQ.exists() else 0.0
            cur = float(GPU_CUR_FREQ.read_text().strip()) if GPU_CUR_FREQ.exists() else 0.0
            gpu_samples.append({"time": time.time(), "act_freq_mhz": act, "cur_freq_mhz": cur})
            time.sleep(0.5)

    gpu_thread = threading.Thread(target=sample_gpu, daemon=True)
    gpu_thread.start()

    # Start asprof
    html_file = output_dir / f"{prefix}_cpu_flamegraph.html"
    collapsed_file = output_dir / f"{prefix}_cpu.collapsed"
    hotspots_file = output_dir / f"{prefix}_cpu_hotspots.txt"

    if ASPROF_BIN.exists():
        subprocess.run([str(ASPROF_BIN), "start", "-e", "itimer", "-i", "2ms", pid], check=True)
        time.sleep(profile_seconds)
        subprocess.run([str(ASPROF_BIN), "dump", "-o", "collapsed", "-f", str(collapsed_file), pid], check=True)
        subprocess.run([str(ASPROF_BIN), "dump", "-o", "flat", "-f", str(hotspots_file), pid], check=True)
        subprocess.run([str(ASPROF_BIN), "stop", "-o", "flamegraph", "-f", str(html_file), pid], check=True)
    else:
        time.sleep(profile_seconds)

    stop_event.set()
    gpu_thread.join(timeout=2)

    # Stop game and clean up
    runner.stop_game()

    act_freqs = [s["act_freq_mhz"] for s in gpu_samples if s["act_freq_mhz"] > 0]
    gpu_summary = {
        "scenario": scenario,
        "configuration": config,
        "configuration_id": CONFIG_IDS[config],
        "profiled_seconds": profile_seconds,
        "gpu_samples_count": len(gpu_samples),
        "gpu_act_freq_min_mhz": min(act_freqs) if act_freqs else None,
        "gpu_act_freq_max_mhz": max(act_freqs) if act_freqs else None,
        "gpu_act_freq_mean_mhz": (sum(act_freqs) / len(act_freqs)) if act_freqs else None,
        "at_max_clock_percentage": (sum(1 for f in act_freqs if f >= 1250) / len(act_freqs) * 100.0) if act_freqs else 0.0,
        "gpu_samples": gpu_samples,
    }

    gpu_summary_file = output_dir / f"{prefix}_gpu_telemetry.json"
    with gpu_summary_file.open("w", encoding="utf-8") as f:
        json.dump(gpu_summary, f, indent=2)

    print(f"[{prefix}] Evidence collection complete:")
    print(f"  - CPU flamegraph: {html_file.name}")
    print(f"  - CPU collapsed: {collapsed_file.name}")
    print(f"  - CPU hotspots: {hotspots_file.name}")
    print(f"  - GPU telemetry: {gpu_summary_file.name} (avg act freq: {gpu_summary['gpu_act_freq_mean_mhz']:.1f} MHz, at max clock: {gpu_summary['at_max_clock_percentage']:.1f}%)")

    return gpu_summary


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--case", help="specific case to run, e.g. A_BENCH-01")
    parser.add_argument("--duration", type=int, default=30, help="profiling duration in seconds")
    args = parser.parse_args()

    targets = [
        ("A", "BENCH-01"),
        ("B", "BENCH-01"),
        ("A", "BENCH-02"),
        ("C", "BENCH-03"),
        ("D", "BENCH-06"),
    ]

    if args.case:
        config, scenario = args.case.split("_", 1)
        targets = [(config, scenario)]

    runner = CampaignRunner(max_attempts_per_cell=3, max_runs=1, selected_cell=None, pilot=True)
    runner.acquire_lock()

    results = {}
    for config, scenario in targets:
        summary = collect_evidence_case(runner, config, scenario, EVIDENCE_DIR, profile_seconds=args.duration)
        results[f"{config}_{scenario}"] = summary

    index_file = EVIDENCE_DIR / "evidence_index.json"
    gl_info = subprocess.check_output(["glxinfo", "-B"], text=True)
    meta = {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "commit_sha": "1db76c053470e158c042c18b2da4c27a3b660340",
        "glxinfo": gl_info,
        "cases": results,
    }
    with index_file.open("w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)

    print(f"All evidence collected and indexed in {index_file}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
