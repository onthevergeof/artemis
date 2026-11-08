"""Small startup probe for the embedded ARTEMIS Android Python runtime.

This deliberately checks availability only. It does not claim that the ARTEMIS
agent or its dependency stack has been ported to Android yet.
"""
from __future__ import annotations

import importlib.util
import json
import platform
import sys


CORE_MODULES = ("langgraph", "langchain", "mcp", "fastapi", "adbutils", "uiautomator2")


def runtime_info() -> str:
    """Return a compact JSON status record for the Android app."""
    available = []
    missing = []
    for module in CORE_MODULES:
        try:
            present = importlib.util.find_spec(module) is not None
        except (ImportError, ModuleNotFoundError, ValueError):
            present = False
        (available if present else missing).append(module)

    return json.dumps(
        {
            "python": sys.version.split()[0],
            "platform": sys.platform,
            "machine": platform.machine(),
            "embedded_runtime": True,
            "agent_integrated": False,
            "available_core_modules": available,
            "missing_core_modules": missing,
        },
        sort_keys=True,
    )
