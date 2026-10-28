"""Archive construction test renders for GitHub; originals remain byte-for-byte intact.

python tools/build_asset_gallery.py             # synchronize local gallery
python tools/build_asset_gallery.py --check     # check coverage and hashes
python tools/build_asset_gallery.py --publish   # synchronize, commit gallery only, push
"""
import argparse
import hashlib
import json
import shutil
import struct
import subprocess
import os
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'docs/asset-gallery'
# Explicit authored-construction allowlist. Purchased source/reference media and
# unrelated weapon source studies are deliberately outside the public archive.
GROUPS = ('construction-plots', 'faction-structures', 'snap-kit',
          'penitent-snap-kit', 'begotten-snap-kit', 'faction-foundations', 'production-kit', 'material-refresh', 'weapon-refinement', 'begotten-surface-pilots')
LABELS = {'begotten-surface-pilots': 'Begotten model-specific surface studies', 'construction-plots': 'Construction plot guides',
          'faction-structures': 'Faction structures and earlier studies',
          'snap-kit': 'Unraveled snap construction',
          'penitent-snap-kit': 'Penitent snap construction',
          'begotten-snap-kit': 'Begotten snap construction',
          'faction-foundations': 'Faction foundations and stairs',
          'production-kit': 'Production machinery and surface placeables',
          'material-refresh': 'Construction material revision 2',
          'weapon-refinement': 'Weapon refinement and retained failed studies'}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def write_manifest(path,value):
    temporary=path.with_suffix('.json.tmp')
    temporary.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
    os.replace(temporary,path)


def sources():
    for group in GROUPS:
        for path in sorted((ROOT / 'previews' / group).rglob('*.png')):
            rel = path.relative_to(ROOT / 'previews')
            if any('reference' in p.lower() for p in rel.parts):
                continue
            if 'comparison' in path.name.lower():
                continue
            yield group, path, rel


def evidence_index():
    gates = {}
    for path in sorted((ROOT / 'reports').glob('*.json')):
        try:
            report = json.loads(path.read_text(encoding='utf-8'))
        except (ValueError, UnicodeError):
            continue
        if not isinstance(report, dict):
            continue
        for item in report.get('results', []):
            if isinstance(item, dict) and item.get('sha256') and item.get('status') in ('REJECTED', 'ART_REVIEW_REQUIRED', 'DELIVERY_ELIGIBLE'):
                prior = gates.get(item['sha256'])
                stamp = path.stat().st_mtime_ns
                if prior is None or stamp > prior[0]:
                    gates[item['sha256']] = (stamp, item['status'])
    evidence = {}
    for path in sorted((ROOT / 'build/candidates').rglob('render-evidence.json')):
        data = json.loads(path.read_text(encoding='utf-8'))
        model_sha = data.get('asset_sha256')
        review_path = ROOT / 'reports/structure-art-reviews' / (path.parent.name + '.json')
        review = json.loads(review_path.read_text(encoding='utf-8')) if review_path.exists() else {}
        for view, render in data.get('renders', {}).items():
            if view == 'comparison':
                continue
            render_path = Path(render['path'])
            if not render_path.is_absolute():
                render_path = ROOT / render_path
            try:
                key = render_path.resolve().relative_to(ROOT).as_posix()
            except ValueError:
                continue
            reviewed = review.get('asset_sha256') == model_sha and review.get('renders', {}).get(view, {}).get('sha256') == render.get('sha256')
            gate = gates.get(model_sha)
            states = {gate[1]} if gate else set()
            status = 'UNREVIEWED'
            if (reviewed and review.get('decision') in ('reject', 'rejected')) or 'REJECTED' in states:
                status = 'REJECTED'
            elif reviewed and review.get('decision') == 'pass' and 'DELIVERY_ELIGIBLE' in states:
                status = 'ROUGH_STAGE_PASS'
            evidence[key] = dict(asset_id=path.parent.name, asset_sha256=model_sha,
                                 render_sha256=render.get('sha256'), view=view,
                                 status=status)
    return evidence


