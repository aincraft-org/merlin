"""Development-only synthetic corpus generation and validation."""
from __future__ import annotations
import argparse, hashlib, json, math, random
from collections import Counter
from pathlib import Path
from typing import Any
from .schema import load_examples
PROFILE="synthetic-development"; SCHEMA_VERSION="glyph-dataset-v1"
POSITIVE_LABELS=("target-ray","damage","heal","push","cooldown","self","target","physical","fire","frost","arcane")
LABELS=(*POSITIVE_LABELS,"reject"); MIN_TEMPLATES=6
FINGERPRINT_QUANTIZATION=1e-3
PROVENANCE_CONFLICT="independent_source conflict within lineage"
 
def _validate_strokes(strokes):
    if not isinstance(strokes, list) or not strokes:
        return "strokes must be a nonempty list"
    for stroke_index, stroke in enumerate(strokes):
        if not isinstance(stroke, list) or not stroke:
            return f"stroke {stroke_index} must be a nonempty list"
        for point_index, point in enumerate(stroke):
            if not isinstance(point, (list, tuple)) or len(point) != 2:
                return f"stroke {stroke_index} point {point_index} must contain exactly two numbers"
            if any(not isinstance(value, (int, float)) or isinstance(value, bool) or not math.isfinite(value) for value in point):
                return f"stroke {stroke_index} point {point_index} must contain exactly two finite numbers"
    return None

def _catalog(path: Path):
    raw=path.read_bytes(); value=json.loads(raw)
    if not isinstance(value,dict) or not isinstance(value.get("glyphs"),dict): raise ValueError("catalog must contain glyphs object")
    return value, hashlib.sha256(raw).hexdigest()
def _fingerprint(strokes):
    validated = _validate_strokes(strokes)
    if validated is not None:
        raise ValueError(validated)
    points_by_stroke=[[(float(x),float(y)) for x,y in stroke] for stroke in strokes]
    all_points=[point for stroke in points_by_stroke for point in stroke]
    scale=max(max(x for x,y in all_points)-min(x for x,y in all_points), max(y for x,y in all_points)-min(y for x,y in all_points), 1.0)
    quant=lambda value: round(value/scale/0.05)*0.05
    descriptors=[]
    for index,points in enumerate(points_by_stroke):
        distances=[math.hypot(b[0]-a[0],b[1]-a[1]) for point_index,a in enumerate(points) for b in points[point_index+1:]]
        cross=[math.hypot(b[0]-a[0],b[1]-a[1]) for other in points_by_stroke[index+1:] for a in points for b in other]
        turns=[abs(math.atan2((b[0]-a[0])*(c[1]-b[1])-(b[1]-a[1])*(c[0]-a[0]), (b[0]-a[0])*(c[0]-a[0])+(b[1]-a[1])*(c[1]-a[1]))) for a,b,c in zip(points,points[1:],points[2:])]
        descriptors.append((len(points), tuple(quant(distance) for distance in distances), tuple(quant(distance) for distance in cross), tuple(quant(turn) for turn in turns), quant(math.hypot(points[-1][0]-points[0][0],points[-1][1]-points[0][1]))))
    return hashlib.sha256(json.dumps(tuple(descriptors),separators=(",",":")).encode()).hexdigest()
def _provenance_map(records):
    result={}
    for record in records:
        label_map=result.setdefault(record["label"], {})
        lineage=record["lineage_group"]; source=record["independent_source"]
        prior=label_map.get(lineage)
        if prior is not None and prior != hashlib.sha256(source.encode()).hexdigest():
            raise ValueError(f"{PROVENANCE_CONFLICT}: {record['label']}:{lineage}")
        label_map[lineage]=hashlib.sha256(source.encode()).hexdigest()
    return {label:dict(sorted(values.items())) for label,values in result.items()}
