import json
from pathlib import Path
p=Path('/home/jlo/dev/wizardry/.worktrees/sigil-reference/training/catalog-geometry-v1.json'); d=json.loads(p.read_text())
params=[(60,62,13,34),(62,63,14,31),(64,64,12,28),(64,65,15,32),(64,66,16,30),(66,67,14,29)]
for i,(cx,cy,r,e) in enumerate(params):
 d['glyphs']['target']['templates'][i]['strokes']=[[[cx,cy-r],[cx+r,cy],[cx,cy+r],[cx-r,cy],[cx,cy-r]],[[cx,cy-r-5],[cx,cy-e]],[[cx,cy+r+5],[cx,cy+e]],[[cx-r-5,cy],[cx-e,cy],[cx-e,cy+0.01],[cx+r+5,cy+0.01],[cx+e,cy+0.01]]]
 # use four strokes: diamond + 3 compounds; compound segments remain separated by tiny orthogonal offset and no center bar
 ar=[[[cx-r-8,cy-r-5],[cx-r-20,cy-r-18],[cx-r-32,cy-r-5]],[[cx+r+8,cy-r-5],[cx+r+20,cy-r-18],[cx+r+32,cy-r-5]],[[cx-r-8,cy+r+5],[cx-r-20,cy+r+18],[cx-r-32,cy+r+5]],[[cx+r+8,cy+r+5],[cx+r+20,cy+r+18],[cx+r+32,cy+r+5]]]
 d['glyphs']['area']['templates'][i]['strokes']=[[[cx,cy-r],[cx+r,cy],[cx,cy+r],[cx-r,cy],[cx,cy-r]], ar[0]+ar[3], ar[1], ar[2]]
p.write_text(json.dumps(d,indent=2)+'\n')
