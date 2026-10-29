#!/usr/bin/env python3
"""Refuse to publish a version that existing installations would ignore."""
import argparse,json,urllib.request,urllib.error
p=argparse.ArgumentParser();p.add_argument('--manifest',required=True);p.add_argument('--repository',required=True);a=p.parse_args()
new=json.load(open(a.manifest))
url=f'https://github.com/{a.repository}/releases/latest/download/update.json'
try:
    with urllib.request.urlopen(url,timeout=30) as response:
        old=json.loads(response.read(32769))
    if int(new['versionCode'])<=int(old['versionCode']):
        raise SystemExit('Increase versionCode before publishing another release.')
except urllib.error.HTTPError as e:
    if e.code!=404:raise
