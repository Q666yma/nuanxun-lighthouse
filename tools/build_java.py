"""Compile Java sources with bundled libraries or dependencies fetched by Maven."""
from pathlib import Path
import os
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT/'java-backend'
sources = sorted((PROJECT/'src/main/java').rglob('*.java'))
build = PROJECT/'build'
classes = build/'classes'
classes.mkdir(parents=True, exist_ok=True)
java_home = os.environ.get('JAVA_HOME')
if not java_home and (ROOT/'.env').exists():
    for raw in (ROOT/'.env').read_text(encoding='utf-8-sig').splitlines():
        if raw.strip().startswith('JAVA_HOME='):
            java_home = raw.strip().split('=', 1)[1].strip().strip('"\'') or None
            break
compiler = Path(java_home)/'bin'/('javac.exe' if os.name == 'nt' else 'javac') if java_home else shutil.which('javac')
if not compiler:
    raise SystemExit('JDK 11 is required; set JAVA_HOME or add javac to PATH.')
arguments = build/'sources.txt'
arguments.write_text('\n'.join('"'+path.as_posix()+'"' for path in sources), encoding='utf8')
library_dir = PROJECT/'lib'
lombok_candidates = [ROOT/'tools/lombok-1.18.36.jar', library_dir/'lombok-1.18.36.jar']
lombok = next((path for path in lombok_candidates if path.is_file()), None)
if not any(library_dir.glob('*.jar')) or lombok is None:
    maven = None
    for variable in ('MAVEN_HOME', 'M2_HOME'):
        home = os.environ.get(variable)
        if home:
            candidate = Path(home)/'bin'/('mvn.cmd' if os.name == 'nt' else 'mvn')
            if candidate.is_file():
                maven = str(candidate)
                break
    maven = maven or shutil.which('mvn')
    if not maven:
        raise SystemExit('Java dependencies are missing. Install Apache Maven and set MAVEN_HOME '
                         'or M2_HOME, or add mvn to PATH, then run this command again.')
    environment = dict(os.environ)
    if java_home:
        environment['JAVA_HOME'] = java_home
    print('Downloading Java dependencies with Maven; the first download may take several minutes.', flush=True)
    try:
        subprocess.run([maven, '-B', 'dependency:copy-dependencies', '-DoutputDirectory=lib'],
                       cwd=PROJECT, env=environment, check=True)
    except (OSError, subprocess.CalledProcessError) as error:
        raise SystemExit('Maven could not prepare Java dependencies. Check the Maven/JDK installation '
                         'and network connection, then run this command again.') from error
    lombok = next((path for path in lombok_candidates if path.is_file()), None)
    if not any(library_dir.glob('*.jar')) or lombok is None:
        raise SystemExit('Maven did not produce the expected Java dependencies and Lombok 1.18.36. '
                         'Check java-backend/pom.xml and the Maven output above.')
classpath = os.pathsep.join([str(library_dir/'*'), str(lombok)])
subprocess.run([str(compiler), '-J-Dfile.encoding=UTF-8', '--release', '11', '-parameters',
                '-encoding', 'UTF-8', '-cp', classpath, '-processorpath', str(lombok),
                '-d', str(classes), '@'+str(arguments)], check=True, cwd=PROJECT)
print('Compiled', len(sources), 'Java source files.')