def _templates(catalog):
    deficiencies=[]; result={}
    for label in LABELS:
        entry=catalog["glyphs"].get(label); templates=entry.get("templates") if isinstance(entry,dict) else None
        if not isinstance(templates,list): templates=[]
        ids=[t.get("id") if isinstance(t,dict) else None for t in templates]
        sources=[t.get("independent_source") if isinstance(t,dict) else None for t in templates]
        valid=[]; geometry_issues=[]
        for template in templates:
            if not isinstance(template,dict) or not isinstance(template.get("id"),str) or not template["id"].strip():
                continue
            error=_validate_strokes(template.get("strokes"))
            if error is not None:
                geometry_issues.append(error)
                continue
            valid.append(template)
        fps=[_fingerprint(t["strokes"]) for t in valid]; issues=[]
        if geometry_issues: issues.append("invalid geometry: "+", ".join(geometry_issues))
        if len(valid)<MIN_TEMPLATES: issues.append(f"requires at least {MIN_TEMPLATES} explicit independent templates (found {len(valid)})")
        if any(not isinstance(i,str) or not i.strip() for i in ids): issues.append("template IDs must be non-blank")
        if len(set(i for i in ids if isinstance(i,str)))!=len(ids): issues.append("template IDs must be unique")
        if any(not isinstance(s,str) or not s.strip() for s in sources): issues.append("independent_source must be non-blank")
        if len(set(s for s in sources if isinstance(s,str)))!=len(sources): issues.append("independent_source values must be unique")
        if len(set(fps))<MIN_TEMPLATES: issues.append(f"requires at least {MIN_TEMPLATES} distinct normalized geometry fingerprints (found {len(set(fps))})")
        result[label]=valid
        if issues: deficiencies.append(f"{label}: "+"; ".join(issues))
    if deficiencies: raise ValueError("invalid catalog prerequisites:\n"+"\n".join(deficiencies))
    return result

def _points(strokes,seed,variant):
    rng=random.Random(seed*1009+variant*9176); tx,ty=rng.uniform(-4,4),rng.uniform(-4,4); scale=1+rng.uniform(-.035,.035); angle=rng.uniform(-math.radians(8),math.radians(8)); ca,sa=math.cos(angle),math.sin(angle); result=[]
    for stroke in strokes:
        transformed=[]
        for x,y in stroke:
            xx,yy=(x-64)*scale,(y-64)*scale; transformed.append([round(min(127.5,max(.5,xx*ca-yy*sa+64+tx)),4),round(min(127.5,max(.5,xx*sa+yy*ca+64+ty)),4)])
        result.append(transformed)
    return result

def _record(example_id,label,strokes,lineage_group,seed_id,source,independent_source):
    return {"schema_version":SCHEMA_VERSION,"example_id":example_id,"label":label,"source":source,"independent_source":independent_source,"lineage_group":lineage_group,"seed_id":seed_id,"author_group":"synthetic-development","session_group":seed_id,"split_group":lineage_group,"consent":None,"strokes":[{"points":[{"x":x,"y":y} for x,y in stroke],"brush_width":6.0,"started_at_millis":0} for stroke in strokes],"generation":{"profile":PROFILE,"kind":"seed-variant" if source=="synthetic" else "balanced-reject"}}

