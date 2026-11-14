"""Loopback-only presentation authentication; callers own disposable credentials."""
import http.cookiejar
import json
import urllib.parse
import urllib.request
BASE = 'http://127.0.0.1:8080'

def sign_in(login, password):
    jar = http.cookiejar.CookieJar()
    client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    token = json.load(client.open(BASE + '/api/v1/auth/csrf', timeout=5))
    request = urllib.request.Request(BASE + '/api/v1/auth/login',
        data=urllib.parse.urlencode(dict(username=login, password=password)).encode(),
        headers={token['headerName']: token['token']})
    with client.open(request, timeout=5) as response:
        if response.status != 204:
            raise RuntimeError('Presentation sign-in failed')
    token = json.load(client.open(BASE + '/api/v1/auth/csrf', timeout=5))
    return client, token, '; '.join(cookie.name + '=' + cookie.value for cookie in jar)
