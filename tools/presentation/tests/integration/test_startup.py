import subprocess,time,urllib.request,json,signal,os,socket
from pathlib import Path
root=Path(__file__).resolve().parents[4]
script=root/'tools/presentation'
p=subprocess.Popen(['python3',str(script/'start.py')],stdout=subprocess.DEVNULL,stderr=subprocess.STDOUT)
try:
 for _ in range(100):
  if p.poll() is not None:raise RuntimeError('Launcher exited')
  try:
   urllib.request.urlopen('http://127.0.0.1:5173',timeout=2)
   data=json.load(urllib.request.urlopen('http://127.0.0.1:8080/api/v1/employees'))
   assert len(data)==5
   assert all(x['email'].endswith('@example.com') for x in data)
   print('PASS startup: five synthetic employees before UI ready',flush=True)
   break
  except OSError:time.sleep(.5)
 else:raise RuntimeError('Startup timed out')
 command=['java','--class-path',(script/'target/classpath.txt').read_text().strip(),str(script/'PresentationSeed.java')]
 for enabled,expected in [('true','Requires an empty isolated presentation database'),('false','Presentation opt-in required')]:
  r=subprocess.run(command,env=dict(os.environ,PRESENTATION_SEED=enabled),capture_output=True,text=True)
  assert r.returncode!=0 and expected in r.stderr
  print('PASS guard: '+expected,flush=True)
finally:
 p.send_signal(signal.SIGINT);p.wait(timeout=30)
for port in (8080,5173):
 with socket.socket() as s:assert s.connect_ex(('127.0.0.1',port))!=0
print('PASS temporary server cleanup',flush=True)
