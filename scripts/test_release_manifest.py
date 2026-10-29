import unittest,tempfile,json,hashlib
from pathlib import Path
from release_manifest import make_manifest

class ManifestTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        root=Path(self.temp.name);self.apk=root/'game.apk';self.apk.write_bytes(b'test apk bytes')
        self.metadata=root/'output-metadata.json'
        self.metadata.write_text(json.dumps({'applicationId':'com.astroloop.game.s24','elements':[{'versionCode':9,'versionName':'1.5-s24.4'}]}))
    def test_manifest_matches_artifact(self):
        m=make_manifest(self.apk,self.metadata,'devil01529/Astroloop','v1.5-s24.4')
        self.assertEqual(m['versionCode'],9)
        self.assertEqual(m['apkBytes'],self.apk.stat().st_size)
        self.assertEqual(m['sha256'],hashlib.sha256(self.apk.read_bytes()).hexdigest())
    def test_wrong_tag_is_rejected(self):
        with self.assertRaises(ValueError):make_manifest(self.apk,self.metadata,'devil01529/Astroloop','v1.5-s24.3')
    def test_wrong_repository_is_rejected(self):
        with self.assertRaises(ValueError):make_manifest(self.apk,self.metadata,'https://evil.example','v1.5-s24.4')

if __name__=='__main__':unittest.main()