def generate_corpus(catalog_path:Path,output_dir:Path,*,seed_variants=3,derivatives_per_label=100,reject_count=None):
    catalog,geometry_hash=_catalog(catalog_path); templates=_templates(catalog); output_dir.mkdir(parents=True,exist_ok=True); reject_count=reject_count if reject_count is not None else len(POSITIVE_LABELS)*derivatives_per_label; records=[]
    for label in LABELS:
        count=reject_count if label=="reject" else derivatives_per_label
        for index,template in enumerate(templates[label]):
            lineage=f"catalog:{label}:{template['id']}"; seed=f"geometry:{label}:{index}"; source="reject" if label=="reject" else "synthetic"; provenance=template["independent_source"]
            records.append(_record(f"{label}:seed:{index}",label,_points(template["strokes"],17+index,index),lineage,seed,source,provenance))
            for derivative in range(count): records.append(_record(f"{label}:derivative:{index}:{derivative}",label,_points(template["strokes"],101+index*max(1,count)+derivative,derivative+seed_variants),lineage,seed,source,provenance))
    provenance=_provenance_map(records)
    jsonl=output_dir/"corpus.jsonl"; jsonl.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in records)); corpus_hash=hashlib.sha256(jsonl.read_bytes()).hexdigest(); counts=Counter((r["source"],r["label"]) for r in records); labels={l:sum(c for (s,x),c in counts.items() if x==l) for l in LABELS}; groups={l:sorted({r["split_group"] for r in records if r["label"]==l}) for l in LABELS}; lineages={l:sorted({r["lineage_group"] for r in records if r["label"]==l}) for l in LABELS}
    manifest={"profile":PROFILE,"source":"synthetic","release_ready":False,"catalog_version":catalog.get("catalog_version"),"geometry_sha256":geometry_hash,"corpus_sha256":corpus_hash,"record_count":len(records),"seed_variants_per_label":seed_variants,"derivatives_per_seed":derivatives_per_label,"reject_count":reject_count,"counts":labels,"source_counts":{f"{s}:{l}":c for (s,l),c in sorted(counts.items())},"groups":groups,"lineages":lineages,"lineage_counts":{l:len(lineages[l]) for l in LABELS},"provenance":provenance}
    (output_dir/"manifest.json").write_text(json.dumps(manifest,indent=2,sort_keys=True)+"\n"); return manifest
def validate_development_corpus(path:Path,manifest_path:Path|None=None):
    try: examples=load_examples(path)
    except ValueError as exc: return [str(exc)]
    errors=[]
    if manifest_path is not None:
        try: manifest=json.loads(manifest_path.read_text())
        except (OSError,json.JSONDecodeError) as exc: return [f"invalid manifest: {exc}"]
        if manifest.get("profile")!=PROFILE or manifest.get("source")!="synthetic" or manifest.get("release_ready") is not False: errors.append("manifest is not development-only")
        if manifest.get("record_count")!=len(examples): errors.append("manifest record_count mismatch")
        if manifest.get("corpus_sha256")!=hashlib.sha256(path.read_bytes()).hexdigest(): errors.append("manifest corpus_sha256 mismatch")
        if manifest.get("counts")!=dict(Counter(e.label for e in examples)): errors.append("manifest counts mismatch")
        groups={l:sorted({e.split_group for e in examples if e.label==l}) for l in LABELS}; lineages={l:sorted({e.lineage_group for e in examples if e.label==l}) for l in LABELS}
        if manifest.get("groups")!=groups: errors.append("manifest groups mismatch")
        if manifest.get("lineages")!=lineages: errors.append("manifest lineages mismatch")
        if manifest.get("lineage_counts")!={l:len(lineages[l]) for l in LABELS}: errors.append("manifest lineage_counts mismatch")
        try:
            expected_provenance=_provenance_map([{"label":e.label,"lineage_group":e.lineage_group,"independent_source":e.independent_source} for e in examples])
        except ValueError as exc:
            errors.append(str(exc)); expected_provenance={}
        if manifest.get("provenance")!=expected_provenance: errors.append("manifest provenance mismatch")
    for e in examples:
        if not isinstance(e.independent_source,str) or not e.independent_source.strip(): errors.append(f"{e.example_id}: missing independent_source")
        if e.source not in {"synthetic","reject"}: errors.append(f"{e.example_id}: non-development source")
        if e.generation is None or e.generation.get("profile")!=PROFILE: errors.append(f"{e.example_id}: missing development profile")
    return errors

def main(argv=None):
    parser=argparse.ArgumentParser(); parser.add_argument("catalog",type=Path); parser.add_argument("output",type=Path); args=parser.parse_args(argv); manifest=generate_corpus(args.catalog,args.output); errors=validate_development_corpus(args.output/"corpus.jsonl",args.output/"manifest.json"); print(json.dumps({"manifest":manifest,"valid":not errors,"errors":errors},sort_keys=True)); return 0 if not errors else 2
if __name__=="__main__": raise SystemExit(main())
