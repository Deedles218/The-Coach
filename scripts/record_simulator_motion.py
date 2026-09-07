#!/usr/bin/env python3
"""Record before a UI action; stop on stdin, extract frames for Java assertions."""
import argparse
from contextlib import contextmanager
import os
from pathlib import Path
import signal
import subprocess
import sys


def _signal_owned_group(process, signum):
    # Every managed child starts its own session. Never signal an inherited group.
    try:
        os.killpg(process.pid, signum)
    except ProcessLookupError:
        pass


def _stop_owned_process(process, graceful_signal, timeout=15, drain=True):
    if process.poll() is None:
        _signal_owned_group(process, graceful_signal)
    wait = process.communicate if drain else process.wait
    try:
        wait(timeout=timeout)
    except subprocess.TimeoutExpired as primary:
        try:
            _signal_owned_group(process, signal.SIGKILL)
            wait(timeout=2)
        except BaseException as cleanup_failure:
            print("Recorder kill/wait failed: " + str(cleanup_failure), file=sys.stderr)
        # A timeout is still a failed recording even after its process was reaped.
        raise primary


class OwnedRecorderProcesses:
    def __init__(self):
        self.current = None

    def terminate(self, signum, frame):
        # Java gives SIGTERM cleanup six seconds. This path is bounded by 2 + 2,
        # including a signal arriving during normal communicate/finalization.
        failure = InterruptedError("Simulator recorder received SIGTERM")
        if self.current is not None:
            try:
                _stop_owned_process(self.current, signal.SIGTERM, timeout=2, drain=False)
            except BaseException as cleanup_failure:
                print("Recorder interrupted cleanup: " + str(cleanup_failure), file=sys.stderr)
        raise failure

    @contextmanager
    def start(self, command, graceful_signal=signal.SIGTERM, **kwargs):
        # Defer a stop request during Popen without passing a blocked signal mask
        # to simctl/ffmpeg. Register the owned group before handling that request.
        pending_stop = []
        previous_handler = signal.signal(signal.SIGTERM, lambda signum, frame: pending_stop.append(True))
        try:
            process = subprocess.Popen(command, start_new_session=True, **kwargs)
            self.current = process
        finally:
            signal.signal(signal.SIGTERM, previous_handler)
        if pending_stop:
            self.terminate(signal.SIGTERM, None)
        try:
            yield process
        finally:
            primary = sys.exc_info()[1]
            try:
                _stop_owned_process(process, graceful_signal)
            except BaseException as cleanup_failure:
                if primary is None:
                    raise
                # Preserve a failed READY/read/extraction instead of replacing it.
                print("Recorder cleanup after failure: " + str(cleanup_failure), file=sys.stderr)
            finally:
                self.current = None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--udid', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    folder = Path(args.output)
    folder.mkdir(parents=True, exist_ok=True)
    children = OwnedRecorderProcesses()
    previous_handler = signal.signal(signal.SIGTERM, children.terminate)
    try:
        with children.start(['xcrun', 'simctl', 'io', args.udid, 'recordVideo',
                             '--codec=h264', str(folder / 'motion.mp4')],
                            graceful_signal=signal.SIGINT, stdout=subprocess.DEVNULL,
                            stderr=subprocess.PIPE, text=True) as recording:
            for line in recording.stderr:
                if 'Recording started' in line:
                    print('READY', flush=True)
                    break
            else:
                raise RuntimeError('Simulator video recording did not start')
            sys.stdin.readline()
        command = ['ffmpeg', '-v', 'error', '-i', str(folder / 'motion.mp4'),
                   '-vf', 'fps=30,scale=390:-1', str(folder / 'frame-%05d.png')]
        with children.start(command) as extraction:
            extraction.communicate(timeout=30)
            if extraction.returncode:
                raise subprocess.CalledProcessError(extraction.returncode, command)
    finally:
        signal.signal(signal.SIGTERM, previous_handler)


if __name__ == '__main__':
    main()
