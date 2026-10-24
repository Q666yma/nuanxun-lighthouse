"""Start either local service using an optional project-root .env file."""
from pathlib import Path
import os
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
environment = dict(os.environ)
env_file = ROOT/'.env'
if env_file.exists():
    for raw in env_file.read_text(encoding='utf-8-sig').splitlines():
        line = raw.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        environment.setdefault(key.strip(), value)

service = sys.argv[1] if len(sys.argv) == 2 else ''
if service not in ('java', 'python', 'init'):
    raise SystemExit('Usage: python tools/run.py java|python|init')
if service == 'python':
    subprocess.run([sys.executable, 'app.py'], cwd=ROOT/'python-service', env=environment, check=True)
else:
    project = ROOT/'java-backend'
    if not (project/'build/classes/com/maka/BakaApplication.class').exists():
        subprocess.run([sys.executable, str(ROOT/'tools/build_java.py')], env=environment, check=True)
    home = environment.get('JAVA_HOME')
    java = Path(home)/'bin'/('java.exe' if os.name == 'nt' else 'java') if home else shutil.which('java')
    if not java:
        raise SystemExit('Install JDK 11 and set JAVA_HOME.')
    if service == 'init':
        subprocess.run([str(java), '-Dfile.encoding=UTF-8', '-cp', str(project/'lib/*'),
                        str(ROOT/'tools/InitDemoAccounts.java')], cwd=project, env=environment, check=True)
        raise SystemExit(0)
    for directory in ('runtime/uploads', 'runtime/reports'):
        (project/directory).mkdir(parents=True, exist_ok=True)
    classpath = os.pathsep.join([str(project/'build/classes'), str(project/'src/main/resources'), str(project/'lib/*')])
    subprocess.run([str(java), '-Dfile.encoding=UTF-8', '-Duser.timezone=Asia/Shanghai',
                    '-cp', classpath, 'com.maka.BakaApplication'], cwd=project, env=environment, check=True)
