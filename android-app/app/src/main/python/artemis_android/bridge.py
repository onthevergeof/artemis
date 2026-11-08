"""Authenticated client for the bundled ARTEMIS accessibility helper.

The helper listens only on 127.0.0.1:18888. A random session token is passed
from the containing Android app and is never placed in a URL or log message.
"""
from __future__ import annotations

import json
from typing import Any
from urllib.error import URLError
from urllib.request import Request, urlopen

_BASE_URL = "http://127.0.0.1:18888"
_SESSION_TOKEN: str | None = None


def configure(token: str) -> None:
    """Configure the in-memory session token supplied by the Android app."""
    global _SESSION_TOKEN
    if not isinstance(token, str) or len(token) < 32:
        raise ValueError("A valid app-generated helper session token is required")
    _SESSION_TOKEN = token


def _request(path: str, *, method: str = "GET", payload: dict[str, Any] | None = None) -> dict[str, Any]:
    if not _SESSION_TOKEN:
        raise RuntimeError("Accessibility bridge is not configured")
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    request = Request(
        _BASE_URL + path,
        data=body,
        headers={
            "X-Artemis-Token": _SESSION_TOKEN,
            "Accept": "application/json",
            **({"Content-Type": "application/json"} if body is not None else {}),
        },
        method=method,
    )
    with urlopen(request, timeout=3.0) as response:
        raw = response.read().decode("utf-8")
    try:
        return json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("The accessibility helper returned invalid JSON") from exc


def ping() -> dict[str, Any]:
    """Check whether the user-enabled local accessibility service is running."""
    try:
        return _request("/ping")
    except (OSError, URLError, TimeoutError) as exc:
        return {"success": False, "reachable": False, "error": type(exc).__name__}


def snapshot() -> dict[str, Any]:
    """Read the current accessibility hierarchy snapshot."""
    return _request("/snapshot")


def dump_xml() -> str:
    """Return the current accessibility hierarchy as XML."""
    if not _SESSION_TOKEN:
        raise RuntimeError("Accessibility bridge is not configured")
    request = Request(_BASE_URL + "/dump_xml", headers={"X-Artemis-Token": _SESSION_TOKEN})
    with urlopen(request, timeout=3.0) as response:
        return response.read().decode("utf-8")


def execute(command: str, **parameters: Any) -> dict[str, Any]:
    """Execute one helper-supported accessibility command."""
    if not command or not command.strip():
        raise ValueError("Command cannot be empty")
    payload = {"cmd": command.strip(), **parameters}
    return _request("/action", method="POST", payload=payload)