def collect():
    evidence = evidence_index()
    records = []
    for group, path, rel in sources():
        digest = sha(path)
        record = dict(source=path.relative_to(ROOT).as_posix(),
                      file='images/' + rel.as_posix(), group=group,
                      faction=next((f for f in ('unraveled', 'penitent', 'begotten') if f in rel.as_posix()), 'unraveled' if group == 'snap-kit' else 'shared'),
                      sha256=digest, bytes=path.stat().st_size,
                      width=0, height=0, status='OVERVIEW_OR_HISTORICAL_STUDY',
                      view=path.stem, asset_id=path.parent.name)
        with path.open('rb') as f:
            header = f.read(24)
        assert header[:8] == b'\x89PNG\r\n\x1a\n', path
        record['width'], record['height'] = struct.unpack('>II', header[16:24])
        info = evidence.get(record['source'])
        if info and info['render_sha256'] == digest:
            record.update(info)
        records.append(record)
    return records


def build():
    records = collect()
    previous = DEST / 'manifest.json'
    old = json.loads(previous.read_text(encoding='utf-8')) if previous.exists() else {}
    known={r['source']:r for r in old.get('images',[])}
    recovered=[]
    for r in records:
        target = DEST / r['file']
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists() and r['source'] not in known and sha(target)!=r['sha256']:
            # Recover PNGs copied by an interrupted synchronization before its
            # manifest could be committed; never erase those test pixels.
            digest=sha(target);archive=DEST/'images/history'/(digest+'.png');archive.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(target,archive)
            prior=dict(r,file=archive.relative_to(DEST).as_posix(),sha256=digest,bytes=target.stat().st_size,status='SUPERSEDED_UNREVIEWED')
            prior.pop('asset_sha256',None);prior.pop('render_sha256',None)
            recovered.append(prior)
        if not target.exists() or sha(target) != r['sha256']:
            shutil.copyfile(ROOT / r['source'], target)
    # Images already recorded are retained as history when a source is renamed
    # or re-rendered. Changed renders are preserved by their content hash.
    history = recovered
    if previous.exists():
        old = json.loads(previous.read_text(encoding='utf-8'))
        current = {r['source']: r for r in records}
        history += old.get('history', [])
        for r in old.get('images', []):
            new = current.get(r['source'])
            if not new or new['sha256'] != r['sha256']:
                # build() below archives before replacing; handled in sync().
                history.append(r)
    unique = {r['file']: r for r in history}
    manifest = dict(schema_version=1, scope='Authored construction test renders',
                    image_count=len(records), total_bytes=sum(r['bytes'] for r in records),
                    status_counts=dict(Counter(r['status'] for r in records)),
                    images=records, history=list(unique.values()),
                    review_note='ROUGH_STAGE_PASS is a hash-matched automated gate and agent art review; it does not mean final user approval. Overviews and earlier studies are not promoted by association.')
    write_manifest(previous,manifest)
    write_pages(manifest)
    return manifest


def sync():
    assert any((ROOT / 'previews' / group).exists() for group in GROUPS), 'No authoring preview folders found. Use --verify-archive in a GitHub clone; generate renders before synchronizing.'
    # Preserve superseded pixels before build copies updated images into place.
    previous = DEST / 'manifest.json'
    if previous.exists():
        old = json.loads(previous.read_text(encoding='utf-8'))
        current = {r['source']: r for r in collect()}
        for r in old.get('images', []):
            new = current.get(r['source'])
            if not new or new['sha256'] != r['sha256']:
                origin = DEST / r['file']
                target = DEST / 'images/history' / (r['sha256'] + '.png')
                target.parent.mkdir(parents=True, exist_ok=True)
                if origin.exists():
                    shutil.copyfile(origin, target)
                    r['file'] = target.relative_to(DEST).as_posix()
                    r['status'] = 'SUPERSEDED_' + r['status']
        write_manifest(previous,old)
    return build()


