"""Start an isolated presentation, seed with Java Faker, then expose the UI locally."""
import os
import json
import secrets
from auth import sign_in, BASE
from pathlib import Path
import signal
import socket
import subprocess
import time
import urllib.request

root = Path(__file__).resolve().parents[2]
here = Path(__file__).resolve().parent
classpath = (here / 'target/classpath.txt').read_text().strip()
processes = []
login = os.environ.get('PRESENTATION_LOGIN')
password = os.environ.get('PRESENTATION_PASSWORD')
if not login or not password:
    raise RuntimeError('Explicit PRESENTATION_LOGIN and PRESENTATION_PASSWORD are required')
admin_login = 'bootstrap-' + secrets.token_hex(8)
admin_password = secrets.token_urlsafe(32)
for port in (8080, 5173):
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', port))  # Refuse to reuse an existing application.
try:
    processes.append(subprocess.Popen(
        ['java', '-jar', 'target/employee-management-api-0.1.0-SNAPSHOT.jar',
         '--server.address=127.0.0.1', '--spring.jpa.hibernate.ddl-auto=create-drop'],
        cwd=root / 'backend', start_new_session=True,
        env=dict(os.environ, DB_URL='jdbc:h2:mem:presentation;DB_CLOSE_DELAY=-1', PORT='8080', DB_USERNAME='sa', DB_PASSWORD='', SESSION_COOKIE_SECURE='false', EMS_BOOTSTRAP_LOGIN=admin_login, EMS_BOOTSTRAP_PASSWORD=admin_password)))
    for attempt in range(120):
        if processes[0].poll() is not None:
            raise RuntimeError('Backend exited before presentation startup')
        try:
            with urllib.request.urlopen('http://127.0.0.1:8080/actuator/health', timeout=2) as response:
                if response.status == 200:
                    break
        except OSError:
            time.sleep(.5)
    else:
        raise RuntimeError('Backend did not become healthy')
    admin, token, _ = sign_in(admin_login, admin_password)
    request = urllib.request.Request(BASE + '/api/v1/admin/accounts',
        data=json.dumps(dict(login=login, password=password, role='HR')).encode(),
        headers={'Content-Type': 'application/json', token['headerName']: token['token']})
    with admin.open(request, timeout=5) as response:
        if response.status != 201:
            raise RuntimeError('Presentation account provisioning failed')
    with admin.open(urllib.request.Request(BASE + '/api/v1/auth/logout', data=b'',
            headers={token['headerName']: token['token']}), timeout=5):
        pass
    _, token, cookie = sign_in(login, password)
    subprocess.run(['java' , '--class-path', classpath, str(here / 'PresentationSeed.java')],
                   env=dict(os.environ, PRESENTATION_SEED='true', PRESENTATION_SESSION=cookie, PRESENTATION_CSRF=token['token']), check=True)
    processes.append(subprocess.Popen(['npm', 'run', 'dev', '--', '--host', '127.0.0.1'],
                                     cwd=root / 'frontend', start_new_session=True))
    print('Synthetic presentation: http://127.0.0.1:5173 — local only. Ctrl-C stops both servers.', flush=True)
    while all(p.poll() is None for p in processes):
        time.sleep(1)
except KeyboardInterrupt:
    pass
finally:
    for process in processes:
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
    for process in processes:
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
