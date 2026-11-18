"""Presentation startup must never attach to an existing application listener."""
import os
from pathlib import Path
import secrets
import socket
import subprocess

root = Path(__file__).resolve().parents[4]
for port in (8080, 5173):
    with socket.socket() as listener:
        listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        listener.bind(('127.0.0.1', port))
        listener.listen()
        result = subprocess.run(['python3', str(root / 'tools/presentation/start.py')],
            env=dict(os.environ, PRESENTATION_LOGIN='synthetic-guard',
                     PRESENTATION_PASSWORD=secrets.token_urlsafe(32)),
            capture_output=True, text=True, timeout=10)
        assert result.returncode != 0
        assert 'Address already in use' in result.stderr
        print(f'PASS refuses active listener on {port}')
