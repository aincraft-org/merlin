from __future__ import annotations
import math
import numpy as np


def _softmax(logits, temperature):
    scaled=np.asarray(logits,dtype=np.float64)/temperature; scaled-=scaled.max(axis=1,keepdims=True); exponent=np.exp(scaled); return exponent/exponent.sum(axis=1,keepdims=True)

def _nll(logits,labels,temperature):
    probabilities=_softmax(logits,temperature); selected=probabilities[np.arange(labels.size),labels]; return float(-np.log(np.clip(selected,1e-12,1)).mean())

def calibrate_temperature(logits,labels):
    logits=np.asarray(logits,dtype=np.float64); labels=np.asarray(labels,dtype=np.int64)
    if logits.ndim!=2 or labels.shape!=(logits.shape[0],) or labels.size==0: raise ValueError("invalid calibration data")
    candidates=np.exp(np.linspace(math.log(.25),math.log(4),121)); losses=np.array([_nll(logits,labels,float(value)) for value in candidates]); return float(candidates[int(losses.argmin())])

def select_thresholds(logits,labels,reject_id,temperature):
    probabilities=_softmax(np.asarray(logits),temperature); labels=np.asarray(labels,dtype=np.int64); ordered=np.sort(probabilities,axis=1); top=ordered[:,-1]; margin=ordered[:,-1]-ordered[:,-2]; predicted=probabilities.argmax(axis=1); correct=(predicted==labels)&(labels!=reject_id)
    if not correct.any(): return 1.,1.
    return max(1e-6,float(np.quantile(top[correct],.10))),max(1e-6,float(np.quantile(margin[correct],.10)))

def evaluate(logits,labels,temperature=1.,*,reject_id=None,top_threshold=0.,margin=0.):
    logits=np.asarray(logits,dtype=np.float64); labels=np.asarray(labels,dtype=np.int64); probabilities=_softmax(logits,float(temperature)); predicted=probabilities.argmax(axis=1); ordered=np.sort(probabilities,axis=1); accepted=(ordered[:,-1]>=top_threshold)&((ordered[:,-1]-ordered[:,-2])>=margin)
    if reject_id is not None: accepted&=predicted!=reject_id
    classes=logits.shape[1]; confusion=np.zeros((classes,classes),dtype=np.int64)
    for expected,actual in zip(labels,predicted): confusion[expected,actual]+=1
    per_class=[]
    for label in range(classes):
        tp=confusion[label,label]; precision_den=confusion[:,label].sum(); recall_den=confusion[label,:].sum(); precision=float(tp/precision_den) if precision_den else 0.; recall=float(tp/recall_den) if recall_den else 0.; f1=2*precision*recall/(precision+recall) if precision+recall else 0.; per_class.append({"label":label,"precision":precision,"recall":recall,"f1":f1,"count":int(recall_den)})
    reject_false_accept=float(accepted[labels==reject_id].mean()) if reject_id is not None and (labels==reject_id).any() else 0.; accepted_correct=accepted&(predicted==labels); macro=float(np.mean([item["f1"] for item in per_class])); weighted=float(sum(item["f1"]*item["count"] for item in per_class)/labels.size)
    return {"accuracy":float((predicted==labels).mean()),"macro_f1":macro,"weighted_f1":weighted,"count":int(labels.size),"temperature":float(temperature),"coverage":float(accepted.mean()),"accepted_precision":float(accepted_correct.sum()/accepted.sum()) if accepted.any() else 0.,"reject_false_accept_rate":reject_false_accept,"confusion":confusion.tolist(),"per_class":per_class}
