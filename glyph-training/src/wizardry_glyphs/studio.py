"""Local draw-and-classify studio for the shipped glyph model."""
from __future__ import annotations

import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

import numpy as np

from .classify import classify_strokes, example_from_strokes, load_checkpoint
from .preprocess import preprocess_example


ROOT = Path(__file__).resolve().parents[2]
PAGE = Path(__file__).with_name("studio.html")


def classify_request(payload: dict, model, labels, torch, calibration=None) -> dict:
    strokes = payload.get("strokes")
    if not isinstance(strokes, list) or not strokes:
        raise ValueError("strokes required")
    result = classify_strokes(strokes, model, labels, torch, calibration=calibration)
    return {
        "label": result["label"],
        "accepted": result["accepted"],
        "reason": result["reason"],
        "suggestion": result["suggestion"],
        "score": result["score"],
        "candidates": result["candidates"],
        "raster": result["raster"].astype(np.float32).tolist(),
        "stroke_count": result["stroke_count"],
    }


def catalog_previews(catalog_path: Path | None = None) -> list[dict]:
    path = catalog_path or (ROOT / "catalog-geometry-v1.json")
    catalog = json.loads(path.read_text())
    previews = []
    for label, spec in catalog["glyphs"].items():
        if label == "reject":
            continue
        strokes = [{"points": [{"x": x, "y": y} for x, y in stroke], "brush_width": catalog.get("brush_width", 6)}
                   for stroke in spec["templates"][0]["strokes"]]
        raster = preprocess_example(example_from_strokes(strokes))["raster"][0]
        previews.append({
            "label": label,
            "intent": spec.get("intent", ""),
            "raster": raster.astype(np.float32).tolist(),
        })
    return previews


def load_studio_model(bundle: Path, torch):
    model, labels, calibration = load_checkpoint(bundle / "model.pt", torch)
    if not labels:
        labels = json.loads((bundle / "manifest.json").read_text())["labels"]
    return model, labels, calibration


class StudioHandler(BaseHTTPRequestHandler):
    model = None
    labels = None
    torch = None
    previews = None
    calibration = None

    def log_message(self, format, *args):
        return

    def _json(self, code: int, payload: dict):
        body = json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path in {"/", "/index.html"}:
            page = PAGE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(page)))
            self.end_headers()
            self.wfile.write(page)
            return
        if self.path == "/api/catalog":
            self._json(200, {"glyphs": type(self).previews})
            return
        self.send_error(404)

    def do_POST(self):
        if self.path != "/api/classify":
            self.send_error(404)
            return
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length)
        try:
            payload = json.loads(raw.decode())
            self._json(200, classify_request(payload, type(self).model, type(self).labels, type(self).torch, type(self).calibration))
        except (ValueError, json.JSONDecodeError) as exc:
            self._json(400, {"error": str(exc)})


def serve(bundle: Path, host: str = "127.0.0.1", port: int = 8765):
    import torch
    model, labels, calibration = load_studio_model(bundle, torch)
    StudioHandler.model = model
    StudioHandler.labels = labels
    StudioHandler.torch = torch
    StudioHandler.calibration = calibration
    StudioHandler.previews = catalog_previews()
    server = ThreadingHTTPServer((host, port), StudioHandler)
    print(f"glyph studio http://{host}:{port}  labels={len(labels)}  bundle={bundle}")
    server.serve_forever()


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle", default=str(ROOT / "artifacts/dev-basic-v1"))
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args(argv)
    serve(Path(args.bundle), args.host, args.port)


if __name__ == "__main__":
    raise SystemExit(main())