def write_pages(manifest):
    records = manifest['images']
    lines = ['# Construction test image gallery', '',
             f"{len(records)} original PNG renders and contact sheets. All four test views are included where generated: front, rear, closeup and neutral clay.", '',
             'Private car/character reference renders and comparison panels containing them are excluded. Earlier studies remain visible; gallery inclusion does not imply approval.', '',
             'Review labels: **ROUGH_STAGE_PASS** = current image and model hashes match a passing rough-stage review/gate; **REJECTED** = retained failed study; **UNREVIEWED** = rendered but no matching pass; **OVERVIEW_OR_HISTORICAL_STUDY** = contact sheet, guide or older study. These assets still require final art and game integration review.', '',
             '[Machine-readable manifest](manifest.json) · [Filterable local gallery](index.html)', '',
             'The HTML gallery runs locally after cloning: `python -m http.server 8000`, then open `http://localhost:8000/docs/asset-gallery/`.', '',
             'Update and publish from the authoring workspace after each render batch: `python tools/build_asset_gallery.py --publish`. This preserves superseded renders, commits only gallery files and its verification workflow, and pushes the current branch. `--check` verifies source coverage and checksums; `--verify-archive` verifies a GitHub clone without ignored authoring sources. GitHub Actions runs the archive check on gallery changes.', '']
    for group in GROUPS:
        items = [r for r in records if r['group'] == group]
        if not items:
            continue
        lines += [f'## {LABELS[group]}', '', f'{len(items)} images · [Full image index]({group}.md)', '']
        preferred = next((r for r in items if Path(r['file']).name in ('preview.png', 'snap-kit-preview.png', 'faction-structures-preview.png', 'construction-plots-preview.png')), items[0])
        lines += [f"[![{LABELS[group]}]({preferred['file']})]({group}.md)", '']
        page = [f'# {LABELS[group]}', '', '[Back to gallery](README.md)', '',
                '| Image | Asset / view | Review status | Pixels |', '|---|---|---|---|']
        for r in items:
            page.append(f"| [<img src=\"{r['file']}\" width=\"180\" alt=\"{r['asset_id']} {r['view']}\">]({r['file']}) | {r['asset_id']} / {r['view']} | {r['status']} | {r['width']}×{r['height']} |")
        (DEST / (group + '.md')).write_text('\n'.join(page) + '\n', encoding='utf-8')
    if manifest['history']:
        lines += ['## Superseded renders', '', f"{len(manifest['history'])} earlier test images retained. [Browse the history gallery](history.md).", '']
        page = ['# Superseded test renders', '', '[Back to current gallery](README.md)', '', '| Image | Asset / view | Earlier review status |', '|---|---|---|']
        for r in manifest['history']:
            page.append(f"| [<img src=\"{r['file']}\" width=\"180\" alt=\"{r['asset_id']} {r['view']}\">]({r['file']}) | {r['asset_id']} / {r['view']} | {r['status']} |")
        (DEST / 'history.md').write_text('\n'.join(page) + '\n', encoding='utf-8')
    (DEST / 'README.md').write_text('\n'.join(lines), encoding='utf-8')
    (DEST / 'index.html').write_text(HTML, encoding='utf-8')


def check(archive_only=False):
    manifest = json.loads((DEST / 'manifest.json').read_text(encoding='utf-8'))
    actual = {r['source']: r['sha256'] for r in manifest['images']}
    if not archive_only:
        expected = {r['source']: r['sha256'] for r in collect()}
        assert actual == expected, 'Gallery coverage is stale; regenerate it.'
    for r in manifest['images'] + manifest.get('history', []):
        path = DEST / r['file']
        assert path.is_file() and sha(path) == r['sha256'], path
        assert 'reference' not in r['file'].lower() and 'comparison' not in r['file'].lower(), path
    print(json.dumps(dict(status='PASS', images=len(actual), history=len(manifest.get('history', [])), bytes=manifest['total_bytes'], statuses=manifest['status_counts'])))


