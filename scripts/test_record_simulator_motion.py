"""Offline emergency recorder cleanup checks; never starts a real simulator or child."""
import contextlib
import io
import signal
import subprocess
import unittest
from unittest.mock import Mock, call, patch

from record_simulator_motion import OwnedRecorderProcesses, _stop_owned_process


class RecorderCleanupTest(unittest.TestCase):
    def process(self):
        process = Mock(pid=43210)
        process.poll.return_value = None
        return process

    def test_normal_recording_stop_targets_only_its_own_group(self):
        process = self.process()
        with patch("record_simulator_motion.os.killpg") as send:
            _stop_owned_process(process, signal.SIGINT)
        send.assert_called_once_with(process.pid, signal.SIGINT)
        process.communicate.assert_called_once_with(timeout=15)

    def test_stop_timeout_kills_and_waits_without_losing_original_timeout(self):
        process = self.process()
        original = subprocess.TimeoutExpired("owned simctl", 15)
        process.communicate.side_effect = [original, None]
        with patch("record_simulator_motion.os.killpg") as send:
            with self.assertRaises(subprocess.TimeoutExpired) as error:
                _stop_owned_process(process, signal.SIGINT)
        self.assertIs(original, error.exception)
        self.assertEqual([call(process.pid, signal.SIGINT), call(process.pid, signal.SIGKILL)], send.call_args_list)
        self.assertEqual([call(timeout=15), call(timeout=2)], process.communicate.call_args_list)

    def test_kill_failure_does_not_replace_the_primary_timeout(self):
        process = self.process()
        original = subprocess.TimeoutExpired("owned simctl", 15)
        process.communicate.side_effect = original
        with patch("record_simulator_motion.os.killpg", side_effect=[None, OSError("kill failed")]), \
                contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(subprocess.TimeoutExpired) as error:
                _stop_owned_process(process, signal.SIGINT)
        self.assertIs(original, error.exception)

    def test_sigterm_waits_two_then_kills_and_waits_two_for_owned_child(self):
        process = self.process()
        process.wait.side_effect = [subprocess.TimeoutExpired("owned ffmpeg", 2), None]
        owner = OwnedRecorderProcesses()
        owner.current = process
        with patch("record_simulator_motion.os.killpg") as send, contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(InterruptedError):
                owner.terminate(signal.SIGTERM, None)
        self.assertEqual([call(process.pid, signal.SIGTERM), call(process.pid, signal.SIGKILL)], send.call_args_list)
        self.assertEqual([call(timeout=2), call(timeout=2)], process.wait.call_args_list)
        process.communicate.assert_not_called()

    def test_ready_failure_survives_cleanup_timeout(self):
        process = self.process()
        original = RuntimeError("READY was not produced")
        process.communicate.side_effect = [subprocess.TimeoutExpired("owned simctl", 15), None]
        owner = OwnedRecorderProcesses()
        with patch("record_simulator_motion.subprocess.Popen", return_value=process) as spawn, \
                patch("record_simulator_motion.os.killpg"), contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(RuntimeError) as error:
                with owner.start(["owned-simctl"], graceful_signal=signal.SIGINT):
                    raise original
        self.assertIs(original, error.exception)
        spawn.assert_called_once_with(["owned-simctl"], start_new_session=True)
        self.assertIsNone(owner.current)

    def test_extraction_timeout_still_reaps_ffmpeg_and_preserves_timeout(self):
        process = self.process()
        original = subprocess.TimeoutExpired("owned ffmpeg", 30)
        process.communicate.side_effect = [original, None]
        owner = OwnedRecorderProcesses()
        with patch("record_simulator_motion.subprocess.Popen", return_value=process), \
                patch("record_simulator_motion.os.killpg") as send:
            with self.assertRaises(subprocess.TimeoutExpired) as error:
                with owner.start(["owned-ffmpeg"]) as extraction:
                    extraction.communicate(timeout=30)
        self.assertIs(original, error.exception)
        send.assert_called_once_with(process.pid, signal.SIGTERM)
        self.assertEqual([call(timeout=30), call(timeout=15)], process.communicate.call_args_list)
        self.assertIsNone(owner.current)

    def test_sigterm_during_spawn_is_handled_after_child_is_registered(self):
        process = self.process()
        owner = OwnedRecorderProcesses()
        original_handler = signal.getsignal(signal.SIGTERM)

        def spawn(*args, **kwargs):
            signal.getsignal(signal.SIGTERM)(signal.SIGTERM, None)
            return process

        with patch("record_simulator_motion.subprocess.Popen", side_effect=spawn), \
                patch("record_simulator_motion.os.killpg") as send:
            with self.assertRaises(InterruptedError):
                with owner.start(["owned-simctl"]):
                    self.fail("Cancelled recorder must not start the scenario")
        send.assert_called_once_with(process.pid, signal.SIGTERM)
        process.wait.assert_called_once_with(timeout=2)
        self.assertIs(original_handler, signal.getsignal(signal.SIGTERM))


if __name__ == "__main__":
    unittest.main()
