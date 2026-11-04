#!/usr/bin/env python3
"""Owner-only offline issuer. NEVER commit or distribute the private signing key."""
import argparse, base64, re
from pathlib import Path
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
p = argparse.ArgumentParser()
p.add_argument('--installation', required=True)
p.add_argument('--private-key', required=True)
a = p.parse_args()
if not re.fullmatch('[0-9a-f]{64}', a.installation):
    p.error('Expected the 64-character installation code shown by the APK')
key = serialization.load_pem_private_key(Path(a.private_key).read_bytes(), password=None)
payload = ('astro-loop-s24:owner:v1:' + a.installation).encode()
print(base64.b64encode(key.sign(payload, ec.ECDSA(hashes.SHA256()))).decode())
