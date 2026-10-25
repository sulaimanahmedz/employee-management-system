"""Start an isolated presentation, seed with Java Faker, then expose the UI locally."""
import os
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
for port in (8080, 5173):
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', port))  # Refuse to reuse an existing application.
try:
    processes.append(subprocess.Popen(
        ['java', '-jar', 'target/employee-management-api-0.1.0-SNAPSHOT.jar',
         '--server.address=127.0.0.1', '--spring.jpa.hibernate.ddl-auto=create-drop'],
        cwd=root / 'backend', start_new_session=True,
        env=dict(os.environ, DB_URL='jdbc:h2:mem:presentation;DB_CLOSE_DELAY=-1', PORT='8080', DB_USERNAME='sa', DB_PASSWORD='')))
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
    subprocess.run(['java', '--class-path', classpath, str(here / 'PresentationSeed.java')],
                   env=dict(os.environ, PRESENTATION_SEED='true'), check=True)
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
