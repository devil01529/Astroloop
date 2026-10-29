#!/usr/bin/env python3
"""Build the public updater manifest from a signed APK and Gradle's output metadata."""
import argparse, hashlib, json, re
from pathlib import Path

def make_manifest(apk, metadata, repository, tag):
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repository):
        raise ValueError('Invalid repository')
    info=json.loads(Path(metadata).read_text())
    if info['applicationId']!='com.astroloop.game.s24' or len(info['elements'])!=1:
        raise ValueError('Expected a single S24 APK')
    version=info['elements'][0]
    if tag != 'v'+version['versionName'] or not re.fullmatch(r'v[A-Za-z0-9_.-]+',tag):
        raise ValueError('Release tag must match the APK versionName')
    path=Path(apk)
    if path.stat().st_size>150*1024*1024:raise ValueError('APK exceeds updater limit')
    return {'schemaVersion':1,'versionCode':version['versionCode'],'versionName':version['versionName'],
        'apkUrl':f'https://github.com/{repository}/releases/download/{tag}/astro-loop-s24.apk',
        'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'apkBytes':path.stat().st_size}

if __name__=='__main__':
    p=argparse.ArgumentParser()
    for name in ['apk','metadata','repository','tag','output']:p.add_argument('--'+name,required=True)
    args=p.parse_args()
    Path(args.output).write_text(json.dumps(make_manifest(args.apk,args.metadata,args.repository,args.tag),indent=2)+'\n')
