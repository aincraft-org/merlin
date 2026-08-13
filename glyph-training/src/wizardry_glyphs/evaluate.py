from __future__ import annotations
import numpy as np

def calibrate_temperature(logits, labels):
    return 1.0

def evaluate(logits, labels, temperature=1.0):
    logits=np.asarray(logits,float)/temperature; labels=np.asarray(labels,int); pred=logits.argmax(1)
    return {'accuracy': float((pred==labels).mean()), 'count': int(labels.size), 'temperature': float(temperature)}
