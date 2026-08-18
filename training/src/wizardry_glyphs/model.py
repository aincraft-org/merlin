from __future__ import annotations

import torch
from torch import nn

from .schema import LABELS

class VectorEncoder(nn.Module):
    """Encode ordered strokes. Sequence position is part of the embedding."""

    def __init__(self, embedding_dim: int = 32) -> None:
        super().__init__()
        self.point = nn.Sequential(nn.Linear(8, 32), nn.ReLU(), nn.Linear(32, embedding_dim))
        self.stroke = nn.Sequential(nn.Linear(embedding_dim + 16, embedding_dim), nn.ReLU())
        self.position = nn.Linear(1, embedding_dim, bias=False)
        self.order = nn.GRU(embedding_dim, embedding_dim, batch_first=True)
        self.mix = nn.Linear(embedding_dim + 8, embedding_dim)

    def forward(self, vectors: torch.Tensor, mask: torch.Tensor | tuple[torch.Tensor, torch.Tensor] | None = None) -> torch.Tensor:
        point_mask = stroke_mask = None
        if isinstance(mask, tuple):
            point_mask, stroke_mask = mask
        elif mask is not None:
            point_mask = mask
        if point_mask is None:
            point_mask = torch.ones(vectors.shape[:-1], device=vectors.device, dtype=vectors.dtype)
        point_mask = point_mask.to(dtype=vectors.dtype)
        point_features = self.point(vectors) * point_mask.unsqueeze(-1)
        denom = point_mask.sum(dim=2, keepdim=True).clamp_min(1.0)
        stroke_features = point_features.sum(dim=2) / denom
        if stroke_mask is None:
            stroke_mask = (point_mask.sum(dim=2) > 0).to(dtype=vectors.dtype)
        stroke_mask = stroke_mask.to(dtype=vectors.dtype)
        first = vectors[:, :, 0, :]
        last_index = point_mask.sum(dim=2).to(dtype=torch.long).clamp_min(1) - 1
        last_points = vectors.gather(2, last_index.unsqueeze(-1).unsqueeze(-1).expand(-1, -1, 1, vectors.size(-1))).squeeze(2)
        positions = torch.arange(vectors.shape[1], device=vectors.device, dtype=vectors.dtype)
        positions = (positions / max(vectors.shape[1] - 1, 1)).view(1, -1, 1)
        stroke_features = self.stroke(torch.cat((stroke_features + self.position(positions), first, last_points), dim=-1)) * stroke_mask.unsqueeze(-1)
        sequence, _ = self.order(stroke_features)
        last_stroke = stroke_mask.sum(dim=1).to(dtype=torch.long).clamp_min(1) - 1
        batch = torch.arange(sequence.size(0), device=sequence.device)
        directions = (last_points[:, :4, :2] - first[:, :4, :2]).reshape(vectors.size(0), 8)
        used = stroke_mask[:, :4].unsqueeze(-1).expand(-1, -1, 2).reshape(vectors.size(0), 8)
        return self.mix(torch.cat((sequence[batch, last_stroke], directions * used), dim=1))

class RasterEncoder(nn.Module):
    def __init__(self, embedding_dim: int = 32) -> None:
        super().__init__()
        self.net = nn.Sequential(
            nn.Conv2d(3, 8, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
            nn.Conv2d(8, 16, 3, padding=1), nn.ReLU(), nn.AdaptiveAvgPool2d((4, 4)),
        )
        self.projection = nn.Linear(16 * 4 * 4, embedding_dim)

    def forward(self, raster: torch.Tensor) -> torch.Tensor:
        return self.projection(self.net(raster).flatten(1))

class VectorClassifier(nn.Module):
    def __init__(self, classes: int = len(LABELS), embedding_dim: int = 32) -> None:
        super().__init__()
        self.encoder = VectorEncoder(embedding_dim)
        self.head = nn.Linear(embedding_dim, classes)

    def encode(self, vectors, mask=None):
        return self.encoder(vectors, mask)

    def forward(self, vectors, mask=None):
        return self.head(self.encode(vectors, mask))


class RasterClassifier(nn.Module):
    def __init__(self, classes: int = len(LABELS), embedding_dim: int = 32) -> None:
        super().__init__()
        self.encoder = RasterEncoder(embedding_dim)
        self.head = nn.Linear(embedding_dim, classes)

    def encode(self, raster):
        return self.encoder(raster)

    def forward(self, raster):
        return self.head(self.encode(raster))


class FusedClassifier(nn.Module):
    def __init__(self, classes: int = len(LABELS), embedding_dim: int = 32) -> None:
        super().__init__()
        self.vector = VectorEncoder(embedding_dim)
        self.raster = RasterEncoder(embedding_dim)
        self.vector_head = nn.Linear(embedding_dim, classes)
        self.raster_head = nn.Linear(embedding_dim, classes)

    def forward(self, vectors, mask, raster):
        return self.vector_head(self.vector(vectors, mask)) + self.raster_head(self.raster(raster))