HTML = '''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Malefic Vale construction gallery</title>
<style>body{background:#151a1b;color:#e0ddd1;font:16px system-ui;margin:24px}a{color:#9ed2bf}header{max-width:1000px}input,select{font:inherit;padding:10px;margin:4px;background:#27302f;color:inherit;border:1px solid #60726b}#grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(290px,1fr));gap:16px;margin-top:20px}article{background:#222b29;border:1px solid #41524a;padding:10px}img{width:100%;height:250px;object-fit:contain}h2{font-size:16px;overflow-wrap:anywhere}small{color:#abb9b1}.badge{font-size:12px;color:#d0c9a4}#error{color:#f2bb9d}</style>
<header><h1>Construction test gallery</h1><p>Unraveled · Penitent · Begotten. Original test renders, clay views and earlier studies. A rough-stage pass does not imply final user approval.</p><p><a href="README.md">GitHub image indexes</a> · <a href="manifest.json">Hashes and review manifest</a></p></header>
<input id="query" type="search" placeholder="Find an asset…" aria-label="Find an asset"><select id="group" aria-label="Collection"><option value="">All collections</option></select><select id="status" aria-label="Review status"><option value="">All statuses</option></select><select id="view" aria-label="Test view"><option value="">All views</option></select><label><input id="history" type="checkbox">Include superseded tests</label><p id="count"></p><p id="error"></p><main id="grid"></main>
<script>
const $=id=>document.getElementById(id);
fetch('manifest.json').then(r=>{if(!r.ok)throw Error(r.status);return r.json()}).then(data=>{
 for(const key of ['group','status','view'])for(const value of [...new Set(data.images.concat(data.history||[]).map(x=>x[key]))].sort()){const o=document.createElement('option');o.value=value;o.textContent=value;$(key).append(o)}
 function draw(){const needle=$('query').value.toLowerCase();const available=data.images.concat($('history').checked?(data.history||[]):[]);const rows=available.filter(x=>(x.asset_id+' '+x.source+' '+x.faction).toLowerCase().includes(needle)&&['group','status','view'].every(k=>!$(k).value||$(k).value===x[k]));$('count').textContent=rows.length+' / '+available.length+' images';$('grid').replaceChildren();for(const x of rows){const card=document.createElement('article'),a=document.createElement('a'),im=document.createElement('img'),title=document.createElement('h2'),state=document.createElement('p'),info=document.createElement('small');a.href=x.file;im.src=x.file;im.loading='lazy';im.alt=x.asset_id+' / '+x.view;a.append(im);title.textContent=im.alt;state.textContent=x.status;state.className='badge';info.textContent=x.width+' × '+x.height+' · '+x.faction+' · SHA-256 '+x.sha256.slice(0,12);card.append(a,title,state,info);$('grid').append(card)}}
 for(const id of ['query','group','status','view','history'])$(id).addEventListener('input',draw);draw();
}).catch(e=>{$('error').textContent='Open this gallery through a local HTTP server (see README), or use the GitHub Markdown indexes. '+e.message});
</script></html>'''


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--publish', action='store_true')
    parser.add_argument('--verify-archive', action='store_true', help='Verify committed PNGs without local ignored authoring sources')
    args = parser.parse_args()
    if not args.check and not args.verify_archive:
        sync()
    check(archive_only=args.verify_archive)
    if args.publish:
        def git(*parts, capture=False):
            return subprocess.run(['git', *parts], cwd=ROOT, check=True, text=True,
                                  stdout=subprocess.PIPE if capture else None).stdout
        paths = ['docs/asset-gallery', 'tools/build_asset_gallery.py', '.github/workflows/asset-gallery.yml']
        branch = git('branch', '--show-current', capture=True).strip()
        assert branch, 'Detached HEAD: select a branch before publication.'
        git('add', '--', *paths)
        changed = subprocess.run(['git', 'diff', '--cached', '--quiet', '--', *paths], cwd=ROOT).returncode
        if changed:
            git('commit', '--quiet', '--only', '-m', 'Record construction test renders in a verified image gallery', '--', *paths)
        git('push', 'origin', 'HEAD:refs/heads/' + branch)
