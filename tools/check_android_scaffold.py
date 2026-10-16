"""Static resource/config checks; deliberately NOT a substitute for Gradle tests."""
import hashlib
from pathlib import Path
import tomllib
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
tomllib.loads((root / 'gradle/libs.versions.toml').read_text())
for path in root.glob('*/src/**/*.xml'):
    ET.parse(path)
strings = []
for locale in ('values', 'values-hi'):
    tree = ET.parse(root / f'ui/src/main/res/{locale}/strings.xml')
    names = [element.attrib['name'] for element in tree.getroot()]
    assert len(names) == len(set(names)), f'Duplicate resources: {locale}'
    strings.append(set(names))
assert strings[0] == strings[1], 'English/Hindi resource keys differ'
assert hashlib.sha256((root / 'gradle/wrapper/gradle-wrapper.jar').read_bytes()).hexdigest() == '81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f', 'Unexpected Gradle wrapper JAR'
for module in ('core', 'ui', 'app', 'sample'):
    assert (root / module / 'build.gradle.kts').exists()
assert 'compose' not in (root / 'core/build.gradle.kts').read_text().lower(), 'Core must not depend on Compose'
print('XML, locales, version catalog and wrapper checks OK; Android compilation not checked.')
